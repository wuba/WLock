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

import com.wuba.wlock.client.config.Factor;
import com.wuba.wlock.client.exception.ProtocolException;
import com.wuba.wlock.client.helper.ByteConverter;
import com.wuba.wlock.client.protocol.WLockRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WatchLockRequest extends WLockRequest {
	public static int EXTEND_FIXED_LENGTH = 37;
	/** 锁版本*/
	protected long fencingToken;
	/**watch事件类型*/
	protected int eventType;
	/**watch ID*/
	protected long watchID;
	/**max wait超时时间*/
	protected long timeout = Factor.WATCH_MAX_WAIT_TIME_MARK;
	/**是否等待获取到锁*/
	protected byte waitAcquire;
	/**锁超时时间*/
	protected int expireTime;
	/**锁权重*/
	protected int weight;

	public int getEventType() {
		return eventType;
	}

	public void setEventType(int eventType) {
		this.eventType = eventType;
	}

	public long getWatchID() {
		return watchID;
	}

	public void setWatchID(long watchID) {
		this.watchID = watchID;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public long getFencingToken() {
		return fencingToken;
	}

	public void setFencingToken(long fencingToken) {
		this.fencingToken = fencingToken;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public byte getWaitAcquire() {
		return waitAcquire;
	}

	public void setWaitAcquire(byte waitAcquire) {
		this.waitAcquire = waitAcquire;
	}

	public int getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(int expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public byte[] genExtraBytes() throws ProtocolException{
		ByteArrayOutputStream stream = null;
		try {
			stream = new ByteArrayOutputStream();
			
			stream.write(ByteConverter.longToBytesLittleEndian(this.fencingToken));
			stream.write(ByteConverter.intToBytesLittleEndian(this.eventType));
			stream.write(ByteConverter.longToBytesLittleEndian(this.watchID));
			stream.write(ByteConverter.longToBytesLittleEndian(this.timeout));
			stream.write(this.waitAcquire);
			stream.write(ByteConverter.intToBytesLittleEndian(this.expireTime));
			stream.write(ByteConverter.intToBytesLittleEndian(this.weight));

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
		
		int eventType = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		this.setEventType(eventType);
		
		long watchID = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;
		this.setWatchID(watchID);
		
		long timeout = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;
		this.setTimeout(timeout);
		
		this.setWaitAcquire(buf[index]);
		index += 1;
		
		int expireTime = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		this.setExpireTime(expireTime);
		
		int weight = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		this.setWeight(weight);
	}

	@Override
	public boolean isAsync() {
		return false;
	}

}