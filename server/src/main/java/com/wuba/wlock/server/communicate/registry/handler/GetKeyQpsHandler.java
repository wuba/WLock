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
package com.wuba.wlock.server.communicate.registry.handler;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.GetRegistryKeyQps;
import com.wuba.wlock.common.registry.protocol.response.GetRegistryKeyQpsRes;
import com.wuba.wlock.server.collector.QpsAbandon;
import com.wuba.wlock.server.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetKeyQpsHandler extends AbstractPaxosHandler implements IPaxosHandler {
	private static final Logger logger = LoggerFactory.getLogger(GetKeyQpsHandler.class);
	private static long version = -1;

	@Override
	protected boolean checkProtocol(byte opaque) {
		if (opaque != OptionCode.RES_REGISTRY_KEY_QPS) {
			logger.error("get key qps response opaque error {}.", opaque);
			return false;
		}
		return true;
	}

	@Override
	public boolean doSuccess(RegistryProtocol registryProtocol) throws Exception {
		GetRegistryKeyQpsRes getRegistryKeyQpsRes = JSON.parseObject(new String(registryProtocol.getBody()), GetRegistryKeyQpsRes.class);
		version = getRegistryKeyQpsRes.getVersion();
		Map<String, Integer> keyQpsMap = getRegistryKeyQpsRes.getKeyQpsMap();
		if (keyQpsMap == null || keyQpsMap.isEmpty()) {
			logger.info("get key qps null");
			return true;
		}
		logger.debug("get key qps {}",new String(registryProtocol.getBody()));
		QpsAbandon.renewQps(keyQpsMap);
		return true;
	}

	@Override
	public boolean doError(RegistryProtocol registryProtocol) {
		logger.error("get key qps error.");
		return false;
	}

	@Override
	public boolean doElse(RegistryProtocol registryProtocol) {
		if (registryProtocol.getMsgType() == MessageType.NO_CHANGE) {
			GetRegistryKeyQpsRes getRegistryKeyQpsRes = JSON.parseObject(new String(registryProtocol.getBody()), GetRegistryKeyQpsRes.class);
			GetKeyQpsHandler.version = getRegistryKeyQpsRes.getVersion();
			logger.info("get key qps no change.");
		}
		return false;
	}

	@Override
	public RegistryProtocol buildMessage() {
		return new GetRegistryKeyQps(ServerConfig.getInstance().getCluster(), version);
	}

	@Override
	public boolean handleResponse(RegistryProtocol registryProtocol) throws Exception {
		return super.doHandler(registryProtocol);
	}
}
