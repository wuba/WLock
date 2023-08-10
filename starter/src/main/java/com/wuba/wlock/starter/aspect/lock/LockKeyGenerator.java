package com.wuba.wlock.starter.aspect.lock;

/**
 * @author huguocai
 */
public interface LockKeyGenerator {

    String generate(Object[] args);
}
