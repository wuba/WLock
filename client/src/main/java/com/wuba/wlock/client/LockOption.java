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


import com.wuba.wlock.client.communication.WatchPolicy;
import com.wuba.wlock.client.config.Factor;
import com.wuba.wlock.client.listener.HoldLockListener;
import com.wuba.wlock.client.listener.LockExpireListener;
import com.wuba.wlock.client.listener.RenewListener;
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.util.TimeUtil;

public class LockOption {
	/**
	 *  0：可重入锁 1：读写锁 , 默认可重入锁
	 */
	protected byte lockType = 0;
	/**
	 * 0为null，读写锁操作码：写锁 1， 读锁 2 , 默认 0
	 */
	protected byte lockOpcode = 0;
	/**
	 * watch 策略 : 一次 watch 还是持续 watch , 默认是 1 次
	 */
	protected WatchPolicy watchPolicy = WatchPolicy.Once;
	/**
	 * 是否阻塞等待获取到锁
	 */
	protected boolean waitAcquire;
	/**
	 * 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 */
	protected long expireTime = Factor.LOCK_MAX_EXPIRETIME;
	/**
	 * 锁权重，默认都为1，取值范围[1, 10],权重越高，获取到锁概率越高
	 */
	protected int weight = Factor.ACQUIRE_LOCK_MIN_WEIGHT;
	/**
	 * 最长阻塞等待时间,默认值为Long.MAX_VALUE
	 */
	protected long maxWaitTime = Factor.WATCH_MAX_WAIT_TIME_MARK;
	/**
	 * 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 */
	protected int renewInterval = Factor.LOCK_NOT_RENEWINTERVAL;
	/**
	 * 续约Listener回调
	 */
	protected RenewListener renewListener;
	/**
	 * 锁过期Listener回调
	 */
	protected LockExpireListener lockExpireListener;
	/**
	 * 监听事件回调
	 */
	protected WatchListener watchListener;
	/**
	 * 长期持有锁回调
	 */
	protected HoldLockListener holdLockListener;

	/**
	 * 真实的过期时间
	 */
	protected long realExpireMills;

	/**
	 * 真实过期 时间戳
	 */
	protected long realExpireTimeStamp;

	public static LockOption newOption() {
		return new LockOption();
	}
	
	/**
	 * 是否阻塞等待获取到锁
	 * @return
	 */
	public boolean isWaitAcquire() {
		return waitAcquire;
	}

	/**
	 * 设置是否阻塞等待获取到锁
	 * @param waitAcquire
	 */
	public LockOption setWaitAcquire(boolean waitAcquire) {
		this.waitAcquire = waitAcquire;
		return this;
	}

	/**
	 * 获取锁过期时间(并不是实际过期时间戳)
	 * @return
	 */
	public long getExpireTime() {
		return expireTime;
	}
	
	/**
	 * 设置锁过期时间 
	 * @param expireTime 单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 */
	public LockOption setExpireTime(long expireTime) {
		this.expireTime = expireTime;
		return this;
	}
	
	/**
	 * 获取锁权重
	 * @return
	 */
	public int getWeight() {
		return weight;
	}
	
	/**
	 * 设置锁权重，
	 * @param weight 默认都为1，取值范围[1, 10],权重越高，获取到锁概率越高
	 */
	public LockOption setWeight(int weight) {
		this.weight = weight;
		return this;
	}
	
	/**
	 * 获取锁最长阻塞等待时间
	 * @return
	 */
	public long getMaxWaitTime() {
		return maxWaitTime;
	}
	
	/**
	 * 设置获取锁最长阻塞等待时间
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 */
	public LockOption setMaxWaitTime(long maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
		return this;
	}
	
	/**
	 * 获取锁自动续约间隔
	 * @return
	 */
	public int getRenewInterval() {
		return renewInterval;
	}
	
	/**
	 * 设置锁自动续约间隔
	 * @param renewInterval 单位毫秒(默认为0，不自动续租，最小自动续租间隔为1000ms，由业务控制)
	 */
	public LockOption setRenewInterval(int renewInterval) {
		this.renewInterval = renewInterval;
		return this;
	}
	
	/**
	 * 获取锁续约listener回调
	 * @return
	 */
	public RenewListener getRenewListener() {
		return renewListener;
	}
	
	/**
	 * 设置锁续约listener回调
	 * @param renewListener
	 */
	public LockOption setRenewListener(RenewListener renewListener) {
		this.renewListener = renewListener;
		return this;
	}
	
	/**
	 * 获取锁过期listener回调
	 * @return
	 */
	public LockExpireListener getLockExpireListener() {
		return lockExpireListener;
	}
	
	/**
	 * 设置锁过期listener回调
	 * @param lockExpireListener
	 */
	public LockOption setLockExpireListener(LockExpireListener lockExpireListener) {
		this.lockExpireListener = lockExpireListener;
		return this;
	}

	/**
	 * 获取锁监听回调
	 * @return
	 */
	public WatchListener getWatchListener() {
		return watchListener;
	}

	/**
	 * 设置锁监听回调
	 * @param watchListener
	 */
	public LockOption setWatchListener(WatchListener watchListener) {
		this.watchListener = watchListener;
		return this;
	}

	public WatchPolicy getWatchPolicy() {
		return watchPolicy;
	}

	public LockOption setWatchPolicy(WatchPolicy watchPolicy) {
		this.watchPolicy = watchPolicy;
		return this;
	}

	public long getRealExpireTimeStamp() {
		return realExpireTimeStamp;
	}

	public void setRealExpireTimeStamp(long realExpireTimeStamp) {
		this.realExpireTimeStamp = realExpireTimeStamp;
	}

	public HoldLockListener getHoldLockListener() {
		return holdLockListener;
	}

	public LockOption setHoldLockListener(HoldLockListener holdLockListener) {
		this.holdLockListener = holdLockListener;
		return this;
	}

	public byte getLockType() {
		return lockType;
	}

	public void setLockType(byte lockType) {
		this.lockType = lockType;
	}

	public byte getLockOpcode() {
		return lockOpcode;
	}

	public void setLockOpcode(byte lockOpcode) {
		this.lockOpcode = lockOpcode;
	}

	public long getRealExpireMills() {
		return realExpireMills;
	}

	public void setRealExpireMills(long realExpireMills) {
		this.realExpireMills = realExpireMills;
	}

	public void updateRealExpireTime() {
		if (realExpireMills > Factor.LOCK_MAX_EXPIRETIME) {
			this.realExpireTimeStamp = realExpireMills + TimeUtil.getCurrentMills();
		}
	}
}
