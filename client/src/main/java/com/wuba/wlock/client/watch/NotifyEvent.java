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
package com.wuba.wlock.client.watch;

public class NotifyEvent {
	private String lockkey;
	private int eventType;
	private long watchID;
	private long fencingToken;
	private long threadID;
	
	public String getLockkey() {
		return lockkey;
	}

	public void setLockkey(String lockkey) {
		this.lockkey = lockkey;
	}

	public int getEventType() {
		return eventType;
	}
	
	public void setEventType(int eventType) {
		this.eventType = eventType;
	}
	
	public long getWatchID() {
		return watchID;
	}
	
	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}

	public long getFencingToken() {
		return fencingToken;
	}

	public void setFencingToken(long fencingToken) {
		this.fencingToken = fencingToken;
	}

	public long getThreadID() {
		return threadID;
	}

	public void setThreadID(long threadID) {
		this.threadID = threadID;
	}

	public boolean isExpiredEvent() {
		return eventType == EventType.LOCK_EXPIRED.getType()
				|| eventType == EventType.READ_LOCK_EXPIRED.getType()
				|| eventType == EventType.WRITE_LOCK_EXPIRED.getType();
	}
}
