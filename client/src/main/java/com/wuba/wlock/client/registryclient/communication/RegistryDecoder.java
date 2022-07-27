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
package com.wuba.wlock.client.registryclient.communication;

import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.helper.ByteConverter;
import com.wuba.wlock.client.helper.ProtocolHelper;
import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;
import com.wuba.wlock.client.registryclient.protocal.*;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;


public class RegistryDecoder implements IFrameDecoder {

	private static final Log logger = LogFactory.getLog(RegistryDecoder.class);
	
	private volatile int index = 0;
	
	private volatile byte[] remainBytes = null;
	
	@Override
	public void decode(SocketChannel sockChannel, RegistryNIOChannel nioChannel)
			throws CommunicationException, IOException, NotYetConnectedException, ProtocolException {
		if (nioChannel == null) {
			return;
		}

		ByteBuffer receiveBuffer = nioChannel.getReceiveBuffer();
		String hashKey = nioChannel.getServer().getHashKey();
		receiveBuffer.clear();
		int ret = sockChannel.read(receiveBuffer);
		if (ret < 0) {
			logger.warn(Version.INFO + ", RegistryDecoder: ret < 0 when decode, close it");
			if (null != nioChannel && null != hashKey) {
				try {
					RegistryKeyFactory.getInsatnce().getSerPool().addDaemonCheckTask(hashKey, nioChannel.getServer().getServerConfig());
					nioChannel.close();
				} catch (Exception e) {
					logger.error(Version.INFO + ", close socket error ", e);
				}
			}
			try {
				if (null != hashKey) {
					RegistryKeyFactory.getInsatnce().getSerPool().replaceRegistryServer(hashKey);
				}
			} catch (Exception e) {
				logger.error(Version.INFO + ", there is no registry server to connect", e);
			}
		}
		receiveBuffer.flip();
		ByteBuffer receiveData = nioChannel.getReceiveData();
		receiveData.clear();
		if (remainBytes != null) {
			receiveData.put(remainBytes);
			remainBytes = null;
		}
		receiveData.put(receiveBuffer);
		receiveData.flip();

		while (receiveData.remaining() > 4) {
			receiveData.mark();
			int totalLen = receiveData.getInt();
			if (receiveData.remaining() >= totalLen + ProtocolConstant.P_END_TAG.length) {
				byte[] dataBuf = new byte[totalLen];
				byte[] endDelimiter = new byte[ProtocolConstant.P_END_TAG.length];
				receiveData.get(dataBuf, 0, totalLen);
				receiveData.get(endDelimiter, 0, ProtocolConstant.P_END_TAG.length);
				if (ProtocolHelper.checkEndDelimiter(endDelimiter)) {
					dispatchData(sockChannel, nioChannel, dataBuf);
					continue;
				} else {
					//跳过错误的数据
					skipErrorMsg(receiveData);
					logger.warn(Version.INFO + ", RegistryDecoder: decode check endDelimter failed, skip this data!");
				}
			} else {
				receiveData.reset();
				break;
			}
		}
		
		if (receiveData.remaining() > 0) {
			receiveData.reset();
			remainBytes = new byte[receiveData.remaining()];
			receiveData.get(remainBytes, 0, remainBytes.length);
		}
		receiveData.clear();
	}
	
	private void skipErrorMsg(ByteBuffer receiveData) {
		receiveData.reset();
		index = 0;
		while (receiveData.remaining() > 0) {
			byte b = receiveData.get();
			if (b == ProtocolConstant.P_END_TAG[index]) {
				index++;
				if (index == ProtocolConstant.P_END_TAG.length) {
					index = 0;
					break;
				}
			} else if (index != 0) {
				if (b == ProtocolConstant.P_END_TAG[0]) {
					index = 1;
				} else {
					index = 0;
				}
			}
		}
	}
	
	private void dispatchData(SocketChannel sockChannel, RegistryNIOChannel nioChannel, byte[] msgData) {
		byte opcode = msgData[5]; //操作码
		long sessionId = ByteConverter.bytesToLongLittleEndian(msgData, 7);
		try {
			nioChannel.syncSend(new ResponseAck(opcode, sessionId).toBytes());
			logger.debug(Version.INFO + ", return ack to registry server success!");
		} catch (Exception e) {
			logger.warn(Version.INFO + ", return ack to registry server failed, because", e);
		}
		switch (opcode) {
			case OptionCode.OPCODE_LOCK_CLIENT_CONFIG_GET : {
				nioChannel.notify(sessionId, msgData);
				break;
			}
			case OptionCode.OPCODE_LOCK_CLIENT_CONFIG_PUSH : {
				RegistryProtocol resProtocol = RegistryProtocol.fromBytes(msgData);
				switch (resProtocol.getMsgType()) {
					case MessageType.PUSH_ALL_DATAS : {
						logger.debug(Version.INFO + ", push config from registry server success!");
						if (resProtocol.getBody() != null) {
							ClientKeyEntity resKey = ClientKeyEntity.parse(new String(resProtocol.getBody()));
							RegistryKeyFactory.getInsatnce().updateConfigs(resKey);
						} else {
							logger.warn(Version.INFO + ", push config from registry server success, but response is null, so ignore!");
						}
						break;
					}
					case MessageType.PUSH_CHANGED_SERVER : {
						logger.info(Version.INFO + ", push changed server from registry server success!");
						break;
					}
					default : {
						logger.info(Version.INFO + ", push config from registry server failed, because messgaeType does not match, error messageType is :" + resProtocol.getMsgType());
					}
				}
				break;
			}
			case OptionCode.OPCODE_LOCK_CLIENT_HEARTBEAT : {
				nioChannel.notify(sessionId, msgData);
				break;
			}
			case OptionCode.OPCODE_LOCK_CLIENT_VERSION : {
				nioChannel.notify(sessionId, msgData);
				break;
			}
			default : {
				logger.warn(Version.INFO + ", pull config from registry server failed, because optionCode does not match. error optionCode is :" + opcode);
			}
		}
	}
	
}
