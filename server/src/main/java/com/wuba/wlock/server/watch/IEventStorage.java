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
import com.wuba.wlock.server.expire.event.WatchExpireEvent;

import java.util.List;

public interface IEventStorage {
	/**
	 * 获取所有watch事件
	 * @param lockkey
	 * @return
	 */
	List<WatchEvent> getWatchEvents(String lockkey, int groupId);
	
	/**
	 * 
	 * @param lockkey
	 * @return
	 */
	WatchEvent getFirstAcquiredEvent(String lockkey, int groupId);

	/**
	 * 添加watch事件
	 * @param lockkey
	 * @param watchEvent
	 * @return
	 */
	boolean addWatchEvent(String lockkey, WatchEvent watchEvent, int groupId);

	
	/**
	 * 删除某watch事件
	 * @param lockkey
	 * @param watchEvent
	 * @return
	 */
	boolean deleteWatchEvent(String lockkey, WatchEvent watchEvent, int groupId);
	
	/**
	 * 
	 * @param lockkey
	 * @param watchEventList
	 * @return
	 */
	boolean deleteWatchEvents(String lockkey, List<WatchEvent> watchEventList, int groupId);
	
	/**
	 * 根据lock client删除watch event
	 * @param lockkey
	 * @param clientList
	 * @return
	 */
	boolean deleteWatchEvent(String lockkey, List<LockClient> clientList, int groupId);
	
	/**
	 * watch event alive check
	 */
	void checkWatchEvent();
	
	/**
	 * watch event timeout notify
	 * @param timeoutEvents
	 */
	void notifyTimeoutEvent(List<WatchExpireEvent> timeoutEvents);
	
	/**
	 * 返回当前server所有watch event数量
	 * @return
	 */
	int getWatchEventCount();

}
