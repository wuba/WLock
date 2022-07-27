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
package com.wuba.wlock.common.registry.protocol.response;

import com.wuba.wlock.common.registry.protocol.ServerNode;

import java.util.Map;
import java.util.Set;

public class GetPaxosConfRes {
	public static final String SEP = ":";
	private String paxosServer;
	/**
	 * key:序号
	 * value:ip:tcp port:pasxos port:keepmaster port
	 */
	private Map<Integer, String> serverMap;


	private Map<Integer, ServerNode> allServerMap;
	private int groupCount;
	private int udpPort;
	private String clusterName;

	private long version;

	/**
	 * groupId -> isUseMaster :  不开启 master 选举的 group 集合
	 */
	private Set<Integer> noUseMasterGroups;
	/**
	 * 是否开启 master 负载均衡 不开启 master 负载均衡的 group
	 */
	private Set<Integer> noLoadBalanceGroups;
	/**
	 * 分组内节点的容量改变
	 * key : groupId
	 * value : 该分组新的成员列表(nodeId) (在下发配置和 serverMap 冲突的时候 , 以 groupCapacityChange 中的集合为准)
	 */
	private Map<Integer, Set<Integer>> groupNodeMap;

	public int getUdpPort() {
		return udpPort;
	}

	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}

	public int getGroupCount() {
		return groupCount;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getPaxosServer() {
		return paxosServer;
	}

	public void setPaxosServer(String paxosServer) {
		this.paxosServer = paxosServer;
	}

	public Map<Integer, ServerNode> getAllServerMap() {
		return allServerMap;
	}

	public void setAllServerMap(Map<Integer, ServerNode> allServerMap) {
		this.allServerMap = allServerMap;
	}

	public Map<Integer, Set<Integer>> getGroupNodeMap() {
		return groupNodeMap;
	}

	public void setGroupNodeMap(Map<Integer, Set<Integer>> groupNodeMap) {
		this.groupNodeMap = groupNodeMap;
	}

	public Set<Integer> getNoUseMasterGroups() {
		return noUseMasterGroups;
	}

	public void setNoUseMasterGroups(Set<Integer> noUseMasterGroups) {
		this.noUseMasterGroups = noUseMasterGroups;
	}

	public Set<Integer> getNoLoadBalanceGroups() {
		return noLoadBalanceGroups;
	}

	public void setNoLoadBalanceGroups(Set<Integer> noLoadBalanceGroups) {
		this.noLoadBalanceGroups = noLoadBalanceGroups;
	}

	public Map<Integer, String> getServerMap() {
		return serverMap;
	}

	public void setServerMap(Map<Integer, String> serverMap) {
		this.serverMap = serverMap;
	}

	@Override
	public String toString() {
		return "GetPaxosConfRes{" +
				"paxosServer='" + paxosServer + '\'' +
				", serverMap=" + allServerMap +
				", groupCount=" + groupCount +
				", udpPort=" + udpPort +
				", clusterName='" + clusterName + '\'' +
				", version=" + version +
				", noUseMasterGroups=" + noUseMasterGroups +
				", noLoadBalanceGroups=" + noLoadBalanceGroups +
				", groupNodeMap=" + groupNodeMap +
				'}';
	}
}
