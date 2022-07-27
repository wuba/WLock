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

import com.wuba.wpaxos.exception.SerializeException;
import com.wuba.wpaxos.utils.ByteConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReentrantLockValue {
	public static final int HEAD_LEN = 1 + 8 + 4 + 4 + 4;

	/**
	 * 写锁为null时，序列号时需要
	 */
	private static final LockOwnerInfo EMPTY_LOCK_OWNER_INFO = new LockOwnerInfo();

	/**
	 * 版本号
	 */
	private byte version;
	/**
	 * 最新锁版本号
	 */
	private long lockVersion;
	/**
	 * 锁状态
	 */
	private int status;
	/**
	 * 锁类型
	 */
	private int lockType;
	/**
	 * 总长度
	 */
	private int totalLen;

	/**
	 * 锁持有者
	 * 读写锁情况下为写锁的持有者
	 */
	private LockOwnerInfo lockOwnerInfo;

	/**
	 * 读锁数量
	 */
	private int readOwnerCount;
	/**
	 * 读锁持有者 key: ip+pid+threadId
	 */
	private Map<String, LockOwnerInfo> rLockOwnerInfos = new HashMap<String, LockOwnerInfo>();


	public byte[] toBytes() throws SerializeException {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			stream.write(version);
			stream.write(ByteConverter.longToBytesLittleEndian(lockVersion));
			stream.write(ByteConverter.intToBytesLittleEndian(status));
			stream.write(ByteConverter.intToBytesLittleEndian(lockType));
			if (LockTypeEnum.reentrantLock.getValue() == lockType) {
				totalLen = HEAD_LEN + LockOwnerInfo.LENGTH;
				stream.write(ByteConverter.intToBytesLittleEndian(totalLen));
				stream.write(lockOwnerInfo.toBytes());
			} else if (LockTypeEnum.readWriteReentrantLock.getValue() == lockType) {
				totalLen = HEAD_LEN + LockOwnerInfo.LENGTH + 4 + readOwnerCount * LockOwnerInfo.LENGTH;
				stream.write(ByteConverter.intToBytesLittleEndian(totalLen));
				if (lockOwnerInfo == null) {
					stream.write(EMPTY_LOCK_OWNER_INFO.toBytes());
				} else {
					stream.write(lockOwnerInfo.toBytes());
				}
				stream.write(ByteConverter.intToBytesLittleEndian(readOwnerCount));
				if (readOwnerCount > 0) {
					for (LockOwnerInfo rOwner : rLockOwnerInfos.values()) {
						stream.write(rOwner.toBytes());
					}
				}
			}
			return stream.toByteArray();
		} catch(IOException e) {
			throw new SerializeException(e);
		}
	}

	public static ReentrantLockValue formBytes(byte[] buf) {
		ReentrantLockValue reentrantLockValue = new ReentrantLockValue();
		if (buf.length < (HEAD_LEN + LockOwnerInfo.LENGTH)) {
			return reentrantLockValue;
		}
		int index = 0;
		byte version = buf[index];
		index += 1;
		long lockVersion = ByteConverter.bytesToLongLittleEndian(buf, index);
		index += 8;
		int status = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		int lockType = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		int totalLen = ByteConverter.bytesToIntLittleEndian(buf, index);
		index += 4;
		LockOwnerInfo lockOwnerInfo = LockOwnerInfo.fromBytes(buf, index);
		index += LockOwnerInfo.LENGTH;
		if (LockTypeEnum.readWriteReentrantLock.getValue() == lockType) {
			int readOwnerCount = ByteConverter.bytesToIntLittleEndian(buf, index);
			index += 4;
			reentrantLockValue.setReadOwnerCount(readOwnerCount);
			if (readOwnerCount > 0) {
				Map<String, LockOwnerInfo> rOwners = LockOwnerInfo.fromBytesList(buf, index);
				reentrantLockValue.setrLockOwnerInfos(rOwners);
			}
		}
		reentrantLockValue.setVersion(version);
		reentrantLockValue.setLockVersion(lockVersion);
		reentrantLockValue.setStatus(status);
		reentrantLockValue.setLockType(lockType);
		reentrantLockValue.setTotalLen(totalLen);
		reentrantLockValue.setLockOwnerInfo(lockOwnerInfo.isEmptyObject() ? null: lockOwnerInfo);
		return reentrantLockValue;
	}

	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	public long getLockVersion() {
		return lockVersion;
	}

	public void setLockVersion(long lockVersion) {
		this.lockVersion = lockVersion;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getLockType() {
		return lockType;
	}

	public void setLockType(int lockType) {
		this.lockType = lockType;
	}

	public int getTotalLen() {
		return totalLen;
	}

	public void setTotalLen(int totalLen) {
		this.totalLen = totalLen;
	}

	public LockOwnerInfo getLockOwnerInfo() {
		return lockOwnerInfo;
	}

	public void setLockOwnerInfo(LockOwnerInfo lockOwnerInfo) {
		this.lockOwnerInfo = lockOwnerInfo;
	}

	public int getReadOwnerCount() {
		return readOwnerCount;
	}

	public void setReadOwnerCount(int readOwnerCount) {
		this.readOwnerCount = readOwnerCount;
	}


	public void setrLockOwnerInfos(Map<String, LockOwnerInfo> rLockOwnerInfos) {
		this.rLockOwnerInfos = rLockOwnerInfos;
	}

	public boolean existReadLock(int ip, long threadId, int pid) {
		return rLockOwnerInfos.containsKey(LockOwnerInfo.uniqueKey(ip, threadId, pid));
	}

	public boolean existReadLock() {
		return !rLockOwnerInfos.isEmpty();
	}

	public boolean existWriteLock() {
		return lockOwnerInfo != null;
	}

	public void addReadLock(LockOwnerInfo lockOwnerInfo) {
		if (rLockOwnerInfos.put(lockOwnerInfo.uniqueKey(), lockOwnerInfo) == null) {
			readOwnerCount++;
		}
	}

	public void removeReadLock(LockOwnerInfo lockOwnerInfo) {
		if (rLockOwnerInfos.remove(lockOwnerInfo.uniqueKey()) != null) {
			readOwnerCount--;
		}
	}

	public LockOwnerInfo getReadLockOwner(int ip, long threadId, int pid) {
		return rLockOwnerInfos.get(LockOwnerInfo.uniqueKey(ip, threadId, pid));
	}

	public boolean existLock() {
		return existWriteLock() || existReadLock();
	}

	public boolean isFree() {
		if (!existWriteLock() && !existReadLock()) {
			return true;
		}

		if (lockOwnerInfo != null && !lockOwnerInfo.isExpire()) {
			return false;
		}

		if (rLockOwnerInfos != null && !rLockOwnerInfos.isEmpty()) {
			for (LockOwnerInfo lockOwnerInfo: rLockOwnerInfos.values()) {
				if (lockOwnerInfo != null && !lockOwnerInfo.isExpire()) {
					return false;
				}
			}
		}

		return true;
	}

	public List<LockOwnerInfo> expireLockOwners() {
		List<LockOwnerInfo> lockOwnerInfos = new ArrayList<LockOwnerInfo>();
		if (lockOwnerInfo != null && lockOwnerInfo.isExpire()) {
			lockOwnerInfos.add(lockOwnerInfo);
		}

		if (rLockOwnerInfos != null && !rLockOwnerInfos.isEmpty()) {
			for (LockOwnerInfo lockOwnerInfo: rLockOwnerInfos.values()) {
				if (lockOwnerInfo != null && lockOwnerInfo.isExpire()) {
					lockOwnerInfos.add(lockOwnerInfo);
				}
			}
		}
		return lockOwnerInfos;
	}

	public Map<String, LockOwnerInfo> getReadLockOwnerInfos() {
		return rLockOwnerInfos;
	}
}
