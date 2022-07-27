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

import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.communication.LockPolicy;
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;

import java.util.Random;

/**
 * 进程级锁
 */
public class ProcessLockDemo {
	static WLockClient wLockClient;
	static Random random = new Random();

	public static void main(String[] args) throws Exception {
		// 初始化client
		init();

		// 进程锁操作
		processAcquireLock();

		// 监听进程锁状态
		continueWatchAndWaitLock();
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
	 * 进程锁操作
	 */
	public static void processAcquireLock() throws Exception {
		try {
			// 创建进程锁 LockPolicy.Process
			WDistributedLock wdLock = wLockClient.newDistributeLock(lockKey(), LockPolicy.Process);
			// 获取锁
			AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(5000, 5000, 2000);

			// 释放进程锁
			wdLock.releaseLock();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 监听进程锁状态
	 */
	public static void continueWatchAndWaitLock() throws Exception {
		WDistributedLock wdLock = wLockClient.newDistributeLock(lockKey(), LockPolicy.Process);
		wdLock.continueWatchAndWaitLock(-1, 1, 2000, new WatchListener() {
			@Override
			public void onLockChange(String lockkey, long lockversion) {
			}

			@Override
			public void onLockReleased(String lockkey) {
			}

			@Override
			public void onLockAcquired(String lockkey) {
			}

			@Override
			public void onTimeout(String lockkey) {
			}
		});
	}

}
