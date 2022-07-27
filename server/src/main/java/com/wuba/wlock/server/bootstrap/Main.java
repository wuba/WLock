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
package com.wuba.wlock.server.bootstrap;

import com.alibaba.fastjson.parser.ParserConfig;
import com.wuba.wlock.server.communicate.TcpServer;
import com.wuba.wlock.server.communicate.registry.RegistryClient;
import com.wuba.wlock.server.communicate.retrans.RetransServerManager;
import com.wuba.wlock.server.communicate.signal.KeepMasterUdpServer;
import com.wuba.wlock.server.constant.ConfigPath;
import com.wuba.wlock.server.expire.ExpireStrategyFactory;
import com.wuba.wlock.server.worker.*;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wlock.server.exception.ConfigException;
import com.wuba.wlock.server.config.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static Logger LOGGER;

	public static void main(String[] args) {
		closeLog4JHook();
		Configurator.initialize(null, ConfigPath.LOG4J_CONFIG_PATH);
		LOGGER = LoggerFactory.getLogger(Main.class);
		LOGGER.info("everything start from here");

		try {
			ParserConfig.getGlobalInstance().setSafeMode(true);//开启fastJson安全模式，完全禁用autoType
			initConfig();
		} catch (ConfigException e) {
			LOGGER.error("init config error.", e);
			System.exit(-1);
		}

		if(ServerConfig.getInstance().isEnableRegistry()){
			RegistryClient.getInstance().start();
			if (!RegistryClient.getInstance().initDynamicConfig()) {
				LOGGER.error("init config from registry error.system exit");
				System.exit(0);
			}
		}

		TcpServer.getInstance().start();

		/**
		 * start wpaxos
		 */
		try {
			WpaxosService.getInstance().start();
		} catch (Exception e) {
			LOGGER.error("start wpaxos error.", e);
			System.exit(0);
		}
		LOGGER.info("paxos start success");

		/**
		 * init retrans service
		 */
		LOGGER.info("RetransServerManager start init.");
		try {
			RetransServerManager.getInstance().init();
		} catch (Exception e) {
			LOGGER.error("RetransServerManager init failed.", e);
			System.exit(0);
		}
		LOGGER.info("RetransServerManager init success.");
		
		/**
		 * start lock service
		 */
		try {
			KeepMasterWorker.getInstance().start();
			HeartbeatWorker.getInstance().start();
			CollectorWorker.getInstance().start();
			ExpireStrategyFactory.getInstance().start(ServerConfig.getInstance().getExpirePattern());
		} catch (Exception e) {
			LOGGER.error("woker start error.", e);
			System.exit(-1);
		}
		KeepMasterUdpServer.getInstance().init(ServerConfig.getInstance().getMyUdpPort());
		registerExitEvent();
	}

	private static void closeLog4JHook() {
		System.setProperty("log4j.shutdownHookEnabled", String.valueOf(false));
	}

	private static void initConfig() throws ConfigException {
		ServerConfig.getInstance().init(ConfigPath.SERVER_CONFIG, true );
		RocksDbConfig.getInstance().init(ConfigPath.ROCKSDB_CONFIG, false);
		PaxosConfig.getInstance().init(ConfigPath.PAXOS_CONFIG, false );
		RegistryConfig.getInstance().init(ConfigPath.REGISTRY_CONFIG, true );
		CheckpointConfig.getInstance().init(ConfigPath.CHECKPOINT_CONFIG, false );
		DynamicConfig.getInstance().init(ConfigPath.DYNAMIC_CONFIG,false);
	}

	private static void registerExitEvent() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				TcpServer.getInstance().stop();
				stopWorker();
				WpaxosService.getInstance().stopPaxos();
				LOGGER.info("......close success......");
				LogManager.shutdown();
			}
		});
	}

	private static void stopWorker() {
		HeartbeatWorker.getInstance().shutdown();
		AckWorker.getInstance().shutdown();
		ExpireStrategyFactory.getInstance().shutdown();
		LockWorker.getInstance().shutdown();
		CollectorWorker.getInstance().shutdown();
	}
}
