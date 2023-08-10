package com.wuba.wlock.starter.annotation;

import com.wuba.wlock.starter.aspect.lock.LockKeyGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author huguocai
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadLock {

    String lockKey() default "";

    String prefix() default "";

    long expireTime() default 0;

    int maxWaitTime() default 0;

    int renewInterval() default 0;

    Class<? extends LockKeyGenerator> lockKeyGenerator() default LockKeyGenerator.class;
}
