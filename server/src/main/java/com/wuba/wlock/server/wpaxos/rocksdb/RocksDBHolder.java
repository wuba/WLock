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
package com.wuba.wlock.server.wpaxos.rocksdb;

import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public final class RocksDBHolder {
	private static RocksDB[] rocksDBS;


	private static int logicGroupCount;

	public static void init(int groupCount) throws RocksDBException {

		logicGroupCount = groupCount / 2;

		rocksDBS = new RocksDB[logicGroupCount];
		for (int i = 0; i < logicGroupCount; i++) {
			rocksDBS[i] = new RocksDB(i);
			rocksDBS[i].init();
		}
	}

	public static void stop(int groupCount) {
		for (int i = 0; i < groupCount; i++) {
			rocksDBS[i].close();
		}
	}

	public static void put(byte[] key, byte[] value, int groupId) throws RocksDBException {
		rocksDBS[getRealGroup(groupId)].put(key, value);
	}

	public static byte[] get(byte[] key, int groupId) throws RocksDBException {
		return rocksDBS[getRealGroup(groupId)].get(key);
	}

	public static void delete(byte[] key, int groupId) throws RocksDBException {
		rocksDBS[getRealGroup(groupId)].delete(key);
	}

	public static RocksIterator newIterator(int groupId) {
		return rocksDBS[getRealGroup(groupId)].getRocksDB().newIterator();
	}

	private static int getRealGroup(int logicGroup) {
		return logicGroup >= logicGroupCount ? logicGroup - logicGroupCount : logicGroup;
	}
}
