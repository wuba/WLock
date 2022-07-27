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
package com.wuba.wlock.client.listener;

public interface WatchListener {
	/**
	 * 锁owner发生变化
	 * @param lockkey
	 */
	void onLockChange(String lockkey, long lockversion);
	
	/**
	 * 锁被释放
	 * @param lockkey
	 */
	void onLockReleased(String lockkey);
	
	/**
	 * 锁被获取到
	 * @param lockkey
	 */
	void onLockAcquired(String lockkey);
	
	/**
	 * watch事件过期回调
	 * @param lockkey
	 */
	void onTimeout(String lockkey);
}
