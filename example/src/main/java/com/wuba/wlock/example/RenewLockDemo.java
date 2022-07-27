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
import com.wuba.wlock.client.listener.RenewListener;

import java.util.Random;

/**
 * 续约锁
 */
public class RenewLockDemo {
	static WLockClient wLockClient;
	static Random random = new Random();

	public static void main(String[] args) throws Exception {
		// 初始化client
		init();

		// 自动续约锁
		autoRenewLock();
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
	 * 锁自动续约，密钥配置ClientKeyEntity字段autoRenew设置自动续约；renewInterval 续约间隔；RenewListener 续约监听器；
	 */
	public static void autoRenewLock() throws Exception{

		WDistributedLock wdLock = null;
		try {
			wdLock = wLockClient.newDistributeLock(lockKey());
			wdLock.tryAcquireLock(15000, 8000, 3000, new RenewListener() {
				@Override
				public void onRenewSuccess(String lockkey) {
				}

				@Override
				public void onRenewFailed(String lockkey) {
				}
			}, null);
			Thread.sleep(15000);
		} finally {

		}
	}
}
