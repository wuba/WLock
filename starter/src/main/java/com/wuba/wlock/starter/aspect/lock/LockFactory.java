package com.wuba.wlock.starter.aspect.lock;


import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.WReadWriteLock;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.starter.enums.LockTypeEnum;

/**
 * @author huguocai
 */
public class LockFactory {
    private static final LockFactory INSTANCE = new LockFactory();


    private LockFactory() {
    }

    public static LockFactory getInstance() {
        return INSTANCE;
    }

    public ILock create(WLockClient lockClient, LockTypeEnum lockType, String lockKey) throws ParameterIllegalException {
        if (lockType == LockTypeEnum.LOCK) {
            WDistributedLock lock = lockClient.newDistributeLock(lockKey);
            return new LockImpl(lock, lockKey);
        } else if (lockType == LockTypeEnum.READ_LOCK) {
            WReadWriteLock readWriteLock = lockClient.newReadWriteLock(lockKey);
            return new ReadLockImpl(readWriteLock.readLock(), lockKey);
        } else if (lockType == LockTypeEnum.WRITE_LOCK) {
            WReadWriteLock readWriteLock = lockClient.newReadWriteLock(lockKey);
            return new WriteLockImpl(readWriteLock.writeLock(), lockKey);
        }

        throw new ParameterIllegalException("lockType not exist");
    }


}
