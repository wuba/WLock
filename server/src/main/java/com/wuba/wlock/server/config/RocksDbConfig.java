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
package com.wuba.wlock.server.config;

import com.wuba.wlock.server.exception.ConfigException;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class RocksDbConfig extends IConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(RocksDbConfig.class);
	private static final String USER_DIR = RootPath.getRootPath();
	private static final boolean CREATE_IF_MISSING = true;
	private static final int MAX_OPEN_FILE = -1;
	private static final long WRITE_BUFFER_SIZE = 67108864;
	private static final int MAX_WRITE_BUFFER_NUMBER = 16;
	private static final int WRITE_BUFFER_NUMBER_TO_MERGE = 1;
	private static final int LEVEL_ZERO_FILE_NUM_COMPACTION_TRIGGER = 10;
	private static final int LEVEL_0_SLOWDOWN_WRITES_TRIGGER = 20;
	private static final int LEVEL_0_STOP_WRITES_TRIGGER = 40;
	private static final int MAX_BACKGROUND_COMPACTIONS = 10;
	private static final int MAX_BACKGROUND_FLUSHES = 1;
	private static final double MEMTABLE_PREFIX_BLOOM_SIZE_RATIO = 0.125;
	private static final int BOOLM_FILTER = 10;
	private static final CompressionType COMPRESSION_TYPE = CompressionType.NO_COMPRESSION;
	private static final CompactionStyle COMPACTION_STYLE = CompactionStyle.LEVEL;
	private static final boolean USE_FSYNC = false;
	private static final long TARGET_FILE_SIZE_BASE = 12582912;
	private static final long MAX_FILE_LEVEL_BASE = 10485760;
	private static final long MAX_LOG_FILE_SIZE = 5368709120L;
	private static final int MAX_BACKGROUND_JOB = 10;
	private static final String STORE_PATH_ROOT_DIR = USER_DIR + File.separator + "db";
	private static final String DB_PATH = "rocksdb";

	private static RocksDbConfig rocksDbConfig = new RocksDbConfig();

	public static RocksDbConfig getInstance() {
		return rocksDbConfig;
	}

	@Override
	public void init(String path, boolean mustExist) throws ConfigException {
		super.initConfig(path, mustExist);
	}

	@Override
	public void loadSpecial() {

	}

	public int getWriteBufferNumberToMerge() {
		return super.getInt("writeBufferNumberToMerge", WRITE_BUFFER_NUMBER_TO_MERGE);
	}

	public int getLevelZeroFileNumCompactionTrigger() {
		return super.getInt("levelZeroFileNumCompactionTrigger", LEVEL_ZERO_FILE_NUM_COMPACTION_TRIGGER);
	}

	public int getLevel0SlowdownWritesTrigger() {
		return super.getInt("level0SlowdownWritesTrigger", LEVEL_0_SLOWDOWN_WRITES_TRIGGER);
	}

	public int getLevel0StopWritesTrigger() {
		return super.getInt("level0StopWritesTrigger", LEVEL_0_STOP_WRITES_TRIGGER);
	}

	public double getMemtablePrefixBloomSizeRatio() {
		return super.getDouble("memtablePrefixBloomSizeRatio", MEMTABLE_PREFIX_BLOOM_SIZE_RATIO);
	}

	public int getMaxOpenFile() {
		return super.getInt("maxOpenFile", MAX_OPEN_FILE);
	}

	public int getBollmFilter() {
		return super.getInt("boolmFilter", BOOLM_FILTER);
	}

	public boolean getCreateIfMissing() {
		return super.getBoolean("createIfMissing", CREATE_IF_MISSING);
	}

	public int getMaxWriteBufferNumber() {
		return super.getInt("maxWriteBufferNumber", MAX_WRITE_BUFFER_NUMBER);
	}

	public int getMaxBackgroundCompactions() {
		return super.getInt("maxBackgroundCompactions", MAX_BACKGROUND_COMPACTIONS);
	}

	public int getMaxBackgroundFlushes() {
		return super.getInt("maxBackgroundFlushes", MAX_BACKGROUND_FLUSHES);
	}

	public CompressionType getCompressionType() {
		return COMPRESSION_TYPE;
	}

	public CompactionStyle getCompactionStyle() {
		return COMPACTION_STYLE;
	}

	public boolean getUseFsync() {
		return super.getBoolean("useFsync", USE_FSYNC);
	}

	public long getTargetFileSizeBase() {
		return super.getLong("targetFileSizeBase", TARGET_FILE_SIZE_BASE);
	}

	public long getMaxFileLevelBase() {
		return super.getLong("maxFileLevelBase", MAX_FILE_LEVEL_BASE);
	}

	public long getMaxLogFileSize() {
		return super.getLong("maxLogFileSize", MAX_LOG_FILE_SIZE);
	}

	public int getMaxBackgroundJob() {
		return super.getInt("maxBackgroundJob", MAX_BACKGROUND_JOB);
	}

	public long getWriteBufferSize() {
		return super.getLong("writeBufferSize", WRITE_BUFFER_SIZE);
	}

	public String getDbPath() {
		return STORE_PATH_ROOT_DIR + File.separator + super.getString("dbPath", DB_PATH);
	}

}
