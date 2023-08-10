package com.wuba.wlock.starter.processor;

import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WReadLock;
import com.wuba.wlock.starter.annotation.ReadLock;
import com.wuba.wlock.starter.config.WLockProperties;
import org.springframework.beans.BeansException;

import java.lang.annotation.Annotation;

/**
 * @author huguocai
 */
public class WReadLockBeanProcessor extends BaseBeanProcessor<ReadLock, WDistributedLock> {
    private static final String PREFIX = "WReadLock_";

    public WReadLockBeanProcessor(WLockProperties properties) {
        super(properties);
    }

    @Override
    protected WReadLock createBean(Annotation annotation) {
        return createWReadLock(annotation);
    }

    @Override
    protected String beanName(Annotation annotation) {
        ReadLock readLock = (ReadLock) annotation;
        return PREFIX + readLock.prefix() + readLock.lockKey();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return this.inject(bean, ReadLock.class);
    }
}
