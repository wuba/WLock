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
package com.wuba.wlock.client.watch;

import com.wuba.wlock.client.InternalLockOption;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WatchEvent {
	/**
	 * 0 写锁，1 读锁
	 */
	private int lockType = 0;
	private String lockKey;
	private long threadID;
	private long watchID;
	private WatchType watchType;
	private long startTimestamp;
	private long timeout;
	private AutoResetEvent event;
	private boolean isCanceled;
	private InternalLockOption lockOption;
	private NotifyEvent notifyEvent;
	private AtomicBoolean triggered = new AtomicBoolean(false);
	
	public WatchEvent(String lockKey, long threadID, long watchID, WatchType watchType, long startTimestamp) {
		super();
		this.lockKey = lockKey;
		this.threadID = threadID;
		this.watchID = watchID;
		this.watchType = watchType;
		this.startTimestamp = startTimestamp;
		this.event = new AutoResetEvent(1);
	}

	public AutoResetEvent getEvent() {
		return event;
	}

	public void setEvent(AutoResetEvent event) {
		this.event = event;
	}

	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

	public long getThreadID() {
		return threadID;
	}

	public void setThreadID(long threadID) {
		this.threadID = threadID;
	}

	public long getWatchID() {
		return watchID;
	}
	
	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}

	public WatchType getWatchType() {
		return watchType;
	}

	public void setWatchType(WatchType watchType) {
		this.watchType = watchType;
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public void setStartTimestamp(long startTimestamp) {
		this.startTimestamp = startTimestamp;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public boolean isTimeout() {
		return System.currentTimeMillis() - this.startTimestamp >= timeout;
	}
	
	public long leftTime() {
		return (long) (timeout - (System.currentTimeMillis() - this.startTimestamp));
	}
	
	public InternalLockOption getLockOption() {
		return lockOption;
	}

	public void setLockOption(InternalLockOption lockOption) {
		this.lockOption = lockOption;
	}

	public NotifyEvent getNotifyEvent() {
		return notifyEvent;
	}

	public void setNotifyEvent(NotifyEvent notifyEvent) {
		this.notifyEvent = notifyEvent;
	}

	public boolean isCanceled() {
		return isCanceled;
	}

	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}
	
	public void countDown() {
		this.event.countDown();
	}

	public AtomicBoolean getTriggered() {
		return triggered;
	}

	public void setTriggered(AtomicBoolean triggered) {
		this.triggered = triggered;
	}

	class AutoResetEvent {
		private final CountDownLatch cdl;
		private final int waitCount;

		public AutoResetEvent() {
			cdl = new CountDownLatch(1);
			this.waitCount = 1;
		}

		public AutoResetEvent(int waitCount) {
			cdl = new CountDownLatch(waitCount);
			this.waitCount = waitCount;
		}

		public void countDown() {
			cdl.countDown();
		}

		public boolean waitOne(long time) throws InterruptedException {
			return cdl.await(time, TimeUnit.MILLISECONDS);
		}

		public int getWaitCount() {
			return waitCount;
		}
	}
}
