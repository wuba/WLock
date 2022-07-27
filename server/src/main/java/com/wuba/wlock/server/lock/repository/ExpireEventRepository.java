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
package com.wuba.wlock.server.lock.repository;

import com.wuba.wlock.server.expire.event.ExpireEvent;
import com.wuba.wlock.server.expire.event.LockExpireEvent;
import com.wuba.wlock.server.lock.protocol.LockOwnerInfo;
import com.wuba.wlock.server.lock.protocol.LockTypeEnum;
import com.wuba.wlock.server.lock.protocol.OpcodeEnum;
import com.wuba.wlock.server.lock.protocol.ReentrantLockValue;
import com.wuba.wlock.server.wpaxos.rocksdb.RocksDBHolder;
import com.google.common.collect.Lists;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class ExpireEventRepository {

	private static final Logger logger = LoggerFactory.getLogger(ExpireEventRepository.class);

	private ExpireEventRepository() {
	}

	private static ExpireEventRepository expireEventRepository = new ExpireEventRepository();

	public static ExpireEventRepository getInstance() {
		return expireEventRepository;
	}

	/**
	 * 获取锁相关的过期事件
	 *
	 * @param groupId
	 */
	public List<ExpireEvent> getAllLockEvent(int groupId) {
		List<ExpireEvent> expireEventList = Lists.newArrayList();
		try {
			RocksIterator rocksIterator = RocksDBHolder.newIterator(groupId);
			for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
				String key = new String(rocksIterator.key());
				byte[] valuseBytes = rocksIterator.value();
				ReentrantLockValue reentrantLockValue = ReentrantLockValue.formBytes(valuseBytes);
				if (reentrantLockValue.getLockType() == LockTypeEnum.readWriteReentrantLock.getValue()) {
					if (reentrantLockValue.existWriteLock()) {
						expireEventList.add(createLockExpireEvent(key, groupId, reentrantLockValue.getLockOwnerInfo(),
								LockTypeEnum.readWriteReentrantLock.getValue(), OpcodeEnum.ReadWriteOpcode.WRITE.getValue()));
					}

					if (reentrantLockValue.existReadLock()) {
						Map<String, LockOwnerInfo> readLockOwnerInfoMap = reentrantLockValue.getReadLockOwnerInfos();
						for (Map.Entry<String, LockOwnerInfo> entry: readLockOwnerInfoMap.entrySet()) {
							expireEventList.add(createLockExpireEvent(key, groupId, entry.getValue(),
									LockTypeEnum.readWriteReentrantLock.getValue(), OpcodeEnum.ReadWriteOpcode.READ.getValue()));
						}
					}

				} else if (reentrantLockValue.getLockType() == LockTypeEnum.reentrantLock.getValue()){
					if (reentrantLockValue.getLockOwnerInfo() != null) {
						expireEventList.add(createLockExpireEvent(key, groupId, reentrantLockValue.getLockOwnerInfo(), LockTypeEnum.reentrantLock.getValue(), 0));
					} else {
						logger.info("lockKey {} owner is null", key);
					}
				}
			}
		} catch (Exception e) {
			logger.info("getAllLockEvent error", e);
		}
		return expireEventList;
	}

	private LockExpireEvent createLockExpireEvent (String key, int groupId, LockOwnerInfo lockOwnerInfo, int lockType, int opcode) {
		return new LockExpireEvent(lockOwnerInfo.getExpireTime(), key, groupId, lockOwnerInfo.getLockVersion(), (byte) lockType, (byte) opcode, lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid());
	}
}
