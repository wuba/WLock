package com.wuba.wlock.starter.aspect.lock;


import com.wuba.wlock.client.WReadLock;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import com.wuba.wlock.starter.enums.LockTypeEnum;

/**
 * @author huguocai
 */
public class ReadLockImpl extends BaseLock {
    private WReadLock lock;

    public ReadLockImpl(WReadLock lock, String lockKey) {
        super(lockKey);
        this.lock = lock;
    }

    @Override
    public AcquireLockResult tryAcquireLock(long expireTime, int maxWaitTime, int renewInterval) throws ParameterIllegalException {
        if (renewInterval > 0) {
            return lock.tryAcquireLock(expireTime, maxWaitTime, renewInterval);
        }
        return lock.tryAcquireLock(expireTime, maxWaitTime);
    }

    @Override
    public LockResult releaseLock() throws ParameterIllegalException {
        return lock.releaseLock();
    }

    @Override
    public LockTypeEnum lockType() {
        return LockTypeEnum.READ_LOCK;
    }

}
