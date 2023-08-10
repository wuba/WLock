package com.wuba.wlock.starter;


import com.wuba.wlock.starter.aspect.lock.LockKeyGenerator;

public class LockKeyGeneratorImpl implements LockKeyGenerator {

    @Override
    public String generate(Object[] args) {

        return args[0]+ "_" + args[1];
    }
}
