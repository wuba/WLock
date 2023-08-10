//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.wuba.wlock.starter.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

public abstract class AnnotationBeanProcessor<A extends Annotation, C> implements BeanPostProcessor, BeanFactoryAware {
    private static final Logger log = LoggerFactory.getLogger(AnnotationBeanProcessor.class);
    protected ConfigurableListableBeanFactory beanFactory;

    public AnnotationBeanProcessor() {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory)beanFactory;
    }

    protected Object inject(Object bean, Class<A> annotationClass) {
        ReflectionUtils.doWithFields(bean.getClass(), (field) -> {
            Annotation annotation = field.getAnnotation(annotationClass);
            Object object = this.getOrCreateBean(beanName(annotation), () -> {
                return this.createBean(annotation);
            });
            field.setAccessible(true);
            field.set(bean, object);
        }, (field) -> {
            return field.isAnnotationPresent(annotationClass);
        });
        return bean;
    }

    protected C getOrCreateBean(String beanName, Supplier<C> creator) {
        if (this.beanFactory.containsBean(beanName)) {
            return (C) this.beanFactory.getBean(beanName);
        } else {
            C obj = creator.get();
            this.beanFactory.registerSingleton(beanName, obj);
            return obj;
        }
    }

    protected abstract C createBean(Annotation annotation);

    protected abstract String beanName(Annotation annotation);
}
