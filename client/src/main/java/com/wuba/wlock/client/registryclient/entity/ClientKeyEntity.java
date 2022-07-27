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
package com.wuba.wlock.client.registryclient.entity;


import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientKeyEntity {

	private String key;

	private int hashCode;

	private long version;

	private boolean autoRenew;

	/**
	 * 对于分配多分组的秘钥,用来存储所有分组的节点信息 <sequence,NodeINfo>
	 */
	private Map<Integer, Node> allNodeMap;

	/**
	 * 分组节点对应关系 : 迁移时候需要保证新分组位置在对应位置上
	 */
	private List<GroupNode> groupNodeList;
	
	public ClientKeyEntity() {
	}
	
	public ClientKeyEntity(String key) {
		this.key = key;
		this.hashCode = 0;
		this.version = 0;

	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public static String toJsonString(ClientKeyEntity clientKeyEntity) {
		return JSON.toJSONString(clientKeyEntity);
	}

	public static ClientKeyEntity parse(String string) {
		return JSON.parseObject(string, ClientKeyEntity.class);
	}
	
	public int getHashCode() {
		return hashCode;
	}

	public void setHashCode(int hashCode) {
		this.hashCode = hashCode;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
	
	public boolean getAutoRenew() {
		return autoRenew;
	}

	public void setAutoRenew(boolean autoRenew) {
		this.autoRenew = autoRenew;
	}

	public boolean isAutoRenew() {
		return autoRenew;
	}

	public List<GroupNode> getGroupNodeList() {
		return groupNodeList;
	}

	public void setGroupNodeList(List<GroupNode> groupNodeList) {
		this.groupNodeList = groupNodeList;
	}

	public Map<Integer, Node> getAllNodeMap() {
		return allNodeMap;
	}

	public void setAllNodeMap(Map<Integer, Node> allNodeMap) {
		this.allNodeMap = allNodeMap;
	}

	public Map<Integer, List<NodeAddr>> groupNodeAddrList() {
		Map<Integer, List<NodeAddr>> groupNodeAddrListMap = new HashMap<Integer, List<NodeAddr>>();
		for (GroupNode groupNode: this.groupNodeList) {
			int groupId = groupNode.getGroupId();
			List<NodeAddr> nodeAddrList = groupNode.getNodeAddrList(this.allNodeMap);
			groupNodeAddrListMap.put(groupId, nodeAddrList);
		}
		return groupNodeAddrListMap;
	}

	public void validate() {
		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("key is null");
		}

		if (allNodeMap == null || allNodeMap.isEmpty()) {
			throw new IllegalArgumentException("all node is null");
		}

		if (groupNodeList == null || groupNodeList.isEmpty()) {
			throw new IllegalArgumentException("group node list is null");
		}

		for (GroupNode groupNode: groupNodeList) {
			boolean hasMaster = false;
			List<Integer> nodeList = groupNode.getNodeList();
			for (Integer nodeId: nodeList) {
				if (!allNodeMap.containsKey(nodeId)) {
					throw new IllegalArgumentException(String.format("all node not contain node[%d]", nodeId));
				}

				if (nodeId == groupNode.getMasterNode()) {
					hasMaster = true;
				}
			}

			if (!hasMaster) {
				throw new IllegalArgumentException("no master node");
			}
		}
	}
}
