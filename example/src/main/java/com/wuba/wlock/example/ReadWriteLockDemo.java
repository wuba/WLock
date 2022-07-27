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
package com.wuba.wlock.example;

import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.WReadLock;
import com.wuba.wlock.client.WReadWriteLock;
import com.wuba.wlock.client.WWriteLock;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.lockresult.AcquireLockResult;

import java.util.Random;


/**
 * 读写锁
 */
public class ReadWriteLockDemo {
	static WLockClient wLockClient;
	static Random random = new Random();

	public static void main(String[] args) throws Exception {
		// 初始化client
		init();

		// 阻塞方式获取读锁
		readAcquireLock();

		// 非阻塞方式获取读锁
		readAcquireLockUnblocked();

		// 阻塞方式获取写锁
		writeAcquireLock();

		// 非阻塞方式获取写锁
		writeAcquireLockUnblocked();
	}

	public static void init() {
		try {
			wLockClient = new WLockClient("test123_8", "127.0.0.1", 22020);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String lockKey() {
		return "test_key_" + random.nextInt(10000);
	}

	/**
	 * 获取读锁，阻塞方式
	 */
    public static void readAcquireLock() throws ParameterIllegalException {
    	// 创建读写锁
		WReadWriteLock wReadWriteLock = wLockClient.newReadWriteLock(lockKey());
		WReadLock readLock = wReadWriteLock.readLock();

		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);

		readLock.releaseLock();
    }

	/**
	 * 获取读锁，非阻塞方式
	 */
    public static void readAcquireLockUnblocked() throws ParameterIllegalException {
		// 创建读写锁
		WReadWriteLock wReadWriteLock = wLockClient.newReadWriteLock(lockKey());
		WReadLock readLock = wReadWriteLock.readLock();

		AcquireLockResult acquireLockResult = readLock.tryAcquireLockUnblocked(1000 * 60);

		readLock.releaseLock();
    }

	/**
	 * 获取写锁，阻塞方式
	 */
	public static void writeAcquireLock() throws ParameterIllegalException {
		// 创建读写锁
		WReadWriteLock wReadWriteLock = wLockClient.newReadWriteLock(lockKey());
		// 拿到读锁
		WWriteLock writeLock = wReadWriteLock.writeLock();

		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);

		writeLock.releaseLock();
	}

	/**
	 * 获取写锁，非阻塞方式
	 */
	public static void writeAcquireLockUnblocked() throws ParameterIllegalException {
		// 创建读写锁
		WReadWriteLock wReadWriteLock = wLockClient.newReadWriteLock(lockKey());
		// 拿到写锁
		WWriteLock writeLock = wReadWriteLock.writeLock();

		AcquireLockResult acquireLockResult = writeLock.tryAcquireLockUnblocked(1000 * 60);

		writeLock.releaseLock();
	}
}
