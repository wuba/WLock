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
package com.wuba.wlock.common.registry.protocol.request;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;

public class GetPaxosConfig extends RegistryProtocol {


	public GetPaxosConfig(String ip, int port, long version) {
		this.setOpaque(OptionCode.GET_PAXOS_CONFIG);
		ServerConfig serverConfig = new ServerConfig();
		serverConfig.setIp(ip);
		serverConfig.setPort(port);
		serverConfig.setVersion(version);
		this.setBody(JSONObject.toJSONString(serverConfig).getBytes());
	}

	public static class ServerConfig {

		private String ip;
		private int port;
		private long version;

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

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}
	}

}
