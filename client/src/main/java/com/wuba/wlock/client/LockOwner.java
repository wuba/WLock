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

public class LockOwner {
	/**
	 * 锁持有者主机host
	 */
	private int ownerHost;
	/**
	 * 锁持有者进程ID
	 */
	private int ownerPID;
	/**
	 * 锁持有者线程ID
	 */
	private long ownerThreadID;
	
	public LockOwner(int ownerHost, long ownerThreadID, int ownerPID) {
		super();
		this.ownerHost = ownerHost;
		this.ownerThreadID = ownerThreadID;
		this.ownerPID = ownerPID;
	}

	/**
	 * 获取锁持有者主机host
	 * @return
	 */
	public int getOwnerHost() {
		return ownerHost;
	}

	public void setOwnerHost(int ownerHost) {
		this.ownerHost = ownerHost;
	}

	/**
	 * 获取锁持有者线程ID
	 * @return
	 */
	public long getOwnerThreadID() {
		return ownerThreadID;
	}

	public void setOwnerThreadID(long ownerThreadID) {
		this.ownerThreadID = ownerThreadID;
	}

	/**
	 * 获取锁持有者进程ID
	 * @return
	 */
	public int getOwnerPid() {
		return ownerPID;
	}

	public void setOwnerPid(int ownerPID) {
		this.ownerPID = ownerPID;
	}

	@Override
	public String toString() {
		return "LockOwner [ownerHost=" + ownerHost + ", ownerThreadID=" + ownerThreadID + ", ownerPid=" + ownerPID
				+ "]";
	}
}
