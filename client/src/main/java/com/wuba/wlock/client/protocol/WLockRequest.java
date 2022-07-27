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
package com.wuba.wlock.client.protocol;

import com.wuba.wlock.client.exception.ProtocolException;
import com.wuba.wlock.client.helper.ByteConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class WLockRequest implements WLockProtocol {
	public static int FIXED_LENGTH = 55;
	/**
	 *  0：可重入锁 1：读写锁
	 */
	private byte lockType;
	/**
	 * 0为null，读写锁操作码：写锁 1， 读锁 2
	 */
	private byte lockOpcode;

	/** 总长度*/
	protected int totalLen = 0;

	protected byte version = 0x02;
	/** command type*/
	protected byte commandType = 0x00;
	/** 协议类型*/
	protected byte protocolType;
	
	protected long sessionID;
	/**重定向次数*/
	protected short redirectTimes;
	/**paxos groupId*/
	protected int groupId;
	/**注册秘钥长度*/
	protected short registryKeyLen;
	/**注册秘钥*/
	protected String registryKey = ""; 
	/** lock key length*/
	protected short lockKeyLen;
	/** 锁的key*/
	protected String lockKey = "";
	/** client ip*/
	protected int host;
	/** client threadID*/
	protected long threadID;
	/** client pid*/
	protected int pid;
	/**请求发送时间戳*/
	protected long timestamp;
	/** key-v属性string长度 */
	protected int propertiesLen = 0;
	/** key-v属性string */
	protected String properties = "";
	
	protected Map<String, String> propertiesMap;
	
	public static final Charset utf8 = Charset.forName("utf-8");
	
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
			this.registryKeyLen = (short) this.registryKey.length();
			this.lockKeyLen = (short) this.lockKey.length();
			int dyncLen = this.registryKeyLen +this.lockKeyLen + this.getPropertiesLen();
			
			this.totalLen = WLockRequest.FIXED_LENGTH + extend_fix_len + dyncLen;
			stream.write(ByteConverter.intToBytesLittleEndian(this.totalLen));
			stream.write(this.version);
			stream.write(this.commandType);
			stream.write(this.protocolType);
			stream.write(ByteConverter.longToBytesLittleEndian(this.sessionID));
			stream.write(ByteConverter.shortToBytesLittleEndian(this.redirectTimes));
			stream.write(ByteConverter.intToBytesLittleEndian(this.groupId));
			stream.write(this.lockType);
			stream.write(this.lockOpcode);
			stream.write(ByteConverter.shortToBytesLittleEndian(this.registryKeyLen));
			stream.write(this.registryKey.getBytes());
			stream.write(ByteConverter.shortToBytesLittleEndian(this.lockKeyLen));
			stream.write(this.lockKey.getBytes());
			stream.write(ByteConverter.intToBytesLittleEndian(host));
			stream.write(ByteConverter.longToBytesLittleEndian(threadID));
			stream.write(ByteConverter.intToBytesLittleEndian(pid));
			stream.write(ByteConverter.longToBytesLittleEndian(this.timestamp));
			stream.write(ByteConverter.intToBytesLittleEndian(this.propertiesLen));
			stream.write(this.properties.getBytes());
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
		
		short redirectTimes = ByteConverter.bytesToShortLittleEndian(buf, index);
		this.setRedirectTimes(redirectTimes);
		index += 2;
		
		int groupId = ByteConverter.bytesToIntLittleEndian(buf, index);
		this.setGroupId(groupId);
		index += 4;

		byte lockType = buf[index];
		this.setLockType(lockType);
		index++;

		byte opcode = buf[index];
		this.setLockOpcode(opcode);
		index++;
		
		short registryKeyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
		this.setRegistryKeyLen(registryKeyLen);
		index += 2;
		
		if (registryKeyLen > 0) {
			byte[] registryKeyBuf = new byte[registryKeyLen];
			System.arraycopy(buf, index, registryKeyBuf, 0, registryKeyLen);
			String registryKey = new String(registryKeyBuf);
			this.setRegistryKey(registryKey);
		}
		index += registryKeyLen;
		
		short lockKeyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
		this.setLockKeyLen(lockKeyLen);
		index += 2;
		
		if (lockKeyLen > 0) {
			byte[] lockkeyBuf = new byte[lockKeyLen];
			System.arraycopy(buf, index, lockkeyBuf, 0, lockKeyLen);
			String lockkey = new String(lockkeyBuf);
			this.setLockKey(lockkey);
		}
		index += lockKeyLen;
		
		int host = ByteConverter.bytesToIntLittleEndian(buf, index);
		this.setHost(host);
		index += 4;
		
		long threadID = ByteConverter.bytesToLongLittleEndian(buf, index);
		this.setThreadID(threadID);
		index += 8;
		
		int pid = ByteConverter.bytesToIntLittleEndian(buf, index);
		this.setPid(pid);
		index += 4;
		
		long timestamp = ByteConverter.bytesToLongLittleEndian(buf, index);
		this.setTimestamp(timestamp);
		index += 8;
		
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
	
	public boolean isAsync() {
		return false;
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

	public short getRegistryKeyLen() {
		return registryKeyLen;
	}

	public void setRegistryKeyLen(short registryKeyLen) {
		this.registryKeyLen = registryKeyLen;
	}

	public String getRegistryKey() {
		return registryKey;
	}

	public void setRegistryKey(String registryKey) {
		this.registryKey = registryKey;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getHost() {
		return host;
	}

	public void setHost(int host) {
		this.host = host;
	}

	public long getThreadID() {
		return threadID;
	}

	public void setThreadID(long threadID) {
		this.threadID = threadID;
	}
	
    public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public String getProperties() {
		return properties;
	}

	public void setProperties(String properties) {
		this.properties = properties;
	}

	public int getPropertiesLen() {
		return propertiesLen;
	}

	public void setPropertiesLen(int propertiesLen) {
		this.propertiesLen = propertiesLen;
	}

	public Map<String, String> getPropertiesMap() {
		return propertiesMap;
	}

	public void setPropertiesMap(Map<String, String> propertiesMap) {
		this.propertiesMap = propertiesMap;
	}

	public short getRedirectTimes() {
		return redirectTimes;
	}

	public void setRedirectTimes(short redirectTimes) {
		this.redirectTimes = redirectTimes;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
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
	
    void putProperty(final String name, final String value) {
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

	public int getLockType() {
		return lockType;
	}

	public void setLockType(byte lockType) {
		this.lockType = lockType;
	}

	public byte getLockOpcode() {
		return lockOpcode;
	}

	public void setLockOpcode(byte lockOpcode) {
		this.lockOpcode = lockOpcode;
	}
}