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
import com.wuba.wlock.server.communicate.protocol.RenewLockRequest;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.util.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RenewLockDO extends BaseLockDO {

	private long expireTime;

	public RenewLockDO() {
	}

	public RenewLockDO(byte lockType, byte opcode) {
		super(lockType, opcode);
	}

	public static RenewLockDO fromRequest(RenewLockRequest renewLockRequest) {
		RenewLockDO renewLockDO = new RenewLockDO(renewLockRequest.getLockType(), renewLockRequest.getOpcode());
		renewLockDO.setExpireTime(TimeUtil.getCurrentTimestamp() + renewLockRequest.getExpireMills());
		renewLockDO.setVersion(renewLockRequest.getVersion());
		renewLockDO.setFencingToken(renewLockRequest.getFencingToken());
		renewLockDO.setProtocolType(renewLockRequest.getProtocolType());
		renewLockDO.setLockKeyLen(renewLockRequest.getLockKeyLen());
		renewLockDO.setLockKey(renewLockRequest.getLockKey());
		renewLockDO.setHost(renewLockRequest.getHost());
		renewLockDO.setPid(renewLockRequest.getPid());
		renewLockDO.setThreadID(renewLockRequest.getThreadID());
		renewLockDO.setLockType(renewLockRequest.getLockType());
		renewLockDO.setOpcode(renewLockRequest.getOpcode());
		return renewLockDO;
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

	public static RenewLockDO fromBytes(byte[] buf) {
		RenewLockDO renewLockDO = new RenewLockDO();
		int index = 0;
		renewLockDO.setVersion(buf[index]);
		index += 1;
		renewLockDO.setProtocolType(buf[index]);
		index += 1;
		renewLockDO.setLockType(buf[index]);
		index += 1;
		renewLockDO.setOpcode(buf[index]);
		index += 1;
		short keyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
		renewLockDO.setLockKeyLen(keyLen);
		index += 2;
		byte[] keyBuf = new byte[keyLen];
		System.arraycopy(buf, index, keyBuf, 0, keyLen);
		String key = new String(keyBuf);
		renewLockDO.setLockKey(key);
		index += keyLen;
		renewLockDO.setHost(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		renewLockDO.setPid(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		renewLockDO.setThreadID(ByteConverter.bytesToLongLittleEndian(buf, index));
		index += 8;
		renewLockDO.setFencingToken(ByteConverter.bytesToLongLittleEndian(buf, index));
		index += 8;
		renewLockDO.setExpireTime(ByteConverter.bytesToLongLittleEndian(buf, index));
		return renewLockDO;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}
}
