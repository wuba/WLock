package com.wuba.wlock.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author huguocai
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface LockKey {

    String value();
}
