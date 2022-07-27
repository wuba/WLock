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
package com.wuba.wlock.server.communicate.retrans;

import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.util.ThreadPoolUtil;
import com.wuba.wlock.server.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RetransWaitWindow {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetransWaitWindow.class);
	private Map<Long, RetransWindowData> waitMap = new HashMap<Long, RetransWindowData>();	
	public final ReentrantLock rlock = new ReentrantLock();
	private static final ScheduledExecutorService scheduledExecutorService = ThreadPoolUtil
			.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "RetransCheckScheduledThread");
				}
			});
	
	public RetransWaitWindow() {
		initScheduleTask();
	}
	
	public void initScheduleTask() {
		scheduledExecutorService.scheduleAtFixedRate(new CleanTask(this), 60, 60, TimeUnit.SECONDS);
	}
	
	public void regisWindowData(long sessionId, RetransWindowData windowData) {
		rlock.lock();
		try {
			waitMap.put(sessionId, windowData);
		} finally {
			rlock.unlock();
		}
	}
	
	public void unregisWindowData(long sessionId) {
		rlock.lock();
		try {
			waitMap.remove(sessionId);
		} finally {
			rlock.unlock();
		}
	}
	
	public RetransWindowData getWindowData(long sessionID) {
		return this.waitMap.get(sessionID);
	} 
	
	public Map<Long, RetransWindowData> getWaitMap() {
		return waitMap;
	}

	public void setWaitMap(Map<Long, RetransWindowData> waitMap) {
		this.waitMap = waitMap;
	}

	static class RetransWindowData {
		private LockContext lockContext;
		private long startTimestamp;
		
		public RetransWindowData(LockContext lockContext, long startTimestamp) {
			super();
			this.lockContext = lockContext;
			this.startTimestamp = startTimestamp;
		}

		public LockContext getLockContext() {
			return lockContext;
		}
		
		public void setLockContext(LockContext lockContext) {
			this.lockContext = lockContext;
		}
		
		public long getStartTimestamp() {
			return startTimestamp;
		}
		
		public void setStartTimestamp(long startTimestamp) {
			this.startTimestamp = startTimestamp;
		}
		
		public boolean isTimeout() {
			return (TimeUtil.getCurrentTimestamp() - this.startTimestamp) >= RetransConfig.RETRANS_REQ_MAX_WAIT_TIME ? true : false;
		}
	}
	
	class CleanTask implements Runnable {
		RetransWaitWindow waitWindow;
		
		CleanTask(RetransWaitWindow waitWindow) {
			this.waitWindow = waitWindow;
		}

		@Override
		public void run() {	
			try {
				Map<Long, RetransWindowData> waitMap = new HashMap<Long, RetransWindowData>();
				Set<Long> toRemove = new HashSet<Long>();
				waitMap.putAll(waitWindow.getWaitMap());
				
				Iterator<Entry<Long, RetransWindowData>> iter1 = waitMap.entrySet().iterator();
				while(iter1.hasNext()) {
					Entry<Long, RetransWindowData> entry = iter1.next();
					RetransWindowData retransWindowData = entry.getValue();
					if (retransWindowData.isTimeout()) {
						toRemove.add(entry.getKey());
					}
				}
				
				waitWindow.rlock.lock();
				try {
					Iterator<Long> iter2 = toRemove.iterator();
					while(iter2.hasNext()) {
						long sid = iter2.next();
						waitWindow.getWaitMap().remove(sid);
					}
				} finally {
					waitWindow.rlock.unlock();
				}
			} catch(Throwable th) {
				LOGGER.error("retrans request timeout clean error.", th);
			}
		}
	}
}
