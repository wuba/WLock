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
package com.wuba.wlock.server.communicate;

import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.util.ByteConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class WLockResponse implements WLockProtocol {
	public static final int SESSION_ID_POS = 7;
	public static int FIXED_LENGTH = 25;
	/** 总长度*/
	protected int totalLen = 0;
	/** 协议版本*/
	protected byte version = 0x01;
	/** command type*/
	protected byte commandType = 0x01;
	/** 协议类型*/
	protected byte protocolType;
	
	protected long sessionID;
	/**请求结果*/
	protected short status;
	/**重定向次数*/
	protected short redirectTimes;
	/** lock key length*/
	protected short lockKeyLen;
	/** 锁的key*/
	protected String lockKey;
	/** key-v属性string长度 */
	protected int propertiesLen = 0;
	/** key-v属性string */
	protected String properties = "";
	
	protected Map<String, String> propertiesMap;
	
	public static final Charset UTF_8 = Charset.forName("utf-8");
	
	public static int STATUS_OFFSET = 15;
	
	public static int SESSIONID_OFFSET = 7;
	
	public abstract byte[] genExtraBytes() throws ProtocolException;
	
	public abstract void parseExtraBytes(byte[] buf, int index) throws ProtocolException;
	
	@Override
	public byte[] toBytes() throws ProtocolException {
		ByteArrayOutputStream stream = null;
		try {
			
			int extend_fix_len = 0;
			byte[] extraBytes = genExtraBytes();
			if (extraBytes != null) {
				extend_fix_len = extraBytes.length;
			}
			
			stream = new ByteArrayOutputStream();
			properties2String();
			this.lockKeyLen = (short) this.lockKey.length();
			int dyncLen = this.lockKeyLen + this.getPropertiesLen();
			
			this.totalLen = WLockResponse.FIXED_LENGTH + dyncLen + extend_fix_len;
			stream.write(ByteConverter.intToBytesLittleEndian(this.totalLen));
			stream.write(this.version);
			stream.write(this.commandType);
			stream.write(this.protocolType);
			stream.write(ByteConverter.longToBytesLittleEndian(this.sessionID));
			stream.write(ByteConverter.shortToBytesLittleEndian(this.status));
			stream.write(ByteConverter.shortToBytesLittleEndian(this.redirectTimes));
			stream.write(ByteConverter.shortToBytesLittleEndian(this.lockKeyLen));
			stream.write(lockKey.getBytes());
			stream.write(ByteConverter.intToBytesLittleEndian(propertiesLen));
			if (propertiesLen > 0) {
				stream.write(properties.getBytes());
			}
			
			if (extend_fix_len > 0) {
				stream.write(extraBytes);
			}
			
			return stream.toByteArray();
		} catch (Exception e) {
			throw new ProtocolException(e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					throw new ProtocolException(e);
				}
			}
		}
	}
	
	@Override
	public void fromBytes(byte[] buf) throws ProtocolException {
		int index = 0;
		
		int totalLen = ByteConverter.bytesToIntLittleEndian(buf, index);
		if (totalLen != buf.length) {
			throw new ProtocolException("total length illegal.");
		}
		this.setTotalLen(totalLen);
		index += 4;
		
		byte version = buf[index];
		this.setVersion(version);
		index++;
		
		byte commandType = buf[index];
		this.setCommandType(commandType);
		index++;
		
		byte protocolType = buf[index];
		this.setProtocolType(protocolType);
		index++;
		
		long sessionID = ByteConverter.bytesToLongLittleEndian(buf, index);
		this.setSessionID(sessionID);
		index += 8;
		
		short status = ByteConverter.bytesToShortLittleEndian(buf, index);
		this.setStatus(status);
		index += 2;
		
		short redirectTimes = ByteConverter.bytesToShortLittleEndian(buf, index);
		this.setRedirectTimes(redirectTimes);
		index += 2;
		
		short lockKeyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
		this.setLockKeyLen(lockKeyLen);
		index += 2;
		
		byte[] lockkeyBuf = new byte[lockKeyLen];
		System.arraycopy(buf, index, lockkeyBuf, 0, lockKeyLen);
		String lockkey = new String(lockkeyBuf);
		this.setLockKey(lockkey);
		index += lockKeyLen;
		
		int propertiesLen = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		this.setPropertiesLen(propertiesLen);
		
		if (propertiesLen > 0) {
			byte[] protsBuf = new byte[propertiesLen];
			System.arraycopy(buf, index, protsBuf, 0, propertiesLen);
			String properties = new String(protsBuf);
			this.setProperties(properties);
			this.propertiesMap = string2messageProperties(properties);
		}
		index += propertiesLen;
		
		parseExtraBytes(buf, index);
	}

	public short getStatus() {
		return status;
	}

	public void setStatus(short status) {
		this.status = status;
	}

	public int getTotalLen() {
		return totalLen;
	}

	public void setTotalLen(int totalLen) {
		this.totalLen = totalLen;
	}

	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	public byte getCommandType() {
		return commandType;
	}

	public void setCommandType(byte commandType) {
		this.commandType = commandType;
	}

	@Override
	public byte getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(byte protocolType) {
		this.protocolType = protocolType;
	}

	public long getSessionID() {
		return sessionID;
	}

	@Override
	public void setSessionID(long sessionID) {
		this.sessionID = sessionID;
	}

	public short getLockKeyLen() {
		return lockKeyLen;
	}

	public void setLockKeyLen(short lockKeyLen) {
		this.lockKeyLen = lockKeyLen;
	}

	@Override
	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}

	public short getRedirectTimes() {
		return redirectTimes;
	}

	public void setRedirectTimes(short redirectTimes) {
		this.redirectTimes = redirectTimes;
	}

	public int getPropertiesLen() {
		return propertiesLen;
	}

	public void setPropertiesLen(int propertiesLen) {
		this.propertiesLen = propertiesLen;
	}

	public String getProperties() {
		return properties;
	}

	public void setProperties(String properties) {
		this.properties = properties;
	}

	public Map<String, String> getPropertiesMap() {
		return propertiesMap;
	}

	public void setPropertiesMap(Map<String, String> propertiesMap) {
		this.propertiesMap = propertiesMap;
	}
	
	public String properties2String() {
		StringBuilder sb = new StringBuilder();
		if (propertiesMap != null) {
			for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
				final String name = entry.getKey();
				final String value = entry.getValue();

				sb.append(name);
				sb.append(ProtocolConst.NAME_VALUE_SEPARATOR);
				sb.append(value);
				sb.append(ProtocolConst.PROPERTY_SEPARATOR_T);
			}

			properties = sb.toString();
			propertiesLen = properties.length();
		} else {
			properties = "";
			propertiesLen = 0;
		}
		return properties;
	}

	public static Map<String, String> string2messageProperties(final String properties) {
		Map<String, String> map = new HashMap<String, String>();
		if (properties != null) {
			String[] items = properties.split(String.valueOf(ProtocolConst.PROPERTY_SEPARATOR));
			if (items != null) {
				for (String i : items) {
					String[] nv = i.split(String.valueOf(ProtocolConst.NAME_VALUE_SEPARATOR));
					if (nv != null && 2 == nv.length) {
						map.put(nv[0], nv[1]);
					}
				}
			}
		}

		return map;
	}

	public void putProperty(final String name, final String value) {
		if (null == this.properties || this.propertiesMap == null) {
			this.propertiesMap = new HashMap<String, String>();
		}

		this.propertiesMap.put(name, value);
	}

	public String getProperty(final String name) {
		if (null == this.propertiesMap) {
			this.propertiesMap = new HashMap<String, String>();
		}

		return this.propertiesMap.get(name);
	}

	void clearProperty(final String name) {
		if (null != this.properties) {
			this.propertiesMap.remove(name);
		}
	}

	@Override
	public void setTimestamp(long timestamp) {
	}
}