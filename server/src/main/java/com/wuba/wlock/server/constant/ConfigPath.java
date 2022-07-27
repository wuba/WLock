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
package com.wuba.wlock.server.constant;

import com.wuba.wlock.server.config.RootPath;

public class ConfigPath {


	private static final String CONFIG_PATH = getConfigPath();

	public static final String DYNAMIC_CONFIG = CONFIG_PATH + "/dynamic.properties";
	public static final String CHECKPOINT_CONFIG = CONFIG_PATH + "/checkpoint.properties";
	public static final String SERVER_CONFIG = CONFIG_PATH + "/server.properties";
	public static final String PAXOS_CONFIG = CONFIG_PATH + "/paxos.properties";
	public static final String ROCKSDB_CONFIG = CONFIG_PATH + "/rocksdb.properties";
	public static final String LOG4J_CONFIG_PATH = CONFIG_PATH + "/log4j.xml";
	public static final String REGISTRY_CONFIG = CONFIG_PATH + "/registry.properties";
	public static final String STORE_CONFIG = CONFIG_PATH + "/store.properties";

	private static final String getConfigPath() {
		if (System.getProperty("os.name").contains("Mac")) {
			return System.getProperty("user.dir") + "/server/src/main/resources/config_offline";
		}
		return RootPath.getRootPath() + "/config";
	}
}
