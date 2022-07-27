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
package com.wuba.wlock.server.trace;

import com.wuba.wlock.server.lock.protocol.LockCodeEnum;

public class LockTrace {
	private long timeStamp;
	private int operation;
	private String ip;
	private long threadId;
	private int pId;
	private String key;
	private long version;
	private String registryKey;
	private long expireTime;
	private String lockcode;


	public LockTrace(long timeStamp, int operation, String ip, long threadId, int pId, String key, long version, String registryKey, long expireTime, LockCodeEnum lockcode) {
		this.timeStamp = timeStamp;
		this.operation = operation;
		this.ip = ip;
		this.threadId = threadId;
		this.pId = pId;
		this.key = key;
		this.version = version;
		this.registryKey = registryKey;
		this.expireTime = expireTime;
		if (lockcode != null) {
			this.lockcode = lockcode.name();
		}
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public int getOperation() {
		return operation;
	}

	public void setOperation(int operation) {
		this.operation = operation;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public int getpId() {
		return pId;
	}

	public void setpId(int pId) {
		this.pId = pId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getRegistryKey() {
		return registryKey;
	}

	public void setRegistryKey(String registryKey) {
		this.registryKey = registryKey;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		sb.append(timeStamp).append("\t");
		sb.append(lockcode).append("\t");
		sb.append(operation).append("\t");
		sb.append(ip).append("\t");
		sb.append(threadId).append("\t");
		sb.append(pId).append("\t");
		sb.append(key).append("\t");
		sb.append(version).append("\t");
		sb.append(registryKey).append("\t");
		sb.append(expireTime);
		return sb.toString();
	}
}
