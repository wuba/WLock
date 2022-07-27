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
package com.wuba.wlock.registry.server.entity;

import com.wuba.wlock.repository.domain.ServerDO;

import java.util.HashMap;
import java.util.List;

public class ClusterMasterGroupDistribute {

	private long version;
	
	private String clusterName;
	
	private int hashCode;
	
	private int groupCount;

	/**
	 * group master  分布
	 */
	private HashMap<Integer/*groupId*/, String/*IP + port*/> groupServerMap;

	/**
	 * group master 的版本
	 */
	private HashMap<Integer/*groupId*/, Long/*group version*/> groupVersionMap;
	
	private List<ServerDO> serverList;
	
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public HashMap<Integer, String> getGroupServerMap() {
		return groupServerMap;
	}

	public void setGroupServerMap(HashMap<Integer, String> groupServerMap) {
		this.groupServerMap = groupServerMap;
	}

	public HashMap<Integer, Long> getGroupVersionMap() {
		return groupVersionMap;
	}

	public void setGroupVersionMap(HashMap<Integer, Long> groupVersionMap) {
		this.groupVersionMap = groupVersionMap;
	}

	public List<ServerDO> getServerList() {
		return serverList;
	}

	public void setServerList(List<ServerDO> serverList) {
		this.serverList = serverList;
	}

	public int getHashCode() {
		return hashCode;
	}

	public void setHashCode(int hashCode) {
		this.hashCode = hashCode;
	}
	
	public int getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append("clusterName:").append(this.clusterName).append(" version:").append(this.version).append(" groupServerMap:").append(this.groupServerMap.toString())
		.append(" groupVersionMap:").append(groupVersionMap.toString()).append("]");
		return sb.toString();
	}
}
