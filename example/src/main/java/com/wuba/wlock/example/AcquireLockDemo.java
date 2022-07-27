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
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;

import java.util.Random;

/**
 * 获取锁
 */
public class AcquireLockDemo {

	static WLockClient wLockClient;
	static Random random = new Random();

	public static void main(String[] args) throws Exception {
		// 初始化client
		init();

		// 阻塞方式获取锁
		tryAcquireLock();

		// 非阻塞方式获取锁
		tryAcquireLockUnblocked();

		// 监听锁状态
		watchlock();
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
	 * 阻塞方式获取锁
	 */
	public static void tryAcquireLock() throws Exception {
		WDistributedLock wdLock = null;
		try {
			wdLock = wLockClient.newDistributeLock(lockKey());
			// 获取锁
			AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(5000, 5000);
			System.out.println(acquireLockResult.toString());
			Thread.sleep(1000);
		} finally {
			// 释放锁
			if (wdLock != null) {
				wdLock.releaseLock();
			}
		}

	}

	/**
	 * 非阻塞方式获取锁
	 */
	public static void tryAcquireLockUnblocked() throws Exception {
		WDistributedLock wdLock = null;
		try {
			wdLock = wLockClient.newDistributeLock(lockKey());
			// 获取锁
			AcquireLockResult acquireLockResult = wdLock.tryAcquireLockUnblocked(5000);
			System.out.println(acquireLockResult.toString());
			Thread.sleep(1000);
		} finally {
			// 释放锁
			if (wdLock != null) {
				wdLock.releaseLock();
			}
		}
	}

	/**
	 * 监听锁状态
	 */
	public static void watchlock() throws Exception {
		WDistributedLock wdLock = wLockClient.newDistributeLock(lockKey());
		// 获取锁
		AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(5000, 5000);
		wdLock.watchlock(acquireLockResult.getLockVersion(), new WatchListener() {
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
