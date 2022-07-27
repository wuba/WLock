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
package com.wuba.wlock.server.domain;

public class BaseLockDO {

	public static final int PROYOCOL_TYPE_OFFSET = 1;
	private byte version = 1;
	private byte protocolType;
	private short lockKeyLen;
	private String lockKey;
	private int host;
	private int pid;
	private long threadID;
	private long fencingToken;
	/**
	 *  0：可重入锁 1：读写锁
	 */
	private byte lockType;
	/**
	 * 0为null，读写锁操作码：写锁 1， 读锁 2
	 */
	private byte opcode;

	public BaseLockDO() {
	}

	public BaseLockDO(byte lockType, byte opcode) {
		this.lockType = lockType;
		this.opcode = opcode;
	}

	public byte getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(byte protocolType) {
		this.protocolType = protocolType;
	}

	public short getLockKeyLen() {
		return lockKeyLen;
	}

	public void setLockKeyLen(short lockKeyLen) {
		this.lockKeyLen = lockKeyLen;
	}

	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

	public int getHost() {
		return host;
	}

	public void setHost(int host) {
		this.host = host;
	}

	public long getThreadID() {
		return threadID;
	}

	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	public void setThreadID(long threadID) {
		this.threadID = threadID;
	}

	public int getPid() {
		return pid;
	}

	public long getFencingToken() {
		return fencingToken;
	}

	public void setFencingToken(long fencingToken) {
		this.fencingToken = fencingToken;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public byte getLockType() {
		return lockType;
	}

	public void setLockType(byte lockType) {
		this.lockType = lockType;
	}

	public byte getOpcode() {
		return opcode;
	}

	public void setOpcode(byte opcode) {
		this.opcode = opcode;
	}
}
