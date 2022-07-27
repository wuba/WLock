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
package com.wuba.wlock.server.client;

import java.util.Objects;

public class LockClient {
	private String lockkey;
	private long watchID;
	private int cHost;
	private long cThreadID;
	private int cPid;
	private int channelId;
	private int groupId;
	private byte version;
	
	public LockClient(int channelId, String lockkey, byte version) {
		this.channelId = channelId;
		this.lockkey = lockkey;
		this.version = version;
	}

	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public String getLockkey() {
		return lockkey;
	}

	public void setLockkey(String lockkey) {
		this.lockkey = lockkey;
	}

	public int getcHost() {
		return cHost;
	}

	public void setcHost(int cHost) {
		this.cHost = cHost;
	}

	public long getcThreadID() {
		return cThreadID;
	}

	public void setcThreadID(long cThreadID) {
		this.cThreadID = cThreadID;
	}

	public int getcPid() {
		return cPid;
	}

	public void setcPid(int cPid) {
		this.cPid = cPid;
	}

	public long getWatchID() {
		return watchID;
	}

	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LockClient that = (LockClient) o;
		return 	cHost == that.cHost &&
				cThreadID == that.cThreadID &&
				cPid == that.cPid &&
				channelId == that.channelId &&
				groupId == that.groupId &&
				Objects.equals(lockkey, that.lockkey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lockkey, cHost, cThreadID, cPid, channelId, groupId);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("LockClient{");
		sb.append("lockkey='").append(lockkey).append('\'');
		sb.append(", watchID=").append(watchID);
		sb.append(", cHost=").append(cHost);
		sb.append(", cThreadID=").append(cThreadID);
		sb.append(", cPid=").append(cPid);
		sb.append(", channelId=").append(channelId);
		sb.append(", groupId=").append(groupId);
		sb.append('}');
		return sb.toString();
	}
}
