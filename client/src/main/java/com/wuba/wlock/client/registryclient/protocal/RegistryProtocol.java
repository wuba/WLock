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

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class RegistryProtocol {
	
	private static int HEADERLEN = 15;
	/**
	 * 数据总长度，不包含头尾长度，去掉头尾后的总长度
	 */
	protected int totalLen;
	/**
	 * 版本
	 */
	protected byte version = 1;
	/**
	 * 操作码，表示进行的操作类型；
	 */
	protected byte opaque;
	/**
	 * 请求的请求类型或响应的结果类型；用于快速处理；
	 */
	protected byte msgType;
	/**
	 * session
	 */
	private long sessionId;
	/**
	 * 二进制的数据实体
	 */
	protected byte[] body;

	public byte[] toBytes() throws Exception {
		ByteArrayOutputStream stream = null;
		try {
			stream = new ByteArrayOutputStream();
			if (null != this.body) {
				this.totalLen = HEADERLEN + this.body.length;
			} else {
				this.totalLen = HEADERLEN;
			}
			//额外长度参数
			stream.write(ByteConverter.IntToBytesBigEndian(this.totalLen));
			
			stream.write(ByteConverter.intToBytesLittleEndian(this.totalLen));
			stream.write(this.getVersion());
			stream.write(this.getOpaque());
			stream.write(this.getMsgType());
			stream.write(ByteConverter.longToBytesLittleEndian(this.getSessionId()));
			if (null != this.body) {
				stream.write(this.getBody());
			}
			stream.write(ProtocolConstant.P_END_TAG);
			return stream.toByteArray();
		} catch (Exception e) {
			throw new Exception(e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ex) {
					throw new Exception(ex);
				}
			}
		}
	}

	public static RegistryProtocol fromBytes(byte[] msg) {
		RegistryProtocol registryProtocol = new RegistryProtocol();
		int index = 0;
		int totalLen = ByteConverter.bytesToIntLittleEndian(msg, index);
		registryProtocol.setTotalLen(totalLen);
		index += 4;
		registryProtocol.setVersion(msg[index]);
		index += 1;
		registryProtocol.setOpaque(msg[index]);
		index += 1;
		registryProtocol.setMsgType(msg[index]);
		index += 1;
		registryProtocol.setSessionId(ByteConverter.bytesToLongLittleEndian(msg, index));
		index += 8;
		byte[] body = null;
		if (totalLen > HEADERLEN) {
			body = new byte[totalLen - HEADERLEN];
			System.arraycopy(msg, index, body, 0, totalLen - HEADERLEN);
		}
		registryProtocol.setBody(body);

		return registryProtocol;
	}

	public int getTotalLen() {
		return totalLen;
	}

	public void setTotalLen(int totalLen) {
		this.totalLen = totalLen;
	}

	public byte getOpaque() {
		return opaque;
	}

	public void setOpaque(byte opaque) {
		this.opaque = opaque;
	}

	public byte getMsgType() {
		return msgType;
	}

	public void setMsgType(byte msgType) {
		this.msgType = msgType;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}
}
