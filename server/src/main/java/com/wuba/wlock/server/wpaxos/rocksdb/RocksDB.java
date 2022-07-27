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

import com.wuba.wlock.server.config.RocksDbConfig;
import com.wuba.wlock.server.util.ThreadPoolUtil;
import com.wuba.wlock.server.util.ThreadRenameFactory;
import com.wuba.wlock.server.util.TimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.*;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RocksDB {
	private static final Logger logger = LogManager.getLogger(RocksDB.class);
	private org.rocksdb.RocksDB rocksDB;

	private final int groupId;

	private WriteOptions writeOptions;

	public RocksDB(int groupId) {
		this.groupId = groupId;

	}

	private ScheduledExecutorService scheduledExecutorService = ThreadPoolUtil.newSingleThreadScheduledExecutor(new ThreadRenameFactory("RocksDB"));

	/**
	 * @throws RocksDBException
	 */
	public void init() throws RocksDBException {
		org.rocksdb.RocksDB.loadLibrary();

		Options options = new Options();
		writeOptions = new WriteOptions();
		writeOptions.setSync(false);
		writeOptions.setDisableWAL(true);
		Statistics statistics = new Statistics();
		options.setCreateIfMissing(RocksDbConfig.getInstance().getCreateIfMissing()).setMaxOpenFiles(RocksDbConfig.getInstance().getMaxOpenFile())
				.setWriteBufferSize(RocksDbConfig.getInstance().getWriteBufferSize())
				.setMaxWriteBufferNumber(RocksDbConfig.getInstance().getMaxWriteBufferNumber())
				.setMinWriteBufferNumberToMerge(RocksDbConfig.getInstance().getWriteBufferNumberToMerge()).
				setLevelZeroFileNumCompactionTrigger(RocksDbConfig.getInstance().getLevelZeroFileNumCompactionTrigger())
				.setLevel0SlowdownWritesTrigger(RocksDbConfig.getInstance().getLevel0SlowdownWritesTrigger())
				.setLevel0StopWritesTrigger(RocksDbConfig.getInstance().getLevel0StopWritesTrigger())
				.setMaxBytesForLevelBase(RocksDbConfig.getInstance().getTargetFileSizeBase())
				.setMaxBackgroundCompactions(RocksDbConfig.getInstance().getMaxBackgroundCompactions())
				.setMaxBackgroundFlushes(RocksDbConfig.getInstance().getMaxBackgroundFlushes())
				.setMemtablePrefixBloomSizeRatio(RocksDbConfig.getInstance().getMemtablePrefixBloomSizeRatio())
				.setCompressionType(RocksDbConfig.getInstance().getCompressionType())
				.setCompactionStyle(RocksDbConfig.getInstance().getCompactionStyle())
				.optimizeLevelStyleCompaction()
				.setUseFsync(RocksDbConfig.getInstance().getUseFsync())
				.setBloomLocality(RocksDbConfig.getInstance().getBollmFilter())
				.setStatsDumpPeriodSec(180)
				.setTargetFileSizeBase(RocksDbConfig.getInstance().getTargetFileSizeBase())
				.setStatistics(statistics);

		String path = RocksDbConfig.getInstance().getDbPath() + File.separator + groupId;
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
		this.rocksDB = org.rocksdb.RocksDB.open(options, path);
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				double average = statistics.getHistogramData(HistogramType.DB_WRITE).getAverage();
				double compaction = statistics.getHistogramData(HistogramType.COMPACTION_TIME).getAverage();
				double get = statistics.getHistogramData(HistogramType.DB_GET).getAverage();
				logger.info("testrocksdb write :{} get :{} compaction :{} ", average, get, compaction);
			}
		}, 1, 10, TimeUnit.MINUTES);
	}


	public byte[] get(byte[] key) throws RocksDBException {
		long start = TimeUtil.getCurrentTimestamp();
		byte[] retBuf = this.rocksDB.get(key);
		long cost = TimeUtil.getCurrentTimestamp() - start;
		if (cost > 200) {
			logger.warn("rocksdb get cost {}", cost);
		}
		return retBuf;
	}

	public void put(byte[] key, byte[] value) throws RocksDBException {
		long start = TimeUtil.getCurrentTimestamp();
		this.rocksDB.put(writeOptions, key, value);
		long cost = TimeUtil.getCurrentTimestamp() - start;
		if (cost > 200) {
			logger.warn("rocksdb put cost {}", cost);
		}
	}

	public void delete(byte[] key) throws RocksDBException {
		long start = TimeUtil.getCurrentTimestamp();
		this.rocksDB.delete(key);
		long cost = TimeUtil.getCurrentTimestamp() - start;
		if (cost > 200) {
			logger.warn("rocksdb delete cost {}", cost);
		}
	}

	public void close() {
		this.rocksDB.close();
		this.rocksDB = null;
		logger.info("rocksdb close.groupid {}", groupId);
	}

	public org.rocksdb.RocksDB getRocksDB() {
		return rocksDB;
	}

	public void setRocksDB(org.rocksdb.RocksDB rocksDB) {
		this.rocksDB = rocksDB;
	}
}
