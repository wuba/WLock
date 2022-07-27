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
package com.wuba.wlock.client.protocol.extend;

import com.wuba.wlock.client.exception.ProtocolException;
import com.wuba.wlock.client.helper.ByteConverter;
import com.wuba.wlock.client.protocol.WLockRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AcquireLockRequest extends WLockRequest {
	public static int EXTEND_FIXED_LENGTH = 33;
	/** 锁版本*/
	protected long fencingToken = -1;
	/** 锁过期时间*/
	protected int expireMills;	
	
	protected byte blocked;
	
	protected long timeout;
	
	protected int weight;	
	/**watch ID*/
	protected long watchID;

	public int getExpireMills() {
		return expireMills;
	}

	public void setExpireMills(int expireMills) {
		this.expireMills = expireMills;
	}

	public byte getBlocked() {
		return blocked;
	}

	public void setBlocked(byte blocked) {
		this.blocked = blocked;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public long getWatchID() {
		return watchID;
	}

	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}

	public long getFencingToken() {
		return fencingToken;
	}

	public void setFencingToken(long fencingToken) {
		this.fencingToken = fencingToken;
	}
	
	@Override
	public byte[] genExtraBytes() throws ProtocolException {
		ByteArrayOutputStream stream = null;
		try {
			stream = new ByteArrayOutputStream();
			
			stream.write(ByteConverter.longToBytesLittleEndian(this.fencingToken));
			stream.write(ByteConverter.intToBytesLittleEndian(this.expireMills));
			stream.write(this.blocked);
			stream.write(ByteConverter.longToBytesLittleEndian(this.timeout));
			stream.write(ByteConverter.intToBytesLittleEndian(this.weight));
			stream.write(ByteConverter.longToBytesLittleEndian(this.watchID));

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
	public void parseExtraBytes(byte[] buf, int index) throws ProtocolException {
		long fencingToken = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;
		this.setFencingToken(fencingToken);
		
		int expireTime = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		this.setExpireMills(expireTime);
		
		this.setBlocked(buf[index]);
		index += 1;
		
		long timeout = ByteConverter.bytesToLongLittleEndian(buf, index);
		this.setTimeout(timeout);
		index += 8;
		
		int weight = ByteConverter.bytesToIntLittleEndian(buf, index);
		this.setWeight(weight);
		index += 4;
		
		long watchid = ByteConverter.bytesToLongLittleEndian(buf, index);
		this.setWatchID(watchid);
		index += 8;
	}

//	@Override
//	public byte[] toBytes() throws ProtocolException {
//		ByteArrayOutputStream stream = null;
//		try {
//			stream = new ByteArrayOutputStream();
//			properties2String();
//			this.registryKeyLen = (short) this.registryKey.length();
//			this.lockKeyLen = (short) this.lockKey.length();
//			int dyncLen = this.registryKeyLen +this.lockKeyLen + this.getPropertiesLen();
//			
//			this.totalLen = WLockRequest.FIXED_LENGTH + EXTEND_FIXED_LENGTH + dyncLen;
//			stream.write(ByteConverter.intToBytesLittleEndian(this.totalLen));
//			stream.write(this.version);
//			stream.write(this.commandType);
//			stream.write(this.protocolType);
//			stream.write(ByteConverter.longToBytesLittleEndian(this.sessionID));
//			stream.write(ByteConverter.shortToBytesLittleEndian(this.registryKeyLen));
//			stream.write(this.registryKey.getBytes());
//			stream.write(ByteConverter.shortToBytesLittleEndian(this.lockKeyLen));
//			stream.write(this.lockKey.getBytes());
//			stream.write(ByteConverter.intToBytesLittleEndian(host));
//			stream.write(ByteConverter.longToBytesLittleEndian(threadID));
//			stream.write(ByteConverter.intToBytesLittleEndian(pid));
//			stream.write(ByteConverter.longToBytesLittleEndian(this.timestamp));
//			stream.write(ByteConverter.shortToBytesLittleEndian(this.redirectTimes));
//			stream.write(ByteConverter.intToBytesLittleEndian(this.propertiesLen));
//			stream.write(this.properties.getBytes());
//			
//			stream.write(ByteConverter.longToBytesLittleEndian(this.fencingToken));
//			stream.write(ByteConverter.intToBytesLittleEndian(this.expireMills));
//			stream.write(this.blocked);
//			stream.write(ByteConverter.longToBytesLittleEndian(this.timeout));
//			stream.write(ByteConverter.intToBytesLittleEndian(this.weight));
//			stream.write(ByteConverter.longToBytesLittleEndian(this.watchID));
//
//			return stream.toByteArray();
//		} catch (Exception e) {
//			throw new ProtocolException(e);
//		} finally {
//			if (stream != null) {
//				try {
//					stream.close();
//				} catch (IOException e) {
//					throw new ProtocolException(e);
//				}
//			}
//		}
//	}

//	@Override
//	public void fromBytes(byte[] buf) throws ProtocolException {
//		int index = 0;
//		
//		int totalLen = ByteConverter.bytesToIntLittleEndian(buf, index);
//		if (totalLen != buf.length) {
//			throw new ProtocolException("total length illegal.");
//		}
//		this.setTotalLen(totalLen);
//		index += 4;
//		
//		byte version = buf[index];
//		this.setVersion(version);
//		index++;
//		
//		byte commandType = buf[index];
//		this.setCommandType(commandType);
//		index++;
//		
//		byte protocolType = buf[index];
//		this.setProtocolType(protocolType);
//		index++;
//		
//		long sessionID = ByteConverter.bytesToLongLittleEndian(buf, index);
//		this.setSessionID(sessionID);
//		index += 8;
//		
//		short registryKeyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
//		this.setRegistryKeyLen(registryKeyLen);
//		index += 2;
//		
//		if (registryKeyLen > 0) {
//			byte[] registryKeyBuf = new byte[registryKeyLen];
//			System.arraycopy(buf, index, registryKeyBuf, 0, registryKeyLen);
//			String registryKey = new String(registryKeyBuf);
//			this.setRegistryKey(registryKey);
//		}
//		index += registryKeyLen;
//		
//		short lockKeyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
//		this.setLockKeyLen(lockKeyLen);
//		index += 2;
//		
//		if (lockKeyLen > 0) {
//			byte[] lockkeyBuf = new byte[lockKeyLen];
//			System.arraycopy(buf, index, lockkeyBuf, 0, lockKeyLen);
//			String lockkey = new String(lockkeyBuf);
//			this.setLockKey(lockkey);
//		}
//		index += lockKeyLen;
//		
//		int host = ByteConverter.bytesToIntLittleEndian(buf, index);
//		this.setHost(host);
//		index += 4;
//		
//		long threadID = ByteConverter.bytesToLongLittleEndian(buf, index);
//		this.setThreadID(threadID);
//		index += 8;
//		
//		int pid = ByteConverter.bytesToIntLittleEndian(buf, index);
//		this.setPid(pid);
//		index += 4;
//		
//		long timestamp = ByteConverter.bytesToLongLittleEndian(buf, index);
//		this.setTimestamp(timestamp);
//		index += 8;
//		
//		short redirectTimes = ByteConverter.bytesToShortLittleEndian(buf, index);
//		this.setRedirectTimes(redirectTimes);
//		index += 2;
//		
//		int propertiesLen = ByteConverter.bytesToIntLittleEndian(buf, index);
//		index += 4;
//		this.setPropertiesLen(propertiesLen);
//		
//		if (propertiesLen > 0) {
//			byte[] protsBuf = new byte[propertiesLen];
//			System.arraycopy(buf, index, protsBuf, 0, propertiesLen);
//			String properties = new String(protsBuf);
//			this.setProperties(properties);
//			this.propertiesMap = string2messageProperties(properties);
//		}
//		index += propertiesLen;
//		
//		long fencingToken = ByteConverter.bytesToLongLittleEndian(buf, index);
//		index += 8;
//		this.setFencingToken(fencingToken);
//		
//		int expireTime = ByteConverter.bytesToIntLittleEndian(buf, index);
//		index += 4;
//		this.setExpireMills(expireTime);
//		
//		this.setBlocked(buf[index]);
//		index += 1;
//		
//		long timeout = ByteConverter.bytesToLongLittleEndian(buf, index);
//		this.setTimeout(timeout);
//		index += 8;
//		
//		int weight = ByteConverter.bytesToIntLittleEndian(buf, index);
//		this.setWeight(weight);
//		index += 4;
//		
//		long watchid = ByteConverter.bytesToLongLittleEndian(buf, index);
//		this.setWatchID(watchid);
//		index += 8;
//	}

	@Override
	public boolean isAsync() {
		return false;
	}
}
