package com.wuba.wlock.starter.processor;

import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WReadWriteLock;
import com.wuba.wlock.starter.annotation.ReadWriteLock;
import com.wuba.wlock.starter.config.WLockProperties;
import org.springframework.beans.BeansException;
import java.lang.annotation.Annotation;

/**
 * @author huguocai
 */
public class WReadWriteLockBeanProcessor extends BaseBeanProcessor<ReadWriteLock, WDistributedLock> {
    static final String PREFIX = "WReadWriteLock_";

    public WReadWriteLockBeanProcessor(WLockProperties properties) {
        super(properties);
    }

    @Override
    protected WReadWriteLock createBean(Annotation annotation) {
        return createWReadWriteLock(annotation);
    }

    @Override
    protected String beanName(Annotation annotation) {
        ReadWriteLock wLock = (ReadWriteLock) annotation;
        return PREFIX + wLock.lockKey();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return this.inject(bean, ReadWriteLock.class);
    }
}
