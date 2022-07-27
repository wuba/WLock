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
package com.wuba.wlock.server.watch.impl;

import com.wuba.wlock.server.client.ClientManager;
import com.wuba.wlock.server.client.LockClient;
import com.wuba.wlock.server.communicate.ProtocolType;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.TrySnatchLockRequest;
import com.wuba.wlock.server.expire.ExpireStrategyFactory;
import com.wuba.wlock.server.expire.event.ExpireEvent;
import com.wuba.wlock.server.expire.event.WatchExpireEvent;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.watch.IEventStorage;
import com.wuba.wlock.server.watch.WatchEvent;
import com.wuba.wlock.server.watch.WatchIndex;
import com.wuba.wlock.server.watch.WatchType;
import com.wuba.wlock.server.worker.LockWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class EventStoreMemImpl implements IEventStorage {
	private ConcurrentHashMap<Integer, EventGroup> eventGroupMap = new ConcurrentHashMap<Integer, EventGroup>();
	private static EventStoreMemImpl instance = new EventStoreMemImpl();
	
	private EventStoreMemImpl() {}
	
	public static EventStoreMemImpl getInstance() {
		return instance;
	}

	@Override
	public List<WatchEvent> getWatchEvents(String lockkey, int groupId) {
		EventGroup eventGroup = eventGroupMap.get(groupId);
		if (eventGroup != null) {
			return eventGroup.getWatchEvents(lockkey);
		}
		return new LinkedList<WatchEvent>();
	}
	
	@Override
	public WatchEvent getFirstAcquiredEvent(String lockkey, int groupId) {
		EventGroup eventGroup = eventGroupMap.get(groupId);
		if (eventGroup != null) {
			return eventGroup.getFirstAcquiredEvent(lockkey);
		}
		return null;
	}

	@Override
	public boolean addWatchEvent(String lockkey, WatchEvent watchEvent, int groupId) {
		EventGroup eventGroup = eventGroupMap.get(groupId);
		if (eventGroup != null) {
			return eventGroup.addWatchEvent(lockkey, watchEvent);
		}else{
			eventGroup = new EventGroup(groupId);
			eventGroup.addWatchEvent(lockkey,watchEvent);
			eventGroupMap.put(groupId,eventGroup);
		}
		return false;
	}

	@Override
	public boolean deleteWatchEvent(String lockkey, WatchEvent watchEvent, int groupId) {
		EventGroup eventGroup = eventGroupMap.get(groupId);
		if (eventGroup != null) {
			return eventGroup.deleteWatchEvent(lockkey, watchEvent);
		}
		return false;
	}

	@Override
	public boolean deleteWatchEvent(String lockkey, List<LockClient> clientList, int groupId) {
		EventGroup eventGroup = eventGroupMap.get(groupId);
		if (eventGroup != null) {
			return eventGroup.deleteWatchEvent(lockkey, clientList);
		}
		return false;
	}
	
	@Override
	public boolean deleteWatchEvents(String lockkey, List<WatchEvent> watchEventList, int groupId) {
		EventGroup eventGroup = eventGroupMap.get(groupId);
		if (eventGroup != null) {
			return eventGroup.deleteWatchEvents(lockkey, watchEventList);
		}
		return false;
	}
	
	@Override
	public void checkWatchEvent() {
		if (eventGroupMap == null) {
			return;
		}
		
		for (EventGroup eventGroup : eventGroupMap.values()) {
			eventGroup.checkAlive();
		}
	}

	@Override
	public void notifyTimeoutEvent(List<WatchExpireEvent> timeoutEvents) {
		if (eventGroupMap == null || timeoutEvents == null) {
			return;
		}
		
		HashMap<Integer, List<WatchExpireEvent>> timeoutMap = new HashMap<Integer, List<WatchExpireEvent>>();
		for (WatchExpireEvent expireEvent: timeoutEvents) {
			int groupId = expireEvent.getGroupId();
			List<WatchExpireEvent> expireEvents = timeoutMap.computeIfAbsent(groupId, k -> new LinkedList<>());
			expireEvents.add(expireEvent);
		}
		
		Iterator<Entry<Integer, List<WatchExpireEvent>>> iter = timeoutMap.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Integer, List<WatchExpireEvent>> entry = iter.next();
			EventGroup eventGroup = eventGroupMap.get(entry.getKey());
			if (eventGroup != null) {
				eventGroup.notifyTimeoutEvent(entry.getValue());
			}
		}
	}

	@Override
	public int getWatchEventCount() {
		int eventCount = 0;
		
		if (eventGroupMap != null) {
			for (EventGroup eventGroup : eventGroupMap.values()) {
				eventCount += eventGroup.getEventCount();
			}
		}
		
		return eventCount;
	}
}

class EventGroup {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventGroup.class);
	private int groupId;
	private ConcurrentHashMap<String, LinkedHashMap<WatchIndex, WatchEvent>> eventMap = new ConcurrentHashMap<String, LinkedHashMap<WatchIndex, WatchEvent>>();
//	private ConcurrentHashMap<String/*lockkey*/, LinkedHashMap<WatchEvent>> eventMap = new ConcurrentHashMap<String/*lockkey*/, LinkedHashMap<WatchEvent>>();
	private ReentrantLock groupEventLock = new ReentrantLock();
	private static LockWorker lockWorker = LockWorker.getInstance();
	
	public EventGroup(int groupId) {
		super();
		this.groupId = groupId;
	}

	public List<WatchEvent> getWatchEvents(String lockkey) {
		List<WatchEvent> eventList = null;
		this.groupEventLock.lock();
		try {
			LinkedHashMap<WatchIndex, WatchEvent> events = eventMap.get(lockkey);
			if (events == null || events.isEmpty()) {
				return new LinkedList<WatchEvent>();
			}

			eventList = new LinkedList<WatchEvent>();
			Iterator<Entry<WatchIndex, WatchEvent>> iter = events.entrySet().iterator();
			while (iter.hasNext()) {
				WatchEvent watchEvent = iter.next().getValue();
				if (watchEvent.isTimeout() || !watchEvent.isAlive()) {
					iter.remove();
					continue;
				}

				eventList.add(watchEvent);
			}
		} finally {
			this.groupEventLock.unlock();
		}
		
		return eventList;
	}
	
	public WatchEvent getFirstAcquiredEvent(String lockkey) {
		WatchEvent watchEventRes = null;
		this.groupEventLock.lock();
		try {
			LinkedHashMap<WatchIndex, WatchEvent> events = eventMap.get(lockkey);
			if (events != null && !events.isEmpty()) {
				Iterator<Entry<WatchIndex, WatchEvent>> iter = events.entrySet().iterator();
				int maxWeight = -1;
				while (iter.hasNext()) {
					WatchEvent watchEvent = iter.next().getValue();
					LOGGER.debug("- getFirstAcquiredEvent watchID {}, weight {}， lastUpdateTime {}, isAlive {}, isTimeout {}, watchType {}..", 
							watchEvent.getWatchID(), watchEvent.getWeight(), watchEvent.getLastUpateTimestamp(), watchEvent.isAlive(), watchEvent.isTimeout(), watchEvent.getWatchType());
					if (watchEvent.isTimeout() || !watchEvent.isAlive()) {
						iter.remove();
						continue;
					}
					if (watchEvent.getWatchType() != WatchType.ACQUIRE && watchEvent.getWatchType() != WatchType.WATCH_AND_ACQUIRE) {
						continue;
					}
					if (watchEvent.getWeight() > maxWeight) {
						watchEventRes = watchEvent;
						maxWeight = watchEvent.getWeight();
					}
				}
			}
		} finally {
			this.groupEventLock.unlock();
		}
		
		return watchEventRes;
	}
	
	public boolean addWatchEvent(String lockkey, WatchEvent watchEvent) {	
		if (watchEvent == null) {
			LOGGER.error("add watchevent null, lockkey : " + lockkey);
			return false;
		}
		this.groupEventLock.lock();
		try {
			LinkedHashMap<WatchIndex, WatchEvent> events = null;
			if (!eventMap.containsKey(lockkey)) {
				events = new LinkedHashMap<WatchIndex, WatchEvent>();
			} else {
				events = eventMap.get(lockkey);
			}
			WatchIndex watchIdx = watchEvent.getWatchIndex();
			if (!events.containsKey(watchIdx)) {
				ExpireEvent expireEvent = new WatchExpireEvent(watchEvent.getTimeoutStamp(), lockkey, watchIdx, groupId, watchEvent.getLockType(), watchEvent.getOpcode());
				LOGGER.debug("addWatchEvent {}, watchIndex {}.", watchEvent.getTimeoutStamp(), watchIdx);
				ExpireStrategyFactory.getInstance().addExpireEvent(expireEvent);
			}
			events.put(watchIdx, watchEvent);
			eventMap.put(lockkey, events);
		} finally {
			this.groupEventLock.unlock();
		}
		
		return true;
	}

	
	public boolean deleteWatchEvent(String lockkey, WatchEvent watchEvent) {
		if (watchEvent == null) {
			LOGGER.error("delete watch event null, lockkey : " + lockkey);
			return false;
		}
		this.groupEventLock.lock();
		try {
			LinkedHashMap<WatchIndex, WatchEvent> events = eventMap.get(lockkey);
			if (events != null && !events.isEmpty()) {
				events.remove(watchEvent.getWatchIndex());
			}
		} finally {
			this.groupEventLock.unlock();
		}
		
		return true;
	}
	
	public boolean deleteWatchEvents(String lockkey, List<WatchEvent> watchEventList) {
		if (watchEventList == null || watchEventList.isEmpty()) {
			LOGGER.error("delete watch event list null, lockkey : " + lockkey);
			return false;
		}
		
		this.groupEventLock.lock();
		try {
			LinkedHashMap<WatchIndex, WatchEvent> events = eventMap.get(lockkey);
			if (events != null && !events.isEmpty()) {
				for (WatchEvent toDelEvent : watchEventList) {
					events.remove(toDelEvent.getWatchIndex());
				}
			}
		} finally {
			this.groupEventLock.unlock();
		}
		
		return true;
	}

	public boolean deleteWatchEvent(String lockkey, List<LockClient> clientList) {
		if (clientList == null) {
			LOGGER.error("delete watch event, clientList null, lockkey : " + lockkey);
			return false;
		}

		WatchEvent watchEvent = null;
		this.groupEventLock.lock();
		try {

			LinkedHashMap<WatchIndex, WatchEvent> events = eventMap.get(lockkey);
			if (events != null && !events.isEmpty()) {
				Iterator<Entry<WatchIndex, WatchEvent>> iter = events.entrySet().iterator();
				while (iter.hasNext()) {
					WatchEvent watchEvent1 = iter.next().getValue();
					for (LockClient lockClient : clientList) {
						if (watchEvent1.getLockClient().equals(lockClient)) {
							iter.remove();
							if (watchEvent == null) {
								watchEvent = watchEvent1;
							}
						}
					}
				}
			}
		} finally {
			this.groupEventLock.unlock();
		}

		if (watchEvent != null) {
			trySnatchLock(lockkey, watchEvent.getLockType(), watchEvent.getOpcode());
		}

		return true;
	}
	
	public void checkAlive() {
		Set<String> nonAliveKeys = new HashSet<String>(16);
		HashMap<String, Set<WatchIndex>> nonAliveEvents = new HashMap<String, Set<WatchIndex>>();
		Iterator<Entry<String, LinkedHashMap<WatchIndex, WatchEvent>>> iter = this.eventMap.entrySet().iterator();
		while(iter.hasNext()) {
			try {
				Entry<String, LinkedHashMap<WatchIndex, WatchEvent>> lockItem = iter.next();
				String lockkey = lockItem.getKey();
				LinkedHashMap<WatchIndex, WatchEvent> events = lockItem.getValue();
				if (events == null || events.isEmpty()) {
					nonAliveKeys.add(lockkey);
					continue;
				}
				
				Set<Entry<WatchIndex, WatchEvent>> eventset = new HashSet<Entry<WatchIndex, WatchEvent>>(events.entrySet());
				Iterator<Entry<WatchIndex, WatchEvent>> iter1 = eventset.iterator();
				while (iter1.hasNext()) {
					try {
						Entry<WatchIndex, WatchEvent> entry = iter1.next();
						if (!entry.getValue().isAlive()) {
							nonAliveEvents.putIfAbsent(lockkey, new HashSet<WatchIndex>());
							nonAliveEvents.get(lockkey).add(entry.getKey());
						}
					} catch(Exception e1) {
						LOGGER.warn("checkAlive error.", e1);
					}
				}
				
				eventset.clear();
			} catch(Exception e2) {
				LOGGER.warn("checkAlive error.", e2);
			}

		}
		
		this.groupEventLock.lock();
		HashMap<String, Set<WatchIndex>> realDeletes = new HashMap<>();
		try {
			for (String key : nonAliveKeys) {
				if (this.eventMap.containsKey(key) && (this.eventMap.get(key) == null || this.eventMap.get(key).isEmpty())) {
					this.eventMap.remove(key);
				}
			}
			
			for (Entry<String, Set<WatchIndex>> entry : nonAliveEvents.entrySet()) {
				LinkedHashMap<WatchIndex, WatchEvent> currentEvents = this.eventMap.get(entry.getKey());
				if (currentEvents == null || currentEvents.isEmpty()) {
					continue;
				}
				WatchEvent watchEvent = null;
				Set<WatchIndex> delEvents = entry.getValue();
				for (WatchIndex delevent : delEvents) {
					Set<WatchIndex> watchIndices = realDeletes.computeIfAbsent(entry.getKey(), k -> new HashSet<>());
					WatchEvent remove = currentEvents.remove(delevent);
					if (watchEvent == null) {
						watchEvent = remove;
					}
					watchIndices.add(delevent);
				}

				if (watchEvent != null) {
					trySnatchLock(entry.getKey(), watchEvent.getLockType(), watchEvent.getOpcode());
				}
			}
		}  finally {
			this.groupEventLock.unlock();
		}
		ClientManager.getInstance().removeLockClient(realDeletes,groupId);
	}
	
	public void notifyTimeoutEvent(List<WatchExpireEvent> expireEvents) {
		if (expireEvents == null) {
			return;
		}
		HashMap<String, Set<WatchIndex>> timeoutEvents = new HashMap<String, Set<WatchIndex>>();
		for (WatchExpireEvent expireEvent : expireEvents) {
			String lockkey = expireEvent.getLockKey();
			WatchIndex watchIdx = expireEvent.getWatchIdx();
			
			timeoutEvents.putIfAbsent(lockkey, new HashSet<WatchIndex>());
			Set<WatchIndex> tSet = timeoutEvents.get(lockkey);
			tSet.add(watchIdx);
		}
		
		this.groupEventLock.lock();
		HashMap<String, Set<WatchIndex>> realDeletes = new HashMap<>();
		try {		
			for (Entry<String, Set<WatchIndex>> entry : timeoutEvents.entrySet()) {
				LinkedHashMap<WatchIndex, WatchEvent> currentEvents = this.eventMap.get(entry.getKey());
				if (currentEvents == null || currentEvents.isEmpty()) {
					continue;
				}

				WatchEvent watchEvent = null;
				Set<WatchIndex> delEvents = entry.getValue();
				for (WatchIndex delevent : delEvents) {
					Set<WatchIndex> watchIndices = realDeletes.computeIfAbsent(entry.getKey(), k -> new HashSet<>());
					WatchEvent remove = currentEvents.remove(delevent);
					if (watchEvent == null) {
						watchEvent = remove;
					}
					watchIndices.add(delevent);
				}

				if (watchEvent != null) {
					trySnatchLock(entry.getKey(), watchEvent.getLockType(), watchEvent.getOpcode());
				}
			}
		} finally {
			this.groupEventLock.unlock();
		}
		ClientManager.getInstance().removeLockClient(realDeletes,groupId);
	}
	
	public int getEventCount() {
		int eventCount = 0;
		
		Iterator<Entry<String, LinkedHashMap<WatchIndex, WatchEvent>>> iter = this.eventMap.entrySet().iterator();
		while(iter.hasNext()) {
			try {
				Entry<String, LinkedHashMap<WatchIndex, WatchEvent>> lockItem = iter.next();
				LinkedHashMap<WatchIndex, WatchEvent> events = lockItem.getValue();
				if (events == null || events.isEmpty()) {
					continue;
				}

				eventCount += events.size();
			} catch(Exception e2) {
				LOGGER.warn("getEventCount error.", e2);
			}
		}
		
		return eventCount;
	}
	
	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	private void trySnatchLock(String lockKey, byte lockType, byte opcode) {
		if (lockKey == null) {
			return;
		}

		try {
			String registryKey = null;
			if (lockKey != null) {
				int index = lockKey.indexOf("_");
				if (index >= 0) {
					registryKey = lockKey.substring(0, index);
				}
			}

			if (registryKey == null) {
				return;
			}

			TrySnatchLockRequest trySnatchLockRequest = createTrySnatchLockRequest(lockKey, registryKey, lockType, opcode);
			LockContext lockContext = new LockContext(trySnatchLockRequest.toBytes());
			lockContext.setLockkey(lockKey);
			lockContext.setRegistryKey(registryKey);

			lockWorker.offer(lockContext, groupId);
		} catch (Exception e) {
		}
	}

	private TrySnatchLockRequest createTrySnatchLockRequest(String lockKey, String registryKey, byte lockType, byte opcode) {
		TrySnatchLockRequest trySnatchLockRequest = new TrySnatchLockRequest();
		trySnatchLockRequest.setProtocolType(ProtocolType.TRY_SNATCH_LOCK);
		trySnatchLockRequest.setLockKey(lockKey);
		trySnatchLockRequest.setRegistryKey(registryKey);
		trySnatchLockRequest.setTimestamp(TimeUtil.getCurrentTimestamp());
		trySnatchLockRequest.setLockType(lockType);
		trySnatchLockRequest.setOpcode(opcode);
		return trySnatchLockRequest;
	}
}
