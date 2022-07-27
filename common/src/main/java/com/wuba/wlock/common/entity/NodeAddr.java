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
package com.wuba.wlock.common.entity;

public class NodeAddr {
	
	private String ip;
	private int port;
	private boolean isMaster;
	private long version;
	
	public NodeAddr() {}
	
	public NodeAddr(String ip, int port, boolean isMaster, long version) {
		super();
		this.ip = ip;
		this.port = port;
		this.isMaster = isMaster;
		this.version = version;
	}

	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public boolean getIsMaster() {
		return isMaster;
	}

	public void setIsMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}
	
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
	
	@Override
	public String toString() {
		StringBuilder toStr = new StringBuilder();
		toStr.append("[IP:").append(ip).append(" port:").append(port).append(" isMaster:").append(isMaster)
			.append(" version:").append(version).append("]");
		return toStr.toString();
	}
	
}
