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

import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.listener.RenewListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;


public class RenewLockTest {
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

	@Test
	public void testRenew() {
		try {
			wdLock.tryAcquireLock(5000, 8000, 3000, new RenewListener() {
				@Override
				public void onRenewSuccess(String lockkey) {
				}

				@Override
				public void onRenewFailed(String lockkey) {
				}
			}, null);
			Thread.sleep(15000);
			LockResult b = wdLock.releaseLock();
			Assert.assertTrue(b.isSuccess());
		} catch (Exception e) {
		}
	}


	@Test
	public void testAutoRenew() {
		try {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						wdLock.tryAcquireLock(5000, 8000, 3000, new RenewListener() {
							@Override
							public void onRenewSuccess(String lockkey) {
							}

							@Override
							public void onRenewFailed(String lockkey) {
							}
						}, null);
						Thread.sleep(15000);
						LockResult b = wdLock.releaseLock();
						Assert.assertTrue(b.isSuccess());
					} catch (Exception e) {
					}
				}
			});
			Thread thread1 = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
						WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
						AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(5000, 80000, 3000, new RenewListener() {
							@Override
							public void onRenewSuccess(String lockkey) {
							}

							@Override
							public void onRenewFailed(String lockkey) {
							}
						}, null);
						Assert.assertTrue(acquireLockResult.isSuccess());
						Thread.sleep(15000);
						LockResult b = wdLock.releaseLock();
						Assert.assertTrue(b.isSuccess());
					} catch (Exception e) {
					}
				}
			});
			thread.start();
			Thread.sleep(15000);
			thread1.start();
			thread1.join();
		} catch (Exception e) {
		}
	}
}
