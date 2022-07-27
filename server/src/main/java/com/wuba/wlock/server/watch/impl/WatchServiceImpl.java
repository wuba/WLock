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
import com.wuba.wlock.server.communicate.protocol.AcquireLockRequest;
import com.wuba.wlock.server.communicate.protocol.EventNotifyRequest;
import com.wuba.wlock.server.communicate.protocol.ProtocolFactoryImpl;
import com.wuba.wlock.server.communicate.protocol.WatchLockRequest;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.exception.LockException;
import com.wuba.wlock.server.expire.event.WatchExpireEvent;
import com.wuba.wlock.server.lock.protocol.ReentrantLockValue;
import com.wuba.wlock.server.lock.repository.LockRepositoryImpl;
import com.wuba.wlock.server.lock.repository.base.ILockRepository;
import com.wuba.wlock.server.util.ThreadPoolUtil;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wpaxos.utils.ThreadRenameFactory;
import com.wuba.wlock.server.watch.*;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WatchServiceImpl implements IWatchService {
	private static final Logger LOGGER = LoggerFactory.getLogger(WatchServiceImpl.class);
	
	private final IEventStorage eventStorage;
	private final ILockRepository lockRepository;
	private static WatchServiceImpl instance = new WatchServiceImpl();
	private ScheduledExecutorService scheduleService = ThreadPoolUtil.newSingleThreadScheduledExecutor(new ThreadRenameFactory("WatchScheduleService"));
	private ExecutorService executorService = ThreadPoolUtil.newFixedThreadPool(1, new ThreadRenameFactory("WatchSyncService"));
	
	
	private WatchServiceImpl() {
		this.eventStorage = EventStoreMemImpl.getInstance();
		this.lockRepository = LockRepositoryImpl.getInstance();
		this.startScheduleTask();
	}
	
	public static WatchServiceImpl getInstance() {
		return instance;
	}
	
	public void startScheduleTask() {
		// watchEvent alive check
		scheduleService.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				checkWatchEvent();
			}
		}, 10, 10, TimeUnit.MINUTES);
		
		scheduleService.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				LOGGER.info("watch event count in memory : " + eventStorage.getWatchEventCount());
			}
			
		}, 20, 20, TimeUnit.SECONDS);
	}
	
	@Override
	public void timeoutTrigger(final List<WatchExpireEvent> timeoutEvents) {
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				if (timeoutEvents == null) {
					return;
				}
				eventStorage.notifyTimeoutEvent(timeoutEvents);
			}
			
		});
	}
	
	@Override
	public void channelClosedTrigger(String lockkey, List<LockClient> removedClients) {
		if(lockkey == null) {
			LOGGER.error("channel close trigger, lockkey null.");
			return;
		}
		
		if (removedClients == null) {
			LOGGER.error("channel close trigger, remove clients null.");
			return;
		}
		
		this.eventStorage.deleteWatchEvent(lockkey, removedClients, removedClients.get(0).getGroupId());
	}

	@Override
	public void migrateEndTrigger(String lockkey, List<LockClient> removedClients) {
		if(lockkey == null) {
			LOGGER.error("migrate end trigger, lockkey null.");
			return;
		}

		if (removedClients == null || removedClients.isEmpty()) {
			LOGGER.error("migrate end trigger, remove clients null.");
			return;
		}

		this.eventStorage.deleteWatchEvent(lockkey, removedClients, removedClients.get(0).getGroupId());
	}

	@Override
	public void lockExpiredTrigger(String lockkey, LockClient lockClient, int groupId, EventType eventType) {
		if(lockkey == null) {
			LOGGER.error("lock expired trigger, lockkey null.");
			return;
		}
		
		if (lockClient == null) {
			LOGGER.error("lock expired trigger, lockClient null.");
			return;
		}
		
		LOGGER.debug("lock expired trigger, lockClient {}, lockkey {}.", lockClient, lockkey);
		
		NotifyEvent notifyEvent = this.genNotifyEvent(lockkey, WatchEvent.DEFAULT_BROADCAST_WATCH_ID, lockClient, eventType);
		if (notifyEvent == null) {
			return;
		}
		
		EventNotifyRequest notifyReq = ProtocolFactoryImpl.getInstance().createEventNotifyRes(notifyEvent);
		int channelId = lockClient.getChannelId();
		Channel channel = ClientManager.getInstance().getChannelByID(channelId, groupId);
		this.sendNotifyRequest(channel, notifyReq, null, lockClient.getVersion());
		ClientManager.getInstance().removeLockClient(lockkey, lockClient, groupId);
	}
	
	@Override
	public void lockUpdateTrigger(String lockkey, LockOwner newLockOwner, int groupId) {
		if(lockkey == null) {
			LOGGER.error("lock update trigger, lockkey null.");
			return;
		}
		
		LOGGER.debug("lock update trigger...key {}, owner {}.", lockkey, newLockOwner);
		
		List<WatchEvent> watchEvents = this.eventStorage.getWatchEvents(lockkey, groupId);
		if (watchEvents == null || watchEvents.isEmpty()) {
			return;
		}
		
		List<WatchEvent> toDeleteEvents = new LinkedList<WatchEvent>();
		
		Iterator<WatchEvent> iter = watchEvents.iterator();
		while(iter.hasNext()) {
			WatchEvent watchEvent = iter.next();
			LOGGER.debug("lock watchevent {}.", watchEvent.getLockClient());
			LockClient lockClient = watchEvent.getLockClient();
			NotifyEvent notifyEvent = null;
			if (watchEvent.getWatchType().getType() == WatchType.ACQUIRE.getType()
					|| watchEvent.getWatchType().getType() == WatchType.WATCH_AND_ACQUIRE.getType()) {
				if (lockClient.getcThreadID() == newLockOwner.getThreadId() 
						&& lockClient.getcHost() == newLockOwner.getIp()
						&& lockClient.getcPid() == newLockOwner.getPid()) {
					LOGGER.debug("lock notify...key {}, owner {}.", lockkey, newLockOwner);
					notifyEvent = this.genNotifyEvent(lockkey, watchEvent.getWatchID(), lockClient, EventType.LOCK_ACQUIRED);
				}
			} else {
				notifyEvent = this.genNotifyEvent(lockkey, watchEvent.getWatchID(), lockClient, EventType.LOCK_UPDATE);
			}
			
			if (notifyEvent != null) {
				notifyEvent.setLockOwner(newLockOwner);
				EventNotifyRequest notifyReq = ProtocolFactoryImpl.getInstance().createEventNotifyRes(notifyEvent);
				int channelId = lockClient.getChannelId();
				Channel channel = ClientManager.getInstance().getChannelByID(channelId, groupId);
				this.sendNotifyRequest(channel, notifyReq, watchEvent, lockClient.getVersion());
				LOGGER.debug("sendNotifyRequest : {}.", watchEvent.getLockClient());
				toDeleteEvents.add(watchEvent);
			}
		}
		
		if (!toDeleteEvents.isEmpty()) {
			eventStorage.deleteWatchEvents(lockkey,toDeleteEvents,groupId);
		}
	}
	
	/**
	 * lock update通知，但是不通知获取到锁
	 * @param lockkey
	 * @param newLockOwner
	 */
	@Override
	public void lockUpdateTrigger2(String lockkey, LockOwner newLockOwner, int groupId) {
		if(lockkey == null) {
			LOGGER.error("lock update trigger, lockkey null.");
			return;
		}
		
		List<WatchEvent> watchEvents = this.eventStorage.getWatchEvents(lockkey, groupId);
		if (watchEvents == null || watchEvents.isEmpty()) {
			return;
		}
		
		List<WatchEvent> toDeleteEvents = new LinkedList<WatchEvent>();
		
		Iterator<WatchEvent> iter = watchEvents.iterator();
		while(iter.hasNext()) {
			WatchEvent watchEvent = iter.next();
			LockClient lockClient = watchEvent.getLockClient();
			NotifyEvent notifyEvent = null;
			notifyEvent = this.genNotifyEvent(lockkey, watchEvent.getWatchID(), lockClient, EventType.LOCK_UPDATE);
			
			if (notifyEvent != null) {
				notifyEvent.setLockOwner(newLockOwner);
				EventNotifyRequest notifyReq = ProtocolFactoryImpl.getInstance().createEventNotifyRes(notifyEvent);
				int channelId = lockClient.getChannelId();
				Channel channel = ClientManager.getInstance().getChannelByID(channelId, groupId);
				this.sendNotifyRequest(channel, notifyReq, watchEvent, lockClient.getVersion());
				toDeleteEvents.add(watchEvent);
			}
		}
		
		if (!toDeleteEvents.isEmpty()) {
			eventStorage.deleteWatchEvents(lockkey,toDeleteEvents,groupId);
		}
	}
	
	@Override
	public void lockDeleteTrigger(String lockkey, int groupId) {
		if(lockkey == null) {
			LOGGER.error("lock delete trigger, lockkey null.");
			return;
		}
		
		LOGGER.debug("- lock delete notify...key {}.", lockkey);
		
		List<WatchEvent> watchEvents = this.eventStorage.getWatchEvents(lockkey, groupId);
		if (watchEvents == null || watchEvents.isEmpty()) {
			LOGGER.debug("- watchevent null...key {}.", lockkey);
			return;
		}
		
		List<WatchEvent> toDeleteEvents = new LinkedList<WatchEvent>();
		
		Iterator<WatchEvent> iter = watchEvents.iterator();
		NotifyEvent notifyEvent = null;
		while(iter.hasNext()) {
			WatchEvent watchEvent = iter.next();
			LockClient lockClient = watchEvent.getLockClient();
			notifyEvent = this.genNotifyEvent(lockkey, watchEvent.getWatchID(), watchEvent.getLockClient(), EventType.LOCK_RELEASE);
			if (notifyEvent != null) {
				EventNotifyRequest notifyReq = ProtocolFactoryImpl.getInstance().createEventNotifyRes(notifyEvent);
				int channelId = lockClient.getChannelId();
				Channel channel = ClientManager.getInstance().getChannelByID(channelId, groupId);
				this.sendNotifyRequest(channel, notifyReq, watchEvent, lockClient.getVersion());
				toDeleteEvents.add(watchEvent);
				LOGGER.debug("- lock notify delete...key {}, owner {}.", lockkey, lockClient);
			}
		}
		
		if (!toDeleteEvents.isEmpty()) {
			eventStorage.deleteWatchEvents(lockkey,toDeleteEvents,groupId);
		}
	}
	
	@Override
	public WatchEvent fetchFirstAcquiredWatchEvent(String lockkey, int groupId) {
		if(lockkey == null) {
			LOGGER.error("get acquired watch event, lockkey null.");
			return null;
		}
		
		return this.eventStorage.getFirstAcquiredEvent(lockkey, groupId);
	}
	
	@Override
	public void addWatchEvent(String lockkey, WatchEvent watchEvent, int groupId) {
		if(lockkey == null) {
			LOGGER.error("add watch event, lockkey null.");
			return;
		}
		
		if (watchEvent == null) {
			LOGGER.error("add watch event, watchEvent null.");
			return;
		}
		
		this.eventStorage.addWatchEvent(lockkey, watchEvent, groupId);
	}
	
	@Override
	public void checkWatchEvent() {
		this.eventStorage.checkWatchEvent();
	}
	
	@Override
	public NotifyEvent genNotifyEvent(String lockkey, long watchID, LockClient lockClient, EventType eventType) {
		NotifyEvent notifyEvent = new NotifyEvent();
		notifyEvent.setEventType(eventType.getType());
		notifyEvent.setLockkey(lockkey);
		notifyEvent.setWatchID(watchID);
		
		Optional<ReentrantLockValue> lockvalOp;
		try {
			lockvalOp = lockRepository.getLock(lockkey, lockClient.getGroupId());
			if (!lockvalOp.isPresent()) {
				if (eventType != EventType.LOCK_RELEASE && eventType != EventType.LOCK_EXPIRED
						&& eventType != EventType.READ_LOCK_EXPIRED && eventType != EventType.WRITE_LOCK_EXPIRED) {
					LOGGER.error("lock state unormal, please check.");
					return null;
				}
			}
			if (eventType == EventType.LOCK_RELEASE || eventType == EventType.LOCK_EXPIRED
					|| eventType == EventType.READ_LOCK_EXPIRED || eventType == EventType.WRITE_LOCK_EXPIRED) {
				notifyEvent.setLockOwner(new LockOwner(lockClient.getcHost(), lockClient.getcThreadID(), lockClient.getcPid(), -1));
			}
		} catch (LockException e) {
			LOGGER.error("get lock state failed.", e);
		}
		return notifyEvent;
	}
	
	@Override
	public WatchEvent genWatchEvent(AcquireLockRequest acquireLockRequest, LockClient lockClient, long watchVersion) {
		WatchEvent watchEvent = new WatchEvent();
		watchEvent.setLockType(acquireLockRequest.getLockType());
		watchEvent.setOpcode(acquireLockRequest.getOpcode());
		watchEvent.setExpireTime(acquireLockRequest.getExpireMills());
		watchEvent.setLockClient(lockClient);
		long timeoutStamp = TimeUtil.getCurrentTimestamp() + acquireLockRequest.getTimeout();
		if (timeoutStamp >= Long.MAX_VALUE || timeoutStamp <= 0) {
			watchEvent.setTimeoutStamp(acquireLockRequest.getTimeout());
		} else {
			watchEvent.setTimeoutStamp(timeoutStamp);
		}
		watchEvent.setWatchID(acquireLockRequest.getWatchID());
		watchEvent.setWatchType(WatchType.ACQUIRE);
		watchEvent.setWatchVersion(watchVersion);
		watchEvent.setWeight(acquireLockRequest.getWeight());
		watchEvent.setLastUpateTimestamp(TimeUtil.getCurrentTimestamp());
		watchEvent.setAsync(false);
		return watchEvent;
	}
	
	@Override
	public WatchEvent genWatchEvent(WatchLockRequest watchLockRequest, LockClient lockClient, long watchVersion) {
		WatchEvent watchEvent = new WatchEvent();
		watchEvent.setLockType(watchLockRequest.getLockType());
		watchEvent.setOpcode(watchLockRequest.getOpcode());
		watchEvent.setExpireTime(watchLockRequest.getExpireTime());
		watchEvent.setLockClient(lockClient);
		long timeoutStamp = TimeUtil.getCurrentTimestamp() + watchLockRequest.getTimeout();
		if (timeoutStamp >= Long.MAX_VALUE || timeoutStamp <= 0) {
			watchEvent.setTimeoutStamp(watchLockRequest.getTimeout());
		} else {
			watchEvent.setTimeoutStamp(timeoutStamp);
		}
		watchEvent.setWatchID(watchLockRequest.getWatchID());
		if (watchLockRequest.getEventType() == WatchType.ACQUIRE.getType()) {
			watchEvent.setWatchType(WatchType.ACQUIRE);
		} else if (watchLockRequest.getEventType() == WatchType.WATCH_AND_ACQUIRE.getType()) {
			watchEvent.setWatchType(WatchType.WATCH_AND_ACQUIRE);
		} else {
			watchEvent.setWatchType(WatchType.WATCH);
		}
		
		watchEvent.setWatchVersion(watchVersion);
		watchEvent.setWeight(watchLockRequest.getWeight());
		watchEvent.setLastUpateTimestamp(TimeUtil.getCurrentTimestamp());
		watchEvent.setAsync(true);
		return watchEvent;
	}

	@Override
	public boolean sendNotifyRequest(Channel channel, EventNotifyRequest notifyReq, WatchEvent readyEvent, byte version) {
		if (channel == null || notifyReq == null) {
			LOGGER.error("event notify failed, channel is null.");
			return false;
		}
		if (!channel.isOpen()) {
			LOGGER.error("event notify failed, channel not open . lockkey {}, eventtype {}.", notifyReq.getLockKey(), notifyReq.getEventType());
			return false;
		}
		try {
			final AtomicBoolean sendRes = new AtomicBoolean(true);
			ChannelFuture future = channel.write(ChannelBuffers.copiedBuffer(notifyReq.toBytes(version)));
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						sendRes.set(false);
						LOGGER.error("event notify failed, lockkey {}, eventtype {}, client {}.", notifyReq.getLockKey(), notifyReq.getEventType(), channel.getRemoteAddress());
					}
				}
			});
			return sendRes.get();
		} catch(Throwable th) {
			LOGGER.error("event notify failed, lockkey {}, eventtype {}, client {}.", notifyReq.getLockKey(), notifyReq.getEventType(), channel.getRemoteAddress(), th);
			return false;
		}
	}

	@Override
	public void removeWatchEvent(String key, int groupId, WatchEvent watchEvent) {
		if(key == null) {
			LOGGER.error("remove watch event, lockkey null.");
			return;
		}

		this.eventStorage.deleteWatchEvent(key, watchEvent,groupId);
	}

	@Override
	public List<WatchEvent> getWatchEvents(String lockkey, int groupId) {
		return this.eventStorage.getWatchEvents(lockkey, groupId);
	}

}
