/*
 * Copyright (C) 2005-present, 58.com.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuba.wlock.server.lock.service;

import com.wuba.wlock.server.domain.AcquireLockDO;
import com.wuba.wlock.server.domain.DeleteLockDO;
import com.wuba.wlock.server.domain.ReleaseLockDO;
import com.wuba.wlock.server.domain.RenewLockDO;
import com.wuba.wlock.server.exception.LockException;
import com.wuba.wlock.server.lock.LockResult;
import com.wuba.wlock.server.lock.protocol.LockOwnerInfo;
import com.wuba.wlock.server.lock.protocol.LockSmCtx;
import com.wuba.wlock.server.lock.protocol.LockTypeEnum;
import com.wuba.wlock.server.lock.protocol.ReentrantLockValue;
import com.wuba.wlock.server.lock.repository.LockRepositoryImpl;
import com.wuba.wlock.server.lock.repository.base.ILockRepository;
import com.wuba.wlock.server.lock.service.base.IReentrantLock;
import com.wuba.wlock.server.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class ReentrantLock implements IReentrantLock {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReentrantLock.class);

	private static ILockRepository lockRepositroy = LockRepositoryImpl.getInstance();

	private ReentrantLock() {
	}

	private static IReentrantLock reentrantLockService = new ReentrantLock();

	public static IReentrantLock getInstance() {
		return reentrantLockService;
	}

	@Override
	public boolean tryAcquireLock(AcquireLockDO acquireLockDO, long instanceID, int groupIdx, LockSmCtx smCtx) {
		String key = acquireLockDO.getLockKey();
		ReentrantLockValue lockvalue = createLockvalue(acquireLockDO, instanceID);
		try {
			Optional<ReentrantLockValue> lock = lockRepositroy.getLock(key, groupIdx);
			if (lock.isPresent() && lock.get().getLockVersion() > acquireLockDO.getFencingToken()) {
				if (smCtx != null) {
					smCtx.setLockRet(LockResult.TOKEN_ERROR);
				}
				return true;
			}
			lockRepositroy.lock(key, lockvalue, groupIdx);
			if (smCtx != null) {
				smCtx.setFencingToken(instanceID);
				smCtx.setExpireTime(acquireLockDO.getExpireTime());
			}
			return true;
		} catch (LockException e) {
			LOGGER.error("groupid {} {} try lock key : {} error.", groupIdx, acquireLockDO.getHost(), key);
			LOGGER.error(e.getMessage(), e);
			if (smCtx != null) {
				smCtx.setLockRet(LockResult.EXCEPTION);
			}
			return false;
		}
	}

	private ReentrantLockValue createLockvalue(AcquireLockDO acquireLockDO, long instanceID) {
		ReentrantLockValue reentrantLockValue = new ReentrantLockValue();
		LockOwnerInfo lockOwnerInfo = new LockOwnerInfo();
		reentrantLockValue.setVersion(acquireLockDO.getVersion());
		reentrantLockValue.setLockVersion(instanceID);
		reentrantLockValue.setStatus(0);
		reentrantLockValue.setLockType(LockTypeEnum.reentrantLock.getValue());
		reentrantLockValue.setLockOwnerInfo(lockOwnerInfo);
		lockOwnerInfo.setExpireTime(acquireLockDO.getExpireTime());
		lockOwnerInfo.setThreadId(acquireLockDO.getThreadID());
		lockOwnerInfo.setIp(acquireLockDO.getHost());
		lockOwnerInfo.setPid(acquireLockDO.getPid());
		lockOwnerInfo.setLockVersion(instanceID);
		return reentrantLockValue;
	}

	@Override
	public boolean renewLock(RenewLockDO renewLockDO, int groupIdx, LockSmCtx smCtx) {
		String key = renewLockDO.getLockKey();
		Optional<ReentrantLockValue> lock = null;
		try {
			lock = lockRepositroy.getLock(key, groupIdx);
			if (!lock.isPresent()) {
				/**
				 * 有可能是通过checkpoint同步的数据，初始化为一个无效锁，60s的浮动是为了避免分钟级的过期时间误差
				 */
				if (renewLockDO.getExpireTime() < (TimeUtil.getCurrentTimestamp() + 60000)) {
					ReentrantLockValue reentrantLockValue = new ReentrantLockValue();
					reentrantLockValue.setVersion(renewLockDO.getVersion());
					reentrantLockValue.setLockVersion(renewLockDO.getFencingToken());
					reentrantLockValue.setStatus(0);
					reentrantLockValue.setLockType(LockTypeEnum.reentrantLock.getValue());
					LockOwnerInfo lockOwnerInfo = new LockOwnerInfo();
					lockOwnerInfo.setExpireTime(renewLockDO.getExpireTime() - 60000);
					lockOwnerInfo.setThreadId(renewLockDO.getThreadID());
					lockOwnerInfo.setIp(renewLockDO.getHost());
					lockOwnerInfo.setPid(renewLockDO.getPid());
					lockOwnerInfo.setLockVersion(renewLockDO.getFencingToken());
					reentrantLockValue.setLockOwnerInfo(lockOwnerInfo);
					lock = Optional.of(reentrantLockValue);
					lockRepositroy.renew(key, reentrantLockValue, groupIdx);
					LOGGER.info("renew lock {},but key not exist, update it.", key);
				} else {
					LOGGER.error("renew lock {},but key not exist, expireTime {}.", key, (TimeUtil.getCurrentTimestamp() - renewLockDO.getExpireTime()));
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.KEY_NOT_EXIST);
					}
					return true;
				}
			}
			if (lock.get().getLockVersion() != renewLockDO.getFencingToken()) {
				if (smCtx != null) {
					smCtx.setLockRet(LockResult.TOKEN_ERROR);
				}
				return true;
			}
			ReentrantLockValue reentrantLockValue = lock.get();
			LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
			lockOwnerInfo.setExpireTime(renewLockDO.getExpireTime());
			lockRepositroy.renew(key, reentrantLockValue, groupIdx);
			if (smCtx != null) {
				smCtx.setExpireTime(lockOwnerInfo.getExpireTime());
				smCtx.setFencingToken(lock.get().getLockVersion());
			}
			return true;
		} catch (LockException e) {
			LOGGER.error("{} renew lock key : {} error.", renewLockDO.getHost(), key);
			LOGGER.error(e.getMessage(), e);
			if (smCtx != null) {
				smCtx.setLockRet(LockResult.EXCEPTION);
			}
			return false;
		}
	}

	@Override
	public boolean releaseLock(ReleaseLockDO releaseLockDO, int groupIdx, LockSmCtx smCtx) {
		String key = releaseLockDO.getLockKey();
		Optional<ReentrantLockValue> lock = null;
		try {
			lock = lockRepositroy.getLock(key, groupIdx);
			if (!lock.isPresent()) {
				LOGGER.error("release lock {},but key not exist", key);
				return true;
			}
			if (lock.get().getLockVersion() != releaseLockDO.getFencingToken()) {
				if (smCtx != null) {
					smCtx.setLockRet(LockResult.TOKEN_ERROR);
				}
				return true;
			}
			lockRepositroy.deleteLock(key, groupIdx);
			return true;
		} catch (LockException e) {
			LOGGER.error("{} release lock key : {} error.", releaseLockDO.getHost(), key);
			LOGGER.error(e.getMessage(), e);
			if (smCtx != null) {
				smCtx.setLockRet(LockResult.EXCEPTION);
			}
			return false;
		}
	}

	@Override
	public boolean deleteLock(DeleteLockDO deleteLockDO, int groupIdx, LockSmCtx smCtx) {
		String key = deleteLockDO.getLockKey();
		try {
			Optional<ReentrantLockValue> lock = lockRepositroy.getLock(key, groupIdx);
			if (!lock.isPresent()) {
				return true;
			}
			if (lock.get().getLockVersion() != deleteLockDO.getFencingToken()) {
				if (smCtx != null) {
					smCtx.setLockRet(LockResult.TOKEN_ERROR);
				}
				return true;
			}
			lockRepositroy.deleteLock(key, groupIdx);
			return true;
		} catch (LockException e) {
			LOGGER.error("{} delete lock key : {} error.", deleteLockDO.getHost(), key);
			LOGGER.error(e.getMessage(), e);
			if (smCtx != null) {
				smCtx.setLockRet(LockResult.EXCEPTION);
			}
			return false;
		}
	}

}
