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
package com.wuba.wlock.server.lock.service.base;

import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.watch.EventType;

public interface ILockNotify {
	/**
	 * 通知锁过期
	 * @param key
	 * @param lockOwner
	 * @param groupIdx
	 */
	void lockNotifyExpired(String key, LockOwner lockOwner, int groupIdx, EventType eventType);

	/**
	 * 通知锁过期
	 * @param key
	 * @param lockOwner
	 * @param groupIdx
	 */
	void lockNotifyExpired(String key, LockOwner lockOwner, int groupIdx);
	/**
	 * 通知锁删除
	 * @param key
	 * @param groupIdx
	 */
	void lockNotifyDelete(String key, int groupIdx);
	/**
	 * 通知锁更新
	 * @param key
	 * @param lockOwner
	 * @param groupIdx
	 */
	void lockNotifyUpdate(String key, LockOwner lockOwner, int groupIdx);
	/**
	 * 通知锁更新（不获取锁）
	 * @param key
	 * @param lockOwner
	 * @param groupIdx
	 */
	void lockNotifyUpdate2(String key, LockOwner lockOwner, int groupIdx);
}
