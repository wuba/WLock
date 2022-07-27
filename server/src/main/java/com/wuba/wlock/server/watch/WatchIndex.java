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

public class WatchIndex implements Comparable<WatchIndex> {
	private long watchID;
	private int host;
	private int pid;
	private long threadID;

	private int weight;

	public WatchIndex(long watchID, int host, int pid, long threadID, int weight) {
		super();
		this.watchID = watchID;
		this.host = host;
		this.pid = pid;
		this.threadID = threadID;
		this.weight = weight;
	}

	public long getWatchID() {
		return watchID;
	}

	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}

	public int getHost() {
		return host;
	}

	public void setHost(int host) {
		this.host = host;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public long getThreadID() {
		return threadID;
	}

	public void setThreadID(long threadID) {
		this.threadID = threadID;
	}

	@Override
	public int compareTo(WatchIndex o) {
		return o.weight - this.weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + host;
		result = prime * result + pid;
		result = prime * result + (int) (threadID ^ (threadID >>> 32));
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
		WatchIndex other = (WatchIndex) obj;
		if (host != other.host) {
			return false;
		}
		if (pid != other.pid) {
			return false;
		}
		if (threadID != other.threadID) {
			return false;
		}
		if (watchID != other.watchID) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "WatchIndex{" +
				"watchID=" + watchID +
				", host=" + host +
				", pid=" + pid +
				", threadID=" + threadID +
				", weight=" + weight +
				'}';
	}
}
