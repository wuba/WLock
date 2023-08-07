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
package com.wuba.wlock.registry.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.exception.ProtocolException;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.GetPaxosConfig;
import com.wuba.wlock.registry.server.command.Command;
import com.wuba.wlock.registry.server.command.ResponseAckCommand;
import com.wuba.wlock.registry.server.command.client.ClientConfigGetCommand;
import com.wuba.wlock.registry.server.command.client.ClientConfigPushCommand;
import com.wuba.wlock.registry.server.command.client.ClientHeartBeatCommand;
import com.wuba.wlock.registry.server.command.client.ClientVersionCommand;
import com.wuba.wlock.registry.server.command.server.*;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.util.ThreadRenameFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AsyncInvokerHandler implements InvokerHandler {
	private static final byte PROTOCOL_VERSION = 1;

	private static Map<String, Long> serverTimeMap = new ConcurrentHashMap<String, Long>();

	ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1,0L,
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadRenameFactory("AsyncInvokerHandler-Thread"));

	@Autowired
    ResponseAckCommand responseAckCommand;

	@Autowired
    ClientConfigGetCommand clientConfigGetCommand;
	@Autowired
    ClientConfigPushCommand clientConfigPushCommand;
	@Autowired
    ClientHeartBeatCommand clientHeartBeatCommand;
	@Autowired
    ClientVersionCommand clientVersionCommand;

	@Autowired
    GetKeyQpsCommand getKeyQpsCommand;
	@Autowired
    GetMigrateConfigCommand getMigrateConfigCommand;
	@Autowired
    ServerGetPaxosConfigCommand serverGetPaxosConfigCommand;
	@Autowired
    ServerUploadMasterCommand serverUploadMasterCommand;
	@Autowired
    UploadMigrateStateCommand uploadMigrateStateCommand;


	private Command command(RegistryProtocol registryProtocol, WLockRegistryContext context) throws Exception {
		RegistryProtocol reqProtocol = RegistryProtocol.fromBytes(context.getRequest());
		if (registryProtocol.getVersion() == PROTOCOL_VERSION) {
			if (reqProtocol.getMsgType() == MessageType.RESPONSE_ACK) {
				return responseAckCommand;
			}

			if (reqProtocol.getOpaque() == OptionCode.OPCODE_LOCK_CLIENT_CONFIG_GET) {
				return clientConfigGetCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.OPCODE_LOCK_CLIENT_CONFIG_PUSH) {
				return clientConfigPushCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.OPCODE_LOCK_CLIENT_HEARTBEAT) {
				return clientHeartBeatCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.OPCODE_LOCK_CLIENT_VERSION) {
				return clientVersionCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.GET_PAXOS_CONFIG) {
				return serverGetPaxosConfigCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.UPLOAD_MASTER_CONFIG) {
				return serverUploadMasterCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.GET_REGISTRY_KEY_QPS) {
				return getKeyQpsCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.GET_GROUP_MIGRATE_CONFIG) {
				return getMigrateConfigCommand;
			} else if (reqProtocol.getOpaque() == OptionCode.UPLOAD_MIGRATE_STATE) {
				return uploadMigrateStateCommand;
			} else {
				throw new ProtocolException(context.getChannel().getRemoteIp() + ":protocol optioncode unmatched the value is " + reqProtocol.getOpaque());
			}
		} else {
			throw new ProtocolException(context.getChannel().getRemoteIp() + ":protocol version unmatched the value is " + reqProtocol.getVersion());
		}
	}

	@Override
	public void invoke(final WLockRegistryContext context) throws Exception {
		RegistryProtocol reqProtocol = RegistryProtocol.fromBytes(context.getRequest());
		Command command = command(reqProtocol, context);
		if (speedLimit(command, reqProtocol)) {
			return;
		}
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					command.execute(context, reqProtocol);
					context.getServerHandler().writeResponse(context);
				} catch (Exception e) {
					log.error("AsyncInvokerHandler command execute error", e);
				}
			}
		});
	}

	private boolean speedLimit(Command command, RegistryProtocol reqProtocol) {
		if (command instanceof ServerGetPaxosConfigCommand) {
			try {
				GetPaxosConfig.ServerConfig getPaxos = JSONObject.parseObject(reqProtocol.getBody(), GetPaxosConfig.ServerConfig.class);
				String ipPort = getPaxos.getIp() + ":" +getPaxos.getPort();

				synchronized (ipPort.intern()) {
					Long time = serverTimeMap.get(ipPort);
					if (time != null && System.currentTimeMillis() - time < 2000) {
						log.warn("speed Limit ipPort: {}", ipPort);
						return true;
					}
					serverTimeMap.put(ipPort, System.currentTimeMillis());
				}
			} catch (Exception e) {
				log.warn("speed Limit error", e);
			}
		}
		return false;
	}
}
