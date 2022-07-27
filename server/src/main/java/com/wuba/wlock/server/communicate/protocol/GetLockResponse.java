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
package com.wuba.wlock.server.communicate.protocol;

import com.wuba.wlock.server.communicate.WLockResponse;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.util.ByteConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GetLockResponse extends WLockResponse {
	public static int EXTEND_FIXED_LENGTH = 24;
	/** 锁版本*/
	protected long fencingToken;
	/** 锁owner ip*/
	protected int ownerHost;
	/** 锁owner 线程ID*/
	protected long ownerThreadID;
	/** 锁owner pid*/
	protected int ownerPID;
	
	@Override
	public byte[] genExtraBytes() throws ProtocolException {
		ByteArrayOutputStream stream = null;
		try {
			stream = new ByteArrayOutputStream();
			
			stream.write(ByteConverter.longToBytesLittleEndian(this.fencingToken));
			stream.write(ByteConverter.intToBytesLittleEndian(this.ownerHost));
			stream.write(ByteConverter.longToBytesLittleEndian(this.ownerThreadID));
			stream.write(ByteConverter.longToBytesLittleEndian(this.ownerPID));
			
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
		
		int host = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		this.setOwnerHost(host);
		
		long threadID = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;
		this.setOwnerThreadID(threadID);
		
		int pid = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		this.setOwnerPID(pid);
	}

	public long getFencingToken() {
		return fencingToken;
	}

	public void setFencingToken(long fencingToken) {
		this.fencingToken = fencingToken;
	}

	public int getOwnerHost() {
		return ownerHost;
	}

	public void setOwnerHost(int ownerHost) {
		this.ownerHost = ownerHost;
	}

	public long getOwnerThreadID() {
		return ownerThreadID;
	}

	public void setOwnerThreadID(long ownerThreadID) {
		this.ownerThreadID = ownerThreadID;
	}
	
	public int getOwnerPID() {
		return ownerPID;
	}

	public void setOwnerPID(int ownerPID) {
		this.ownerPID = ownerPID;
	}
}