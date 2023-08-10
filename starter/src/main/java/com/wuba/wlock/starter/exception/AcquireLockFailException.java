package com.wuba.wlock.starter.exception;

public class AcquireLockFailException extends RuntimeException{

    public AcquireLockFailException(String message) {
        super(message);
    }
}
