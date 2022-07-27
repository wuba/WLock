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
package com.wuba.wlock.server.lock.service;

import com.wuba.wlock.server.client.ClientManager;
import com.wuba.wlock.server.client.LockClient;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.lock.service.base.ILockNotify;
import com.wuba.wlock.server.watch.EventType;
import com.wuba.wlock.server.watch.IWatchService;
import com.wuba.wlock.server.watch.impl.WatchServiceImpl;
import com.wuba.wlock.server.wpaxos.WpaxosService;

public class LockNotify implements ILockNotify {
	IWatchService watchService = WatchServiceImpl.getInstance();
	WpaxosService wpaxosService = WpaxosService.getInstance();
	private static LockNotify instance = new LockNotify();
	
	private LockNotify() {}
	
	public static LockNotify getInstance() {
		return instance;
	}

	@Override
	public void lockNotifyDelete(String key, int groupIdx) {
		if (!wpaxosService.isIMMaster(groupIdx)) {
			return;
		}
		
		watchService.lockDeleteTrigger(key, groupIdx);
	}

	@Override
	public void lockNotifyExpired(String key, LockOwner lockOwner, int groupIdx, EventType eventType) {
		if (!wpaxosService.isIMMaster(groupIdx)) {
			return;
		}
		
		LockClient lockOwnerClient = ClientManager.getInstance().getLockOwnerClient(key, lockOwner, groupIdx);
		watchService.lockExpiredTrigger(key, lockOwnerClient, groupIdx, eventType);
	}

	@Override
	public void lockNotifyExpired(String key, LockOwner lockOwner, int groupIdx) {
		lockNotifyExpired(key, lockOwner, groupIdx, EventType.LOCK_EXPIRED);
	}

	@Override
	public void lockNotifyUpdate(String key, LockOwner lockOwner, int groupIdx) {
		if (!wpaxosService.isIMMaster(groupIdx)) {
			return;
		}
		
		watchService.lockUpdateTrigger(key, lockOwner, groupIdx);
	}
	
	@Override
	public void lockNotifyUpdate2(String key, LockOwner lockOwner, int groupIdx) {
		if (!wpaxosService.isIMMaster(groupIdx)) {
			return;
		}
		
		watchService.lockUpdateTrigger2(key, lockOwner, groupIdx);
	}
}
