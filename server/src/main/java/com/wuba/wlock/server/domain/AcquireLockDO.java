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
package com.wuba.wlock.server.domain;

import com.wuba.wlock.common.util.ByteConverter;
import com.wuba.wlock.server.communicate.ProtocolType;
import com.wuba.wlock.server.communicate.protocol.AcquireLockRequest;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.watch.WatchEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AcquireLockDO extends BaseLockDO {

	private long expireTime;

	public AcquireLockDO() {
	}

	public AcquireLockDO(byte lockType, byte opcode) {
		super(lockType, opcode);
	}

	public static AcquireLockDO fromRequest(AcquireLockRequest acquireLockRequest, long version) {
		AcquireLockDO acquireLockDO = new AcquireLockDO(acquireLockRequest.getLockType(), acquireLockRequest.getOpcode());
//		acquireLockDO.setVersion(acquireLockRequest.getVersion()); // 版本是否合理
		acquireLockDO.setExpireTime(TimeUtil.getCurrentTimestamp() + acquireLockRequest.getExpireMills());
		acquireLockDO.setProtocolType(acquireLockRequest.getProtocolType());
		acquireLockDO.setLockKeyLen(acquireLockRequest.getLockKeyLen());
		acquireLockDO.setLockKey(acquireLockRequest.getLockKey());
		acquireLockDO.setHost(acquireLockRequest.getHost());
		acquireLockDO.setThreadID(acquireLockRequest.getThreadID());
		acquireLockDO.setFencingToken(version);
		acquireLockDO.setPid(acquireLockRequest.getPid());
		return acquireLockDO;
	}

	public static AcquireLockDO fromWatchEvent(String lockkey, WatchEvent watchEvent,long verison) {
		AcquireLockDO acquireLockDO = new AcquireLockDO(watchEvent.getLockType(), watchEvent.getOpcode());
		int exipreTime = watchEvent.getExpireTime();
		if (watchEvent.isAsync()) {
			exipreTime = Math.min(watchEvent.getExpireTime(), ServerConfig.getInstance().getLockInitExpireTime());
		} 
		acquireLockDO.setExpireTime(TimeUtil.getCurrentTimestamp() + exipreTime);
		acquireLockDO.setProtocolType(ProtocolType.ACQUIRE_LOCK);
		acquireLockDO.setLockKeyLen((short) lockkey.length());
		acquireLockDO.setLockKey(lockkey);
		acquireLockDO.setHost(watchEvent.getLockClient().getcHost());
		acquireLockDO.setThreadID(watchEvent.getLockClient().getcThreadID());
		acquireLockDO.setPid(watchEvent.getLockClient().getcPid());
		acquireLockDO.setFencingToken(verison);
		return acquireLockDO;
	}

	public byte[] toBytes() throws ProtocolException {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			stream.write(this.getVersion());
			stream.write(this.getProtocolType());
			stream.write(this.getLockType());
			stream.write(this.getOpcode());
			stream.write(ByteConverter.shortToBytesLittleEndian(this.getLockKeyLen()));
			stream.write(this.getLockKey().getBytes());
			stream.write(ByteConverter.intToBytesLittleEndian(this.getHost()));
			stream.write(ByteConverter.intToBytesLittleEndian(this.getPid()));
			stream.write(ByteConverter.longToBytesLittleEndian(this.getThreadID()));
			stream.write(ByteConverter.longToBytesLittleEndian(this.getFencingToken()));
			stream.write(ByteConverter.longToBytesLittleEndian(this.getExpireTime()));
			return stream.toByteArray();
		} catch(IOException e) {
			throw new ProtocolException(e);
		}
	}

	public static AcquireLockDO fromBytes(byte[] buf) {
		AcquireLockDO acquireLockDO = new AcquireLockDO();
		int index = 0;
		acquireLockDO.setVersion(buf[index]);
		index += 1;
		acquireLockDO.setProtocolType(buf[index]);
		index += 1;
		acquireLockDO.setLockType(buf[index]);
		index += 1;
		acquireLockDO.setOpcode(buf[index]);
		index += 1;
		short keyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
		acquireLockDO.setLockKeyLen(keyLen);
		index += 2;
		byte[] keyBuf = new byte[keyLen];
		System.arraycopy(buf, index, keyBuf, 0, keyLen);
		String key = new String(keyBuf);
		acquireLockDO.setLockKey(key);
		index += keyLen;
		acquireLockDO.setHost(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		acquireLockDO.setPid(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		acquireLockDO.setThreadID(ByteConverter.bytesToLongLittleEndian(buf, index));
		index += 8;
		acquireLockDO.setFencingToken(ByteConverter.bytesToLongLittleEndian(buf, index));
		index += 8;
		acquireLockDO.setExpireTime(ByteConverter.bytesToLongLittleEndian(buf, index));
		return acquireLockDO;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}
}
