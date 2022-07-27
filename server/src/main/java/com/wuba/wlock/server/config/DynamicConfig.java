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

import com.wuba.wlock.server.collector.QpsAbandon;
import com.wuba.wlock.server.collector.log.GroupLog;
import com.wuba.wlock.server.collector.log.KeyGroupLog;
import com.wuba.wlock.server.collector.log.KeyLog;
import com.wuba.wlock.server.collector.log.ServerLog;
import com.wuba.wlock.server.communicate.retrans.RetransConfig;
import com.wuba.wlock.server.exception.ConfigException;
import com.wuba.wlock.server.trace.TraceWorker;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DynamicConfig extends IConfig implements IDynamicConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfig.class);
	private ScheduledExecutorService executorService;
	private static long lastModifyTime = 0L;

	private DynamicConfig() {
	}

	private static DynamicConfig dynamicConfig = new DynamicConfig();


	public static DynamicConfig getInstance() {
		return dynamicConfig;
	}

	@Override
	public void reload() {
		executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("dynamic_config_reload_worker"));
		executorService.scheduleAtFixedRate(() -> {
			File file = new File(path);
			if (!file.exists()) {
				return;
			}
			if (lastModifyTime != file.lastModified()) {
				lastModifyTime = file.lastModified();
				LOGGER.info("reload config {}", path);
				try (FileInputStream fileInputStream = new FileInputStream(path)) {
					properties = new Properties();
					properties.load(fileInputStream);
					reloadTrigger();
					loadExpireLimitStart();
				} catch (IOException e) {
					LOGGER.error("reload config path {} error.", path, e);
				}
			}
		}, 60000, 30000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void reloadTrigger() {
		boolean logDebugEnabled = super.getBoolean("log.debug.enable", false);
		if (logDebugEnabled) {
			LogConfig.getInstance().enableDebug();
		} else {
			LogConfig.getInstance().disableDebug();
		}
		TraceWorker.traceEnable = super.getBoolean("tracelog.enable", true);

		/*客户端请求直接转发最大次数*/
		int clientRedirectMaxTime = super.getInt("client.redirect.maxtime", 2);
		RetransConfig.CLIENT_REDIRECT_MAX_TIMES = clientRedirectMaxTime;
		boolean limitEnable = super.getBoolean("limit.enable", false);
		QpsAbandon.limitEnable = limitEnable;

		ServerLog.enable = super.getBoolean("server.log.enable", false);
		GroupLog.enable = super.getBoolean("group.log.enable", false);
		KeyLog.enable = super.getBoolean("key.log.enable", false);
		KeyGroupLog.enable = super.getBoolean("key.group.log.enable", false);
		KeyGroupLog.enable = super.getBoolean("collector.log.main.enable", false);

	}

	@Override
	public void init(String path, boolean mustExist) throws ConfigException {
		super.initConfig(path, mustExist);
		File file = new File(path);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				LOGGER.error("config file path {} isDynamic but not exist,create new file failed.", path, e);
			}
		}
		reload();
	}

	@Override
	public void loadSpecial() {

	}

	public void loadExpireLimitStart() {
		boolean expireLimitStart = super.getBoolean("expire.limit.start", false);
		ServerConfig.getInstance().setExpireLimitStart(expireLimitStart);
	}

}
