package com.wuba.wlock.starter.processor;

import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.starter.annotation.Lock;
import com.wuba.wlock.starter.config.WLockProperties;
import org.springframework.beans.BeansException;
import java.lang.annotation.Annotation;

/**
 * @author huguocai
 */
public class WLockBeanProcessor extends BaseBeanProcessor<Lock, WDistributedLock> {
    private static final String PREFIX = "WLock_";

    public WLockBeanProcessor(WLockProperties properties) {
        super(properties);
    }

    @Override
    protected WDistributedLock createBean(Annotation annotation) {
        return createWDistributedLock(annotation);
    }

    @Override
    protected String beanName(Annotation annotation) {
        Lock lock = (Lock) annotation;
        return PREFIX + lock.prefix() + lock.lockKey();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return this.inject(bean, Lock.class);
    }
}
