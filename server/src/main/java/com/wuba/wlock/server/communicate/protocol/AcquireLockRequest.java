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

import com.wuba.wlock.server.communicate.WLockRequest;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.util.ByteConverter;

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
	
	public boolean isBlocked() {
		return this.blocked == 1;
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
	public byte[] genExtraBytes() throws ProtocolException{
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

	@Override
	public boolean isAsync() {
		return false;
	}
}
