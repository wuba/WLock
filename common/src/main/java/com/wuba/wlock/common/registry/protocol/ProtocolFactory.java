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
package com.wuba.wlock.common.registry.protocol;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.registry.protocol.response.GetGroupMigrateConfigRes;
import com.wuba.wlock.common.registry.protocol.response.GetPaxosConfRes;
import com.wuba.wlock.common.registry.protocol.response.GetRegistryKeyQpsRes;
import com.wuba.wlock.common.util.ByteConverter;
import com.wuba.wlock.common.util.SessionIDGenerator;

public class ProtocolFactory {

	private static ProtocolFactory instance = new ProtocolFactory();

	private ProtocolFactory() {
	}

	public static ProtocolFactory getInstance() {
		return instance;
	}

	public RegistryProtocol createClientConfigResponse(byte opCode, byte msgType, byte[] body, long sessionId) {
		RegistryProtocol response = new RegistryProtocol();
		response.setOpaque(opCode);
		response.setMsgType(msgType);
		response.setBody(body);
		response.setSessionId(sessionId);
		return response;
	}

	public RegistryProtocol createReponseAck(byte opCode, long sessionId) {
		RegistryProtocol response = new RegistryProtocol();
		response.setOpaque(opCode);
		response.setMsgType(MessageType.RESPONSE_ACK);
		response.setSessionId(sessionId);
		response.setBody(ByteConverter.shortToBytesLittleEndian(ResponseStatus.ACK_SUCCESS.getValue()));
		return response;
	}

	public RegistryProtocol createClientConfigPush(byte msgType, byte[] body) {
		RegistryProtocol protocol = new RegistryProtocol();
		protocol.setOpaque(OptionCode.OPCODE_LOCK_CLIENT_CONFIG_PUSH);
		protocol.setMsgType(msgType);
		protocol.setBody(body);
		protocol.setSessionId(SessionIDGenerator.getSessionID());
		return protocol;
	}

	public RegistryProtocol createPaxosConfigResponse(byte messageType, GetPaxosConfRes getPaxosConfRes, long sid) {
		RegistryProtocol protocol = new RegistryProtocol();
		protocol.setOpaque(OptionCode.RES_PAXOS_CONFIG);
		protocol.setMsgType(messageType);
		if (getPaxosConfRes != null) {
			protocol.setBody(JSON.toJSONString(getPaxosConfRes).getBytes());
		}
		protocol.setSessionId(sid);
		return protocol;
	}

	public RegistryProtocol createPaxosConfigResponse(byte messageType, long sid) {
		return createPaxosConfigResponse(messageType, null, sid);
	}

	public RegistryProtocol createCommonAck(byte optionCode, byte messageType, long sid) {
		RegistryProtocol protocol = new RegistryProtocol();
		protocol.setOpaque(optionCode);
		protocol.setMsgType(messageType);
		protocol.setSessionId(sid);
		return protocol;
	}

	public RegistryProtocol createKeyQpsResponse(byte messageType, long sid, GetRegistryKeyQpsRes getRegistryKeyQpsRes) {
		RegistryProtocol protocol = new RegistryProtocol();
		protocol.setOpaque(OptionCode.RES_REGISTRY_KEY_QPS);
		protocol.setMsgType(messageType);
		protocol.setSessionId(sid);
		protocol.setBody(JSON.toJSONString(getRegistryKeyQpsRes).getBytes());
		return protocol;
	}

	public RegistryProtocol createGroupMigrateConfigResponse(byte messageType, long sid, GetGroupMigrateConfigRes getGroupMigrateConfigRes) {
		RegistryProtocol protocol = new RegistryProtocol();
		protocol.setOpaque(OptionCode.RES_GROUP_MIGRATE_CONFIG);
		protocol.setMsgType(messageType);
		protocol.setSessionId(sid);
		protocol.setBody(JSON.toJSONString(getGroupMigrateConfigRes).getBytes());
		return protocol;
	}

}
