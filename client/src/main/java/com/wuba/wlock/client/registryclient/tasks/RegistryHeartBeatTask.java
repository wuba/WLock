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
package com.wuba.wlock.client.registryclient.tasks;

import com.wuba.wlock.client.config.RegistryClientConfig;
import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import com.wuba.wlock.client.helper.ThreadPool;
import com.wuba.wlock.client.registryclient.communication.RegistryServer;
import com.wuba.wlock.client.registryclient.communication.RegistryServerPool;
import com.wuba.wlock.client.registryclient.protocal.ProtocolParser;
import com.wuba.wlock.client.registryclient.protocal.RegistryProtocol;
import com.wuba.wlock.client.registryclient.protocal.RequestProtocolFactory;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKey;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistryHeartBeatTask extends Thread {
	
	private static Log logger = LogFactory.getLog(RegistryHeartBeatTask.class);
	
	private static AtomicBoolean started = new AtomicBoolean(false);
	
	private static Map<String/*IP + port*/, AtomicInteger/*heartBeat failedTimes*/> heartBeatMap = new HashMap<String, AtomicInteger>();
	
	private static final int HEARTBEAT_TIMEOUT_TIMES = 2;
	
	private static final String SEPARATOR = ":";
	
	public RegistryHeartBeatTask() {
	}
	
	public static void startTask() {
		if (started.compareAndSet(false, true)) {
			ThreadPool.registryScheduler.scheduleAtFixedRate(new RegistryHeartBeatTask(), 0, RegistryClientConfig.HEARTBEAT_PERIOD, TimeUnit.SECONDS);
			logger.debug(Version.INFO + ", start registry client heartbeat task success!");
		}
	}
	
	@Override
	public void run() {
		RegistryServerPool serverPool = null;
		try {
			serverPool = RegistryKeyFactory.getInsatnce().getSerPool();
			RegistryProtocol protocol = RequestProtocolFactory.getInstacne().getHeartBeatRequest();
			ConcurrentHashMap<String, RegistryKey> keyMap = RegistryKeyFactory.getInsatnce().getKeyMap();
			for (String key : keyMap.keySet()) {
				try {
					RegistryServer registryServer = serverPool.getServer(key);
					if (!registryServer.isConnectCorrectRegistry()) {
						ServerConfig serverConfig = serverPool.getFormalServerConfig(key);
						serverPool.addDaemonCheckTask(key, serverConfig);
					}
					RegistryProtocol response = registryServer.syncInvoke(protocol, RegistryClientConfig.WAIT_COUNT, RegistryClientConfig.HEARTBEAT_TIMEOUT);
					ProtocolParser.parse(response);
					String registryServerAddr = registryServer.getServerConfig().getIp() + SEPARATOR + registryServer.getServerConfig().getPort();
					ClientHeartSuccessCallBack(registryServerAddr);
				} catch (Throwable e) {
					registryServerIsUnAvailable(key, serverPool);
				}
			}
		} catch (Exception e) {
			logger.info(Version.INFO + ", run registry hearbeat task error!", e);
		} 
	}
	
	private void registryServerIsUnAvailable(String key, RegistryServerPool serverPool) throws RegistryClientRuntimeException, IOException {
		RegistryServer registryServer = serverPool.getServer(key);
		if (null == registryServer) {
			return;
		}
		String registryServerIP = registryServer.getServerConfig().getIp();
		int registryServerPort = registryServer.getServerConfig().getPort();
		if (isNullAddress(registryServerIP, registryServerPort)) {
			logger.debug(Version.INFO + ", current registry server Address is null");
			// 清理出错的链接
			registryServer.destroy();
			serverPool.replaceRegistryServer(key);
			return;
		}
		String registryServerAddr = registryServerIP + SEPARATOR + registryServerPort;
		synchronized (RegistryHeartBeatTask.class) {
			if (heartBeatMap.containsKey(registryServerAddr)) {
				if (heartBeatMap.get(registryServerAddr).incrementAndGet() >= HEARTBEAT_TIMEOUT_TIMES) {
					// 清理出错的链接
					registryServer.destroy();
					serverPool.replaceRegistryServer(key);
				}
			} else {
				heartBeatMap.put(registryServerAddr, new AtomicInteger(1));
			}
		}
	}
	
	private void ClientHeartSuccessCallBack(String registryServerAddr) {
		synchronized (RegistryHeartBeatTask.class) {
			if (heartBeatMap.containsKey(registryServerAddr)) {
				heartBeatMap.remove(registryServerAddr);
			}
		}
	}
	
	private boolean isNullAddress(String ip, int port) {
		if (ip == null || ip.length() == 0) {
			return true;
		}
		return port < 0 ? true : false;
	}
	
}
