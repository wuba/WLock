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
import com.wuba.wlock.server.lock.protocol.*;
import com.wuba.wlock.server.lock.repository.LockRepositoryImpl;
import com.wuba.wlock.server.lock.repository.base.ILockRepository;
import com.wuba.wlock.server.lock.service.base.IReadWriteLock;
import com.wuba.wlock.server.lock.service.base.IReentrantLock;
import com.wuba.wlock.server.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class ReadWriteLock implements IReadWriteLock {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteLock.class);

	private static ILockRepository lockRepositroy = LockRepositoryImpl.getInstance();
	private ReadLock readLock;
	private WriteLock writeLock;

	private ReadWriteLock() {
		readLock = new ReadLock();
		writeLock = new WriteLock();
	}

	private static IReentrantLock reentrantLock = new ReadWriteLock();

	public static IReentrantLock getInstance() {
		return reentrantLock;
	}



	@Override
	public boolean tryAcquireLock(AcquireLockDO acquireLockDO, long instanceID, int groupIdx, LockSmCtx smCtx) {
		return lock(acquireLockDO.getOpcode()).tryAcquireLock(acquireLockDO, instanceID, groupIdx, smCtx);
	}



	@Override
	public boolean renewLock(RenewLockDO renewLockDO, int groupIdx, LockSmCtx smCtx) {
		return lock(renewLockDO.getOpcode()).renewLock(renewLockDO, groupIdx, smCtx);
	}

	@Override
	public boolean releaseLock(ReleaseLockDO releaseLockDO, int groupIdx, LockSmCtx smCtx) {
		return lock(releaseLockDO.getOpcode()).releaseLock(releaseLockDO, groupIdx, smCtx);
	}

	@Override
	public boolean deleteLock(DeleteLockDO deleteLockDO, int groupIdx, LockSmCtx smCtx) {
		return lock(deleteLockDO.getOpcode()).deleteLock(deleteLockDO, groupIdx, smCtx);
	}

	private ReentrantLockValue createWriteLockvalue(AcquireLockDO acquireLockDO, long instanceID) {
		ReentrantLockValue reentrantLockValue = new ReentrantLockValue();
		reentrantLockValue.setVersion(acquireLockDO.getVersion());
		reentrantLockValue.setLockVersion(instanceID);
		reentrantLockValue.setStatus(0);
		reentrantLockValue.setLockVersion(instanceID);
		reentrantLockValue.setLockType(LockTypeEnum.readWriteReentrantLock.getValue());
		reentrantLockValue.setLockOwnerInfo(createLockOwnerInfo(acquireLockDO, instanceID));
		return reentrantLockValue;
	}

	private ReentrantLockValue createReadLockvalue(AcquireLockDO acquireLockDO, long instanceID) {
		ReentrantLockValue reentrantLockValue = new ReentrantLockValue();
		reentrantLockValue.setVersion(acquireLockDO.getVersion());
		reentrantLockValue.setStatus(0);
		reentrantLockValue.setLockType(LockTypeEnum.readWriteReentrantLock.getValue());
		reentrantLockValue.addReadLock(createLockOwnerInfo(acquireLockDO, instanceID));
		return reentrantLockValue;
	}

	private LockOwnerInfo createLockOwnerInfo(AcquireLockDO acquireLockDO, long instanceID) {
		LockOwnerInfo lockOwnerInfo = new LockOwnerInfo();
		lockOwnerInfo.setExpireTime(acquireLockDO.getExpireTime());
		lockOwnerInfo.setThreadId(acquireLockDO.getThreadID());
		lockOwnerInfo.setIp(acquireLockDO.getHost());
		lockOwnerInfo.setPid(acquireLockDO.getPid());
		lockOwnerInfo.setLockVersion(instanceID);
		return lockOwnerInfo;
	}

	private class ReadLock implements IReentrantLock {

		@Override
		public boolean tryAcquireLock(AcquireLockDO acquireLockDO, long instanceID, int groupIdx, LockSmCtx smCtx) {
			String key = acquireLockDO.getLockKey();
			try {
				ReentrantLockValue lockvalue = null;
				Optional<ReentrantLockValue> lock = lockRepositroy.getLock(key, groupIdx);
				if (lock.isPresent() && lock.get().existLock()) {
					lockvalue = lock.get();
					// 存在写锁，并且写锁不是自己，失败
					if (lockvalue.existWriteLock() && !lockvalue.getLockOwnerInfo().equals(acquireLockDO.getHost(), acquireLockDO.getThreadID(), acquireLockDO.getPid())) {
						if (smCtx != null) {
							smCtx.setLockRet(LockResult.OWNER_ERROR);
						}
						return true;
					}

					LockOwnerInfo lockOwnerInfo = createLockOwnerInfo(acquireLockDO, instanceID);
					lockvalue.setLockVersion(instanceID);
					lockvalue.addReadLock(lockOwnerInfo);
				} else {
					lockvalue = createReadLockvalue(acquireLockDO, instanceID);
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

		@Override
		public boolean renewLock(RenewLockDO renewLockDO, int groupIdx, LockSmCtx smCtx) {
			String key = renewLockDO.getLockKey();
			Optional<ReentrantLockValue> lock = null;
			try {
				lock = lockRepositroy.getLock(key, groupIdx);
				if (!lock.isPresent() || !lock.get().existReadLock(renewLockDO.getHost(), renewLockDO.getThreadID(), renewLockDO.getPid())) {
					LOGGER.error("renew lock {},but key not exist, expireTime {}.", key, (TimeUtil.getCurrentTimestamp() - renewLockDO.getExpireTime()));
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.KEY_NOT_EXIST);
					}
					return true;
				}

				ReentrantLockValue reentrantLockValue = lock.get();
				LockOwnerInfo lockOwnerInfo = reentrantLockValue.getReadLockOwner(renewLockDO.getHost(), renewLockDO.getThreadID(), renewLockDO.getPid());
				if (lockOwnerInfo.getLockVersion() != renewLockDO.getFencingToken()) {
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.TOKEN_ERROR);
					}
					return true;
				}

				lockOwnerInfo.setExpireTime(renewLockDO.getExpireTime());
				lockRepositroy.renew(key, reentrantLockValue, groupIdx);
				if (smCtx != null) {
					smCtx.setExpireTime(lockOwnerInfo.getExpireTime());
					smCtx.setFencingToken(lockOwnerInfo.getLockVersion());
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

				ReentrantLockValue reentrantLockValue = lock.get();
				if (!reentrantLockValue.existReadLock(releaseLockDO.getHost(), releaseLockDO.getThreadID(), releaseLockDO.getPid())) {
					LOGGER.error("release lock {},but lock owner not exist", key);
					return true;
				}

				LockOwnerInfo readLockOwner = reentrantLockValue.getReadLockOwner(releaseLockDO.getHost(), releaseLockDO.getThreadID(), releaseLockDO.getPid());
				if (readLockOwner.getLockVersion() != releaseLockDO.getFencingToken()) {
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.TOKEN_ERROR);
					}
					return true;
				}

				reentrantLockValue.removeReadLock(readLockOwner);
				if (reentrantLockValue.existLock()) {
					lockRepositroy.update(key, reentrantLockValue, groupIdx);
				} else {
					lockRepositroy.deleteLock(key, groupIdx);
				}

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
					LOGGER.error("delete lock {},but key not exist", key);
					return true;
				}

				ReentrantLockValue reentrantLockValue = lock.get();
				if (!reentrantLockValue.existReadLock(deleteLockDO.getHost(), deleteLockDO.getThreadID(), deleteLockDO.getPid())) {
					LOGGER.error("delete lock {},but key not exist", key);
					return true;
				}

				LockOwnerInfo lockOwnerInfo = reentrantLockValue.getReadLockOwner(deleteLockDO.getHost(), deleteLockDO.getThreadID(), deleteLockDO.getPid());
				if (lockOwnerInfo.getLockVersion() != deleteLockDO.getFencingToken()) {
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.TOKEN_ERROR);
					}
					return true;
				}

				reentrantLockValue.removeReadLock(lockOwnerInfo);
				if (reentrantLockValue.existLock()) {
					lockRepositroy.update(key, reentrantLockValue, groupIdx);
				} else {
					lockRepositroy.deleteLock(key, groupIdx);
				}

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

	private class WriteLock implements IReentrantLock {

		@Override
		public boolean tryAcquireLock(AcquireLockDO acquireLockDO, long instanceID, int groupIdx, LockSmCtx smCtx) {
			String key = acquireLockDO.getLockKey();

			try {
				Optional<ReentrantLockValue> lock = lockRepositroy.getLock(key, groupIdx);
				ReentrantLockValue reentrantLockValue = null;
				if (lock.isPresent() && lock.get().existLock()) {
					reentrantLockValue = lock.get();
					LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
					if (lockOwnerInfo != null && !lockOwnerInfo.equals(acquireLockDO.getHost(), acquireLockDO.getThreadID(), acquireLockDO.getPid())) {
						if (smCtx != null) {
							smCtx.setLockRet(LockResult.OWNER_ERROR);
							return true;
						}
					} else {
						reentrantLockValue.setLockOwnerInfo(createLockOwnerInfo(acquireLockDO, instanceID));
						reentrantLockValue.setLockVersion(instanceID);
					}
				}

				if (reentrantLockValue == null) {
					reentrantLockValue = createWriteLockvalue(acquireLockDO, instanceID);
				}

				lockRepositroy.lock(key, reentrantLockValue, groupIdx);
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

		@Override
		public boolean renewLock(RenewLockDO renewLockDO, int groupIdx, LockSmCtx smCtx) {
			String key = renewLockDO.getLockKey();
			Optional<ReentrantLockValue> lock = null;
			try {
				lock = lockRepositroy.getLock(key, groupIdx);
				if (!lock.isPresent()) {
					LOGGER.error("renew lock {},but key not exist, expireTime {}.", key, (TimeUtil.getCurrentTimestamp() - renewLockDO.getExpireTime()));
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.KEY_NOT_EXIST);
					}
					return true;
				}

				ReentrantLockValue reentrantLockValue = lock.get();
				LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
				if (lockOwnerInfo == null) {
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.KEY_NOT_EXIST);
					}
					return true;
				}

				if (lockOwnerInfo.getLockVersion() != renewLockDO.getFencingToken()) {
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.TOKEN_ERROR);
					}
					return true;
				}

				lockOwnerInfo.setExpireTime(renewLockDO.getExpireTime());
				lockRepositroy.renew(key, reentrantLockValue, groupIdx);
				if (smCtx != null) {
					smCtx.setExpireTime(lockOwnerInfo.getExpireTime());
					smCtx.setFencingToken(lockOwnerInfo.getLockVersion());
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
				if (!lock.isPresent() || lock.get().getLockOwnerInfo() == null) {
					LOGGER.error("release lock {},but key not exist", key);
					return true;
				}

				if (lock.get().getLockOwnerInfo().getLockVersion() != releaseLockDO.getFencingToken()) {
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.TOKEN_ERROR);
					}
					return true;
				}

				ReentrantLockValue reentrantLockValue = lock.get();
				reentrantLockValue.setLockOwnerInfo(null);
				if (reentrantLockValue.existLock()) {
					lockRepositroy.update(key, reentrantLockValue, groupIdx);
				} else {
					lockRepositroy.deleteLock(key, groupIdx);
				}

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
					LOGGER.error("delete lock {},but key not exist", key);
					return true;
				}

				ReentrantLockValue reentrantLockValue = lock.get();
				LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
				if (lockOwnerInfo == null) {
					LOGGER.error("delete lock {},but key not exist", key);
					return true;
				}

				if (lockOwnerInfo.getLockVersion() != deleteLockDO.getFencingToken()) {
					if (smCtx != null) {
						smCtx.setLockRet(LockResult.TOKEN_ERROR);
					}
					return true;
				}

				reentrantLockValue.setLockOwnerInfo(null);
				if (reentrantLockValue.existLock()) {
					lockRepositroy.update(key, reentrantLockValue, groupIdx);
				} else {
					lockRepositroy.deleteLock(key, groupIdx);
				}

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

	private IReentrantLock lock(byte opcode) {
		if (opcode == OpcodeEnum.ReadWriteOpcode.READ.getValue()) {
			return readLock;
		} else if (opcode == OpcodeEnum.ReadWriteOpcode.WRITE.getValue()) {
			return writeLock;
		}

		throw new IllegalArgumentException("opcode["+opcode+"] not exit");
	}
}
