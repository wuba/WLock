package com.wuba.wlock.starter;


import com.wuba.wlock.client.*;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.starter.annotation.*;
import com.wuba.wlock.starter.enums.LockTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class TestService {

    @LockClient
    WLockClient wlockClient;

    @Lock(lockKey = "test")
    WDistributedLock wDistributedLock;

    @ReadWriteLock(lockKey = "test")
    WReadWriteLock wReadWriteLock;

    @ReadLock(lockKey = "test")
    WReadLock readLock;

    @WriteLock(lockKey = "test")
    WWriteLock writeLock;

    /**
     * @WlockClient 用法
     */
    public void test1() throws Exception {
        WDistributedLock lock = wlockClient.newDistributeLock("test");
        AcquireLockResult acquireLockResult = lock.tryAcquireLock(5000, 5000);
        // 业务逻辑
        lock.releaseLock();
    }

    /**
     * @Lock 用法
     */
    public void test2() throws Exception {
        AcquireLockResult acquireLockResult = wDistributedLock.tryAcquireLock(5000, 5000);
        // 业务逻辑
        wDistributedLock.releaseLock();
    }

    /**
     * @ReadWriteLock 用法
     */
    public void test3() throws Exception {
        WReadLock wReadLock = wReadWriteLock.readLock();
        WWriteLock wWriteLock = wReadWriteLock.writeLock();
    }

    /**
     * @ReadLock 用法
     */
    public void test4() throws Exception {
        AcquireLockResult acquireLockResult = readLock.tryAcquireLock(5000, 5000);

        readLock.releaseLock();
    }

    /**
     * @WriteLock用法
     */
    public void test5() throws Exception {
        AcquireLockResult acquireLockResult = writeLock.tryAcquireLock(5000, 5000);

        writeLock.releaseLock();
    }

    /**
     * @Lock @ReadLock @WriteLock 注解方法上的使用
     */
    @Lock(lockKey = "test", prefix = "lock_", expireTime = 6000, maxWaitTime = 6000)
    public void test6() {

    }

    /**
     * @Lock @ReadLock @WriteLock 注解方法上的使用
     */
    @Lock(prefix = "lock_", expireTime = 6000, maxWaitTime = 6000, lockKeyGenerator = LockKeyGeneratorImpl.class)
    public void test61(int a, String b) {
    }

    /**
     * @Lock @ReadLock @WriteLock 注解方法上的使用，指定参数作为lockkey
     */
    @Lock(lockKey = "#{lockKey}")
    public void test7(@LockKey("lockKey") String lockKey) {

    }

    /**
     * @ReadLock 注解方法上的使用
     */
    @ReadLock(lockKey = "test", prefix = "lock_", expireTime = 6000, maxWaitTime = 6000)
    public void test8() {

    }

    @ReadLock(prefix = "lock_", expireTime = 6000, maxWaitTime = 6000, lockKeyGenerator = LockKeyGeneratorImpl.class)
    public void test81(int a, String b) {
    }

    /**
     * @ReadLock 注解方法上的使用，指定参数作为lockkey
     */
    @ReadLock(lockKey = "#{lockKey}")
    public void test9(@LockKey("lockKey") String lockKey) {

    }

    /**
     * @ReadLock 注解方法上的使用
     */
    @WriteLock(lockKey = "test", prefix = "lock_", expireTime = 6000, maxWaitTime = 6000)
    public void test10() {

    }

    @WriteLock(prefix = "lock_", expireTime = 6000, maxWaitTime = 6000, lockKeyGenerator = LockKeyGeneratorImpl.class)
    public void test101(int a, String b) {
    }

    /**
     * @WriteLock 注解方法上的使用，指定参数作为lockkey
     */
    @WriteLock(lockKey = "#{lockKey}")
    public void test11(@LockKey("lockKey") String lockKey) {

    }

    @MultiLock({
            @LockItem(lockType = LockTypeEnum.LOCK, lockKey = "#{a}"),
            @LockItem(lockType = LockTypeEnum.LOCK, lockKey = "#{b}"),
            @LockItem(lockType = LockTypeEnum.READ_LOCK, lockKey = "testRead"),
            @LockItem(lockType = LockTypeEnum.WRITE_LOCK, lockKey = "testWrite"),
    })
    public void test12(@LockKey("a") int a, @LockKey("b") String s) {

    }

    @MultiLock({
            @LockItem(lockType = LockTypeEnum.LOCK, lockKey = "#{a}"),
            @LockItem(lockType = LockTypeEnum.LOCK, lockKey = "#{b}"),
            @LockItem(lockType = LockTypeEnum.READ_LOCK, lockKey = "testRead"),
            @LockItem(lockType = LockTypeEnum.WRITE_LOCK, lockKeyGenerator = LockKeyGeneratorImpl.class),
    })
    public void test121(@LockKey("a") int a, @LockKey("b") String s) {

    }
}
