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
import com.wuba.wlock.server.exception.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DeleteLockDO extends BaseLockDO {

	public DeleteLockDO() {
	}

	public DeleteLockDO(byte lockType, byte opcode) {
		super(lockType, opcode);
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

	public static DeleteLockDO fromBytes(byte[] buf) {
		DeleteLockDO deleteLockDO = new DeleteLockDO();
		int index = 0;
		deleteLockDO.setVersion(buf[index]);
		index += 1;
		deleteLockDO.setProtocolType(buf[index]);
		index += 1;
		deleteLockDO.setLockType(buf[index]);
		index += 1;
		deleteLockDO.setOpcode(buf[index]);
		index += 1;
		short keyLen = ByteConverter.bytesToShortLittleEndian(buf, index);
		deleteLockDO.setLockKeyLen(keyLen);
		index += 2;
		byte[] keyBuf = new byte[keyLen];
		System.arraycopy(buf, index, keyBuf, 0, keyLen);
		String key = new String(keyBuf);
		deleteLockDO.setLockKey(key);
		index += keyLen;
		deleteLockDO.setHost(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		deleteLockDO.setPid(ByteConverter.bytesToIntLittleEndian(buf, index));
		index += 4;
		deleteLockDO.setThreadID(ByteConverter.bytesToLongLittleEndian(buf, index));
		index += 8;
		deleteLockDO.setFencingToken(ByteConverter.bytesToLongLittleEndian(buf, index));
		return deleteLockDO;
	}
}
