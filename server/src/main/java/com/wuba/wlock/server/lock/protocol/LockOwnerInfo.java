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
package com.wuba.wlock.server.lock.protocol;

import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wpaxos.exception.SerializeException;
import com.wuba.wpaxos.utils.ByteConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LockOwnerInfo {
	public static final int LENGTH = 4 + 8 + 4 + 8 + 8;
	private int ip;
	private long threadId;
	private int pid;
	//锁过期时间
	private long expireTime;

	//锁版本号
	private long lockVersion;

	public LockOwnerInfo() {
	}

	public LockOwnerInfo(int ip, long threadId, int pid) {
		this.ip = ip;
		this.threadId = threadId;
		this.pid = pid;
	}

	public byte[] toBytes() throws SerializeException {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			stream.write(ByteConverter.intToBytesLittleEndian(ip));
			stream.write(ByteConverter.intToBytesLittleEndian(pid));
			stream.write(ByteConverter.longToBytesLittleEndian(threadId));
			stream.write(ByteConverter.longToBytesLittleEndian(expireTime));
			stream.write(ByteConverter.longToBytesLittleEndian(lockVersion));
			return stream.toByteArray();
		} catch(IOException e) {
			throw new SerializeException(e);
		}
	}

	public static LockOwnerInfo fromBytes(byte[] buf) {
		return fromBytes(buf, 0);
	}

	public static LockOwnerInfo fromBytes(byte[] buf, int index) {
		LockOwnerInfo lockOwnerInfo = new LockOwnerInfo();
		if (buf.length < (index + LENGTH)) {
			return lockOwnerInfo;
		}
		int ip = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		int pid = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		long threadId = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;
		long expireTime = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;
		long lockVersion = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;

		lockOwnerInfo.setExpireTime(expireTime);
		lockOwnerInfo.setThreadId(threadId);
		lockOwnerInfo.setPid(pid);
		lockOwnerInfo.setIp(ip);
		lockOwnerInfo.setLockVersion(lockVersion);
		return lockOwnerInfo;
	}

	public static Map<String, LockOwnerInfo> fromBytesList(byte[] buf, int index) {
		Map<String, LockOwnerInfo> lockOwnerInfos = new HashMap<String, LockOwnerInfo>();
		if (buf.length < (index + LENGTH)) {
			return new HashMap<String, LockOwnerInfo>();
		}
		LockOwnerInfo lockOwnerInfo;
		while ((buf.length - index) >= LENGTH) {
			lockOwnerInfo = fromBytes(buf, index);
			index += LENGTH;
 			lockOwnerInfos.put(lockOwnerInfo.uniqueKey(), lockOwnerInfo);
		}
		return lockOwnerInfos;
	}

	public static Map<String, LockOwnerInfo> fromBytesList(byte[] buf) {
		return fromBytesList(buf, 0);
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public int getIp() {
		return ip;
	}

	public void setIp(int ip) {
		this.ip = ip;
	}

	public long getLockVersion() {
		return lockVersion;
	}

	public void setLockVersion(long lockVersion) {
		this.lockVersion = lockVersion;
	}

	public String uniqueKey() {
		return String.format("%d_%d_%d", ip, pid, threadId);
	}

	public static String uniqueKey(int ip, long threadId, int pid) {
		return String.format("%d_%d_%d", ip, pid, threadId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LockOwnerInfo lockOwnerInfo = (LockOwnerInfo) o;
		return ip == lockOwnerInfo.ip &&
				threadId == lockOwnerInfo.threadId &&
				pid == lockOwnerInfo.pid;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ip, pid, threadId);
	}


	public boolean equals(long ip, long threadId, int pid) {
		return this.ip == ip &&
				this.threadId == threadId &&
				this.pid == pid;
	}

	public boolean isExpire() {
		return expireTime <= TimeUtil.getCurrentTimestamp();
	}

	public boolean isEmptyObject() {
		return ip == 0 && threadId == 0 && pid == 0 && expireTime == 0 && lockVersion == 0;
	}
}
