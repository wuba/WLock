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
package com.wuba.wlock.client;

import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import org.junit.Assert;
import org.junit.Before;

import java.util.Random;


public class ReleaseLockTest {

	WLockClient wLockClient;
	WDistributedLock wdLock;
	String lock;

	@Before
	public void init() {
		try {
			wLockClient = new WLockClient("test123_8", "127.0.0.1", 22020);
			Random random = new Random();
			lock = random.nextInt(10000) + "test_key_";
			wdLock = wLockClient.newDistributeLock(lock);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 不存在，返回失败
	 */
	@org.junit.Test
	public void testReleaseLockVersionError() {
		try {
			LockResult releaseLock = wdLock.releaseLock();
			Assert.assertFalse(releaseLock.isSuccess());
		} catch (Exception e) {
		}
	}

	/**
	 * 锁存在，版本号不一致
	 */
	@org.junit.Test
	public void testReleaseLock() {
		try {
			AcquireLockResult lockResult0 = wdLock.tryAcquireLock(5000, 10000);
			Assert.assertTrue(lockResult0.isSuccess());
			long version = lockResult0.getLockVersion();
			Assert.assertFalse(wdLock.releaseLock().isSuccess());
			Assert.assertTrue(wdLock.releaseLock().isSuccess());
		} catch (Exception e) {
		}
	}

	/**
	 * 存在，自己不是owner
	 */
	@org.junit.Test
	public void testReleaseLockOwnerOther() {
		try {
			final long[] version = {0L};
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult lockResult = wdLock.tryAcquireLock(10000, 8000);
						Assert.assertTrue(lockResult.isSuccess());
						version[0] = lockResult.getLockVersion();
						try {
							Thread.sleep(9000);
						} catch (InterruptedException e) {
						}
						Assert.assertTrue(wdLock.releaseLock().isSuccess());
					} catch (ParameterIllegalException e) {
					}
				}
			});

			Thread thread1 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						Assert.assertFalse(wdLock.releaseLock().isSuccess());
					} catch (ParameterIllegalException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();
			Thread.sleep(2000);
			thread1.start();
			thread1.join();
		} catch (Exception e) {
		}


	}


	/**
	 * 存在，成功释放锁
	 */
	@org.junit.Test
	public void testReleaseLockNormal() {
		try {
			AcquireLockResult lockResult = wdLock.tryAcquireLock(5000, 7000);
			Assert.assertTrue(lockResult.isSuccess());
			long lockVersion = lockResult.getLockVersion();
			Assert.assertTrue(wdLock.releaseLock().isSuccess());
		} catch (Exception e) {
		}
	}
}
