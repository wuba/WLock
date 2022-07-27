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

import java.util.Objects;

public class ClientConfInfo {

	private String clusterName;
	@Deprecated
	private int groupId;
	/**
	 * 多分组
	 */
	private String groupIds;
	
	private boolean autoRenew;

	/**
	 * 该秘钥是否是多分组 : true : 多分组 , false : 单分组
	 */
	private boolean multiGroup;

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
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

	public boolean isMultiGroup() {
		return multiGroup;
	}

	public void setMultiGroup(boolean multiGroup) {
		this.multiGroup = multiGroup;
	}

	public String getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(String groupIds) {
		this.groupIds = groupIds;
	}

	@Override
	public String toString() {
		return "ClientConfInfo{" +
				"clusterName='" + clusterName + '\'' +
				", groupId=" + groupId +
				", groupIDs='" + groupIds + '\'' +
				", autoRenew=" + autoRenew +
				", multiGroup=" + multiGroup +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClientConfInfo that = (ClientConfInfo) o;
		return groupId == that.groupId && autoRenew == that.autoRenew && multiGroup == that.multiGroup && Objects.equals(clusterName, that.clusterName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clusterName, groupId, autoRenew, multiGroup);
	}
}
