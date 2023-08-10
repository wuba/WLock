package com.wuba.wlock.starter.processor;


import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WWriteLock;
import com.wuba.wlock.starter.annotation.WriteLock;
import com.wuba.wlock.starter.config.WLockProperties;
import org.springframework.beans.BeansException;
import java.lang.annotation.Annotation;

/**
 * @author huguocai
 */
public class WWriteLockBeanProcessor extends BaseBeanProcessor<WriteLock, WDistributedLock> {
    private static final String PREFIX = "WWriteLock_";

    public WWriteLockBeanProcessor(WLockProperties properties) {
        super(properties);
    }

    @Override
    protected WWriteLock createBean(Annotation annotation) {
        return createWWriteLock(annotation);
    }

    @Override
    protected String beanName(Annotation annotation) {
        WriteLock writeLock = (WriteLock) annotation;
        return PREFIX + writeLock.prefix() + writeLock.lockKey();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return this.inject(bean, WriteLock.class);
    }
}
