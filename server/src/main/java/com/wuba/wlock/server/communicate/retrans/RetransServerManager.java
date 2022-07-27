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
package com.wuba.wlock.server.communicate.retrans;

import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.comm.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class RetransServerManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetransServerManager.class);
	
	private ConcurrentHashMap<Long, RetransServer> ipClientMap = new ConcurrentHashMap<Long, RetransServer>();
	private volatile ConcurrentHashMap<Integer, Long> groupMasterMap = new ConcurrentHashMap<Integer, Long>();
	private long myNodeID;
	private boolean inited = false;
	private Object initLock = new Object();
	
	private static RetransServerManager instance = new RetransServerManager();
	
	private RetransServerManager() {
	}
	
	public static RetransServerManager getInstance() {
		return instance;
	}
	
	public void init() {
		if (inited) {
			return;
		}
		synchronized (initLock) {	
			if (!inited) {
				myNodeID = WpaxosService.getInstance().getMyNodeID();
				try {
					initIpClientMap();
					initGroupMasterMap();
				} catch (Exception e) {
					LOGGER.error("RetransClientManager init failed", e);
				}
				inited = true;
			}
		}
	}
	
	/**
	 * 初始化ip和client对应关系
	 * @param serverConfig
	 * @throws Exception 
	 */
	private void initIpClientMap() throws Exception {
		ServerConfig serverConfig = ServerConfig.getInstance();
		Map<Long, String> servMap = serverConfig.getTcpIpPorts();
		
		//retansServerSet = servSet;
		if (servMap == null) {
			throw new Exception("server set is null!!!");
		}
		Iterator<Entry<Long, String>> iter = servMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Long, String> entry = iter.next();
			if (entry.getKey() == this.myNodeID) {
				continue;
			}
			
			String[] servAddr = entry.getValue().split(":");
			RetransServerConfig retransServeConfig = new RetransServerConfig(servAddr[0], Integer.valueOf(servAddr[1]));
			RetransServer retransServer = new RetransServer(retransServeConfig);
			ipClientMap.put(entry.getKey(), retransServer);
		}
	}
	
	/**
	 * server节点变化，回调处理
	 * @param serverNodes
	 */
	public void serverChanged(Map<Long, String> serverNodes) {
		if (!inited) {
			return;
		}
		
		if (serverNodes == null) {
			return;
		}
		
		Iterator<Entry<Long, String>> iter1 = serverNodes.entrySet().iterator();
		while (iter1.hasNext()) {
			Entry<Long, String> entry = iter1.next();
			long nodeID = entry.getKey();
			if (nodeID != myNodeID && !ipClientMap.containsKey(nodeID)) {
				String[] servAddr = entry.getValue().split(":");
				RetransServerConfig retransServeConfig = new RetransServerConfig(servAddr[0], Integer.valueOf(servAddr[1]));
				RetransServer retransServer = new RetransServer(retransServeConfig);
				ipClientMap.put(nodeID, retransServer);
				LOGGER.info("add retrans server : {}.", entry.getValue());
			}
		}
		
		Iterator<Entry<Long, RetransServer>> iter = ipClientMap.entrySet().iterator();
		while (iter.hasNext()) {
			try {
				Entry<Long, RetransServer> entry = iter.next();
				if (!serverNodes.containsKey(entry.getKey())) {
					entry.getValue().delete();
					iter.remove();
					LOGGER.info("remove retrans server : {}.", entry.getValue());
				}
			} catch(Exception e) {
				LOGGER.error("serverChanged error.", e);
			}
		}
	}
	
	/**
	 * 初始化group master信息
	 */
	private void initGroupMasterMap() {
		int groupCount = PaxosConfig.getInstance().getGroupCount();
		for (int i = 0; i < groupCount; i++) {
			NodeInfo nodeInfo = WpaxosService.getInstance().getMaster(i);
			this.groupMasterMap.put(i, nodeInfo.getNodeID());
		}
	}
	
	/**
	 * master变化，回调处理
	 * @param groupId
	 */
	public void masterChanged(int groupId) {
		if (!inited) {
			return;
		}
		try {
			NodeInfo nodeInfo = WpaxosService.getInstance().getMaster(groupId);
			this.groupMasterMap.put(groupId, nodeInfo.getNodeID());
		} catch(Exception e) {
			LOGGER.error("masterChanged error.", e);
		}
	}
	
	/**
	 * 获取指定group的master client
	 * @param group
	 * @return
	 */
	public RetransServer getRetransServerByGroup(int groupId) {
		if (this.groupMasterMap.containsKey(groupId)) {
			long nodeID = this.groupMasterMap.get(groupId);
			return this.ipClientMap.get(nodeID);
		}
		
		return null;
	}

	public boolean isMasterNormal(int groupId) {
		RetransServer retransServer = getRetransServerByGroup(groupId);
		if (retransServer != null && RetransServerState.Normal == retransServer.getState()) {
			return true;
		}

		return false;
	}
}
