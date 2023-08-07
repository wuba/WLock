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

import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.listener.HoldLockListener;
import com.wuba.wlock.client.listener.LockExpireListener;
import com.wuba.wlock.client.listener.RenewListener;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.GetLockResult;
import com.wuba.wlock.client.lockresult.LockResult;

public interface WLock {

	/**
	 * 竞争锁，阻塞模式
	 *
	 * @param expireTime  锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime 最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLock(long expireTime, int maxWaitTime) throws ParameterIllegalException;

	/**
	 * 竞争锁，阻塞模式
	 *
	 * @param expireTime    锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime   最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLock(long expireTime, int maxWaitTime, int renewInterval) throws ParameterIllegalException;

	/**
	 * 竞争锁，阻塞模式
	 *
	 * @param expireTime         锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime        最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLock(long expireTime, int maxWaitTime, LockExpireListener lockExpireListener) throws ParameterIllegalException;


	/**
	 * 竞争锁，阻塞模式
	 *
	 * @param expireTime         锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param maxWaitTime        最大等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE
	 * @param renewInterval      自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param renewListener      续租结果回调
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLock(long expireTime, int maxWaitTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener) throws ParameterIllegalException;

	/**
	 *
	 * 竞争锁，阻塞模式
	 *
	 * @param expireTime       锁过期时间，单位毫秒，默认值为5分钟，支持无限长时间
	 * @param holdLockListener 持有锁回调   : 只有在锁过期时间超过 5min 时候才有效
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLock(long expireTime, HoldLockListener holdLockListener) throws ParameterIllegalException;

	/**
	 * 竞争锁，所有参数自定义
	 *
	 * @param lockOption 自定义锁选项
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLock(LockOption lockOption) throws ParameterIllegalException;

	/**
	 * 竞争锁，非阻塞模式
	 *
	 * @param expireTime 锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLockUnblocked(long expireTime) throws ParameterIllegalException;

	/**
	 * 竞争锁，非阻塞模式
	 *
	 * @param expireTime    锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param renewInterval 自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLockUnblocked(long expireTime, int renewInterval) throws ParameterIllegalException;

	/**
	 *
	 * 竞争锁，非阻塞模式
	 *
	 * @param expireTime       锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param holdLockListener 持有锁回调   : 只有在锁过期时间超过 5min 时候才有效
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLockUnblocked(long expireTime, HoldLockListener holdLockListener) throws ParameterIllegalException;

	/**
	 * 竞争锁，非阻塞模式
	 *
	 * @param expireTime         锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLockUnblocked(long expireTime, LockExpireListener lockExpireListener) throws ParameterIllegalException;

	/**
	 * 竞争锁，非阻塞模式
	 *
	 * @param expireTime         锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟,最小值5秒
	 * @param renewInterval      自动续租间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)
	 * @param renewListener      续租结果回调
	 * @param lockExpireListener 锁过期回调
	 * @return 获取锁结果
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	AcquireLockResult tryAcquireLockUnblocked(long expireTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener) throws ParameterIllegalException;


	/**
	 * 释放锁，释放当前线程持有的锁，只有锁owner与lockversion（version从缓存上下文中获取）未发生变化，且锁未被过期删除时才可以释放成功
	 *
	 * @return 返回结果 true 请求发送成功， false 请求发送
	 */
	LockResult releaseLock() throws ParameterIllegalException;

	/**
	 * 锁续约,只有锁owner与lockversion未发生变化，且锁未被过期删除时才可以续约成功
	 *
	 * @param expireTime 锁过期时间，单位毫秒，默认值5分钟，最大取值5分钟,最小值5秒
	 * @return 返回结果 true 请求发送成功， false 请求发送
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	LockResult renewLock(long expireTime) throws ParameterIllegalException;

	/**
	 * 读取锁当前状态
	 *
	 * @return 锁当前状态信息
	 * @throws ParameterIllegalException 参数校验失败异常
	 */
	GetLockResult getLockState() throws ParameterIllegalException;

	/**
	 * 获取lockkey
	 */
	String getLockkey();
}
