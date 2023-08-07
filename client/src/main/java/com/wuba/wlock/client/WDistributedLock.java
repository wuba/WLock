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
import com.wuba.wlock.client.communication.WatchPolicy;
import com.wuba.wlock.client.config.Factor;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.listener.HoldLockListener;
import com.wuba.wlock.client.listener.LockExpireListener;
import com.wuba.wlock.client.listener.RenewListener;
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.GetLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKey;
import com.wuba.wlock.client.util.UniqueCodeGenerator;

public class WDistributedLock implements WLock {
	private WLockClient wlockClient;
	private String lockkey;
	private RegistryKey registryKey;
	private LockPolicy lockPolicy;

	private final int PROCESS_LOCK_THREAD_ID = -1;

	protected WDistributedLock(String lockkey, WLockClient wlockClient) {
		this(lockkey,LockPolicy.Thread,wlockClient);
	}

	protected WDistributedLock(String lockkey, LockPolicy lockPolicy, WLockClient wlockClient) {
		super();
		this.lockkey = lockkey;
		this.wlockClient = wlockClient;
		this.registryKey = this.wlockClient.getRegistryKey();
		this.lockPolicy = lockPolicy;
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLock(long expireTime , int maxWaitTime) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLock(long expireTime , int maxWaitTime, int renewInterval) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setRenewInterval(renewInterval);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLock(long expireTime , int maxWaitTime, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param weight 锁权重，默认都为1，取值范围[1, 10],权重越高，获取到锁概率越高
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public AcquireLockResult tryAcquireLock(long expireTime , int maxWaitTime, int weight, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setWeight(weight);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param renewListener 续租结果回调
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLock(long expireTime , int maxWaitTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setRenewListener(renewListener);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param weight 锁权重，默认都为1，取值范围[1, 10],权重越高，获取到锁概率越高
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param renewListener 续租结果回调
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public AcquireLockResult tryAcquireLock(long expireTime , int maxWaitTime, int weight, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setWeight(weight);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setRenewListener(renewListener);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，支持无限长时间
	 * @param holdLockListener 持有锁回调   : 只有在锁过期时间超过 5min 时候才有效
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLock(long expireTime, HoldLockListener holdLockListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setHoldLockListener(holdLockListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，支持无限长时间
	 * @param maxWaitTime 最大等待时间
	 * @param holdLockListener 持有锁回调   : 只有在锁过期时间超过 5min 时候才有效
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public AcquireLockResult tryAcquireLock(long expireTime, long maxWaitTime, HoldLockListener holdLockListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setHoldLockListener(holdLockListener);
		lockOption.setMaxWaitTime(maxWaitTime);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，非阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLockUnblocked(long expireTime) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，非阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLockUnblocked(long expireTime, int renewInterval) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setRenewInterval(renewInterval);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，非阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param holdLockListener 持有锁回调   : 只有在锁过期时间超过 5min 时候才有效
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLockUnblocked(long expireTime, HoldLockListener holdLockListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setHoldLockListener(holdLockListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，非阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLockUnblocked(long expireTime, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，非阻塞模式
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param renewListener 续租结果回调
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLockUnblocked(long expireTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(false);
		lockOption.setExpireTime(expireTime);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setRenewListener(renewListener);
		lockOption.setLockExpireListener(lockExpireListener);

		return this.tryAcquireLock(lockOption);
	}

	/**
	 * 竞争锁，所有参数自定义
	 * @param lockOption 自定义锁选项
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	@Override
	public AcquireLockResult tryAcquireLock(LockOption lockOption) throws ParameterIllegalException {
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

	/**
	 * watchlock 有锁变化事件触发
	 * @param lockversion 锁当前版本(fencingToken)
	 * @param watchListener 锁变化回调
	 * @return 返回结果，true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult watchlock(long lockversion, WatchListener watchListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(false);
		lockOption.setWatchListener(watchListener);

		return watchlock(lockversion, lockOption);
	}

	/**
	 * watchlock 有锁变化事件触发
	 * @param lockversion 锁当前版本(fencingToken)
	 * @param maxWaitTime 最长watch等待时间,默认值为Long.MAX_VALUE
	 * @param watchListener 锁变化回调
	 * @return 返回结果 ，true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult watchlock(long lockversion, long maxWaitTime, WatchListener watchListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(false);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setWatchListener(watchListener);

		return watchlock(lockversion, lockOption);
	}

	/**
	 * watch并且等待获取锁
	 * @param lockversion 锁当前版本(fencingToken)
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param watchListener 锁变化回调
	 * @return 返回结果 ，true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult watchAndWaitLock(long lockversion, long expireTime, WatchListener watchListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setWatchListener(watchListener);

		return watchlock(lockversion, lockOption);
	}

	/**
	 * watch并且等待获取锁
	 * @param lockversion 锁当前版本(fencingToken)，每次锁owner发生变更时值会更新
	 * @param expireTime 锁过期时间，单位毫秒，默认值5分钟，最大取值5分钟,最小值5秒
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param watchListener 锁变化回调
	 * @return 返回结果 ，true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult watchAndWaitLock(long lockversion, long expireTime, int renewInterval, WatchListener watchListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setWatchListener(watchListener);

		return watchlock(lockversion, lockOption);
	}

	/**
	 * watch并且等待获取锁
	 * @param lockversion 锁当前版本(fencingToken)，每次锁owner发生变更时值会更新
	 * @param expireTime 锁过期时间，单位毫秒，默认值5分钟，最大取值5分钟,最小值5秒
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param weight 锁权重，默认都为1，取值范围[1, 10],权重越高，获取到锁概率越高
	 * @param watchListener 锁变化回调
	 * @return 返回结果 ，true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult watchAndWaitLock(long lockversion, long expireTime, int renewInterval, int weight, WatchListener watchListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setWeight(weight);
		lockOption.setWatchListener(watchListener);

		return watchlock(lockversion, lockOption);
	}

	/**
	 * watch并且等待获取锁
	 * @param lockversion 锁当前版本(fencingToken)，每次锁owner发生变更时值会更新
	 * @param expireTime 锁过期时间，单位毫秒，默认值5分钟，最大取值5分钟,最小值5秒
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param weight 锁权重，默认都为1，取值范围[1, 10],权重越高，获取到锁概率越高
	 * @param maxWaitTime 最长watch等待时间,默认值为Long.MAX_VALUE
	 * @param watchListener 锁变化回调
	 * @return 返回结果，true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult watchAndWaitLock(long lockversion, long expireTime, int renewInterval, int weight, long maxWaitTime, WatchListener watchListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setExpireTime(expireTime);
		lockOption.setRenewInterval(renewInterval);
		lockOption.setWeight(weight);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setWatchListener(watchListener);

		return watchlock(lockversion, lockOption);
	}

	/**
	 * watch并且等待获取锁
	 * @param lockVersion 锁当前版本(fencingToken)，每次锁owner发生变更时值会更新
	 * @param weight 锁权重，默认都为1，取值范围[1, 10],权重越高，获取到锁概率越高
	 * @param maxWaitTime 最长watch等待时间,默认值为Long.MAX_VALUE
	 * @param watchListener 锁变化回调
	 * @return 返回结果，true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult continueWatchAndWaitLock(long lockVersion, int weight, long maxWaitTime, WatchListener watchListener) throws ParameterIllegalException {
		LockOption lockOption = new LockOption();
		lockOption.setWaitAcquire(true);
		lockOption.setWeight(weight);
		lockOption.setMaxWaitTime(maxWaitTime);
		lockOption.setWatchPolicy(WatchPolicy.Continue);
		lockOption.setWatchListener(watchListener);
		return watchlock(lockVersion, lockOption);
	}

	/**
	 * watch并且等待获取锁 参数自定义
	 * @param lockVersion
	 * @param lockOption
	 * @return
	 * @throws ParameterIllegalException
	 */
	public LockResult continueWatchAndWaitLock(long lockVersion, LockOption lockOption) throws ParameterIllegalException {
		return watchlock(lockVersion, lockOption);
	}

	/**
	 * watchlock, 所有参数自定义
	 * @param lockversion 锁当前版本(fencingToken)，每次锁owner发生变更时值会更新
	 * @param lockOption 自定义锁选项
	 * @return 返回结果 true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult watchlock(long lockversion, LockOption lockOption) throws ParameterIllegalException {
		if (LockPolicy.Process == lockPolicy) {
			synchronized (lockkey.intern()) {
				return getWatchLockResult(lockversion, lockOption);
			}
		}
		return getWatchLockResult(lockversion, lockOption);
	}

	private LockResult getWatchLockResult(long lockversion, LockOption lockOption) throws ParameterIllegalException {
		dealExpireTime(lockOption, lockOption.getExpireTime());

		InternalLockOption watchLockOption = new InternalLockOption();
		watchLockOption.copyLockOption(lockOption);
		watchLockOption.setLockversion(lockversion);
		long watchID = UniqueCodeGenerator.getUniqueCode();
		watchLockOption.setWatchID(watchID);
		long threadID = Thread.currentThread().getId();
		if (LockPolicy.Process == lockPolicy) {
			threadID = PROCESS_LOCK_THREAD_ID;
		}
		watchLockOption.setThreadID(threadID);
		watchLockOption.setPID(WLockClient.currentPid);
		watchLockOption.setRegistryKey(this.registryKey.getRegistryKey());
		watchLockOption.setAutoRenewEnabled(this.registryKey.getAutoRenew());
		return this.wlockClient.getLockService().watchLock(lockkey, watchLockOption);
	}

//	public boolean unWatchLock(String lockkey) {
//		return false;
//	}

	/**
	 * 释放锁，释放当前线程持有的锁，只有锁owner与lockversion（version从缓存上下文中获取）未发生变化，且锁未被过期删除时才可以释放成功
	 * @return 返回结果 true 请求发送成功， false 请求发送
	 */
	@Override
	public LockResult releaseLock() throws ParameterIllegalException {
		return releaseLock(-1);
	}

	/**
	 * 释放锁，释放当前线程持有的锁，只有锁owner与lockversion未发生变化，且锁未被过期删除时才可以释放成功
	 * @param lockversion 锁当前版本(fencingToken)
	 * @return 返回结果 true 请求发送成功， false 请求发送
	 */
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
		return this.wlockClient.getLockService().releaseLock(lockkey, lockversion, threadID, LockTypeEnum.reentrantLock.getValue(), ReadWriteLockTypeEnum.None.getOpcode());
	}

	/**
	 * 锁续约,只有锁owner与lockversion未发生变化，且锁未被过期删除时才可以续约成功
	 * @param expireTime 锁过期时间，单位毫秒，默认值5分钟，最大取值5分钟,最小值5秒
	 * @return 返回结果 true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	public LockResult renewLock(long expireTime) throws ParameterIllegalException {
		return renewLock(-1, expireTime);
	}

	/**
	 * 锁续约,只有锁owner与lockversion未发生变化，且锁未被过期删除时才可以续约成功
	 * @param lockversion 锁当前版本(fencingToken)
	 * @param expireTime 锁过期时间，单位毫秒，默认值5分钟，最大取值5分钟,最小值5秒
	 * @return 返回结果 true,续约成功, false, 续约失败
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	private LockResult renewLock(long lockversion, long expireTime) throws ParameterIllegalException {
		long threadID = Thread.currentThread().getId();
		if (LockPolicy.Process == lockPolicy) {
			threadID = PROCESS_LOCK_THREAD_ID;
		}
		return renewLock(lockversion, threadID, expireTime);
	}

	/**
	 * 锁续约,只有锁owner与lockversion未发生变化，且锁未被过期删除时才可以续约成功
	 * @param lockversion 锁当前版本(fencingToken)
	 * @param expireTime 锁过期时间，单位毫秒，默认值5分钟，最大取值5分钟,最小值5秒
	 * @param ownerThreadID owner线程ID 说明进程锁 id 需要设置-1
	 * @return 返回结果 true, 续约成功, false, 续约失败
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	private LockResult renewLock(long lockversion, long ownerThreadID, long expireTime) throws ParameterIllegalException {
		if (LockPolicy.Process == lockPolicy) {
			synchronized (lockkey.intern()){
				return getRenewLockResult(lockversion, ownerThreadID, expireTime);
			}
		}
		return getRenewLockResult(lockversion, ownerThreadID, expireTime);
	}

	private LockResult getRenewLockResult(long lockversion, long ownerThreadID, long expireTime) throws ParameterIllegalException {
		return this.wlockClient.getLockService().renewLock(lockkey, lockversion, expireTime, ownerThreadID, LockTypeEnum.reentrantLock.getValue(), 0);
	}


	/**
	 * 读取锁当前状态
	 * @return 锁当前状态信息
	 */
	@Override
	public GetLockResult getLockState() {
		return this.wlockClient.getLockService().getLockState(lockkey);
	}

	@Override
	public String getLockkey() {
		return lockkey;
	}

	protected void dealExpireTime(LockOption lockOption , long expireTime){
		if (expireTime > Factor.LOCK_MAX_EXPIRETIME) {
			lockOption.setExpireTime(Factor.LOCK_MAX_EXPIRETIME);
			lockOption.setRenewInterval(Factor.HOLD_LOCK_RENEWINTERVAL);
			lockOption.setRealExpireMills(expireTime);
		} else {
			lockOption.setExpireTime(expireTime);
		}
	}
}
