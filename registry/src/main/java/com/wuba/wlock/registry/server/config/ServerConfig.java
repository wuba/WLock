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
package com.wuba.wlock.registry.server.config;

import com.wuba.wlock.registry.server.util.ConversionUtil;
import com.wuba.wlock.registry.util.Validator;

import java.util.Map;
import java.util.Map.Entry;

public class ServerConfig implements Configuration {
	private static final String KEEP_ALIVE_KEY = "keepAlive";
	private static final String NAGLE_KEY = "nagle";
	private static final String MAX_PAKAGE_SIZE_KEY = "maxPakageSize";
	private static final String RECV_BUFFER_SIZE_KEY = "recvBufferSize";
	private static final String SEND_BUFFER_SIZE_KEY = "sendBufferSize";
	private static final String POLL_WAIT_TIME_KEY = "pollWaitTime";
	private static final String LOCAL_KEY = "local";
	private static final String PORT_KEY = "port";
	private static final String WORKER_LENGTH_KEY = "workerLength";

	
	private boolean keepAlive = true;
	private boolean nagle = false;
	private int maxPakageSize = 1024 * 32;
	private int recvBufferSize = 1024;
	private int sendBufferSize = 1024 * 32;
	private int pollWaitTime = 2000;
	private String local = "127.0.0.1";
	private int port;
	private boolean tcpNoDelay;
	private static int workerLength = 1024*128;
	
	public ServerConfig() {
		super();
	}

	@Override
	public void setOptions(Map<String, Object> options) {
		for (Entry<String, Object> e : options.entrySet()) {
			setOption(e.getKey(), e.getValue());
		}
	}

	@Override
	public boolean setOption(String key, Object value) {
		if (KEEP_ALIVE_KEY.equals(key)) {
			setKeepAlive((Validator.notNullAndEmpty(value))? ConversionUtil.toBoolean(value) : this.keepAlive);
		} else if (NAGLE_KEY.equals(key)) {
			setNagle((Validator.notNullAndEmpty(value))? ConversionUtil.toBoolean(value) : this.nagle);
		} else if (MAX_PAKAGE_SIZE_KEY.equals(key)) {
			setMaxPakageSize((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : this.maxPakageSize);
		} else if (RECV_BUFFER_SIZE_KEY.equals(key)) {
			setRecvBufferSize((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : this.recvBufferSize);
		} else if (SEND_BUFFER_SIZE_KEY.equals(key)) {
			setSendBufferSize((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : this.sendBufferSize);
		} else if (POLL_WAIT_TIME_KEY.equals(key)) {
			setPollWaitTime((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : this.pollWaitTime);
		} else if (LOCAL_KEY.equals(key)) {
			setLocal((Validator.notNullAndEmpty(value))? String.valueOf(value) : this.local);
		} else if (PORT_KEY.equals(key)) {
			setPort((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : this.port);
		} else if (WORKER_LENGTH_KEY.equals(key)) {
			setWorkerLength((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : workerLength);
		} else {
			System.out.println("ServerConfig no this key:" +key);
		}
		return true;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
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

	public int getPollWaitTime() {
		return pollWaitTime;
	}

	public void setPollWaitTime(int pollWaitTime) {
		this.pollWaitTime = pollWaitTime;
	}

	public String getLocal() {
		return local;
	}

	public void setLocal(String local) {
		this.local = local;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isTcpNoDelay() {
		return tcpNoDelay;
	}

	public void setTcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}
	
	public static void setWorkerLength(int length) {
		workerLength = length;
	}
	
	public static int getWorkerLength() {
		return workerLength;
	}
	
}
