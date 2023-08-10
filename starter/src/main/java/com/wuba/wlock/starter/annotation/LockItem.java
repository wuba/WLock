package com.wuba.wlock.starter.annotation;

import com.wuba.wlock.starter.aspect.lock.LockKeyGenerator;
import com.wuba.wlock.starter.enums.LockTypeEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author huguocai
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LockItem {
    LockTypeEnum lockType();

    String lockKey() default "";

    String prefix() default "";

    long expireTime() default 0;

    int maxWaitTime() default 0;

    int renewInterval() default 0;

    Class<? extends LockKeyGenerator> lockKeyGenerator() default LockKeyGenerator.class;
}
