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
import com.wuba.wlock.client.listener.LockExpireListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;


public class AcquireLockTest {

	WLockClient wLockClient;
	WDistributedLock wdLock;
	String lock;

	@Before
	public void init() {
		try {
			wLockClient = new WLockClient("B7BB4E1E8BD1C78C5C611C76F27ACDD7", "127.0.0.1", 22020);
			Random random = new Random();
			lock = random.nextInt(10000) + "test_key_";
			wdLock = wLockClient.newDistributeLock(lock);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 不存在，直接获取锁
	 */
	@Test
	public void testGetLock() throws Exception {
		try {
			WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
			AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(5000, 5000);
			Assert.assertTrue(acquireLockResult.isSuccess());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * 存在，自己是owner
	 */
	@Test
	public void testOwnerGetLock() {
		try {
			AcquireLockResult lockResult0 = wdLock.tryAcquireLock(5000, 10000);
			Assert.assertTrue(lockResult0.isSuccess());
			long version = lockResult0.getLockVersion();
			AcquireLockResult lockResult1 = wdLock.tryAcquireLock(5000, 10000);
			Assert.assertTrue(lockResult1.isSuccess());
			Assert.assertEquals(lockResult0.getLockVersion(), lockResult1.getLockVersion());
			Assert.assertTrue(wdLock.releaseLock().isSuccess());
		} catch (Exception e) {
		}
	}

	/**
	 * 存在，自己不是owner，阻塞与非阻塞
	 */
	@Test
	public void testNoOwnerGetLock() {
		try {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult lockResult = wdLock.tryAcquireLock(10000, 8000);
						Assert.assertTrue(lockResult.isSuccess());
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
						AcquireLockResult lockResult = wdLock.tryAcquireLockUnblocked(5000);
						Assert.assertFalse(lockResult.isSuccess());
						AcquireLockResult lockResult1 = wdLock.tryAcquireLock(6000, 15000);
						Assert.assertTrue(lockResult1.isSuccess());
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
						}
						Assert.assertTrue(wdLock.releaseLock().isSuccess());
					} catch (ParameterIllegalException e) {
					}
				}
			});
			thread.start();
			Thread.sleep(1000);
			thread1.start();
			Thread.sleep(15000);
		} catch (Exception e) {
		}


	}


	/**
	 * 存在，优先级高的获取
	 */
	@Test
	public void testGetLockWithWeight() {
		try {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult lockResult = wdLock.tryAcquireLock(10000, 8000);
						Assert.assertTrue(lockResult.isSuccess());
						Thread.sleep(9000);
						Assert.assertTrue(wdLock.releaseLock().isSuccess());
					} catch (Exception e) {
					}
				}
			});

			Thread thread1 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult lockResult1 = wdLock.tryAcquireLock(15000, 15000, 10, null);
						Assert.assertTrue(lockResult1.isSuccess());
						Thread.sleep(13000);
						Assert.assertTrue(wdLock.releaseLock().isSuccess());
					} catch (Exception e) {
					}
				}
			});

			Thread thread2 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult lockResult1 = wdLock.tryAcquireLock(15000, 15000, 5, null);
						Assert.assertFalse(lockResult1.isSuccess());
						Thread.sleep(3000);
					} catch (Exception e) {
					}
				}
			});
			thread.start();
			thread1.start();
			thread2.start();

			thread.join();
			thread1.join();
			thread2.join();
		} catch (Exception e) {
		}
	}


	@Test
	public void testAThreadAcquireExpireBThreadAcquire() {
		try {
			final WLockClient wLockClient = new WLockClient("/Users/chengzheyan/idea/wlock_read_write/client/src/test/java/liudan_test.wlockkey");
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult lockResult = wdLock.tryAcquireLock(5000, 10000, new LockExpireListener() {
							@Override
							public void onExpire(String key) {
								Assert.assertEquals(key, key);
							}
						});
						Assert.assertTrue(lockResult.isSuccess());
					} catch (ParameterIllegalException e) {
					}
				}
			});
			Thread thread1 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(3000, 20000);
						Assert.assertTrue(acquireLockResult.isSuccess());
						Thread.sleep(2000);
						LockResult b = wdLock.releaseLock();
						Assert.assertTrue(b.isSuccess());
					} catch (Exception e) {
					}
				}
			});

			thread.start();
			Thread.sleep(6000);
			thread1.start();
			thread1.join();
		} catch (Exception e) {
		}

	}

	/**
	 * 锁重入测试
	 * @throws Exception
	 */
	@Test
	public void testRetryLock() throws Exception {
		AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(50000, 5000);
		Assert.assertTrue(acquireLockResult.isSuccess());

		acquireLockResult = wdLock.tryAcquireLock(50000, 5000);
		Assert.assertTrue(acquireLockResult.isSuccess());

		acquireLockResult = wdLock.tryAcquireLock(50000, 5000);
		Assert.assertTrue(acquireLockResult.isSuccess());

		acquireLockResult = wdLock.tryAcquireLock(50000, 5000);
		Assert.assertTrue(acquireLockResult.isSuccess());
	}


}
