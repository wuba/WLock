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
package com.wuba.wlock.server.watch;

import com.wuba.wlock.server.client.LockClient;
import com.wuba.wlock.server.communicate.protocol.AcquireLockRequest;
import com.wuba.wlock.server.communicate.protocol.EventNotifyRequest;
import com.wuba.wlock.server.communicate.protocol.WatchLockRequest;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.expire.event.WatchExpireEvent;
import org.jboss.netty.channel.Channel;

import java.util.List;

public interface IWatchService {
	
	/**
	 * 连接关闭事件触发
	 * @param lockkey
	 */
	void channelClosedTrigger(String lockkey, List<LockClient> removedClients);

	/**
	 * 迁移结束触发
	 */
	void migrateEndTrigger(String lockkey, List<LockClient> removedClients);

	/**
	 * 锁状态变化，事件触发
	 * @param lockkey
	 */
	void lockUpdateTrigger(String lockkey, LockOwner newLockOwner, int groupId);
	
	/**
	 * 锁状态变化，事件触发(不判断是否获取到锁)
	 * @param lockkey
	 */
	void lockUpdateTrigger2(String lockkey, LockOwner newLockOwner, int groupId);
	
	/**
	 * 锁过期，事件触发
	 * @param lockkey
	 * @param lockClient
	 * 说明：只需要向owner
	 */
	void lockExpiredTrigger(String lockkey, LockClient lockClient, int groupId, EventType eventType);
	
	/**
	 * 锁删除，事件触发
	 * @param lockkey
	 */
	void lockDeleteTrigger(String lockkey, int groupId);
	
	/**
	 * 添加watch事件
	 * @param lockkey
	 * @param watchEvent
	 */
	void addWatchEvent(String lockkey, WatchEvent watchEvent, int groupId);
	
	/**
	 * 检查watch事件是否不再活跃
	 */
	void checkWatchEvent();
	
	/**
	 * watch事件超时触发
	 * @param timeoutEvents
	 */
	void timeoutTrigger(final List<WatchExpireEvent> timeoutEvents);
	
	/**
	 * 获取第一个等待夺取锁的watchevent
	 * @param lockkey
	 * @return
	 */
	WatchEvent fetchFirstAcquiredWatchEvent(String lockkey, int groupId);
	
	/**
	 * 生成通知事件
	 * @param lockClient
	 * @param eventType
	 */
	NotifyEvent genNotifyEvent(String lockkey, long watchID, LockClient lockClient, EventType eventType);
	
	/**
	 * acquirerequest生成watchevent
	 * @param acquireLockRequest
	 * @param lockClient
	 * @param watchVersion
	 * @return
	 */
	WatchEvent genWatchEvent(AcquireLockRequest acquireLockRequest, LockClient lockClient, long watchVersion);
	
	/**
	 * 
	 * @param lockClient
	 * @param watchVersion
	 * @return
	 */
	WatchEvent genWatchEvent(WatchLockRequest watchLockRequest, LockClient lockClient, long watchVersion);
	
	/**
	 * 发送事件通知请求
	 * @param channel
	 */
	boolean sendNotifyRequest(Channel channel, EventNotifyRequest notifyReq, WatchEvent readyEvent, byte version);

	/**
	 * 删除指定watchevent
	 * @param key
	 * @param groupId
	 * @param watchEvent
	 */
	void removeWatchEvent(String key, int groupId, WatchEvent watchEvent);

	List<WatchEvent> getWatchEvents(String lockkey, int groupId);
}
