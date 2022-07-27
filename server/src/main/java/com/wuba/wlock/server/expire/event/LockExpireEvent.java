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

public class LockExpireEvent extends ExpireEvent {

	private long lockVersion;

	protected int host;

	protected long threadID;

	protected int pid;

	public LockExpireEvent(long expireTimeStamp, String lockKey, int groupId, long lockVersion, byte lockType, byte opcode, int host, long threadID, int pid) {
		super(ExpireEventType.EXPIRE_LOCK_EVENT, expireTimeStamp, lockKey, groupId, lockType, opcode);
		this.lockVersion = lockVersion;
		this.host = host;
		this.threadID = threadID;
		this.pid = pid;
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

	public void setThreadID(long threadID) {
		this.threadID = threadID;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public long getLockVersion() {
		return lockVersion;
	}

	public void setLockVersion(long lockVersion) {
		this.lockVersion = lockVersion;
	}

	@Override
	public String toString() {
		return super.toString() + " lockVersion:" + this.lockVersion;
	}

}
