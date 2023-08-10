package com.wuba.wlock.starter.processor;

import com.wuba.wlock.client.*;
import com.wuba.wlock.starter.annotation.Lock;
import com.wuba.wlock.starter.annotation.ReadLock;
import com.wuba.wlock.starter.annotation.ReadWriteLock;
import com.wuba.wlock.starter.annotation.WriteLock;
import com.wuba.wlock.starter.config.WLockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;

import java.lang.annotation.Annotation;

/**
 * @author huguocai
 */
public abstract class BaseBeanProcessor<A extends Annotation, C> extends AnnotationBeanProcessor {
    private static final Logger log = LoggerFactory.getLogger(BaseBeanProcessor.class);

    private final WLockProperties properties;

    public BaseBeanProcessor(WLockProperties properties) {
        this.properties = properties;
    }

    protected WLockClient createWLockClient(Annotation annotation) {
        WLockClient wLockClient;
        try {
            wLockClient = new WLockClient(properties.getKey(),
                    properties.getRegistryServerIp(),
                    properties.getRegistryServerPort());
        } catch (Exception e) {
            log.error("WLockClient creation failed", e);
            throw new BeanInitializationException("WLockClient creation failed", e);
        }

        if (properties.getTimeoutForReq() > 0) {
            wLockClient.setDefaultTimeoutForReq(wLockClient.getDefaultTimeoutForReq());
        }

        if (properties.getRetryTimes() > 0) {
            wLockClient.setDefaultRetries(properties.getRetryTimes());
        }

        return wLockClient;
    }

    protected WDistributedLock createWDistributedLock(Annotation annotation) {
        WLockClient wLockClient = (WLockClient) getOrCreateBean(WLockClientBeanProcessor.NAME, () -> {
            return createWLockClient(null);
        });

        try {
            Lock lock = (Lock) annotation;
            String prefix = lock.prefix();
            String lockKey = lock.lockKey();
            if (lockKey == null || lockKey.isEmpty()) {
                throw new IllegalArgumentException("lockKey is required!");
            }

            return wLockClient.newDistributeLock(prefix + lockKey);
        } catch (Exception e) {
            log.error("WDistributedLock creation failed", e);
            throw new BeanInitializationException("WDistributedLock creation failed", e);
        }
    }

    protected WReadWriteLock createWReadWriteLock(Annotation annotation) {
        try {
            ReadWriteLock readWriteLock = (ReadWriteLock) annotation;
            String lockKey = readWriteLock.lockKey();
            return createWReadWriteLock(lockKey);
        } catch (Exception e) {
            log.error("WReadWriteLock creation failed", e);
            throw new BeanInitializationException("WReadWriteLock creation failed", e);
        }
    }

    protected WReadWriteLock createWReadWriteLock(String lockKey) {
        try {
            if (lockKey == null || lockKey.isEmpty()) {
                throw new IllegalArgumentException("lockKey is required!");
            }

            WLockClient wLockClient = (WLockClient) getOrCreateBean(WLockClientBeanProcessor.NAME, () -> {
                return createWLockClient(null);
            });

            return wLockClient.newReadWriteLock(lockKey);
        } catch (Exception e) {
            log.error("WReadWriteLock creation failed", e);
            throw new BeanInitializationException("WReadWriteLock creation failed", e);
        }
    }

    protected WReadLock createWReadLock(Annotation annotation) {
        try {
            ReadLock wLock = (ReadLock) annotation;
            String prefix = wLock.prefix();
            String lockKey = wLock.lockKey();
            if (lockKey == null || lockKey.isEmpty()) {
                throw new IllegalArgumentException("lockKey is required!");
            }

            WReadWriteLock readWriteLock = (WReadWriteLock) getOrCreateBean(WReadWriteLockBeanProcessor.PREFIX + prefix + lockKey, () -> {
                return createWReadWriteLock(prefix + lockKey);
            });

            return readWriteLock.readLock();
        } catch (Exception e) {
            log.error("WReadLock creation failed", e);
            throw new BeanInitializationException("WReadLock creation failed", e);
        }
    }

    protected WWriteLock createWWriteLock(Annotation annotation) {
        try {
            WriteLock wLock = (WriteLock) annotation;
            String prefix = wLock.prefix();
            String lockKey = wLock.lockKey();
            if (lockKey == null || lockKey.isEmpty()) {
                throw new IllegalArgumentException("lockKey is required!");
            }

            WReadWriteLock readWriteLock = (WReadWriteLock) getOrCreateBean(WReadWriteLockBeanProcessor.PREFIX + prefix + lockKey, () -> {
                return createWReadWriteLock(prefix + lockKey);
            });

            return readWriteLock.writeLock();
        } catch (Exception e) {
            log.error("WReadLock creation failed", e);
            throw new BeanInitializationException("WReadLock creation failed", e);
        }
    }

}
