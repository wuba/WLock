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
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.util.ThreadPoolUtil;
import com.wuba.wlock.client.util.ThreadRenameFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class EventCachedHandler {
	private static final Log logger = LogFactory.getLog(EventCachedHandler.class);
	
	private ConcurrentHashMap<Long/*watchID*/, WatchEvent> watchIdMap = new ConcurrentHashMap<Long, WatchEvent>();
	private ConcurrentHashMap<String/*lockkey*/, Set<Long>/*watchID*/> keyWatchMap = new ConcurrentHashMap<String, Set<Long>>();
	private ConcurrentHashMap<String, HashMap<Long, AcquireEvent>> regisAcquireEvents = new ConcurrentHashMap<String, HashMap<Long, AcquireEvent>>();
	private ReentrantLock watchRegisLock = new ReentrantLock();
	private ReentrantLock acquireRegisLock = new ReentrantLock();
	private final WLockClient wlockClient;
	public static int timeCheckoutInterval = 5; //5 seconds
	public static int reSendEventInterval = 10; //10 seconds
	public static ConcurrentHashMap<Long, EventCachedHandler> map = new ConcurrentHashMap<Long, EventCachedHandler>();
	public static final ScheduledExecutorService scheduleExecutorService = ThreadPoolUtil.newScheduledThreadPool(1, new ThreadRenameFactory("EventCachedHandler_"));
	
	public EventCachedHandler(WLockClient wlockClient) {
		this.wlockClient = wlockClient;
		initScheduleTask();
	}
	
	/**
	 * 定时任务初始化
	 */
	public void initScheduleTask() {
		scheduleExecutorService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				timeoutCheck();
			}
			
		}, timeCheckoutInterval, timeCheckoutInterval, TimeUnit.SECONDS);
		
		scheduleExecutorService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					watchEventMakeUp(null);
				} catch(Throwable th) {
					logger.error(Version.INFO + ", watchEvent makeup failed.");
				}
			}
			
		}, reSendEventInterval, reSendEventInterval, TimeUnit.SECONDS);
	}
	
	public static EventCachedHandler getInstance(WLockClient wlockClient) {
		EventCachedHandler handler = map.get(wlockClient.getUniqueCode());
		if (handler == null) {
			synchronized (EventCachedHandler.class) {
				handler = map.get(wlockClient.getUniqueCode());
				if (handler == null) {
					handler = new EventCachedHandler(wlockClient);
					map.put(wlockClient.getUniqueCode(), handler);
				}
			}
		}

		return handler;
	}
	
	/**
	 * 注册watch事件
	 * @param lockkey
	 * @param watchEvent
	 */
	public void registerWatchEvent(String lockkey, WatchEvent watchEvent) {
		watchRegisLock.lock();
		try {
			this.watchIdMap.put(watchEvent.getWatchID(), watchEvent);
			Set<Long> watchSet = this.keyWatchMap.get(lockkey);
			if (watchSet != null) {
				watchSet.add(watchEvent.getWatchID());
			} else {
				Set<Long> newWatchSet = new HashSet<Long>();
				newWatchSet.add(watchEvent.getWatchID());
				keyWatchMap.put(lockkey, newWatchSet);
			}
		} finally {
			watchRegisLock.unlock();
		}
	}
	
	/**
	 * 获取watch事件
	 * @param lockkey
	 * @return
	 */
	public List<WatchEvent> getWatchEvents(String lockkey) {
		List<WatchEvent> watchList = new LinkedList<WatchEvent>();
		watchRegisLock.lock();
		try {
			Set<Long> watchSet = this.keyWatchMap.get(lockkey);
			if (watchSet != null && !watchSet.isEmpty()) {
				Iterator<Long> iter = watchSet.iterator();
				while(iter.hasNext()) {
					long watchID = iter.next();
					WatchEvent watchEvent = watchIdMap.get(watchID);
					if (watchEvent != null) {
						watchList.add(watchEvent);
					} else {
						logger.error(Version.INFO + ", WatchWindow watchIdMap keyWatchMap inconsistence!!!");
					}

				}
			}
		} finally {
			watchRegisLock.unlock();
		}
		return watchList;
	}
	
	/**
	 * 取消某个key注册的所有watch事件
	 * @param lockkey
	 * @return
	 */
	public List<WatchEvent> unRegisterWatchEvent(String lockkey) {
		List<WatchEvent> watchList = new LinkedList<WatchEvent>();
		watchRegisLock.lock();
		try {
			Set<Long> watchSet = this.keyWatchMap.remove(lockkey);
			if (watchSet != null && !watchSet.isEmpty()) {
				Iterator<Long> iter = watchSet.iterator();
				while(iter.hasNext()) {
					long watchID = iter.next();
					WatchEvent watchEvent = watchIdMap.get(watchID);
					if (watchEvent != null) {
						watchList.add(watchEvent);
					} else {
						logger.error(Version.INFO + ", WatchWindow watchIdMap keyWatchMap inconsistence!!!");
					}

				}
			}
		} finally {
			watchRegisLock.unlock();
		}
		return watchList;
	}
	
	/**
	 * 取消某个watch事件
	 * @param lockkey
	 * @param watchID
	 * @return
	 */
	public WatchEvent unRegisterWatchEvent(String lockkey, long watchID) {
		WatchEvent watchEvent;
		watchRegisLock.lock();
		try {
			watchEvent = watchIdMap.remove(watchID);
			if (watchEvent == null) {
				logger.debug(Version.INFO + ", WatchWindow watchIdMap remove event failed, watchID : " + watchID);
			}

			Set<Long> watchSet = this.keyWatchMap.get(lockkey);
			if (watchSet != null && !watchSet.isEmpty()) {
				watchSet.remove(watchID);

				if (watchSet.isEmpty()) {
					this.keyWatchMap.remove(lockkey);
				}
			}
		} finally {
			watchRegisLock.unlock();
		}
		return watchEvent;
	}
	
	/**
	 * 超时检测任务
	 */
	public void timeoutCheck() {
		try {
			Iterator<Entry<Long, WatchEvent>> iter = watchIdMap.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<Long, WatchEvent> entry = iter.next();
				WatchEvent wt = entry.getValue();
				String lockkey = wt.getLockKey();
				if (wt.isTimeout()) {
					if (wt.getTriggered().compareAndSet(false, true) && wt.getLockOption().getWatchListener() != null) {
						wt.getLockOption().getWatchListener().onTimeout(lockkey);
					}
					iter.remove();
					wt.countDown();
					Set<Long> watchSet = keyWatchMap.get(lockkey);
					if (watchSet != null && !watchSet.isEmpty()) {
						watchSet.remove(entry.getKey());
						
						if (watchSet.isEmpty()) {
							keyWatchMap.remove(lockkey);
						}
					}
				}
			}
			
		} catch(Throwable th) {
			logger.error(Version.INFO + ", WatchEvent timeoutCheck error.", th);
		}
	}
	
	public WatchEvent getWatchEvent(long watchID) {
		return this.watchIdMap.get(watchID);
	}
	
	/**
	 * 未达到状态的acquireevent，需要向新节点发起续约
	 * @param acquireEvent
	 */
	public void reSendAcquireEvent(AcquireEvent acquireEvent) {
		String lockkey = acquireEvent.getLockkey();
		long lockVersion = acquireEvent.getLockversion();
		InternalLockOption lockOption = acquireEvent.getLockOption();
		
		try {
			this.wlockClient.getLockService().renewLock(lockkey, lockVersion, lockOption.getExpireTime(), lockOption.getThreadID(),
					!lockOption.isAutoRenew(), acquireEvent.getLockOption().getLockType(), acquireEvent.getLockOption().getLockOpcode());
		} catch (ParameterIllegalException e) {
			logger.error(Version.INFO + ", resend acquireEvent error.", e);
		}
	}
	
	/**
	 * 重新发送未达到状态的watchevent
	 * @param watchEvent
	 */
	public void reSendWatchEvent(WatchEvent watchEvent) {
		String lockkey = watchEvent.getLockKey();

		long leftTime = watchEvent.leftTime();
		if (leftTime > 200) {
			watchEvent.getLockOption().setMaxWaitTime(leftTime);
			try {
				this.wlockClient.getLockService().reWatchLock(lockkey, watchEvent);
				logger.info(Version.INFO + ", resend watchEvent, timeout : " + watchEvent.getTimeout() + ", watchID : " + watchEvent.getWatchID() + ", ThreadID : " + watchEvent.getThreadID());
			} catch (ParameterIllegalException e) {
				logger.error(Version.INFO + ", resend watchEvent error.", e);
			}
		} 
	}
	
	/**
	 * 注册持有锁的事件
	 * @param acquireEvent
	 */
	public void registerAcquireEvent(AcquireEvent acquireEvent) {
		acquireRegisLock.lock();
		try {
			this.regisAcquireEvents.putIfAbsent(acquireEvent.getLockkey(), new HashMap<Long, AcquireEvent>(16));
			HashMap<Long, AcquireEvent> acquireMap = this.regisAcquireEvents.get(acquireEvent.getLockkey());
			acquireMap.put(acquireEvent.getThreadID(), acquireEvent);
		} finally {
			acquireRegisLock.unlock();
		}
	}
	
	/**
	 * 取消某个key下注册的持有锁事件
	 * @param lockkey
	 */
	public void unRegisterAcquireEvent(String lockkey) {
		acquireRegisLock.lock();
		try {
			this.regisAcquireEvents.remove(lockkey);
		} finally {
			acquireRegisLock.unlock();
		}
	}
	
	/**
	 * 取消某个持有锁事件
	 * @param lockkey
	 * @param threadID
	 */
	public void unRegisterAcquireEvent(String lockkey, long threadID) {
		acquireRegisLock.lock();
		try {
			HashMap<Long, AcquireEvent> acquireEvents = this.regisAcquireEvents.get(lockkey);
			if (acquireEvents != null) {
				acquireEvents.remove(threadID);
				if (acquireEvents.isEmpty()) {
					this.regisAcquireEvents.remove(lockkey);
				}
			}
		} finally {
			acquireRegisLock.unlock();
		}
	}
	
	public void eventMakeUp(final Integer groupId) {
		Random random = new Random(5000);
		int delay = random.nextInt(5000);
		
		scheduleExecutorService.schedule(new Runnable() {

			@Override
			public void run() {
				watchEventMakeUp(groupId);
				acquireEventMakeUp(groupId);
			}
			
		}, delay, TimeUnit.MILLISECONDS);
	}
	
	/**
	 *watch事件补全
	 * @param groupId
	 */
	public void watchEventMakeUp(Integer groupId) {
		Collection<WatchEvent> watchEvents = this.watchIdMap.values();
		watchRegisLock.lock();
		try {
			watchEvents = this.watchIdMap.values();
		} finally {
			watchRegisLock.unlock();
		}
		
		for (WatchEvent watchEvent : watchEvents) {
			if (groupId == null || wlockClient.getRegistryKey().getGroupId(watchEvent.getLockKey()) == groupId) {
				long pass = System.currentTimeMillis() - watchEvent.getStartTimestamp();
				if (pass > 3000) {
					// acquire lock未ack watch event不需要发送
					reSendWatchEvent(watchEvent);
				}
			}
		}
	}
	
	/**
	 * 持有锁续约
	 * @param groupId
	 */
	public void acquireEventMakeUp(Integer groupId) {
		List<AcquireEvent> acquireEvents = new LinkedList<AcquireEvent>();
		acquireRegisLock.lock();
		try {
			Iterator<Entry<String, HashMap<Long, AcquireEvent>>> iter = this.regisAcquireEvents.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, HashMap<Long, AcquireEvent>> entry = iter.next();
				HashMap<Long, AcquireEvent> allEvents = entry.getValue();
				for (AcquireEvent acquireEvent : allEvents.values()) {
					if (groupId == null || wlockClient.getRegistryKey().getGroupId(acquireEvent.getLockkey()) == groupId) {
						acquireEvents.add(acquireEvent);
					}
				}
			}
		} finally {
			acquireRegisLock.unlock();
		}
		
		for (AcquireEvent acquireEvent : acquireEvents) {
			reSendAcquireEvent(acquireEvent);
		}
	}
	
	
}