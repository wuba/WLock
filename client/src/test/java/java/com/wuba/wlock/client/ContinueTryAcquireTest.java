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
package java.com.wuba.wlock.client;

import com.wuba.wlock.client.LockOption;
import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.listener.HoldLockListener;
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;


public class ContinueTryAcquireTest {

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
	 * 持有锁超过 5min 必须设置锁过期回调
	 *
	 * @throws Exception
	 */
	@Test(expected = ParameterIllegalException.class)
	public void testContinueTryAcquireMustSetCallBack() throws Exception {
		AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(10000000, 5000, 2000);
	}

	/**
	 * 持有锁超过 5min 测试
	 *
	 * @throws Exception
	 */
	@Test
	public void tryAcquireLockUnblockedTest() throws Exception {
		AcquireLockResult acquireLockResult = wdLock.tryAcquireLockUnblocked(6 * 60 * 1000, new HoldLockListener() {
			@Override
			public void onOwnerChange(String lockKey, String type) {
			}

		});
		Assert.assertTrue(acquireLockResult.isSuccess());
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					AcquireLockResult lockResult = wdLock.tryAcquireLock(5000, 5000);
					Assert.assertFalse(lockResult.isSuccess());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		thread.join();
	}

	/**
	 * A 持续获取锁 ,B watch 锁,验证获取锁版本是否正确
	 *
	 * @throws Exception
	 */
	@Test
	public void tryContinueAcquireAndWatchLockVersionTest() throws Exception {
		AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(360000, new HoldLockListener() {
			@Override
			public void onOwnerChange(String lockKey, String type) {
			}
		});
		Assert.assertTrue(acquireLockResult.isSuccess());

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					LockOption lockOption = new LockOption();
					lockOption.setExpireTime(420000);
					lockOption.setHoldLockListener(new HoldLockListener() {
						@Override
						public void onOwnerChange(String lockKey, String type) {
						}
					});
					lockOption.setWaitAcquire(true);
					lockOption.setWatchListener(new WatchListener() {
						@Override
						public void onLockChange(String lockkey, long lockversion) {
							Assert.assertEquals(lockversion, acquireLockResult.getLockVersion());
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
					wdLock.watchlock(-1, lockOption);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		thread.join();
	}


}
