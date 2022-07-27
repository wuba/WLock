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
import com.wuba.wlock.server.communicate.protocol.ReleaseLockRequest;
import com.wuba.wlock.server.exception.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReleaseLockDO extends BaseLockDO {

	public ReleaseLockDO() {
	}

	public ReleaseLockDO(byte lockType, byte opcode) {
		super(lockType, opcode);
	}

	public static ReleaseLockDO fromRequest(ReleaseLockRequest releaseLockRequest) {
		ReleaseLockDO releaseLockDO = new ReleaseLockDO(releaseLockRequest.getLockType(), releaseLockRequest.getOpcode());
		releaseLockDO.setVersion(releaseLockRequest.getVersion());
		releaseLockDO.setFencingToken(releaseLockRequest.getFencingToken());
		releaseLockDO.setProtocolType(releaseLockRequest.getProtocolType());
		releaseLockDO.setLockKeyLen(releaseLockRequest.getLockKeyLen());
		releaseLockDO.setLockKey(releaseLockRequest.getLockKey());
		releaseLockDO.setHost(releaseLockRequest.getHost());
		releaseLockDO.setPid(releaseLockRequest.getPid());
		releaseLockDO.setThreadID(releaseLockRequest.getThreadID());
		releaseLockDO.setLockType(releaseLockRequest.getLockType());
		releaseLockDO.setOpcode(releaseLockRequest.getOpcode());
		return releaseLockDO;
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
			return stream.toByteArray();
		} catch(IOException e) {
			throw new ProtocolException(e);
		}
	}

	public static ReleaseLockDO fromBytes(byte[] buf) {
		ReleaseLockDO releaseLockDO = new ReleaseLockDO();
		int index = 0;
		releaseLockDO.setVersion(buf[index]);
		index += 1;
		releaseLockDO.setProtocolType(buf[index]);
		index += 1;
		releaseLockDO.setLockType(buf[index]);
		index += 1;
		releaseLockDO.setOpcode(buf[index]);
		index += 1;
		short keyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
		releaseLockDO.setLockKeyLen(keyLen);
		index += 2;
		byte[] keyBuf = new byte[keyLen];
		System.arraycopy(buf, index, keyBuf, 0, keyLen);
		String key = new String(keyBuf);
		releaseLockDO.setLockKey(key);
		index += keyLen;
		releaseLockDO.setHost(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		releaseLockDO.setPid(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		releaseLockDO.setThreadID(ByteConverter.bytesToLongLittleEndian(buf, index));
		index += 8;
		releaseLockDO.setFencingToken(ByteConverter.bytesToLongLittleEndian(buf, index));
		return releaseLockDO;
	}

}
