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
package com.wuba.wlock.benchmark;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Lock;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.lock.LockResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class EtcdDistributedLock {

    private static Client client;
    private Lock lockClient;
    private Lease leaseClient;
    private String lockKey;
    private String lockPath;
    //租约有效期。作用 1：客户端崩溃，租约到期后自动释放锁，防止死锁 2：正常执行自动进行续租
    private Long leaseTTL;


    private static final ThreadLocal<LockData> threadData = new ThreadLocal<LockData>();

    static {
        client = Client.builder().endpoints("http://127.0.0.1:2379","http://127.0.0.1:2379","http://127.0.0.1:2379").build();
    }


    public EtcdDistributedLock(Client client, String lockKey, Long leaseTTL, TimeUnit unit) {
        this.lockClient = client.getLockClient();
        this.leaseClient = client.getLeaseClient();
        this.lockKey = lockKey;
        this.leaseTTL = unit.toNanos(leaseTTL);
    }

    public EtcdDistributedLock(String lockKey) {
        this(client, lockKey, 50L, TimeUnit.SECONDS);
    }


    public void lock() {
        long leaseId;
        try {
            leaseId = leaseClient.grant(TimeUnit.NANOSECONDS.toSeconds(leaseTTL)).get().getID();
            //加锁
            LockResponse lockResponse = lockClient.lock(ByteSequence.fromBytes(lockKey.getBytes()), leaseId).get();
            if (lockResponse != null) {
                lockPath = lockResponse.getKey().toString(StandardCharsets.UTF_8);
            }

            //加锁成功，设置锁对象
            LockData lockData = new LockData(lockKey, leaseId);
            threadData.set(lockData);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unlock() {
        LockData lockData = threadData.get();
        if (lockData == null) {
            throw new IllegalMonitorStateException("没有获得锁，lockKey：" + lockKey);
        }
        try {
            //正常释放锁
            if (lockPath != null) {
                lockClient.unlock(ByteSequence.fromBytes(lockPath.getBytes())).get();
            }
            //删除租约
            if (lockData.getLeaseId() != 0L) {
                leaseClient.revoke(lockData.getLeaseId());
            }
        } catch (InterruptedException | ExecutionException e) {
            //e.printStackTrace();
        } finally {
            //移除当前线程资源
            threadData.remove();
        }

    }

    public class LockData {
        private long leaseId;
        private String lockKey;

        public LockData(String lockKey, long leaseId) {
            this.lockKey = lockKey;
            this.leaseId = leaseId;
        }

        public void setLeaseId(long leaseId) {
            this.leaseId = leaseId;
        }

        public long getLeaseId() {
            return leaseId;
        }

    }
}