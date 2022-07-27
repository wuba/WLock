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

import com.wuba.wlock.client.helper.ByteConverter;
import com.wuba.wlock.client.helper.OpaqueGenerator;

public class ResponseAck extends RegistryProtocol {
	
	public ResponseStatus status;

	public ResponseAck(byte opcode, ResponseStatus status) {
		this.status = status;
		this.setOpaque(opcode);
		this.setMsgType(MessageType.RESPONSE_ACK);
		this.setSessionId(OpaqueGenerator.getOpaque());
		this.setBody(ByteConverter.shortToBytesLittleEndian(this.status.getValue()));
	}

	public ResponseAck(byte opcode) {
		this.setOpaque(opcode);
		this.setMsgType(MessageType.RESPONSE_ACK);
		this.setSessionId(OpaqueGenerator.getOpaque());
		this.setBody(ByteConverter.shortToBytesLittleEndian(ResponseStatus.ACK_SUCCESS.getValue()));
	}

	public ResponseAck(byte opcode, long sessionId) {
		this.setOpaque(opcode);
		this.setMsgType(MessageType.RESPONSE_ACK);
		this.setSessionId(sessionId);
		this.setBody(ByteConverter.shortToBytesLittleEndian(ResponseStatus.ACK_SUCCESS.getValue()));
	}

}
