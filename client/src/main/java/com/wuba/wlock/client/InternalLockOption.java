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

import com.wuba.wlock.client.config.Factor;

public class InternalLockOption extends LockOption {
	/**
	 * 锁版本号
	 */
	private long lockversion;
	/**
	 * watch event ID
	 */
	private long watchID;
	/**
	 * 发起请求的线程ID
	 */
	private long threadID;
	/**
	 * 当前进程ID
	 */
	private int PID;
	/**
	 * 秘钥hash key
	 */
	private String registryKey;
	
	/**
	 * 自动重试接口调用是否允许
	 */
	private boolean autoRenewEnabled;

	public void copyLockOption(LockOption lockOption) {
		this.waitAcquire = lockOption.isWaitAcquire();
		this.expireTime = lockOption.getExpireTime();
		this.maxWaitTime = lockOption.getMaxWaitTime();
		this.weight = lockOption.getWeight();
		this.renewInterval = lockOption.getRenewInterval();
		this.renewListener = lockOption.getRenewListener();
		this.lockExpireListener = lockOption.getLockExpireListener();
		this.watchListener = lockOption.getWatchListener();
		this.watchPolicy = lockOption.getWatchPolicy();
		this.holdLockListener = lockOption.getHoldLockListener();
		this.realExpireMills = lockOption.getRealExpireMills();
		this.realExpireTimeStamp = lockOption.getRealExpireTimeStamp();
		this.lockType = lockOption.getLockType();
		this.lockOpcode = lockOption.getLockOpcode();
	}
	
	public boolean isAutoRenew() {
		return this.renewInterval != Factor.LOCK_NOT_RENEWINTERVAL && realExpireMills == 0;
	}

	public boolean isAutoRenewEnabled() {
		return autoRenewEnabled;
	}

	public void setAutoRenewEnabled(boolean autoRenewEnabled) {
		this.autoRenewEnabled = autoRenewEnabled;
	}

	public long getLockversion() {
		return lockversion;
	}

	public void setLockversion(long lockversion) {
		this.lockversion = lockversion;
	}

	public long getWatchID() {
		return watchID;
	}

	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}

	@Override
	public int getRenewInterval() {
		return renewInterval;
	}

	public long getThreadID() {
		return threadID;
	}

	public void setThreadID(long threadID) {
		this.threadID = threadID;
	}

	public String getRegistryKey() {
		return registryKey;
	}

	public void setRegistryKey(String registryKey) {
		this.registryKey = registryKey;
	}

	public int getPID() {
		return PID;
	}

	public void setPID(int pID) {
		PID = pID;
	}
}