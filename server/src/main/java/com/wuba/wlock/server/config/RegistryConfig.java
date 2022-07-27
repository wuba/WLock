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
package com.wuba.wlock.server.config;

import com.wuba.wlock.server.exception.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegistryConfig extends IConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaxosConfig.class);

	private RegistryConfig() {
	}

	private static RegistryConfig registryConfig = new RegistryConfig();

	public static RegistryConfig getInstance() {
		return registryConfig;
	}


	@Override
	public void init(String path, boolean mustExist) throws ConfigException {
		super.initConfig(path, mustExist);
	}

	@Override
	public void loadSpecial() {

	}


	public String getHost() throws ConfigException {
		return super.getString("registryServerIp");
	}

	public int getPort() throws ConfigException {
		return super.getInt("registryServerPort");
	}

	public int getRecvBufferSize() {
		return super.getInt("recvBufferSize", 1024 * 1024 * 2);
	}

	public int getSendBufferSize() {
		return super.getInt("sendBufferSize", 1024 * 1024 * 2);
	}

	public int getMaxPakageSize() {
		return super.getInt("maxPakageSize", 1024 * 1024 * 2);
	}

	public int getConnectTimeOut() {
		return super.getInt("connectTimeOut", 3000);
	}

	public int getFrameMaxLength() {
		return super.getInt("frameMaxLength", 1024 * 1024 * 2);
	}

}
