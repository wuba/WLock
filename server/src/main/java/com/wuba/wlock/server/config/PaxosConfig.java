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
import com.wuba.wpaxos.comm.NodeInfo;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public final class PaxosConfig extends IConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaxosConfig.class);

	private static final String USER_DIR = RootPath.getRootPath();

	private static final boolean OPTIONS_USE_MEMBERSHIP = true;
	private static final boolean OPTIONS_USE_BATCH_PROPOSE = true;
	private static final boolean OPTIONS_USE_MASTER = true;
	private static final int OPTIONS_UDP_MAX_SIZE = 4096;
	private static final int OPTIONS_PREPARE_TIMEOUT = 1000;
	private static final int OPTIONS_ACCEPT_TIMEOUT = 1000;
	private static final int OPTIONS_COMMIT_TIMEOUT = 2000;
	private static final int OPTIONS_ASK_FOR_LEARN_TIMEOUT = 3000;
	private int optionsIoThreadCount;
	private static final int OPTIONSLEARNER_SEND_SPEED = 100 * 1024 * 1024;
	private static final String STORE_PATH_ROOT_DIR = USER_DIR + File.separator + "db";
	private static final String INDEX_LOG = "indexlog";


	private static volatile long version = 0;


	public static final int REGISTRY_TIMEOUT = 3000;

	/**
	 * dynamic config
	 */
	private NodeInfo myNode;
	private Map<Integer, NodeInfo> nodeMap;

	/**
	 * 分组扩缩容时候使用,用于验证节点,需要进行初始化,初始化时候每个 groupMap 中的节点应该一样
	 */
	private Map<Integer, Set<Integer>> groupNodeMap;

	private int groupCount;

	/**
	 * groupId -> isUseMaster :  全量 group
	 */
	private Set<Integer> noUseMasterGroups;
	/**
	 * 是否开启 master 负载均衡
	 */
	private Set<Integer> noLoadBalanceGroups = new HashSet<Integer>();

	private PaxosConfig() {
	}

	private static PaxosConfig paxosConfig = new PaxosConfig();

	public static PaxosConfig getInstance() {
		return paxosConfig;
	}

	@Override
	public void init(String path, boolean mustExist) throws ConfigException {
		super.initConfig(path, mustExist);
	}

	@Override
	public void loadSpecial() {
		String myNodeStr = properties.getProperty("myNode", "");
		if (!Strings.isNullOrEmpty(myNodeStr)) {
			String[] ipPort = myNodeStr.split(":");
			if (ipPort.length == 2) {
				try {
					NodeInfo nodeInfo = new NodeInfo(ipPort[0], Integer.parseInt(ipPort[1]));
					this.myNode = nodeInfo;
				} catch(Exception e) {
					LOGGER.error("load local myNode error,continue......");
				}
			}
		}
		String nodeListStr = properties.getProperty("nodeList", "");
		if (!Strings.isNullOrEmpty(nodeListStr)) {
			String[] ipPorts = nodeListStr.split(",");
			for (String ipPortStr : ipPorts) {
				String[] ipPort = ipPortStr.split(":");
				if (ipPort.length == 3) {
					this.nodeMap = new HashMap<>();
					try {
						NodeInfo nodeInfo = new NodeInfo(ipPort[1], Integer.parseInt(ipPort[2]));
						nodeMap.put(Integer.valueOf(ipPort[0]),nodeInfo);
					} catch(Exception e) {
						LOGGER.error("load local nodeList error,continue......");
					}
				}
			}
		}
//		groupCount = super.getInt("optionsGroupCount", defaulgroupCount);
//		optionsIoThreadCount = super.getInt("optionsIoThreadCount", defaulgroupCount);
	}

	public int getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}

	public boolean isOptionsUseMembership() {
		return super.getBoolean("optionsUseMembership", OPTIONS_USE_MEMBERSHIP);
	}

	public boolean isOptionsUseBatchPropose() {
		return super.getBoolean("optionsUseBatchPropose", OPTIONS_USE_BATCH_PROPOSE);
	}

	public int getOptionsUDPMaxSize() {
		return super.getInt("optionsUDPMaxSize", OPTIONS_UDP_MAX_SIZE);
	}

	public int getOptionsPrepareTimeout() {
		return super.getInt("optionsPrepareTimeout", OPTIONS_PREPARE_TIMEOUT);
	}

	public int getOptionsAcceptTimeout() {
		return super.getInt("optionsAcceptTimeout", OPTIONS_ACCEPT_TIMEOUT);
	}

	public int getOptionsAskForLearnTimeout() {
		return super.getInt("optionsAskForLearnTimeout", OPTIONS_ASK_FOR_LEARN_TIMEOUT);
	}

	public int getOptionsIoThreadCount() {
		return optionsIoThreadCount;
	}

	public void setOptionsIoThreadCount(int optionsIoThreadCount) {
		this.optionsIoThreadCount = optionsIoThreadCount;
	}

	public boolean isOptionsUseMaster() {
		return super.getBoolean("optionsUseMaster", OPTIONS_USE_MASTER);
	}

	public String getStorePathIndexLog() {
		return STORE_PATH_ROOT_DIR + File.separator + super.getString("indexlog", INDEX_LOG);
	}

	public int getOptionslearnerSendSpeed() {
		return super.getInt("optionslearnerSendSpeed", OPTIONSLEARNER_SEND_SPEED);
	}

	public int getOptionsCommitTimeout() {
		return super.getInt("optionsCommitTimeout", OPTIONS_COMMIT_TIMEOUT);
	}

	public NodeInfo getMyNode() {
		return myNode;
	}

	public void setMyNode(NodeInfo myNode) {
		this.myNode = myNode;
	}

	public ArrayList<NodeInfo> getNodeList() {
		return new ArrayList<>(nodeMap.values());
	}

	public Map<Integer, NodeInfo> getNodeMap() {
		return nodeMap;
	}

	public void setNodeMap(Map<Integer, NodeInfo> nodeMap) {
		this.nodeMap = nodeMap;
	}

	public static long getVersion() {
		return version;
	}

	public static void setVersion(long version) {
		PaxosConfig.version = version;
	}

	public Map<Integer, Set<Integer>> getGroupNodeMap() {
		return groupNodeMap;
	}

	public void setGroupNodeMap(Map<Integer, Set<Integer>> groupNodeMap) {
		this.groupNodeMap = groupNodeMap;
	}


	public boolean isEnableBalance(int group) {
		if (noLoadBalanceGroups.contains(group)) {
			return false;
		}

		return true;
	}

	public void setNoLoadBalanceGroups(Set<Integer> noLoadBalanceGroups) {
		this.noLoadBalanceGroups = noLoadBalanceGroups;
	}

	public void enableBalance(int group) {
		this.noLoadBalanceGroups.remove(group);
	}


	public void setNoUseMasterGroups(Set<Integer> noUseMasterGroups) {
		this.noUseMasterGroups = noUseMasterGroups;
	}

	public Set<Integer> getNoUseMasterGroups() {
		return noUseMasterGroups;
	}
}
