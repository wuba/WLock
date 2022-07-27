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
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.helper.FileManager;
import com.wuba.wlock.client.helper.PathUtil;
import com.wuba.wlock.client.helper.PropertiesHelper;
import com.wuba.wlock.client.helper.XmlParser;
import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;
import com.wuba.wlock.client.registryclient.entity.NodeAddr;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class RegistryKey {

	private static final Log logger = LogFactory.getLog(RegistryKey.class);

	private String registryServerIP = null;

	private Integer registryServerPort = null;

	private String registryKey = null; //hashKey

	private volatile List<Integer> groupIds;

	private boolean autoRenew = false;

	private volatile ClientKeyEntity clusterConf = null; // 所属集群配置

	private List<WLockClient> wlockClients = new ArrayList<WLockClient>();

	public RegistryKey(String registryKey, String registryIp, int registryPort) throws Exception {
		this.registryKey = registryKey;
		this.registryServerIP = registryIp;
		this.registryServerPort = registryPort;
		if (this.registryKey == null || this.registryKey.length() == 0) {
			throw new Exception("key is error!");
		}
		if (this.registryServerIP == null || this.registryServerIP.length() == 0) {
			throw new Exception("registry IP is error!");
		}
		if (this.registryServerPort == null || this.registryServerPort <= 0) {
			throw new Exception("registry port is error!");
		}
	}

	public RegistryKey(String keyPath) throws Exception {
		PropertiesHelper properties = new PropertiesHelper(keyPath);
		this.registryKey = properties.getString("key");
		this.registryServerIP = properties.getString("registry_server_ip");
		this.registryServerPort = properties.getInt("registry_server_port");
		if (this.registryKey == null || this.registryKey.length() == 0) {
			throw new Exception("key is error!");
		}
		if (this.registryServerIP == null || this.registryServerIP.length() == 0) {
			throw new Exception("registry IP is error!");
		}
		if (this.registryServerPort == null || this.registryServerPort <= 0) {
			throw new Exception("registry port is error!");
		}
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

	public String getRegistryKey() {
		return registryKey;
	}

	public void setRegistryKey(String registryKey) {
		this.registryKey = registryKey;
	}

	public List<WLockClient> getLockClients() {
		return wlockClients;
	}

	public void setLockClients(List<WLockClient> lockClients) {
		this.wlockClients = lockClients;
	}

	public synchronized void addLockClient(WLockClient client) {
		this.wlockClients.add(client);
	}

	public synchronized void initClusters(ClientKeyEntity clientKey, boolean save2files) throws Exception {
		if (clientKeyEntityIsEmpty(clientKey)) {
			throw new Exception("config initing has some error, the body is: " + ClientKeyEntity.toJsonString(clientKey));
		}

		if (null == this.clusterConf || clientKey.getVersion() > this.clusterConf.getVersion()) {
			this.setClusterConf(clientKey);

			if (save2files) {
				try {
					FileManager.getInstance().saveConfigs2Files(clientKey);
				} catch (TransformerException e) {
					logger.warn(Version.INFO + ", save configs to files failed, because: ", e);
				}
			}
		}
	}

	public void updateClusters(ClientKeyEntity clientKey) {
		if (clientKeyEntityIsEmpty(clientKey)) {
			logger.warn(Version.INFO + ", config get from registry server has some error, the body is:" + ClientKeyEntity.toJsonString(clientKey));
			return;
		}

		try {
			updateClusterConfigs(clientKey);
		} catch (Exception e) {
			logger.error(Version.INFO + "updateClusters error", e);
		}

		try {
			FileManager.getInstance().saveConfigs2Files(clientKey);
		} catch (TransformerException e) {
			logger.warn(Version.INFO + ", update cluster to files failed, because: ", e);
		}
	}

	public void updateClusterConfigs(ClientKeyEntity clientKey) throws IOException, ExecutionException, InterruptedException {
		clientKey.validate();
		logger.info(Version.INFO + ", updateClustersconfig...  clientKey: " + ClientKeyEntity.toJsonString(clientKey));
		if (clientKey.getVersion() <= this.clusterConf.getVersion()) {
			logger.warn(Version.INFO + ", new config' version is less than current version, ignore update clusterConfig!");
		} else {

			for (WLockClient wlockClient : this.wlockClients) {
				wlockClient.getServerPoolHandler().updateClusters(clientKey.groupNodeAddrList());
			}

			this.setClusterConf(clientKey);
		}
	}

	public synchronized void loadClusters() throws Exception {
		ClientKeyEntity clientKey = null;
		String parentPath = PathUtil.getFilePath(this.registryKey);
		String[] files = FileManager.getInstance().getClustersFile(parentPath);
		if (files == null || files.length == 0) {
			throw new Exception("local does not have some clusters files!");
		}
		for (String file : files) {
			if ("clusters.xml".equalsIgnoreCase(file)) {
				clientKey = XmlParser.parseClusterXmlFromFile(parentPath + "/" + file);
			}
		}
		if (clientKeyEntityIsEmpty(clientKey)) {
			throw new Exception("local files may be broken!");
		}
		initClusters(clientKey, false);
	}

	public ClientKeyEntity getClusterConf() {
		return this.clusterConf;
	}

	public void setClusterConf(ClientKeyEntity clusterConf) {
		this.clusterConf = clusterConf;
		this.autoRenew = clusterConf.getAutoRenew();
	}

	public void notifyGroupChange(List<Integer> groupIdList) {
		final int size = groupIdList.size();
		Collections.sort(groupIdList, new Comparator<Integer>() {
			@Override
			public int compare(Integer groupId1, Integer groupId2) {
				return (groupId1 % size) - (groupId2 % size);
			}
		});

		this.groupIds = groupIdList;
	}

	public int getGroupId(String lockkey) {
		if (groupIds.size() == 1) {
			return groupIds.get(0);
		}

		return groupIds.get(Math.abs(lockkey.hashCode() % groupIds.size()));
	}

	public boolean getAutoRenew() {
		return autoRenew;
	}

	public void setAutoRenew(boolean autoRenew) {
		this.autoRenew = autoRenew;
	}

	public static boolean clientKeyEntityIsEmpty(ClientKeyEntity clientKey) {
		if (clientKey == null) {
			return true;
		}
		Map<Integer, List<NodeAddr>> groupNodeAddrList = clientKey.groupNodeAddrList();
		if (groupNodeAddrList == null || groupNodeAddrList.isEmpty()) {
			return true;
		}

		for (Map.Entry<Integer, List<NodeAddr>> entry: groupNodeAddrList.entrySet()) {
			Integer key = entry.getKey();
			List<NodeAddr> value = entry.getValue();

			if (key == null || value == null || value.isEmpty()) {
				return true;
			}
		}


		return false;
	}
}