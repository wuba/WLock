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
package com.wuba.wlock.server.lock.repository.base;

import com.wuba.wlock.server.exception.LockException;
import com.wuba.wlock.server.lock.protocol.ReentrantLockValue;

import java.util.Optional;


public interface ILockRepository {
	/**
	 * 获取锁信息
	 *
	 * @param key
	 * @return
	 */
	Optional<ReentrantLockValue> getLock(String key, int groupId) throws LockException;

	/**
	 * 删除锁
	 *
	 * @param key
	 * @return
	 */
	void deleteLock(String key,int groupId) throws LockException;

	/**
	 * 加锁
	 *
	 * @param reentrantLockValue
	 * @return
	 */
	void lock(String key, ReentrantLockValue reentrantLockValue,int groupId) throws LockException;

	/**
	 * 修改锁
	 */
	void update(String key, ReentrantLockValue reentrantLockValue,int groupId) throws LockException;

	/**
	 * 续约
	 *
	 * @param reentrantLockValue
	 * @return
	 */
	void renew(String key, ReentrantLockValue reentrantLockValue,int groupId) throws LockException;

	/**
	 * 释放锁
	 * @param key
	 * @param reentrantLockValue
	 * @throws LockException
	 */
	void release(String key, ReentrantLockValue reentrantLockValue,int groupId) throws LockException;
	
}
