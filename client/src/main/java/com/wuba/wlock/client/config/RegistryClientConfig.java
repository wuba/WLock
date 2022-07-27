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

import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import com.wuba.wlock.client.helper.HostUtil;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;

import java.util.ArrayList;
import java.util.List;

public class RegistryClientConfig {

	public static final int GET_CONFIG_TIMEOUT = 8000;
	public static final int GET_CONFIG_PERIOD = 60;
	public static final int SEND_VERSION_PERIOD = 1;
	public static final int MAX_WRITE_QUEUE_LEN = 10000;
	public static final int WAIT_COUNT = 1;
	public static final int HEARTBEAT_PERIOD = 5;
	public static final int HEARTBEAT_TIMEOUT = 2000;


	public List<ServerConfig> getAllServerConfig() throws RegistryClientRuntimeException {
		List<String> ips = HostUtil.getInstance().getAllServerIP();
		List<ServerConfig> listServerConfig = new ArrayList<ServerConfig>();
		for (String ip : ips) {
			ServerConfig serConfig = new ServerConfig();
			serConfig.setIp(ip);
			serConfig.setPort(RegistryKeyFactory.getInsatnce().getRegistryServerPort());
			listServerConfig.add(serConfig);
		}
		return listServerConfig;
	}

}
