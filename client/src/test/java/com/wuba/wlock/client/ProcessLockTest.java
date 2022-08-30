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
import com.wuba.wlock.client.communication.LockPolicy;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.listener.LockExpireListener;
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;


public class ProcessLockTest {

	WLockClient wLockClient;
	WDistributedLock wdLock;
	String lock;

	@Before
	public void init() {
		try {
			wLockClient = new WLockClient("test123_8", "127.0.0.1", 22020);
			Random random = new Random();
			lock = random.nextInt(10000) + "test_key_";
			wdLock = wLockClient.newDistributeLock(lock, LockPolicy.Process);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A 进程 a 线程获取锁 ,续约锁 ,释放锁
	 *
	 * @throws Exception
	 */
	@Test
	public void testProcessAcquireLock() throws Exception {
		try {
			AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(5000, 5000, 2000);
			Assert.assertTrue(acquireLockResult.isSuccess());
			LockResult lockResult = wdLock.renewLock(5000);
			Assert.assertTrue(lockResult.isSuccess());
			LockResult lockResult1 = wdLock.releaseLock();
			Assert.assertTrue(lockResult1.isSuccess());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * a 线程获取进程锁,b 线程续约进程锁 , c 线程释放进程锁
	 *
	 * @throws Exception
	 */
	@Test
	public void testProcessLockAThreadGetBThreadRenewCThreadRelease() throws Exception {
		final List<Long> tmp = new CopyOnWriteArrayList<Long>();
		Thread aThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(5000, 5000, 2000);
					tmp.add(acquireLockResult.getLockVersion());
					Assert.assertTrue(acquireLockResult.isSuccess());
					Thread.sleep(5000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread bThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
					long version = tmp.get(0);
					LockResult lockResult = wdLock.renewLock(5000);
					Assert.assertTrue(lockResult.isSuccess());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread cThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					LockResult lockResult = wdLock.releaseLock();
					Assert.assertTrue(lockResult.isSuccess());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		aThread.start();
		bThread.start();
		cThread.start();

		aThread.join();
		bThread.join();
		cThread.join();
	}


	/**
	 * 测试同一个 lock key 获取进程锁和线程锁
	 *
	 * @throws Exception
	 */
	@Test
	public void testAcquireProcessLockAndAcquireThreadLock() throws Exception {
		final WDistributedLock wdLock = wLockClient.newDistributeLock(lock, LockPolicy.Process);
		AcquireLockResult acquireLockResult = wdLock.tryAcquireLock(10000, 5000);
		Assert.assertTrue(acquireLockResult.isSuccess());
		final WDistributedLock wdLock2 = wLockClient.newDistributeLock(lock, LockPolicy.Thread);
		acquireLockResult = wdLock2.tryAcquireLock(10000, 5000, new LockExpireListener() {
			@Override
			public void onExpire(String lockkey) {
			}
		});
		Assert.assertFalse(acquireLockResult.isSuccess());
		LockResult lockResult = wdLock2.releaseLock();
		Assert.assertFalse(lockResult.isSuccess());
	}

	@Test
	public void testContinueWatch() throws Exception {
		wdLock.continueWatchAndWaitLock(-1, 1, 1200000, new WatchListener() {
			@Override
			public void onLockChange(String lockkey, long lockversion) {
				System.out.println("lock change");
			}

			@Override
			public void onLockReleased(String lockkey) {
				System.out.println("onLockReleased");
			}

			@Override
			public void onLockAcquired(String lockkey) {

				System.out.println(Calendar.getInstance().getTime() + " : onLockAcquired");
				try {
					LockResult lockResult = wdLock.releaseLock();
					Assert.assertTrue(lockResult.isSuccess());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onTimeout(String lockkey) {
				System.out.println("onTimeout");
			}
		});
	}


	/**
	 * 持有锁超过 5min 必须设置锁过期回调
	 *
	 * @throws Exception
	 */
	@Test(expected = ParameterIllegalException.class)
	public void testContinueWatchMustUseProcessLock() throws Exception {
		WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
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
