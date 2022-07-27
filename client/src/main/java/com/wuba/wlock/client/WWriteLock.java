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
package com.wuba.wlock.client;

import com.wuba.wlock.client.communication.LockPolicy;
import com.wuba.wlock.client.communication.LockTypeEnum;
import com.wuba.wlock.client.communication.ReadWriteLockTypeEnum;
import com.wuba.wlock.client.config.Factor;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.listener.HoldLockListener;
import com.wuba.wlock.client.listener.LockExpireListener;
import com.wuba.wlock.client.listener.RenewListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.GetLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKey;
import com.wuba.wlock.client.util.UniqueCodeGenerator;

public class WWriteLock implements WLock {
	private WLockClient wlockClient;
	private String lockkey;
	private RegistryKey registryKey;
	private LockPolicy lockPolicy;

	private final int PROCESS_LOCK_THREAD_ID = -1;

	protected WWriteLock(String lockkey, LockPolicy lockPolicy, WLockClient wlockClient) {
		this.lockkey = lockkey;
		this.wlockClient = wlockClient;
		this.registryKey = this.wlockClient.getRegistryKey();
		this.lockPolicy = lockPolicy;
	}

	@Override
	public AcquireLockResult tryAcquireLock(int expireTime, int maxWaitTime) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);

		return this.tryAcquireLock(lockOption);
	}

	@Override
	public AcquireLockResult tryAcquireLock(int expireTime, int maxWaitTime, int renewInterval) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setRenewInterval(renewInterval);

		return this.tryAcquireLock(lockOption);
	}

	@Override
	public AcquireLockResult tryAcquireLock(int expireTime, int maxWaitTime, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	@Override
	public AcquireLockResult tryAcquireLock(int expireTime, int maxWaitTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setRenewListener(renewListener);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 *
	 * 竞争锁，阻塞模式
	 *
	 * @param expireTime       锁过期时间，单位毫秒，默认值为5分钟，支持无限长时间
	 * @param holdLockListener 持有锁回调   : 只有在锁过期时间超过 5min 时候才有效
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLock(int expireTime, HoldLockListener holdLockListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setHoldLockListener(holdLockListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 获取写锁前校验，是否有读锁
	 */
	private void checkCanGetLock() throws ParameterIllegalException {
		LockContext writeLockContext = this.wlockClient.getLockService().getLockManager().getLocalLockContext(lockkey, Thread.currentThread().getId(),
				LockTypeEnum.readWriteReentrantLock.getValue(), ReadWriteLockTypeEnum.Write.getOpcode());
		if (writeLockContext != null && writeLockContext.getAquiredCount() > 0) {
			return;
		}

		LockContext localLockContext = this.wlockClient.getLockService().getLockManager().getLocalLockContext(lockkey, Thread.currentThread().getId(),
				LockTypeEnum.readWriteReentrantLock.getValue(), ReadWriteLockTypeEnum.Read.getOpcode());
		if (localLockContext != null && localLockContext.getAquiredCount() > 0) {
			throw new ParameterIllegalException("Has held a reading lock, it is not allowed to apply for a write lock.");
		}
	}

	@Override
	public AcquireLockResult tryAcquireLock(LockOption lockOption) throws ParameterIllegalException {
		checkCanGetLock();

		if (LockPolicy.Process == lockPolicy) {
			synchronized (lockkey.intern()) {
				return getAcquireLockResult(lockOption);
			}
		}
		return getAcquireLockResult(lockOption);
	}

	private AcquireLockResult getAcquireLockResult(LockOption lockOption) throws ParameterIllegalException {
		dealExpireTime(lockOption, lockOption.getExpireTime());

		InternalLockOption acquireLockOption = new InternalLockOption();
		acquireLockOption.copyLockOption(lockOption);
		long watchID = UniqueCodeGenerator.getUniqueCode();
		acquireLockOption.setWatchID(watchID);
		long threadID = Thread.currentThread().getId();
		if (LockPolicy.Process == lockPolicy) {
			threadID = PROCESS_LOCK_THREAD_ID;
		}
		acquireLockOption.setThreadID(threadID);
		acquireLockOption.setPID(WLockClient.currentPid);
		acquireLockOption.setRegistryKey(this.registryKey.getRegistryKey());
		acquireLockOption.setAutoRenewEnabled(this.registryKey.getAutoRenew());
		return this.wlockClient.getLockService().tryAcquireLock(lockkey, acquireLockOption);
	}


	@Override
	public AcquireLockResult tryAcquireLockUnblocked(int expireTime) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);

		return this.tryAcquireLock(lockOption);
	}

	@Override
	public AcquireLockResult tryAcquireLockUnblocked(int expireTime, int renewInterval) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setRenewInterval(renewInterval);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 *
	 * 竞争锁，非阻塞模式
	 *
	 * @param expireTime       锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param holdLockListener 持有锁回调   : 只有在锁过期时间超过 5min 时候才有效
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLockUnblocked(int expireTime, HoldLockListener holdLockListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setHoldLockListener(holdLockListener);

		return this.tryAcquireLock(lockOption);
	}

	@Override
	public AcquireLockResult tryAcquireLockUnblocked(int expireTime, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	@Override
	public AcquireLockResult tryAcquireLockUnblocked(int expireTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setLockType((byte) LockTypeEnum.readWriteReentrantLock.getValue());
		lockOption.setLockOpcode((byte) ReadWriteLockTypeEnum.Write.getOpcode());
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setRenewListener(renewListener);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	@Override
	public LockResult releaseLock() throws ParameterIllegalException {
		return releaseLock(-1);
	}

	private LockResult releaseLock(long lockversion) throws ParameterIllegalException {
		if (LockPolicy.Process == lockPolicy) {
			synchronized (lockkey.intern()) {
				return getReleaseLockResult(lockversion);
			}
		}
		return getReleaseLockResult(lockversion);
	}

	private LockResult getReleaseLockResult(long lockversion) {
		long threadID = Thread.currentThread().getId();
		if (LockPolicy.Process == lockPolicy) {
			threadID = PROCESS_LOCK_THREAD_ID;
		}
		return this.wlockClient.getLockService().releaseLock(lockkey, lockversion, threadID, LockTypeEnum.readWriteReentrantLock.getValue(), ReadWriteLockTypeEnum.Write.getOpcode());
	}

	@Override
	public LockResult renewLock(int expireTime) throws ParameterIllegalException {
		return renewLock(-1, expireTime);
	}

	private LockResult renewLock(long lockversion, int expireTime) throws ParameterIllegalException {
		long threadID = Thread.currentThread().getId();
		if (LockPolicy.Process == lockPolicy) {
			threadID = PROCESS_LOCK_THREAD_ID;
		}
		return renewLock(lockversion, threadID, expireTime);
	}

	private LockResult renewLock(long lockversion, long ownerThreadID, int expireTime) throws ParameterIllegalException {
		if (LockPolicy.Process == lockPolicy) {
			synchronized (lockkey.intern()) {
				return getRenewLockResult(lockversion, ownerThreadID, expireTime);
			}
		}
		return getRenewLockResult(lockversion, ownerThreadID, expireTime);
	}

	private LockResult getRenewLockResult(long lockversion, long ownerThreadID, int expireTime) throws ParameterIllegalException {
		return this.wlockClient.getLockService().renewLock(lockkey, lockversion, expireTime, ownerThreadID, LockTypeEnum.readWriteReentrantLock.getValue(), ReadWriteLockTypeEnum.Write.getOpcode());
	}

	protected void dealExpireTime(LockOption lockOption, int expireTime) {
		if (expireTime > Factor.LOCK_MAX_EXPIRETIME) {
			lockOption.setExpireTime(Factor.LOCK_MAX_EXPIRETIME);
			lockOption.setRenewInterval(Factor.HOLD_LOCK_RENEWINTERVAL);
			lockOption.setRealExpireMills(expireTime);
		} else {
			lockOption.setExpireTime(expireTime);
		}
	}

	/**
	 * 读取锁当前状态
	 *
	 * @return 锁当前状态信息
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public GetLockResult getLockState() throws ParameterIllegalException {
		return this.wlockClient.getLockService().getLockState(lockkey);
	}

	@Override
	public String getLockkey() {
		return this.lockkey;
	}

}
