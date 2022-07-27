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

import com.wuba.wlock.server.exception.LockException;
import com.wuba.wlock.server.lock.protocol.ReentrantLockValue;
import com.wuba.wlock.server.lock.repository.base.ILockRepository;
import com.wuba.wlock.server.wpaxos.rocksdb.RocksDBHolder;
import com.wuba.wpaxos.exception.SerializeException;
import org.rocksdb.RocksDBException;

import java.util.Optional;

public class LockRepositoryImpl implements ILockRepository {
	private LockRepositoryImpl() {
	}

	public static ILockRepository lockRepository = new LockRepositoryImpl();

	public static ILockRepository getInstance() {
		return lockRepository;
	}


	@Override
	public Optional<ReentrantLockValue> getLock(String key,int groupId) throws LockException {
		try {
			byte[] bytes = RocksDBHolder.get(key.getBytes(),groupId);
			if (bytes != null) {
				ReentrantLockValue lockValue = ReentrantLockValue.formBytes(bytes);
				return Optional.of(lockValue);
			}
			return Optional.empty();
		} catch(RocksDBException e) {
			throw new LockException(e);
		}
	}

	@Override
	public void deleteLock(String key,int groupId) throws LockException {
		try {
			RocksDBHolder.delete(key.getBytes(),groupId);
		} catch(RocksDBException e) {
			throw new LockException(e);
		}
	}

	@Override
	public void lock(String key, ReentrantLockValue reentrantLockValue,int groupId) throws LockException {
		try {
			RocksDBHolder.put(key.getBytes(), reentrantLockValue.toBytes(),groupId);
		} catch(RocksDBException | SerializeException e) {
			throw new LockException(e);
		}
	}

	@Override
	public void update(String key, ReentrantLockValue reentrantLockValue, int groupId) throws LockException {
		try {
			RocksDBHolder.put(key.getBytes(), reentrantLockValue.toBytes(),groupId);
		} catch(RocksDBException | SerializeException e) {
			throw new LockException(e);
		}
	}

	@Override
	public void renew(String key, ReentrantLockValue reentrantLockValue,int groupId) throws LockException {
		try {
			RocksDBHolder.put(key.getBytes(), reentrantLockValue.toBytes(),groupId);
		} catch(RocksDBException e) {
			throw new LockException(e);
		} catch(SerializeException e) {
			throw new LockException(e);
		}
	}

	@Override
	public void release(String key, ReentrantLockValue reentrantLockValue,int groupId) throws LockException {
		try {
			RocksDBHolder.put(key.getBytes(), reentrantLockValue.toBytes(),groupId);
		} catch(RocksDBException e) {
			throw new LockException(e);
		} catch(SerializeException e) {
			throw new LockException(e);
		}
	}

}
