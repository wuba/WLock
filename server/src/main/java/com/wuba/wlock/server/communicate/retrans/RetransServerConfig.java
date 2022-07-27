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
package com.wuba.wlock.server.communicate.retrans;

public class RetransServerConfig {
	
	private String ip;
	private int port;
	private static int initConn = 1;
	private static boolean keepAlive = true;
	private static int connectTimeOut = 1000 * 3;
	private static boolean nagle = false;
	private static int maxPakageSize = 1024 * 1024 * 2;
	private static int recvBufferSize = 1024 * 1024 * 20;
	private static int sendBufferSize = 1024 * 1024 * 20;
	private static int frameMaxLength = 1024 * 1024 * 10;
	
	public RetransServerConfig(String ip, int port) {
		super();
		this.ip = ip;
		this.port = port;
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

	public static int getInitConn() {
		return initConn;
	}

	public static void setInitConn(int initConn) {
		RetransServerConfig.initConn = initConn;
	}

	public static boolean isKeepAlive() {
		return keepAlive;
	}

	public static void setKeepAlive(boolean keepAlive) {
		RetransServerConfig.keepAlive = keepAlive;
	}

	public static int getConnectTimeOut() {
		return connectTimeOut;
	}

	public static void setConnectTimeOut(int connectTimeOut) {
		RetransServerConfig.connectTimeOut = connectTimeOut;
	}

	public static boolean isNagle() {
		return nagle;
	}

	public static void setNagle(boolean nagle) {
		RetransServerConfig.nagle = nagle;
	}

	public static int getMaxPakageSize() {
		return maxPakageSize;
	}

	public static void setMaxPakageSize(int maxPakageSize) {
		RetransServerConfig.maxPakageSize = maxPakageSize;
	}

	public static int getRecvBufferSize() {
		return recvBufferSize;
	}

	public static void setRecvBufferSize(int recvBufferSize) {
		RetransServerConfig.recvBufferSize = recvBufferSize;
	}

	public static void setSendBufferSize(int sendBufferSize) {
		RetransServerConfig.sendBufferSize = sendBufferSize;
	}

	public static int getSendBufferSize() {
		return sendBufferSize;
	}
	
	public static int getFrameMaxLength() {
		return frameMaxLength;
	}

	public static void setFrameMaxLength(int frameMaxLength) {
		RetransServerConfig.frameMaxLength = frameMaxLength;
	}
	
	public String getAddr() {
		return this.getIp() + ":" + this.getPort();
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
		RetransServerConfig other = (RetransServerConfig) obj;
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
}
