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
package com.wuba.wlock.client.registryclient.registrykey;

import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.config.RegistryClientConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.exception.ConnectTimeoutException;
import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import com.wuba.wlock.client.registryclient.communication.RegistryServer;
import com.wuba.wlock.client.registryclient.communication.RegistryServerPool;
import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;
import com.wuba.wlock.client.registryclient.protocal.ProtocolParser;
import com.wuba.wlock.client.registryclient.protocal.RegistryProtocol;
import com.wuba.wlock.client.registryclient.protocal.RequestProtocolFactory;
import com.wuba.wlock.client.registryclient.tasks.RegistryHeartBeatTask;
import com.wuba.wlock.client.registryclient.tasks.UpdateClustersTask;
import com.wuba.wlock.client.registryclient.tasks.VersionSendTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryKeyFactory {
	
	private static final Log logger = LogFactory.getLog(RegistryKeyFactory.class);
	
	private ConcurrentHashMap<String/*key hashCode*/, RegistryKey> keyMap = new ConcurrentHashMap<String, RegistryKey>();

	private RegistryServerPool serPool = null;
	
	private String registryServerIP = null;
	
	private Integer registryServerPort = null;
	
	private static RegistryKeyFactory instance = new RegistryKeyFactory();
	
	public static RegistryKeyFactory getInsatnce() {
		return instance;
	}
	
	public synchronized RegistryKey keyInit(String hashKey, String keyIp, int keyPort, WLockClient client) throws Exception {
		RegistryKey registryKey = new RegistryKey(hashKey, keyIp, keyPort);
		if (!this.keyMap.containsKey(hashKey)) {
			this.keyMap.put(hashKey, registryKey);
			this.registryServerIP = keyIp;
			this.registryServerPort = keyPort;
			if (null == serPool) {
				serPool = new RegistryServerPool(new RegistryClientConfig());
			}
			
			try {
				if (!serPool.isExistRegistryServer(hashKey)) {
					serPool.addRegistryServer(hashKey);
					logger.info(Version.INFO + ", registry client init: connect to wlock registry center success, the ip is :" + serPool.getServer(registryKey.getRegistryKey()).getServerConfig().getIp());
				}
			} catch (Exception e) {
				int id = serPool.getFormalIdIndex(hashKey);
				String failedIP = serPool.getRegistryConfig().getAllServerConfig().get(id).getIp();
				logger.info(Version.INFO + ", registry client init: connect to wlock registry center failed, the ip is :" + failedIP);
				serPool.replaceRegistryServer(hashKey);
			}
			
			try {
				updateKeyConfigs(registryKey.getRegistryKey());
			} catch (Exception e) {
				logger.warn(Version.INFO + ", get config from registry failed!", e);
			}
		}
		
		registryKey = this.keyMap.get(registryKey.getRegistryKey());
		
		if (null != client) {
			registryKey.addLockClient(client);
		} else {
			throw new RegistryClientRuntimeException(Version.INFO + ", WLockClient is null!");
		}
		//配置更新定时任务
		UpdateClustersTask.startTask();
		//版本上报定时任务
		VersionSendTask.startTask();
		//与注册中心的心跳定时任务
		RegistryHeartBeatTask.startTask();
		logger.info(Version.INFO + ", init key path " + hashKey + " success!");
		return registryKey;
	}
	
	public synchronized RegistryKey keyInit(String keyPath, WLockClient client) throws Exception {
		RegistryKey registryKey = new RegistryKey(keyPath);
		String hashKey = registryKey.getRegistryKey();
		if (!this.keyMap.containsKey(hashKey)) {
			this.keyMap.put(hashKey, registryKey);
			this.registryServerIP = registryKey.getRegistryServerIP();
			this.registryServerPort = registryKey.getRegistryServerPort();
			if (null == serPool) {
				serPool = new RegistryServerPool(new RegistryClientConfig());
			}
			
			try {
				if (!serPool.isExistRegistryServer(hashKey)) {
					serPool.addRegistryServer(hashKey);
					logger.info(Version.INFO + ", registry client init: connect to wlock registry center success, the ip is :" + serPool.getServer(registryKey.getRegistryKey()).getServerConfig().getIp());
				}
			} catch (Exception e) {
				int id = serPool.getFormalIdIndex(hashKey);
				String failedIP = serPool.getRegistryConfig().getAllServerConfig().get(id).getIp();
				logger.info(Version.INFO + ", registry client init: connect to wlock registry center failed, the ip is :" + failedIP);
				serPool.replaceRegistryServer(hashKey);
			}
			
			try {
				updateKeyConfigs(registryKey.getRegistryKey());
			} catch (Exception e) {
				logger.warn(Version.INFO + ", get config from registry failed!", e);
			}
		}
		
		registryKey = this.keyMap.get(registryKey.getRegistryKey());
		
		if (null != client) {
			registryKey.addLockClient(client);
		} else {
			throw new RegistryClientRuntimeException(Version.INFO + ", WLockClient is null!");
		}
		//配置更新定时任务
		UpdateClustersTask.startTask();
		//版本上报定时任务
		VersionSendTask.startTask();
		//与注册中心的心跳定时任务
		RegistryHeartBeatTask.startTask();
		logger.info(Version.INFO + ", init key path " + keyPath + " success!");
		return registryKey;
	}

	private void updateKeyConfigs(String key) throws CommunicationException, RegistryClientRuntimeException, IOException, ConnectTimeoutException, Exception {
		ClientKeyEntity keyObj = new ClientKeyEntity(key);
		RegistryProtocol protocol = RequestProtocolFactory.getInstacne().getCongigGetRequest(ClientKeyEntity.toJsonString(keyObj).getBytes());
		RegistryProtocol response = null;
		RegistryServer server = null;
		try {
			server = serPool.getServer(key);
			response = server.syncInvoke(protocol, RegistryClientConfig.WAIT_COUNT, RegistryClientConfig.GET_CONFIG_TIMEOUT);
		} catch (Exception e) {
			RegistryServer otherServer = null;
			try {
				otherServer = serPool.getOtherServer(key, server);
				response = otherServer.syncInvoke(protocol, RegistryClientConfig.WAIT_COUNT, RegistryClientConfig.GET_CONFIG_TIMEOUT);

				serPool.replaceRegistryServer(key);
			} catch (Exception ex) {
				throw new Exception(Version.INFO + ", get config from registry server failed!", ex);
			} finally {
				if (otherServer != null) {
					otherServer.destroy();
				}
			}
		}
		ProtocolParser.parse(response);
	}
	
	public RegistryServerPool getSerPool() {
		if (this.serPool == null) {
			try {
				serPool = new RegistryServerPool(new RegistryClientConfig());
			} catch (IOException e) {
				logger.info(Version.INFO + ", init registry server Pool failed:", e);
			} catch (RegistryClientRuntimeException e) {
				logger.info(Version.INFO + ", init registry server Pool failed:", e);
			}
		}
		return serPool;
	}
	
	public ConcurrentHashMap<String, RegistryKey> getKeyMap() {
		return this.keyMap;
	}

	public void setKeyMap(ConcurrentHashMap<String, RegistryKey> keyMap) {
		this.keyMap = keyMap;
	}

	public String getRegistryServerIP() {
		return registryServerIP;
	}

	public void setRegistryServerIP(String registryServerIP) {
		this.registryServerIP = registryServerIP;
	}

	public Integer getRegistryServerPort() {
		return registryServerPort;
	}

	public void setRegistryServerPort(Integer registryServerPort) {
		this.registryServerPort = registryServerPort;
	}
	
	public void updateConfigs(ClientKeyEntity clientKey) {
		for (Entry<String, RegistryKey> entry : keyMap.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(clientKey.getKey())) {
				entry.getValue().updateClusters(clientKey);
				return;
			}
		}
		logger.warn(Version.INFO + ", receive key does not match local key, so ignore it. receive key is " + clientKey.getKey());
	}
	
	public void initConfigs(ClientKeyEntity clientKey) throws Exception {
		for (Entry<String, RegistryKey> entry : keyMap.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(clientKey.getKey())) {
				entry.getValue().initClusters(clientKey, true);
				return;
			}
		}
		throw new RegistryClientRuntimeException(Version.INFO + ", receive key does not match local key, so ignore it. receive key is " + clientKey.getKey()); 
	}
	
}
