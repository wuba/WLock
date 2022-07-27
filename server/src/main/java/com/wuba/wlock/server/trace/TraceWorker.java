/*
 * Copyright (C) 2005-present, 58.com.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuba.wlock.server.trace;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TraceWorker {
	private static final Logger logger = LoggerFactory
			.getLogger(TraceWorker.class);

	public static boolean traceEnable = false;
	private final static int WORKER_TOTAL = 2;
	private RecordWorker[] recordWorkers = new RecordWorker[WORKER_TOTAL];

	private static TraceWorker instance = new TraceWorker();
	private AtomicInteger countOffer = new AtomicInteger(0);

	private TraceWorker() {
		for (int i = 0; i < WORKER_TOTAL; i++) {
			recordWorkers[i] = new RecordWorker(
					"lockTraceLog", "traceLog_work" + i);
		}
	}

	public static TraceWorker getInstance() {
		return instance;
	}

	public void offer(LockTrace lockTrace) {
		if(traceEnable){
			recordWorkers[Math.abs(countOffer.incrementAndGet()) % WORKER_TOTAL].offer(lockTrace);
		}
	}

	public int getQsize() {
		int size = 0;
		for (int i = 0; i < WORKER_TOTAL; i++) {
			size += recordWorkers[i].getQsize();
		}
		return size;
	}

	public static class RecordWorker {
		@SuppressWarnings("unused")
		private final String logName;
		private org.apache.logging.log4j.Logger recordLog = null;
		private volatile List<LockTrace> recordRec = new LinkedList<>();
		private volatile List<LockTrace> recordHandle = new LinkedList<>();
		private Thread thread = null;
		private final int TRACE_QUEUE_MAX_LENGTH = 200000;
		private AtomicInteger abandonCount = new AtomicInteger(0);
		private int MAX_AGGREGATE_SIZE = 200;
		// 是否已经被Notify过
		private volatile boolean hasNotified = false;

		public RecordWorker(String logName, String thname) {
			this.logName = logName;
			//用的是store  这里可能需要改
			this.recordLog = LogManager.getLogger(logName);
			this.thread = new Thread(new RecordTask());
			this.thread.setName(thname + "_thd");
			this.thread.setDaemon(true);
			this.thread.start();
		}

		public void offer(LockTrace record) {
			if (recordRec.size() > TRACE_QUEUE_MAX_LENGTH) {
				if (abandonCount.incrementAndGet() % TRACE_QUEUE_MAX_LENGTH == 0) {
					logger.warn(
							"Trace record name: {}, queue size > {} , abandoned num : {}.",
							"locktrace", TRACE_QUEUE_MAX_LENGTH,
							abandonCount.get());
				}
				return;
			}
			// 加数据
			try {
				putRecord(record);
			} catch (Exception e) {
				logger.warn("put Record exception:" + e.getMessage());
			}
		}

		public void putRecord(final LockTrace record) {
			synchronized (this) {
				this.recordRec.add(record);
				if (!this.hasNotified) {
					this.hasNotified = true;
					this.notify();
				}
			}
		}

		public void onWaitEnd() {
			this.swapRequestsList();
		}

		public void waitForRunning(long interval) {
			synchronized (this) {
				if (this.hasNotified) {
					this.hasNotified = false;
					this.onWaitEnd();
					return;
				}
				try {
					this.wait(interval);
				} catch (InterruptedException e) {
					logger.error("waitForRunning", e);
				} finally {
					this.hasNotified = false;
					this.onWaitEnd();
				}
			}
		}

		public void clearHandList() {
			synchronized (this) {
				recordHandle.clear();
			}
		}

		private void swapRequestsList() {
			List<LockTrace> tmp = this.recordHandle;
			this.recordHandle = this.recordRec;
			this.recordRec = tmp;
		}

		public int getQsize() {
			return recordRec.size() + recordHandle.size() + aggregator.size();
		}

		private LinkedList<LockTrace> aggregator = new LinkedList<>();

		private class RecordTask implements Runnable {
			StringBuilder batchLogBuilder = null;
			final boolean isLinux = !"win".startsWith(System.getProperty("os.name").toLowerCase());

			public void handleRecord() {
				try {
					if (recordHandle.isEmpty()) {
						return;
					}
					for (LockTrace record : recordHandle) {
						aggregator.add(record);
						if (aggregator.size() > MAX_AGGREGATE_SIZE) {
							storeRecords(aggregator);
						}
					}
					storeRecords(aggregator);
					clearHandList();
				} catch (Exception e) {
					logger.warn(thread.getName() + " service has exception. ",
							e);
				}
			}

			@Override
			public void run() {
				long recordCount = 0;
				while (true) {
					try {
						waitForRunning(0);
						// 一定是交换完了才会执行handle
						handleRecord();
						if (Math.abs(++recordCount) % 5000 == 0) {
							Thread.sleep(2);
						}
					} catch (Exception e) {
						logger.error("RecordTask error", e);
					}
				}
			}

			private void storeRecords(LinkedList<LockTrace> aggregator) {
				if (!aggregator.isEmpty()) {
					batchLogBuilder = new StringBuilder(20000);
					for (LockTrace record : aggregator) {
						if (!isLinux) {
							batchLogBuilder.append("\r");
						} else {
							batchLogBuilder.append("\n");
						}
						batchLogBuilder.append(record.toString());
					}
					recordLog.info(batchLogBuilder.toString());
					aggregator.clear();
				}
			}
		}
	}
}
