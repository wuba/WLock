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
package com.wuba.wlock.common.entity;

import java.util.List;
import java.util.Map;

public class ClientKeyEntity {

	private String key;

	private int hashCode;

	private long version;

	/**
	 *  老版本节点列表
	 */
	private List<NodeAddr> nodeList;

	@Deprecated
	private int groupId;

	private boolean autoRenew;

	/**
	 * 对于分配多分组的秘钥,用来存储所有分组的节点信息 <sequence,NodeINfo>
	 */
	private Map<Integer, Node> allNodeMap;

	/**
	 * 分组节点对应关系 : 迁移时候需要保证新分组位置在对应位置上
	 */
	private List<GroupNode> groupNodeList;

	/**
	 * 是否使用多分组 : 0 不使用 , 1 使用
	 */
	private boolean multiGroup;

	public ClientKeyEntity() {
	}

	public ClientKeyEntity(String key) {
		this.key = key;
		this.hashCode = 0;
		this.version = 0;
		this.nodeList = null;

	}

	public ClientKeyEntity(String key, int hashCode, long version, List<NodeAddr> nodeList, int groupId, boolean autoRenew) {
		this.key = key;
		this.hashCode = hashCode;
		this.version = version;
		this.nodeList = nodeList;
		this.groupId = groupId;
		this.autoRenew = autoRenew;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<NodeAddr> getNodeList() {
		return nodeList;
	}

	public void setNodeList(List<NodeAddr> nodeList) {
		this.nodeList = nodeList;
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

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public boolean getAutoRenew() {
		return this.autoRenew;
	}

	public void setAutoRenew(boolean autoRenew) {
		this.autoRenew = autoRenew;
	}

	public boolean isAutoRenew() {
		return autoRenew;
	}

	public Map<Integer, Node> getAllNodeMap() {
		return allNodeMap;
	}

	public void setAllNodeMap(Map<Integer, Node> allNodeMap) {
		this.allNodeMap = allNodeMap;
	}

	public List<GroupNode> getGroupNodeList() {
		return groupNodeList;
	}

	public void setGroupNodeList(List<GroupNode> groupNodeList) {
		this.groupNodeList = groupNodeList;
	}

	public boolean isMultiGroup() {
		return multiGroup;
	}

	public void setMultiGroup(boolean multiGroup) {
		this.multiGroup = multiGroup;
	}

	@Override
	public String toString() {
		return "ClientKeyEntity{" +
				"key='" + key + '\'' +
				", hashCode=" + hashCode +
				", version=" + version +
				", nodeList=" + nodeList +
				", groupId=" + groupId +
				", autoRenew=" + autoRenew +
				", allNodeMap=" + allNodeMap +
				", groupNodeList=" + groupNodeList +
				", multiGroup=" + multiGroup +
				'}';
	}
}
