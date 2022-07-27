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
package com.wuba.wlock.server.expire.queue.all;

import com.wuba.wlock.server.expire.ExpireTriggerProcessor;
import com.wuba.wlock.server.expire.event.ExpireEvent;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExpireQueueAllDispatcher {

	private static Logger logger = LoggerFactory.getLogger(ExpireQueueAllDispatcher.class);

	private int groupId;

	private ArrayList<PriorityBlockingQueue<ExpireEvent>> priorityBlockingQueueList;

	private QueueAllExpireManager queueAllExpireManager;

	private ExpireTriggerProcessor expireTriggerProcessor;

	private ExecutorService learnExecutorService;

	private final BlockingQueue<Byte> learnTask = new LinkedBlockingQueue<Byte>(1);

	private final Byte leanrnByte = 0x1;

	private ExpireRunnable[] workers;

	private Thread[] workerThreads;

	private volatile boolean isPaused = true;

	private volatile boolean isStop = false;

	private static final int WORKER_COUNT = 3;

	private static final int SLEEP_INTERVAL = 200;

	public ExpireQueueAllDispatcher(int groupId, QueueAllExpireManager queueAllExpireManager) {
		this.groupId = groupId;
		this.queueAllExpireManager = queueAllExpireManager;
		this.learnExecutorService = Executors.newSingleThreadExecutor(new ThreadFactoryImpl("ExpireQueueAllLearnThread-" + groupId + "-"));
		this.expireTriggerProcessor = new ExpireTriggerProcessor(groupId, queueAllExpireManager);
		this.priorityBlockingQueueList = new ArrayList<PriorityBlockingQueue<ExpireEvent>>(WORKER_COUNT);
		this.workers = new ExpireRunnable[WORKER_COUNT];
		for (int i = 0; i < WORKER_COUNT; i++) {
			PriorityBlockingQueue<ExpireEvent> taskQueue = new PriorityBlockingQueue<ExpireEvent>();
			priorityBlockingQueueList.add(taskQueue);
			workers[i] = new ExpireRunnable(taskQueue);
		}
	}

	public void start() {
		if (queueAllExpireManager.getWpaxosService().isIMMaster(groupId)) {
			isPaused = false;
		} else {
			isPaused = true;
		}
		expireTriggerProcessor.start();

		workerThreads = new Thread[WORKER_COUNT];
		for (int i = 0; i < WORKER_COUNT; i++) {
			Thread thread = new Thread(workers[i], "ExpireQueueAllDispatcherThread-" + groupId + "-" + i);
			thread.start();
			workerThreads[i] = thread;
		}

		learnExecutorService.execute(new Runnable() {
			@Override
			public void run() {
				while (true) {
					doLearn();
				}
			}
		});

		logger.info("ExpireQueueAllDispatcher worker groupId {} start.", groupId);
	}

	public void resume() {
		expireTriggerProcessor.resume();
		if (isPaused) {
			isPaused = false;
		}
		logger.info("ExpireQueueAllDispatcher groupId {} resume.", groupId);
	}

	private void taskQueueClear() {
		if (null == priorityBlockingQueueList || priorityBlockingQueueList.size() < 1) {
			return;
		}
		for (int i = 0; i < WORKER_COUNT; i++) {
			priorityBlockingQueueList.get(i).clear();
		}
	}

	private boolean taskQueueIsEmpty() {
		if (null == priorityBlockingQueueList || priorityBlockingQueueList.size() < 1) {
			return true;
		}
		for (int i = 0; i < WORKER_COUNT; i++) {
			if (priorityBlockingQueueList.get(i).size() > 0) {
				return false;
			}
		}
		return true;
	}

	public void pause() {
		if (!isPaused) {
			isPaused = true;
			taskQueueClear();
		}
		expireTriggerProcessor.pause();
		logger.info("ExpireQueueAllDispatcher groupId {} pause.", groupId);
	}

	public void stop() {
		if (!taskQueueIsEmpty()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		isPaused = true;
		isStop = true;

		if (!learnExecutorService.isShutdown()) {
			learnExecutorService.shutdown();
			try {
				if (learnExecutorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
					learnExecutorService.shutdownNow();
				}
			} catch (InterruptedException e) {
			} finally {
				if (!learnExecutorService.isShutdown()) {
					learnExecutorService.shutdownNow();
				}
			}
		}
		taskQueueClear();
		expireTriggerProcessor.shutdown();
		logger.info("ExpireQueueAllDispatcher groupId {} stop", groupId);
	}

	public void learnMaster() {
		if (learnTask.isEmpty()) {
			learnTask.offer(leanrnByte);
		} else {
			learnTask.clear();
			learnTask.offer(leanrnByte);
		}
	}

	private void doLearn() {
		try {
			Byte poll = learnTask.poll(1, TimeUnit.HOURS);
			if (poll == null) {
				return;
			}
			if (!queueAllExpireManager.getWpaxosService().isIMMaster(groupId)) {
				return;
			}
			resume();
			List<ExpireEvent> expireEventList = queueAllExpireManager.getExpireEventRepository().getAllLockEvent(groupId);
			if (expireEventList == null || expireEventList.size() <= 0) {
				return;
			}
			for (int i = 0, n = expireEventList.size(); i < n; i++) {
				this.offer(expireEventList.get(i));
			}
			logger.info("ExpireQueueAllDispatcher groupId {} learn finished.", groupId);
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
		}
	}

	public int getQueueSize() {
		int size = 0;
		for (int i = 0; i < WORKER_COUNT; i++) {
			size += priorityBlockingQueueList.get(i).size();
		}
		return size;
	}

	public void offer(ExpireEvent expireEvent) {
		int id = Math.abs(expireEvent.getLockKey().hashCode() % WORKER_COUNT);
		priorityBlockingQueueList.get(id).offer(expireEvent);
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public QueueAllExpireManager getQueueExpireManager() {
		return queueAllExpireManager;
	}

	public void setQueueExpireManager(QueueAllExpireManager queueAllExpireManager) {
		this.queueAllExpireManager = queueAllExpireManager;
	}

	class ExpireRunnable implements Runnable {

		private PriorityBlockingQueue<ExpireEvent> taskQueue;

		public ExpireRunnable(PriorityBlockingQueue<ExpireEvent> taskQueue) {
			this.taskQueue = taskQueue;
		}

		@Override
		public void run() {
			while (!isStop) {
				try {
					if (isPaused) {
						Thread.sleep(SLEEP_INTERVAL);
						continue;
					}

					ExpireEvent expireEvent = taskQueue.peek();
					if (null == expireEvent) {
						Thread.sleep(SLEEP_INTERVAL);
					} else {
						long currentTime = TimeUtil.getCurrentTimestamp();
						if (expireEvent.getExpireTimestamp() <= currentTime) {
							ExpireEvent hasExpiredEvent = taskQueue.poll(10, TimeUnit.MILLISECONDS);
							expireTriggerProcessor.offer(hasExpiredEvent);
						} else {
							Thread.sleep(Math.min(expireEvent.getExpireTimestamp() - currentTime, SLEEP_INTERVAL));
						}
					}

				} catch (Exception e) {
					logger.info("QueueAllDispatcherThread error", e);
				}
			}
		}

	}

}
