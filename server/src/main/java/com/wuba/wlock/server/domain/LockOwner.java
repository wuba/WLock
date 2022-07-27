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

import java.util.Objects;

public class LockOwner {
	private int ip;
	private long threadId;
	private int pid;
	private long lockversion;

	public LockOwner(int ip, long threadId, int pid) {
		this.ip = ip;
		this.threadId = threadId;
		this.pid = pid;
	}

	public LockOwner(int ip, long threadId, int pid, long lockversion) {
		this.ip = ip;
		this.threadId = threadId;
		this.lockversion = lockversion;
		this.pid = pid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LockOwner lockOwner = (LockOwner) o;
		return ip == lockOwner.ip &&
				threadId == lockOwner.threadId &&
				pid == lockOwner.pid;
	}

	@Override
	public int hashCode() {

		return Objects.hash(ip, pid, threadId);
	}


	public int getIp() {
		return ip;
	}

	public void setIp(int ip) {
		this.ip = ip;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public long getLockversion() {
		return lockversion;
	}

	public void setLockversion(long lockversion) {
		this.lockversion = lockversion;
	}

	@Override
	public String toString() {
		return "LockOwner{" +
				" ip=" + ip +
				", threadId=" + threadId +
				", pid=" + pid +
				'}';
	}
}
