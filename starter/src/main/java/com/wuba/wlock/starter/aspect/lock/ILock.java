package com.wuba.wlock.starter.aspect.lock;

import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import com.wuba.wlock.starter.enums.LockTypeEnum;

public interface ILock {

    AcquireLockResult tryAcquireLock(long expireTime, int maxWaitTime, int renewInterval) throws ParameterIllegalException;

    LockResult releaseLock() throws ParameterIllegalException;

    LockTypeEnum lockType();

    String lockKey();
}
