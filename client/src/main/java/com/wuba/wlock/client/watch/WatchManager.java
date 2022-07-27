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
import com.wuba.wlock.client.LockManager;
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.communication.Server;
import com.wuba.wlock.client.communication.WatchPolicy;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.ProtocolException;
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.protocol.IProtocolFactory;
import com.wuba.wlock.client.protocol.extend.EventNotifyRequest;
import com.wuba.wlock.client.protocol.extend.EventNotifyResponse;
import com.wuba.wlock.client.protocol.extend.ProtocolFactoryImpl;
import com.wuba.wlock.client.util.ThreadPoolUtil;
import com.wuba.wlock.client.util.ThreadRenameFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ExecutorService;

public class WatchManager {
	private static final Log logger = LogFactory.getLog(WatchManager.class);
	
	private final EventCachedHandler eventCachedHandler;
	private final LockManager lockManager;
	private final WLockClient wlockClient;
	private final IProtocolFactory protocolFactory = ProtocolFactoryImpl.getInstance();
	public static final ExecutorService executor = ThreadPoolUtil.newFixedThreadPool(4, new ThreadRenameFactory("WatchManager"));

	public WatchManager(WLockClient wlockClient, LockManager lockManager) {
		eventCachedHandler = EventCachedHandler.getInstance(wlockClient);
		this.lockManager = lockManager;
		this.wlockClient = wlockClient;
	}
	
	/**
	 * 注册watch事件到本地缓存
	 * @param lockkey
	 * @param watchEvent
	 */
	public void registerWatchEvent(String lockkey, WatchEvent watchEvent) {
		this.eventCachedHandler.registerWatchEvent(lockkey, watchEvent);
	}
	
	/**
	 * 定向事件通知，触发
	 * @param notifyEvent
	 */
	public void eventTrigger(final NotifyEvent notifyEvent) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					WatchEvent watchEvent = eventCachedHandler.getWatchEvent(notifyEvent.getWatchID());
					if (watchEvent != null) {
						// logger.info("eventTrigger : " + notifyEvent.getWatchID() + ", watchEvent : " + watchEvent + ", thd : " + watchEvent.getThreadID());
						boolean isTriggered = false;
						if (watchEvent.getWatchType().getType() == WatchType.WATCH.getType()) {
							watchEvent.setNotifyEvent(notifyEvent);
							watchEvent.countDown();
							
							InternalLockOption lockOption = watchEvent.getLockOption();
							WatchListener watchListener = lockOption.getWatchListener();
							String lockkey = watchEvent.getLockKey();
							if (notifyEvent.getEventType() == EventType.LOCK_UPDATE.getType()) {
								if (watchEvent.getTriggered().compareAndSet(false, true)) {
									isTriggered= true;
									watchListener.onLockChange(lockkey, notifyEvent.getFencingToken());
								}
							} else if (notifyEvent.getEventType() == EventType.LOCK_RELEASE.getType()){
								if (watchEvent.getTriggered().compareAndSet(false, true)) {
									isTriggered = true;
									watchListener.onLockReleased(lockkey);
								}
							} else {
								logger.warn(Version.INFO + ", unkown notify event, type : " + notifyEvent.getEventType());
							}
							
							eventCachedHandler.unRegisterWatchEvent(lockkey, notifyEvent.getWatchID());
						}
						
						if (watchEvent.getWatchType().getType() == WatchType.WATCH_AND_ACQUIRE.getType()) {
							watchEvent.setNotifyEvent(notifyEvent);
							watchEvent.countDown();
							InternalLockOption lockOption = watchEvent.getLockOption();
							WatchListener watchListener = lockOption.getWatchListener();
							String lockkey = watchEvent.getLockKey();
							if (notifyEvent.getEventType() == EventType.LOCK_ACQUIRED.getType()) {
								if (watchEvent.getTriggered().compareAndSet(false, true)) {
									lockManager.updateLockLocal(lockkey, notifyEvent.getFencingToken(), lockOption, true);
									watchListener.onLockAcquired(lockkey);
									// 执行完业务获取锁逻辑,
									if (watchEvent.getLockOption().getWatchPolicy() == WatchPolicy.Continue) {
										// 更新触发状态
										watchEvent.getTriggered().compareAndSet(true, false);
										// 设置最新的锁版本
										watchEvent.getLockOption().setLockversion(notifyEvent.getFencingToken());
										// 重新注册 watch 事件
										reSendWatchEventAsync(watchEvent);
									} else {
										unRegisterWatchEvent(lockkey, watchEvent.getWatchID());
									}
								}
								return;
							} else if (notifyEvent.getEventType() == EventType.LOCK_UPDATE.getType()) {
								watchEvent.getLockOption().setLockversion(notifyEvent.getFencingToken());
								logger.info("TEST ---- eventTrigger WATCH_AND_ACQUIRE LOCK_UPDATE version : " + notifyEvent.getFencingToken());
							} else {
								logger.warn(Version.INFO + " unkown notify event, type : " + notifyEvent.getEventType());
							}
							
							if (!isTriggered) {
								reSendWatchEventAsync(watchEvent);
							}
						}
						
						if (watchEvent.getWatchType().getType() == WatchType.ACQUIRE.getType()) {
							if (notifyEvent.getEventType() == EventType.LOCK_ACQUIRED.getType()) {
								if (watchEvent.getTriggered().compareAndSet(false, true)) {
									watchEvent.setNotifyEvent(notifyEvent);
									watchEvent.countDown();
								}
								return;
							} else if (notifyEvent.getEventType() == EventType.LOCK_UPDATE.getType()) {
								watchEvent.getLockOption().setLockversion(notifyEvent.getFencingToken());
							} else {
								logger.warn(Version.INFO + " unkown notify event, type : " + notifyEvent.getEventType());
							}
							
							if (!isTriggered) {
								reSendWatchEventAsync(watchEvent);
							}
						}
					}
				} catch (Throwable th) {
					logger.error(Version.INFO + " eventTrigger error.", th);
				}
			}
			
		});
	}
	
	/**
	 * 等待notify事件通知
	 * @param watchID
	 * @param timeout
	 * @return
	 */
	public NotifyEvent waitNotifyEvent(long watchID, long timeout) {
		WatchEvent wet = this.eventCachedHandler.getWatchEvent(watchID);
		if (wet == null) {
			logger.error(Version.INFO + ",watchevent has been notified, watchID : " + watchID);
			return null;
		}
		
		try {
			if (!wet.getEvent().waitOne(timeout)) {
				logger.error(Version.INFO + ",waitTriggerEvent timeout, watchID " + watchID + ", timeout : " + timeout);
				return null;
			}
		} catch (InterruptedException e) {
			logger.error("", e);
			return null;
		}
		
		if (wet.isCanceled()) {
			logger.error(Version.INFO + ",waitTriggerEvent has been canceled, watchID " + watchID + ", timeout : " + timeout);
			return null;
		}
		
		return wet.getNotifyEvent();
	}
	
	public WatchEvent getWatchEvent(long watchID) {
		return this.eventCachedHandler.getWatchEvent(watchID);
	}
	
	/**
	 * 取消注册watch事件
	 * @param lockkey
	 * @param watchID
	 * @return
	 */
	public WatchEvent unRegisterWatchEvent(String lockkey, long watchID) {
		return this.eventCachedHandler.unRegisterWatchEvent(lockkey, watchID);
	}
	
	/**
	 * 服务端事件通知
	 * @param databuf
	 */
	public void notifyWatchEvent(byte[] databuf) {
		EventNotifyRequest notifyReq = new EventNotifyRequest();
		try {
			notifyReq.fromBytes(databuf);
			int eventType = notifyReq.getEventType();
			NotifyEvent notifyEvent = new NotifyEvent();
			notifyEvent.setEventType(eventType);
			notifyEvent.setFencingToken(notifyReq.getFencingToken());
			notifyEvent.setLockkey(notifyReq.getLockKey());
			notifyEvent.setWatchID(notifyReq.getWatchID());
			notifyEvent.setThreadID(notifyReq.getThreadID());
			if (notifyEvent.getWatchID() < 0) {
				// broadcast event
				this.lockManager.dealBroadcastEvent(notifyEvent);
			} else {
				eventTrigger(notifyEvent);
			}
		} catch (ProtocolException e) {
			logger.error(Version.INFO + ", parse event notify request failed.", e);
		}
	}
	
	public void sendNotifyResponse(final EventNotifyRequest notifyReq, final Server server) {
		final int defaultTimeout = 500;
		executor.execute(new Runnable() {

			@Override
			public void run() {
				EventNotifyResponse notifyResp = protocolFactory.createEventNotifyResponse(notifyReq);
				try {
					server.syncInvoke(notifyResp, defaultTimeout);
				} catch (Exception e) {
					logger.error(Version.INFO + ", send event notify response error.", e);
				} 
			}
			
		});
	}
	
	/**
	 * 异步重新发送watch事件
	 * @param watchEvent
	 */
	public void reSendWatchEventAsync(final WatchEvent watchEvent) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				eventCachedHandler.reSendWatchEvent(watchEvent);
			}
		});
	}

	public WLockClient getWlockClient() {
		return wlockClient;
	}
}
