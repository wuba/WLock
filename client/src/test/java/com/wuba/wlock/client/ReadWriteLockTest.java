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

import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.WReadLock;
import com.wuba.wlock.client.WReadWriteLock;
import com.wuba.wlock.client.WWriteLock;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReadWriteLockTest {
	ExecutorService threadPool = Executors.newFixedThreadPool(10);

    WLockClient wlockClient;
    WReadLock readLock;
    WWriteLock writeLock;

    @Before
    public void init() {
        try {
            wlockClient= new WLockClient("test123_8", "127.0.0.1", 22020);
            Random random = new Random();
            WReadWriteLock wReadWriteLock = wlockClient.newReadWriteLock("test_key_" + random.nextInt(100000));
            readLock = wReadWriteLock.readLock();
            writeLock = wReadWriteLock.writeLock();
		} catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	/**
	 * 获取读锁，阻塞方式
	 */
	@Test
    public void readTryAcquireLock() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);
    }

	/**
	 * 获取读锁，非阻塞方式
	 */
	@Test
    public void readTryAcquireLockUnblocked() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = readLock.tryAcquireLockUnblocked(1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);
    }

	/**
	 *  读锁重入
	 */
	@Test
    public void readReentryTryAcquireLock() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);
        AcquireLockResult acquireLockResult2 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult2.isSuccess(), true);
    }

	/**
	 * 读锁获取释放
	 */
	@Test
    public void readReleaseLock() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        LockResult lockResult = readLock.releaseLock();
        Assert.assertEquals(lockResult.isSuccess(), true);
    }

	/**
	 * 读锁续约
	 */
    @Test
    public void readRenewLock() throws Exception {
        AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        long lockVersion = acquireLockResult.getLockVersion();
        LockResult lockResult = readLock.renewLock(1000 * 60);
        Assert.assertEquals(lockResult.isSuccess(), true);

    }

	/**
	 * 获取写锁，阻塞方式
	 */
	@Test
    public void writeTryAcquireLock() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);
    }

	/**
	 * 获取读锁，非阻塞方式
	 */
	@Test
    public void writeTryAcquireLockUnblocked() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLockUnblocked(1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);
    }

	/**
	 * 写锁重入
	 */
	@Test
    public void writeReentryTryAcquireLock() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);
        AcquireLockResult acquireLockResult2 = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult2.isSuccess(), true);
    }

	/**
	 * 写锁获取释放
	 */
	@Test
    public void writeReleaseLock() throws ParameterIllegalException {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        LockResult lockResult = writeLock.releaseLock();
        Assert.assertEquals(lockResult.isSuccess(), true);
    }

	/**
	 * 写锁续约
	 */
	@Test
    public void writeRenewLock() throws Exception {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        long lockVersion = acquireLockResult.getLockVersion();
        LockResult lockResult = writeLock.renewLock(1000 * 60);
        Assert.assertEquals(lockResult.isSuccess(), true);

    }

	/**
	 * 同一线程，先获取读，再获取写锁
	 */
	@Test
    public void readWriteTryAcquireLock() throws Exception {
        AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        try {
            AcquireLockResult write = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(e.getMessage(), "Has held a reading lock, it is not allowed to apply for a write lock.");
        }
    }

	/**
	 * 同一线程，先获取写锁，再获取读锁
	 */
	@Test
    public void writeReadTryAcquireLock() throws Exception {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        AcquireLockResult acquireLockResult2 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult2.isSuccess(), true);
    }


    @Test
    public void writeReadWriteReadTryAcquireLock() throws Exception {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        AcquireLockResult acquireLockResult2 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult2.isSuccess(), true);

        AcquireLockResult acquireLockResult3 = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult3.isSuccess(), true);

        AcquireLockResult acquireLockResult4 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult4.isSuccess(), true);
    }

    @Test
    public void writeReadTryAcquireLockReadWriteRelease() throws Exception {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        AcquireLockResult acquireLockResult2 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult2.isSuccess(), true);

        LockResult lockResult = readLock.releaseLock();
        Assert.assertEquals(lockResult.isSuccess(), true);

        LockResult lockResult2 = writeLock.releaseLock();
        Assert.assertEquals(lockResult2.isSuccess(), true);
    }

    /**
     * 先释放写锁再释放读锁
     */
    @Test
    public void writeReadTryAcquireLockWriteReadRelease() throws Exception {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        AcquireLockResult acquireLockResult2 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult2.isSuccess(), true);

        LockResult lockResult2 = writeLock.releaseLock();
        Assert.assertEquals(lockResult2.isSuccess(), true);

        LockResult lockResult = readLock.releaseLock();
        Assert.assertEquals(lockResult.isSuccess(), true);
    }

    /**
     * 先获取写锁，再获取读锁，再获取写锁，再获取读锁，释放读写锁
     */
    @Test
    public void writeReadWriteReadTryAcquireLockWriteReadRelease() throws Exception {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult.isSuccess(), true);

        AcquireLockResult acquireLockResult2 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult2.isSuccess(), true);

        AcquireLockResult acquireLockResult3 = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult3.isSuccess(), true);

        AcquireLockResult acquireLockResult4 = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
        Assert.assertEquals(acquireLockResult4.isSuccess(), true);

        LockResult lockResult2 = writeLock.releaseLock();
        Assert.assertEquals(lockResult2.isSuccess(), true);

        LockResult lockResult = readLock.releaseLock();
        Assert.assertEquals(lockResult.isSuccess(), true);

        LockResult lockResult3 = writeLock.releaseLock();
        Assert.assertEquals(lockResult3.isSuccess(), true);

        LockResult lockResult4 = readLock.releaseLock();
        Assert.assertEquals(lockResult4.isSuccess(), true);
    }

	/**
	 * 线程 A 获取读锁成功后,线程 B 获取读锁 : A成功,B成功
	 * @throws Exception
	 */
	@Test
	public void threadAReadThreadBReadTest() throws Exception {
		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		System.out.println("Thread A get lock result is {}" + acquireLockResult);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<String> bResult = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
					Assert.assertTrue(acquireLockResult.isSuccess());
					System.out.println("Thread B get lock result is {}" + acquireLockResult);
					return "0";
				} catch (ParameterIllegalException e) {
					e.printStackTrace();
					return "-1";
				}
			}
		});
		String result = bResult.get();
		Assert.assertEquals("0", result);
	}

	/**
	 * 线程 A 获取读锁成功后,线程 B 获取写锁 : A成功 ,B 阻塞
	 * @throws Exception
	 */
	@Test
	public void threadAReadThreadBWriteTest() throws Exception {
		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		System.out.println("Thread A get lock result is {}" + acquireLockResult);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<String> bResult = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 5);
					Assert.assertFalse(acquireLockResult.isSuccess());
					System.out.println("Thread B get lock result is {}" + acquireLockResult);
					return "0";
				} catch (Exception e) {
					e.printStackTrace();
					return "-1";
				}
			}
		});
		String result = bResult.get();
		Assert.assertEquals("0", result);
	}


	@Test
	public void threadAWriteThreadBReadTest() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		System.out.println("Thread A get lock result is {}" + acquireLockResult);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<String> bResult = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 5);
					Assert.assertFalse(acquireLockResult.isSuccess());
					System.out.println("Thread B get lock result is {}" + acquireLockResult);
					return "0";
				} catch (Exception e) {
					e.printStackTrace();
					return "-1";
				}
			}
		});
		String result = bResult.get();
		Assert.assertEquals("0", result);
	}

	@Test
	public void threadAWriteThreadBWriteTest() throws Exception {
		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		System.out.println("Thread A get lock result is {}" + acquireLockResult);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<String> bResult = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 5);
					Assert.assertFalse(acquireLockResult.isSuccess());
					System.out.println("Thread B get lock result is {}" + acquireLockResult);
					return "0";
				} catch (Exception e) {
					e.printStackTrace();
					return "-1";
				}
			}
		});
		String result = bResult.get();
		Assert.assertEquals("0", result);
	}


	@Test
	public void threadAReadThreadBWriteTestUnBlock() throws Exception {
		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		System.out.println("Thread A get lock result is {}" + acquireLockResult);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<String> bResult = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					AcquireLockResult acquireLockResult = writeLock.tryAcquireLockUnblocked(1000 * 60);
					Assert.assertFalse(acquireLockResult.isSuccess());
					System.out.println("Thread B get lock result is {}" + acquireLockResult);
					return "0";
				} catch (Exception e) {
					e.printStackTrace();
					return "-1";
				}
			}
		});
		String result = bResult.get();
		Assert.assertEquals("0", result);
	}


	@Test
	public void threadAWriteThreadBReadTestUnBlock() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		System.out.println("Thread A get lock result is {}" + acquireLockResult);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<String> bResult = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					AcquireLockResult acquireLockResult = readLock.tryAcquireLockUnblocked(1000 * 60);
					Assert.assertFalse(acquireLockResult.isSuccess());
					System.out.println("Thread B get lock result is {}" + acquireLockResult);
					return "0";
				} catch (Exception e) {
					e.printStackTrace();
					return "-1";
				}
			}
		});
		String result = bResult.get();
		Assert.assertEquals("0", result);
	}

	@Test
	public void threadAWriteThreadBWriteTestUnBlock() throws Exception{
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		System.out.println("Thread A get lock result is {}" + acquireLockResult);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<String> bResult = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					AcquireLockResult acquireLockResult = writeLock.tryAcquireLockUnblocked(1000 * 60);
					Assert.assertFalse(acquireLockResult.isSuccess());
					System.out.println("Thread B get lock result is {}" + acquireLockResult);
					return "0";
				} catch (Exception e) {
					e.printStackTrace();
					return "-1";
				}
			}
		});
		String result = bResult.get();
		Assert.assertEquals("0", result);
	}


	/**
	 * 线程A先获取读锁，线程B后获取读锁，AB释放锁
	 */
	@Test
	public void testReadReadReleaseRelease() throws Exception {
		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());

		Future<String> submit = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());

				LockResult lockResult = readLock.releaseLock();
				Assert.assertTrue(lockResult.isSuccess());
				return "0";
			}
		});

		String s = submit.get();
		Assert.assertEquals("0", s);

		LockResult lockResult = readLock.releaseLock();
		Assert.assertTrue(lockResult.isSuccess());
	}

	/**
	 * 线程A先获取读锁，线程B后获取写锁，A释放锁
	 * A释放成功，B获取到锁
	 */
	@Test
	public void test1() throws Exception {
		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		Future<String> submit = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		Thread.sleep(5000);
		LockResult lockResult = readLock.releaseLock();
		Assert.assertTrue(lockResult.isSuccess());
		System.out.println("A释放锁");

		String s = submit.get();
		Assert.assertEquals("0", s);
	}

	/**
	 * 线程A先获取写锁，线程B后获取读锁，A释放锁
	 * A释放成功，B获取到锁
	 */
	@Test
	public void test2() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		Future<String> submit = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		Thread.sleep(5000);
		LockResult lockResult = writeLock.releaseLock();
		Assert.assertTrue(lockResult.isSuccess());
		System.out.println("A释放锁");

		String s = submit.get();
		Assert.assertEquals("0", s);
	}

	/**
	 * 线程A先获取写锁，线程B后获取写锁，A释放锁
	 * A释放成功，B获取到锁
	 */
	@Test
	public void test3() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		Future<String> submit = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		Thread.sleep(5000);
		LockResult lockResult = writeLock.releaseLock();
		Assert.assertTrue(lockResult.isSuccess());
		System.out.println("A释放锁");

		String s = submit.get();
		Assert.assertEquals("0", s);
	}

	/**
	 * 线程A先获取写锁，线程B后获取读锁，线程C获取读锁，线程A释放
	 * A释放成功，BC都获取锁
	 */
	@Test
	public void test4() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		Future<String> bFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		Thread.sleep(1000);
		Future<String> cFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("C获取锁成功");
				return "0";
			}
		});

		Thread.sleep(5000);
		LockResult lockResult = writeLock.releaseLock();
		Assert.assertTrue(lockResult.isSuccess());
		System.out.println("A释放锁");

		String s = bFuture.get();
		Assert.assertEquals("0", s);

		String s1 = cFuture.get();
		Assert.assertEquals("0", s1);
	}

	/**
	 * 线程A先获取写锁，线程B后获取读锁，线程C获取读锁，线程D获取写锁，线程E获取读锁，线程F获取读锁，
	 */
	@Test
	public void test4_1 () throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());
		Thread.sleep(100);
		Future<String> bFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");

				LockResult lockResult = readLock.releaseLock();
				Assert.assertTrue(lockResult.isSuccess());
				System.out.println("B释放锁成功");
				return "0";
			}
		});

		Thread.sleep(100);
		Future<String> cFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("C获取锁成功");

				LockResult lockResult = readLock.releaseLock();
				Assert.assertTrue(lockResult.isSuccess());
				System.out.println("C释放锁成功");
				return "0";
			}
		});

		Thread.sleep(100);
		Future<String> dFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("D获取锁成功");

				LockResult lockResult = writeLock.releaseLock();
				Assert.assertTrue(lockResult.isSuccess());
				System.out.println("D释放锁成功");
				return "0";
			}
		});

		Thread.sleep(100);
		Future<String> eFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("e获取锁成功");
				return "0";
			}
		});

		Thread.sleep(100);
		Future<String> fFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("f获取锁成功");
				return "0";
			}
		});

		Thread.sleep(5000);
		LockResult lockResult = writeLock.releaseLock();
		Assert.assertTrue(lockResult.isSuccess());
		System.out.println("A释放锁");

		String s = bFuture.get();
		Assert.assertEquals("0", s);

		String s1 = cFuture.get();
		Assert.assertEquals("0", s1);

		String s2 = dFuture.get();
		Assert.assertEquals("0", s2);

		String s3 = eFuture.get();
		Assert.assertEquals("0", s3);

		String s4 = fFuture.get();
		Assert.assertEquals("0", s4);
	}

	/**
	 * 线程A先获取写锁，线程A后获取读锁，线程B获取读锁，线程A释放写锁
	 * 锁降级：线程B获取锁成功
	 */
	@Test
	public void test5() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());

		AcquireLockResult readResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
		Assert.assertTrue(readResult.isSuccess());

		Future<String> bFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		Thread.sleep(3000);
		LockResult lockResult = writeLock.releaseLock();
		Assert.assertTrue(lockResult.isSuccess());
		System.out.println("线程A释放写锁成功");

		String s = bFuture.get();
		Assert.assertEquals("0", s);
	}

	/**
	 * 线程A先获取读锁，线程B后获取写锁，A锁过期
	 * B获取到锁
	 */
	@Test
	public void test6() throws Exception {
		AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 10, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());

		Future<String> bFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		String s = bFuture.get();
		Assert.assertEquals("0", s);
	}

	/**
	 * 线程A先获取写锁，线程B后获取读锁，A锁过期
	 * B获取到锁
	 */
	@Test
	public void test7() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 10, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());

		Future<String> bFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		String s = bFuture.get();
		Assert.assertEquals("0", s);
	}

	/**
	 * 线程A先获取写锁，线程B后获取写锁，A锁过期
	 * B获取到锁
	 */
	@Test
	public void test8() throws Exception {
		AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 10, 1000 * 60);
		Assert.assertTrue(acquireLockResult.isSuccess());

		Future<String> bFuture = threadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
				Assert.assertTrue(acquireLockResult.isSuccess());
				System.out.println("B获取锁成功");
				return "0";
			}
		});

		String s = bFuture.get();
		Assert.assertEquals("0", s);
	}
}
