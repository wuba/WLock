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
package com.wuba.wlock.server.expire.event;

public abstract class ExpireEvent implements Comparable<ExpireEvent> {

	protected byte expireType;

	protected int groupId;

	protected long expireTimestamp;

	protected String lockKey;

	protected String registryKey;

	protected byte lockType;

	protected byte opcode;

	public ExpireEvent(byte expireType, long expireTimeStamp, String lockKey, int groupId, byte lockType, byte opcode) {
		this.expireType = expireType;
		this.expireTimestamp = expireTimeStamp;
		this.lockKey = lockKey;
		this.groupId = groupId;
		this.lockType = lockType;
		this.opcode = opcode;
		if (lockKey != null) {
			int index = lockKey.indexOf("_");
			if (index >= 0) {
				this.registryKey = lockKey.substring(0, index);
			}
		}
	}


	public byte getExpireType() {
		return expireType;
	}

	public void setExpireType(byte expireType) {
		this.expireType = expireType;
	}

	public long getExpireTimestamp() {
		return expireTimestamp;
	}

	public void setExpireTimestamp(long expireTimestamp) {
		this.expireTimestamp = expireTimestamp;
	}

	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
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

	public String getRegistryKey() {
		return registryKey;
	}

	public void setRegistryKey(String registryKey) {
		this.registryKey = registryKey;
	}

	@Override
	public int compareTo(ExpireEvent expireEvent) {
		if (this.expireTimestamp > expireEvent.expireTimestamp) {
			return 1;
		}
		if (this.expireTimestamp < expireEvent.expireTimestamp) {
			return -1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "ExpireEvent{" +
				"expireType=" + expireType +
				", groupId=" + groupId +
				", expireTimestamp=" + expireTimestamp +
				", lockKey='" + lockKey + '\'' +
				", registryKey='" + registryKey + '\'' +
				", lockType=" + lockType +
				", opcode=" + opcode +
				'}';
	}
}
