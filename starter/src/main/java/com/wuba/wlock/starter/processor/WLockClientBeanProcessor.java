package com.wuba.wlock.starter.processor;

import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.starter.annotation.LockClient;
import com.wuba.wlock.starter.config.WLockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import java.lang.annotation.Annotation;

/**
 * @author huguocai
 */
public class WLockClientBeanProcessor extends BaseBeanProcessor<LockClient, WLockClient> {
    private static final Logger log = LoggerFactory.getLogger(WLockClientBeanProcessor.class);

    static final String NAME = "WLockClient";

    public WLockClientBeanProcessor(WLockProperties properties) {
        super(properties);
    }

    @Override
    protected WLockClient createBean(Annotation annotation) {
        return createWLockClient(annotation);
    }

    @Override
    protected String beanName(Annotation annotation) {
        return NAME;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return this.inject(bean, LockClient.class);
    }
}
