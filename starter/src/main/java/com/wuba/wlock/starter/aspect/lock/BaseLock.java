package com.wuba.wlock.starter.aspect.lock;

/**
 * @author huguocai
 */
public abstract class BaseLock implements ILock{
    private String lockKey;

    public BaseLock(String lockKey) {
        this.lockKey = lockKey;
    }

    @Override
    public String lockKey() {
        return this.lockKey;
    }
}
