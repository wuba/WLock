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

import com.wuba.wlock.server.expire.ExpireManager;
import com.wuba.wlock.server.expire.event.ExpireEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueAllExpireManager extends ExpireManager {

	private static Logger logger = LoggerFactory.getLogger(QueueAllExpireManager.class);
	
	private volatile static ExpireManager instance = null;

	private ExpireQueueAllDispatcher[] expireQueueDispatcherArray;
	
	public static ExpireManager getInstance() {
		if (null == instance) {
			synchronized (QueueAllExpireManager.class) {
				if (null == instance) {
					instance = new QueueAllExpireManager();
				}
			}
		}
		return instance;
	}

	private QueueAllExpireManager() {
		int groupCount = wpaxosService.getOptions().getGroupCount();
		expireQueueDispatcherArray = initArray(groupCount);
	}

	private ExpireQueueAllDispatcher[] initArray(int groupCount) {
		ExpireQueueAllDispatcher[] expireQueueDispatcherArray = new ExpireQueueAllDispatcher[groupCount];
		for (int i = 0; i < groupCount; i++) {
			expireQueueDispatcherArray[i] = new ExpireQueueAllDispatcher(i, this);
		}
		return expireQueueDispatcherArray;
	}

	@Override
	public void start() {
		for (ExpireQueueAllDispatcher expireQueueAllDispatcher : expireQueueDispatcherArray) {
			expireQueueAllDispatcher.start();
		}
		logger.info("PriorityQueueAll ExpireEvent processor start!");
	}

	@Override
	public void stop() {
		for (ExpireQueueAllDispatcher expireQueueAllDispatcher : expireQueueDispatcherArray) {
			expireQueueAllDispatcher.stop();
		}
		logger.info("PriorityQueueAll ExpireEvent processor stop!");
	}

	@Override
	public void resume(int groupId) {
		if (wpaxosService.isIMMaster(groupId)) {
			expireQueueDispatcherArray[groupId].resume();
		}
	}

	@Override
	public void pause(int groupId) {
		expireQueueDispatcherArray[groupId].pause();
	}

	@Override
	public void addExpireEvent(ExpireEvent expireEvent) {
		int groupId = expireEvent.getGroupId();
		expireQueueDispatcherArray[groupId].offer(expireEvent);
		//logger.info("add expireEvent to PriorityQueueAll groupId {}, expireTimestamp is {};", groupId, expireEvent.getExpireTimestamp());
	}

	@Override
	public void learnMaster(int groupId) {
		if (wpaxosService.isIMMaster(groupId)) {
			expireQueueDispatcherArray[groupId].learnMaster();
		}
	}
	
	public ExpireQueueAllDispatcher[] getExpireQueueDispatcherArray() {
		return expireQueueDispatcherArray;
	}

	public void setExpireQueueDispatcherArray(ExpireQueueAllDispatcher[] expireQueueDispatcherArray) {
		this.expireQueueDispatcherArray = expireQueueDispatcherArray;
	}
	
	public ExpireQueueAllDispatcher getExpireQueueAllDispatcherByGroupId(int groupId) {
		return this.expireQueueDispatcherArray[groupId];
	}

}
