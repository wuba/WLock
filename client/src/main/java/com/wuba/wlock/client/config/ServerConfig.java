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
package com.wuba.wlock.client.config;

public class ServerConfig {
	public static final int BASE_BUFFER_SIZE = 1024 * 512 * 1;

	private String ip;
	private int port;
	private boolean isMaster;
	private int initConn;
	private boolean keepAlive;
	private int connectTimeOut;
	private boolean nagle;
	private int maxPakageSize;
	private int recvBufferSize;
	private int sendBufferSize;
	private int maxFrameLength;
	private int maxWriteQueueLen;
	
	public ServerConfig() {
		super();
		this.initConn = 1;
		this.keepAlive = true;
		this.connectTimeOut = 1000 * 3;
		this.nagle = false;
		this.maxPakageSize = BASE_BUFFER_SIZE * 2;
		this.recvBufferSize = BASE_BUFFER_SIZE;
		this.sendBufferSize = BASE_BUFFER_SIZE;
		this.maxFrameLength = 1024 * 1024 * 1;
		this.maxWriteQueueLen = 100000;
	}

	public ServerConfig(String ip, int port, boolean isMaster) {
		this();
		this.ip = ip;
		this.port = port;
		this.isMaster = isMaster;
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

	public int getInitConn() {
		return initConn;
	}

	public void setInitConn(int initConn) {
		this.initConn = initConn;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public int getConnectTimeOut() {
		return connectTimeOut;
	}

	public void setConnectTimeOut(int connectTimeOut) {
		this.connectTimeOut = connectTimeOut;
	}

	public boolean isNagle() {
		return nagle;
	}

	public void setNagle(boolean nagle) {
		this.nagle = nagle;
	}

	public int getMaxPakageSize() {
		return maxPakageSize;
	}

	public void setMaxPakageSize(int maxPakageSize) {
		this.maxPakageSize = maxPakageSize;
	}

	public int getRecvBufferSize() {
		return recvBufferSize;
	}

	public void setRecvBufferSize(int recvBufferSize) {
		this.recvBufferSize = recvBufferSize;
	}

	public int getSendBufferSize() {
		return sendBufferSize;
	}

	public void setSendBufferSize(int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
	}

	public int getMaxFrameLength() {
		return maxFrameLength;
	}

	public void setMaxFrameLength(int maxFrameLength) {
		this.maxFrameLength = maxFrameLength;
	}

	public int getMaxWriteQueueLen() {
		return maxWriteQueueLen;
	}

	public void setMaxWriteQueueLen(int maxWriteQueueLen) {
		this.maxWriteQueueLen = maxWriteQueueLen;
	}

	public boolean isMaster() {
		return isMaster;
	}

	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
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
		ServerConfig other = (ServerConfig) obj;
		if (ip == null) {
			if (other.ip != null) {
				return false;
			}
		} else if (!ip.equals(other.ip)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ServerConfig [ip=" + ip + ", port=" + port + "]";
	}
}
