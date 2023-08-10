package com.wuba.wlock.starter.aspect;


import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import com.wuba.wlock.client.protocol.ResponseStatus;
import com.wuba.wlock.starter.annotation.*;
import com.wuba.wlock.starter.aspect.lock.ILock;
import com.wuba.wlock.starter.aspect.lock.LockFactory;
import com.wuba.wlock.starter.aspect.lock.LockKeyGenerator;
import com.wuba.wlock.starter.config.WLockProperties;
import com.wuba.wlock.starter.enums.LockTypeEnum;
import com.wuba.wlock.starter.exception.AcquireLockFailException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@ConditionalOnProperty(name = "wlock.enabled", havingValue = "true")
public class WLockAspect {
    private final WLockProperties wLockProperties;

    @LockClient
    private WLockClient wLockClient;

    public WLockAspect(WLockProperties wLockProperties) {
        this.wLockProperties = wLockProperties;
    }

    @Around(value = "@annotation(com.wuba.wlock.starter.annotation.Lock)")
    public Object doAroundWLock(ProceedingJoinPoint point) throws Throwable {
        Lock lock = (Lock) getAnnotation(point, Lock.class);
        if (lock == null) {
            return point.proceed();
        }
        return process(point, LockTypeEnum.LOCK, lock.prefix(), lock.lockKey(), lock.expireTime(), lock.maxWaitTime(), lock.renewInterval(), lock.lockKeyGenerator());
    }

    @Around(value = "@annotation(com.wuba.wlock.starter.annotation.ReadLock)")
    public Object doAroundWReadLock(ProceedingJoinPoint point) throws Throwable {
        ReadLock readLock = (ReadLock) getAnnotation(point, ReadLock.class);
        if (readLock == null) {
            return point.proceed();
        }
        return process(point, LockTypeEnum.READ_LOCK, readLock.prefix(), readLock.lockKey(), readLock.expireTime(), readLock.maxWaitTime(), readLock.renewInterval(), readLock.lockKeyGenerator());
    }

    @Around(value = "@annotation(com.wuba.wlock.starter.annotation.WriteLock)")
    public Object doAroundWWriteLock(ProceedingJoinPoint point) throws Throwable {
        WriteLock writeLock = (WriteLock) getAnnotation(point, WriteLock.class);
        if (writeLock == null) {
            return point.proceed();
        }
        return process(point, LockTypeEnum.WRITE_LOCK, writeLock.prefix(), writeLock.lockKey(), writeLock.expireTime(), writeLock.maxWaitTime(), writeLock.renewInterval(), writeLock.lockKeyGenerator());
    }

    @Around(value = "@annotation(com.wuba.wlock.starter.annotation.MultiLock)")
    public Object doAroundWMultiLock(ProceedingJoinPoint point) throws Throwable {
        MultiLock multiLock = (MultiLock) getAnnotation(point, MultiLock.class);
        if (multiLock == null || multiLock.value().length == 0) {
            return point.proceed();
        }

        LockItem[] lockItems = multiLock.value();
        LockItemChain lockItemChain = new LockItemChain(lockItems, point);
        return lockItemChain.doHandle();
    }

    private String generateLockKey(Class<? extends LockKeyGenerator> LockKeyGeneratorClass, Object[] args) throws Exception {
        if (LockKeyGeneratorClass == LockKeyGenerator.class) {
            throw new IllegalArgumentException("lockKey or lockKeyGenerator not set");
        }

        Method generate = LockKeyGeneratorClass.getMethod("generate", Object[].class);
        return (String) generate.invoke(LockKeyGeneratorClass.newInstance(), new Object[]{args});
    }

    private Annotation getAnnotation(ProceedingJoinPoint point, Class<? extends Annotation> annotationClass) {
        Signature signature = point.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null && method.isAnnotationPresent(annotationClass)) {
            return method.getAnnotation(annotationClass);
        }

        return null;
    }

    private ILock acquireLock(ProceedingJoinPoint point, LockTypeEnum lockTypeEnum, String prefix, String lockKey, long expireTimeValue, int maxWaitTimeValue, int renewIntervalValue, Class<? extends LockKeyGenerator> lockKeyGeneratorClass) throws Throwable{
        if (lockKey.isEmpty()) {
            lockKey = generateLockKey(lockKeyGeneratorClass, point.getArgs());
        }

        if (lockKey == null || lockKey.isEmpty()) {
            throw new IllegalArgumentException("lockKey is required!");
        }

        if (lockKey.startsWith("#{") && lockKey.endsWith("}")) {
            String placeholder = lockKey.substring(2, lockKey.length() - 1);
            MethodSignature signature = (MethodSignature) point.getSignature();
            Parameter[] parameters = signature.getMethod().getParameters();
            boolean exist = false;
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (parameter.isAnnotationPresent(LockKey.class)) {
                    LockKey lockKeyAnnotation = parameter.getAnnotation(LockKey.class);
                    if (placeholder.equals(lockKeyAnnotation.value())) {
                        lockKey = String.valueOf(point.getArgs()[i]);
                        exist = true;
                        break;
                    }
                }
            }

            if (!exist) {
                throw new IllegalArgumentException(String.format("@LockKey(%s) not exist", placeholder));
            }
        }

        Long expireTime = wLockProperties.getExpireTime();
        if (expireTimeValue > 0) {
            expireTime = expireTimeValue;
        }

        if (expireTime == null || expireTime <= 0) {
            throw new IllegalArgumentException("expireTime is required!");
        }

        Integer maxWaitTime = wLockProperties.getMaxWaitTime();
        if (maxWaitTimeValue > 0) {
            maxWaitTime = maxWaitTimeValue;
        }

        if (maxWaitTime == null || maxWaitTime <= 0) {
            throw new IllegalArgumentException("maxWaitTime is required!");
        }

        String lockKeyFinal = prefix + lockKey;

        ILock ilock = LockFactory.getInstance().create(wLockClient, lockTypeEnum, lockKeyFinal);
        AcquireLockResult acquireLockResult = ilock.tryAcquireLock(expireTime, maxWaitTime, renewIntervalValue);
        if (!acquireLockResult.isSuccess()) {
            String errorMessage = String.format("acquire %s lock fail! lockKey: %s, responseStatus: %s", ilock.lockType(), lockKeyFinal, ResponseStatus.toStr(acquireLockResult.getResponseStatus()));
            log.error(errorMessage);
            throw new AcquireLockFailException(errorMessage);
        }

        if (log.isDebugEnabled()) {
            log.debug("acquire {} lock success! lockKey: {}", ilock.lockType(), lockKeyFinal);
        }

        return ilock;
    }

    private void releaseLock(ILock ilock) throws Throwable {
        LockResult lockResult = ilock.releaseLock();
        if (!lockResult.isSuccess()) {
            log.error(String.format("release %s lock fail! lockKey: %s, responseStatus: %s", ilock.lockType(), ilock.lockKey(), ResponseStatus.toStr(lockResult.getResponseStatus())));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("release {} lock success! lockKey: {}", ilock.lockType(), ilock.lockKey());
            }
        }
    }

    private Object process(ProceedingJoinPoint point, LockTypeEnum lockTypeEnum, String prefix, String lockKey, long expireTimeValue, int maxWaitTimeValue, int renewIntervalValue, Class<? extends LockKeyGenerator> lockKeyGeneratorClass) throws Throwable {
        ILock ilock = acquireLock(point, lockTypeEnum, prefix, lockKey, expireTimeValue, maxWaitTimeValue, renewIntervalValue, lockKeyGeneratorClass);
        try {

            return point.proceed();
        } finally {
            releaseLock(ilock);
        }
    }

    public class LockItemChain {
        private ProceedingJoinPoint point;
        private LockItem[] lockItems;
        private int index = 0;

        public LockItemChain(LockItem[] lockItems, ProceedingJoinPoint point) {
            this.lockItems = lockItems;
            this.point = point;
        }

        public ProceedingJoinPoint getPoint() {
            return point;
        }

        public boolean hasNext() {
            return index < lockItems.length;
        }

        public Object doHandle() throws Throwable {
            return new LockItemHandler(lockItems[index++]).doHandle(this);
        }
    }

    public class LockItemHandler {
        private LockItem lockItem;

        public LockItemHandler(LockItem lockItem) {
            this.lockItem = lockItem;
        }

        public Object doHandle(LockItemChain chain) throws Throwable {
            ILock iLock = acquireLock(chain.getPoint(), lockItem.lockType(), lockItem.prefix(), lockItem.lockKey(), lockItem.expireTime(), lockItem.maxWaitTime(), lockItem.renewInterval(), lockItem.lockKeyGenerator());
            try {
                if (chain.hasNext()) {
                    return chain.doHandle();
                } else {
                    return chain.getPoint().proceed();
                }
            } finally {
                releaseLock(iLock);
            }
        }
    }
}
