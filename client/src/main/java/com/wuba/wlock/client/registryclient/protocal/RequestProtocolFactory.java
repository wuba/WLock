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
package com.wuba.wlock.client.registryclient.protocal;

import com.wuba.wlock.client.helper.OpaqueGenerator;

public class RequestProtocolFactory {

	private static RequestProtocolFactory instance = new RequestProtocolFactory();
	
	private RequestProtocolFactory() {
	}
	
	public static RequestProtocolFactory getInstacne() {
		return instance;
	}
	
	public RegistryProtocol getClientVersionRequest(byte[] bytes) {
		RegistryProtocol request = new RegistryProtocol();
		request.setOpaque(OptionCode.OPCODE_LOCK_CLIENT_VERSION);
		request.setMsgType(MessageType.CLIENT_VERSION);
		request.setSessionId(OpaqueGenerator.getOpaque());
		request.setBody(bytes);
		return request;
	}
	
	public RegistryProtocol getCongigGetRequest(byte[] bytes) {
		RegistryProtocol request = new RegistryProtocol();
		request.setOpaque(OptionCode.OPCODE_LOCK_CLIENT_CONFIG_GET);
		request.setMsgType(MessageType.REQUEST_ALL_DATAS);
		request.setSessionId(OpaqueGenerator.getOpaque());
		request.setBody(bytes);
		return request;
	}
	
	public RegistryProtocol getCongigValidatorRequest(byte[] bytes) {
		RegistryProtocol request = new RegistryProtocol();
		request.setOpaque(OptionCode.OPCODE_LOCK_CLIENT_CONFIG_GET);
		request.setMsgType(MessageType.REQUEST_VALIDATION);
		request.setSessionId(OpaqueGenerator.getOpaque());
		request.setBody(bytes);
		return request;
	}
	
	public RegistryProtocol getHeartBeatRequest() {
		RegistryProtocol protocol = new RegistryProtocol();
		protocol.setMsgType(MessageType.CLIENT_HEARTBEAT);
		protocol.setOpaque(OptionCode.OPCODE_LOCK_CLIENT_HEARTBEAT);
		protocol.setSessionId(OpaqueGenerator.getOpaque());
		return protocol;
	}
	
}
