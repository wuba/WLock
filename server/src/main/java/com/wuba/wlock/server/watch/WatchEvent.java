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
package com.wuba.wlock.server.watch;

import com.wuba.wlock.server.client.LockClient;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.util.TimeUtil;

public class WatchEvent {
	private byte lockType;
	private byte opcode;
	private long watchID;
	private WatchType watchType;
	private long watchVersion;
	private long timeoutStamp;
	private int weight;
	private int expireTime;
	private LockClient lockClient;
	private long lastUpateTimestamp;
	/**
	 * watch request async true,
	 * acquire request async false
	 */
	private boolean async;
	
	public static int DEFAULT_BROADCAST_WATCH_ID = -1;
	
	public long getWatchID() {
		return watchID;
	}
	
	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}
	
	public WatchType getWatchType() {
		return watchType;
	}
	
	public void setWatchType(WatchType watchType) {
		this.watchType = watchType;
	}
	
	public long getTimeoutStamp() {
		return timeoutStamp;
	}

	public void setTimeoutStamp(long timeoutStamp) {
		this.timeoutStamp = timeoutStamp;
	}

	public LockClient getLockClient() {
		return lockClient;
	}
	
	public void setLockClient(LockClient lockClient) {
		this.lockClient = lockClient;
	}

	public long getWatchVersion() {
		return watchVersion;
	}

	public void setWatchVersion(long watchVersion) {
		this.watchVersion = watchVersion;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public int getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(int expireTime) {
		this.expireTime = expireTime;
	}
	
	public boolean isTimeout() {
		return TimeUtil.getCurrentTimestamp() > timeoutStamp;
	}
	
	public boolean isAlive() {
		return (TimeUtil.getCurrentTimestamp() - this.lastUpateTimestamp) <= ServerConfig.getInstance().getWatchMaxUntouchMills();
	}

	public long getLastUpateTimestamp() {
		return lastUpateTimestamp;
	}

	public void setLastUpateTimestamp(long lastUpateTimestamp) {
		this.lastUpateTimestamp = lastUpateTimestamp;
	}
	
	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public WatchIndex getWatchIndex() {
		return new WatchIndex(watchID, this.lockClient.getcHost(), this.lockClient.getcPid(), this.lockClient.getcThreadID(), this.getWeight());
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lockClient.getcHost();
		result = prime * result + lockClient.getcPid();
		result = prime * result + (int) (lockClient.getcThreadID() ^ (lockClient.getcThreadID() >>> 32));
		result = prime * result + (int) (watchID ^ (watchID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WatchEvent other = (WatchEvent) obj;
		if (lockClient == null) {
			if (other.lockClient != null) {
				return false;
			}
		} else if (!lockClient.equals(other.lockClient)) {
			return false;
		} else if (lockClient.getcHost() != other.lockClient.getcHost()) {
			return false;
		} else if (lockClient.getcThreadID() != other.lockClient.getcThreadID()) {
			return false;
		} else if (lockClient.getcPid() != other.lockClient.getcPid()) {
			return false;
		}
		if (watchID != other.watchID) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "WatchEvent [watchID=" + watchID + ", watchType=" + watchType + ", watchVersion=" + watchVersion
				+ ", timeoutStamp=" + timeoutStamp + ", weight=" + weight + ", expireTime=" + expireTime
				+ ", lockClient=" + lockClient + ", lastUpateTimestamp=" + lastUpateTimestamp + ", async=" + async
				+ "]";
	}
}
