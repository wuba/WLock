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
import com.wuba.wlock.client.communication.ServerPoolHandler;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKey;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import com.wuba.wlock.client.service.LockService;
import com.wuba.wlock.client.util.UniqueCodeGenerator;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class WLockClient {
	private static final int LOCK_KEY_MAX_LEN = Short.MAX_VALUE - 100;
	private static final String HASHKEY_LOCKKEY_SEP = "_";

	private final long uniqueCode;
	
	private final RegistryKey registryKey;
	
	private final LockService lockService;
	
	private final ServerPoolHandler serverPoolHandler;
	
	private int defaultTimeoutForReq = 3000;
	
	private int defaultRetries = 3;
	
	public static int currentPid = getPid();
	
	private int autoRenewThreadCount = 4;
	
	public WLockClient(String hashKey, String registryIp, int registryPort) throws Exception {
		this.registryKey = RegistryKeyFactory.getInsatnce().keyInit(hashKey, registryIp, registryPort, this);
		this.uniqueCode = UniqueCodeGenerator.getUniqueCode();
		this.serverPoolHandler = ServerPoolHandler.getInstance(this);
		this.lockService = new LockService(this);
	}
	
	public WLockClient(String confpath) throws Exception {
		this.registryKey = RegistryKeyFactory.getInsatnce().keyInit(confpath, this);
		this.uniqueCode = UniqueCodeGenerator.getUniqueCode();
		this.serverPoolHandler = ServerPoolHandler.getInstance(this);
		this.lockService = new LockService(this);
	}
	
	/**
	 * 创建分布式锁
	 * @param lockkey 锁名称
	 * @return
	 * @throws ParameterIllegalException
	 */
	public WDistributedLock newDistributeLock(String lockkey) throws ParameterIllegalException {
		String uniqueLockKey = buildLockKey(this.registryKey.getRegistryKey(), lockkey);
		WDistributedLock distributedLock = new WDistributedLock(uniqueLockKey, this);
		return distributedLock;
	}

	public WReadWriteLock newReadWriteLock(String lockkey) throws ParameterIllegalException  {
		String uniqueLockKey = buildLockKey(this.registryKey.getRegistryKey(), lockkey);
		return new WReadWriteLock(uniqueLockKey, this);
	}

	/**
	 * 创建分布式锁
	 * @param lockkey 锁名称
	 * @return
	 * @throws ParameterIllegalException
	 */
	public WDistributedLock newDistributeLock(String lockkey, LockPolicy lockPolicy) throws ParameterIllegalException {
		String uniqueLockKey = buildLockKey(this.registryKey.getRegistryKey(), lockkey);
		WDistributedLock distributedLock = new WDistributedLock(uniqueLockKey, lockPolicy, this);
		return distributedLock;
	}

	public WReadWriteLock newReadWriteLock(String lockkey, LockPolicy lockPolicy) throws ParameterIllegalException  {
		String uniqueLockKey = buildLockKey(this.registryKey.getRegistryKey(), lockkey);
		return new WReadWriteLock(uniqueLockKey, lockPolicy, this);
	}
	
	private String buildLockKey(String hashKey, String key) throws ParameterIllegalException {
		String result = hashKey + HASHKEY_LOCKKEY_SEP + key;
		if (result.length() > LOCK_KEY_MAX_LEN) {
			throw new ParameterIllegalException("lock key is too long");
		}
		if (key.getBytes().length != key.length()) {
			throw new ParameterIllegalException("the lock key does not support in Chinese");
		}
		return result;
	}

	public RegistryKey getRegistryKey() {
		return registryKey;
	}
	
	private static int getPid() {
	    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		// format: "pid@hostname"
	    String name = runtime.getName();
	    try {
	        return Integer.parseInt(name.substring(0, name.indexOf('@')));
	    } catch (Exception e) {
	        return -1;
	    }
	}

	public long getUniqueCode() {
		return uniqueCode;
	}

	public int getDefaultTimeoutForReq() {
		return defaultTimeoutForReq;
	}

	public int getDefaultRetries() {
		return defaultRetries;
	}

	public void setDefaultRetries(int defaultRetries) {
		this.defaultRetries = defaultRetries;
	}

	public void setDefaultTimeoutForReq(int defaultTimeoutForReq) {
		this.defaultTimeoutForReq = defaultTimeoutForReq;
	}

	public int getAutoRenewThreadCount() {
		return autoRenewThreadCount;
	}

	public void setAutoRenewThreadCount(int autoRenewThreadCount) {
		this.autoRenewThreadCount = autoRenewThreadCount;
	}

	public ServerPoolHandler getServerPoolHandler() {
		return serverPoolHandler;
	}

	public LockService getLockService() {
		return lockService;
	}
}