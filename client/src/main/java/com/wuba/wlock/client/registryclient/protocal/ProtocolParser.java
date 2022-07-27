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

import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import com.wuba.wlock.client.helper.ByteConverter;
import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ProtocolParser {
	
	private static final Log logger = LogFactory.getLog(ProtocolParser.class);

	public static void parse(RegistryProtocol resProtocol) throws Exception {
		switch (resProtocol.getOpaque()) {
			case OptionCode.OPCODE_LOCK_CLIENT_CONFIG_GET : {
				switch (resProtocol.getMsgType()) {
					case MessageType.RESPONSE_ALL_DATAS_SUCCESS : {
						ClientKeyEntity resKey = ClientKeyEntity.parse(new String(resProtocol.getBody()));
						if (resKey != null) {
							RegistryKeyFactory.getInsatnce().initConfigs(resKey);
						} else {
							throw new RegistryClientRuntimeException(Version.INFO + ", receive key is error. error key:" + new String(resProtocol.getBody()));
						}
						break;
					}
					
					case MessageType.RESPONSE_ALL_DATAS_ERROR : {
						throw new RegistryClientRuntimeException(Version.INFO + ", pull config from registry server failed, because:" + new String(resProtocol.getBody()));
					}
					
					case MessageType.RESPONSE_HAS_CHANGE : {
						if (resProtocol.getBody() != null) {
							ClientKeyEntity resKey = ClientKeyEntity.parse(new String(resProtocol.body));
							RegistryKeyFactory.getInsatnce().updateConfigs(resKey);
							logger.info(Version.INFO + ", pull config from registry server success, some configs have been changed. message is " + resKey.toString());
						} else {
							logger.warn(Version.INFO + ", pull config from registry server success, but response body is null, so ignore.");
						}
						break;
					}
					
					case MessageType.RESPONSE_NO_CHANGE : {
						logger.debug(Version.INFO + ", pull config from registry server success, all configs have no change.");
						break;
					}
					
					case MessageType.RESPONSE_ERROR : {
						logger.warn(Version.INFO + ", pull config from registry server failed, because:" + new String(resProtocol.getBody()));
						break;
					}

					default : {
						logger.warn(Version.INFO + ", pull config from registry server failed, because messageType does not match. error messageType is: " + resProtocol.getMsgType());
					}
				}
				break;
			}
			case OptionCode.OPCODE_LOCK_CLIENT_VERSION : {
				if (resProtocol.getMsgType() == MessageType.RESPONSE_ACK) {
					if (ResponseStatus.parse(ByteConverter.bytesToShortLittleEndian(resProtocol.getBody())) == ResponseStatus.ACK_SUCCESS) {
						logger.debug(Version.INFO + ", response ack is success.");
					} else {
						logger.debug(Version.INFO + ", response ack is failed.");
					}
				} else {
					logger.warn(Version.INFO + ", send version to registry server failed, because messageType does not match. error messageType is: " + resProtocol.getMsgType());
				}
				break;
			}
			case OptionCode.OPCODE_LOCK_CLIENT_HEARTBEAT : {
				if (resProtocol.getMsgType() == MessageType.RESPONSE_ACK) {
					if (ResponseStatus.parse(ByteConverter.bytesToShortLittleEndian(resProtocol.getBody())) == ResponseStatus.ACK_SUCCESS) {
						logger.debug(Version.INFO + ", response registry heartbeat ack is success.");
					} else {
						logger.debug(Version.INFO + ", response registry heartbeat ack is failed.");
					}
				} else {
					logger.warn(Version.INFO + ", send heartbeat to registry server failed, because messageType does not match. error messageType is: " + resProtocol.getMsgType());
				}
				break;
			}
			default : {
				logger.warn(Version.INFO + ", pull config from registry server failed, because optionCode does not match. error optionCode is: " + resProtocol.getOpaque());
			}
		}
	}

}
