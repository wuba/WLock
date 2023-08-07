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
package com.wuba.wlock.client.registryclient.communication;

import com.wuba.wlock.client.config.RegistryClientConfig;
import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import com.wuba.wlock.client.registryclient.entity.DaemonCheckTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryServerPool {

	private static final Log logger = LogFactory.getLog(RegistryServerPool.class);
	
	private RegistryClientConfig registryConfig;
	
	private ConcurrentHashMap<String/*HashKey*/, RegistryServer> registryServerMap = new ConcurrentHashMap<String, RegistryServer>();

	public RegistryServerPool(RegistryClientConfig registryConfig) throws IOException, RegistryClientRuntimeException {
		this.registryConfig = registryConfig;
	}

	public synchronized RegistryServer getServer(String hashKey) {
		return registryServerMap.get(hashKey);
	}

	public synchronized boolean isExistRegistryServer(String hashKey) {
		RegistryServer registryServer = registryServerMap.get(hashKey);
		return null != registryServer ? true : false;
	}
	
	public synchronized void addRegistryServer(String hashKey) throws IOException, RegistryClientRuntimeException {
		if (registryServerMap.containsKey(hashKey)) {
			return;
		}
		int idx = getFormalIdIndex(hashKey);
		RegistryServer registryServer = new RegistryServer(this.registryConfig.getAllServerConfig().get(idx), this, idx, hashKey);
		registryServerMap.put(hashKey, registryServer);
	}
	
	/**
	 * 当前节点是否在注册中心配置链表中
	 * @param ip
	 * @param port
	 * @return
	 * @throws RegistryClientRuntimeException 
	 */
	public synchronized boolean isExistRegistryServerConfig(String ip, int port) throws RegistryClientRuntimeException {
		List<ServerConfig> configList = null;
		configList = this.registryConfig.getAllServerConfig();
		if (null == configList || configList.size() < 1) {
			throw new RegistryClientRuntimeException(Version.INFO + " , there is no registry server to connect.");
		}
		for (int i = 0; i < configList.size(); i++) {
			ServerConfig tempConfig = configList.get(i);
			if (tempConfig.getIp().equals(ip) && tempConfig.getPort() == port) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 获取客户端应该链接的注册中心的位置
	 * @param hashKey
	 * @return
	 * @throws RegistryClientRuntimeException 
	 */
	public synchronized int getFormalIdIndex(String hashKey) throws RegistryClientRuntimeException {
		List<ServerConfig> configList = this.registryConfig.getAllServerConfig();
		if (null == configList || configList.size() < 1) {
			throw new RegistryClientRuntimeException(Version.INFO + " , there is no registry server to connect.");
		}
		return Math.abs(hashKey.hashCode() % configList.size());
	}
	
	/**
	 * 获取应该链接的注册中心的配置
	 * @param hashKey
	 * @return
	 * @throws RegistryClientRuntimeException 
	 */
	public synchronized ServerConfig getFormalServerConfig(String hashKey) throws RegistryClientRuntimeException {
		int formalIdIndex = getFormalIdIndex(hashKey);
		List<ServerConfig> configList = this.registryConfig.getAllServerConfig();
		if (null == configList || configList.size() < 1) {
			throw new RegistryClientRuntimeException(Version.INFO + " , there is no registry server to connect.");
		}
		return configList.get(formalIdIndex);
	}
	
	/**
	 * 获取客户端当前链接的注册中心的位置
	 * @param hashKey
	 * @param serverConfig
	 * @return
	 * @throws RegistryClientRuntimeException 
	 */
	public synchronized int checkCurrentIdIndexAndGet(String hashKey, ServerConfig serverConfig) throws RegistryClientRuntimeException {
		int currentIdIndex = -1;
		List<ServerConfig> configList = null;
		configList = this.registryConfig.getAllServerConfig();
		if (null == configList || configList.size() < 1) {
			throw new RegistryClientRuntimeException(Version.INFO + " , there is no registry server to connect.");
		}
		for (int i = 0; i < configList.size(); i++) {
			ServerConfig tempConfig = configList.get(i);
			if (tempConfig.getIp().equals(serverConfig.getIp()) && tempConfig.getPort() == serverConfig.getPort()) {
				currentIdIndex = i;
			}
		}
		return currentIdIndex;
	}
	
	public synchronized void replaceRegistryServer(String hashKey) throws RegistryClientRuntimeException, IOException {
		List<ServerConfig> confs = this.registryConfig.getAllServerConfig();
		if (null == confs || confs.size() < 1) {
			throw new RegistryClientRuntimeException(Version.INFO + " , there is no registry server to connect.");
		}
		int oldIdIndex = -1;
		RegistryServer oldRegistryServer = registryServerMap.get(hashKey);
		if (null != oldRegistryServer) {
			// 验证一次,避免重复切换
			if (oldRegistryServer.isConnectCorrectRegistry()) {
				if (oldRegistryServer.getChannelPool().count() > 0) {
					return;
				}
			}
			oldIdIndex = this.checkCurrentIdIndexAndGet(hashKey, oldRegistryServer.getServerConfig());
			if (-1 == oldIdIndex) { // 链接的注册中心不在注册中的列表内
				oldIdIndex = 0;
			}
		} else {
			oldIdIndex = this.getFormalIdIndex(hashKey);
		}
		// 避免一个注册中心异常,导致下一个注册中心的流量翻倍
		int newIdIndex = this.getRandomIndex(oldIdIndex, confs.size());
		this.cleanOldServer(hashKey);
		RegistryServer newRegistryServer = null;
		boolean isSuccess = false;
		try {
			newRegistryServer = new RegistryServer(confs.get(newIdIndex), this, newIdIndex, hashKey);
			logger.info("redirect connect registry server success, the ip is:" + confs.get(newIdIndex).getIp());
			isSuccess = true;
		} catch (IOException e) {
			logger.warn(Version.INFO + ", connect to registry server " + confs.get(newIdIndex).getIp() + " failed.");
			int size = confs.size();
			int baseIndex = (newIdIndex + 1) % size;
			int index = 0;
			while (index < size) {
				int i = (index + baseIndex) % size;
				if (i != oldIdIndex && i != newIdIndex) {
					try {
						newRegistryServer = new RegistryServer(confs.get(i), this, i, hashKey);
						isSuccess = true;
						logger.info("redirect connect registry server success, the ip is:" + confs.get(i).getIp());
						break;
					} catch (IOException ex) {
						logger.warn(Version.INFO + ", connect to registry server " + confs.get(i).getIp() + " failed.");
					}
				}

				index++;
			}
		}
		if (isSuccess) {
			registryServerMap.put(hashKey, newRegistryServer);
		} else {
			throw new IOException(Version.INFO + ", can not connect to all registry servers, add RegistryServer failed.");
		}
	}

	private int getRandomIndex(int oldIdIndex, int registryConfigSize) {
		Random rand = new Random();
		int randomNum = rand.nextInt(registryConfigSize);
		return randomNum != oldIdIndex ? randomNum : ((randomNum + 1) % registryConfigSize);
	}
	
	private synchronized void cleanOldServer(String hashKey) {
		if (registryServerMap.containsKey(hashKey)) {
			if (registryServerMap.get(hashKey).getChannelPool().count() > 0) {
				registryServerMap.get(hashKey).destroy();
			}
		}
	}

	public synchronized void oldRegistryServerResumeCallBack(String hashKey) throws RegistryClientRuntimeException, IOException {
		List<ServerConfig> confs = this.registryConfig.getAllServerConfig();
		RegistryServer newRegistryServer = null;
		int newIdIndex = this.getFormalIdIndex(hashKey);
		newRegistryServer = new RegistryServer(confs.get(newIdIndex), this, newIdIndex, hashKey);
		// 无异常的话,则进行原registryServer重连
		this.cleanOldServer(hashKey);
		registryServerMap.put(hashKey, newRegistryServer);
		logger.info(Version.INFO + "client hashKey:" + hashKey + " anew connect formal Registry Server IP:" + confs.get(newIdIndex).getIp());
	}
	
	public RegistryClientConfig getRegistryConfig() {
		return registryConfig;
	}
	
	public void addDaemonCheckTask(String hashKey, ServerConfig serverConfig) {
		if (null != hashKey && null != serverConfig) {
			DaemonCheckTask task = new DaemonCheckTask(hashKey, serverConfig.getIp(), serverConfig.getPort());
			RegistryDaemonChecker.check(task);
		}
	}

	public RegistryServer getOtherServer(String hashKey, RegistryServer curServer) throws RegistryClientRuntimeException, IOException {
		List<ServerConfig> confs = this.registryConfig.getAllServerConfig();
		Random rand = new Random();
		int randomNum = rand.nextInt(confs.size());
		if (curServer != null && confs.get(randomNum).equals(curServer.getServerConfig())) {
			randomNum = (randomNum + 1) % confs.size();
		}

		String curServerIp = null;
		if (curServer != null) {
			curServerIp = curServer.getServerConfig().getIp();
		}

		logger.info(Version.INFO + " current registry server " + curServerIp + " error. get other server " + confs.get(randomNum).getIp());
		return new RegistryServer(confs.get(randomNum), this, randomNum, hashKey);
	}
}
