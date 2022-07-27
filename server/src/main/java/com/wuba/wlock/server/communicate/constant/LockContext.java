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
package com.wuba.wlock.server.communicate.constant;

import org.jboss.netty.channel.Channel;

public class LockContext {
	private Channel channel;
	private byte[] buf;
	private long inTime;
	private String lockkey;
	private String registryKey;
	private long sessionId;

	public LockContext(Channel channel, byte[] buf, long inTime) {
		this.channel = channel;
		this.buf = buf;
		this.inTime = inTime;
	}

	public LockContext(Channel channel, byte[] buf) {
		this.channel = channel;
		this.buf = buf;
	}

	public LockContext(byte[] buf) {
		this.buf = buf;
	}
	
	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public byte[] getBuf() {
		return buf;
	}

	public void setBuf(byte[] buf) {
		this.buf = buf;
	}

	public long getInTime() {
		return inTime;
	}

	public void setInTime(long inTime) {
		this.inTime = inTime;
	}

	public String getLockkey() {
		return lockkey;
	}

	public void setLockkey(String lockkey) {
		this.lockkey = lockkey;
	}

	public long getSessionId() {
		return sessionId;
	}

	public String getRegistryKey() {
		return registryKey;
	}

	public void setRegistryKey(String registryKey) {
		this.registryKey = registryKey;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}
}

