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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CheckpointConfig extends IConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckpointConfig.class);
	private static CheckpointConfig checkpointConfig = new CheckpointConfig();
	private static final String USER_DIR = RootPath.getRootPath();
	private static final String STORE_PATH_ROOT_DIR = USER_DIR + File.separator + "db";
	private static final String DEFAULT_PATH = "/checkpoint";
	/**
	 * 保存文件、删除文件间隔，必须被60整除
	 */
	private static final int DEFAULT_INTERVAL_MIN = 10;

	@Override
	public void init(String path, boolean mustExist) throws ConfigException {
		super.initConfig(path, mustExist);
	}

	@Override
	public void loadSpecial() {

	}

	private CheckpointConfig() {
	}

	public static CheckpointConfig getInstance() {
		return checkpointConfig;
	}


	public String getCheckpointDir() {
		return STORE_PATH_ROOT_DIR + File.separator + super.getString("checkpoint.dir", DEFAULT_PATH);
	}

	public int getIntervalMin() {
		try {
			int interval = super.getInt("checkpoint.intervalmin");
			if ((60 % interval) == 0) {
				return interval;
			}
		} catch(ConfigException e) {
		}
		return DEFAULT_INTERVAL_MIN;
	}

}
