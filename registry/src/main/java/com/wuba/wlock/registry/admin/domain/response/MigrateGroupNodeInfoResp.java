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
package com.wuba.wlock.registry.admin.domain.response;

import java.util.List;

public class MigrateGroupNodeInfoResp {
	private String groupId;
	private List<MigrateNodeInfo> allNode;


	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public List<MigrateNodeInfo> getAllNode() {
		return allNode;
	}

	public void setAllNode(List<MigrateNodeInfo> allNode) {
		this.allNode = allNode;
	}

	public static class MigrateNodeInfo{
		private long id;
		private String node;
		private int state;
		/**
		 * 是不是新节点 : 1 老节点 , 0 新节点
		 */
		private int isNewNode;
		/**
		 * 是不是master 节点 : 1  不是 , 0 是
		 */
		private int isMaster = 1;

		public String getNode() {
			return node;
		}

		public void setNode(String node) {
			this.node = node;
		}

		public int getState() {
			return state;
		}

		public void setState(int state) {
			this.state = state;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public int getIsNewNode() {
			return isNewNode;
		}

		public void setIsNewNode(int isNewNode) {
			this.isNewNode = isNewNode;
		}

		public int getIsMaster() {
			return isMaster;
		}

		public void setIsMaster(int isMaster) {
			this.isMaster = isMaster;
		}

		@Override
		public String toString() {
			return "MigrateNodeInfo{" +
					"id=" + id +
					", node='" + node + '\'' +
					", state=" + state +
					", isNewNode=" + isNewNode +
					", isMaster=" + isMaster +
					'}';
		}
	}

	@Override
	public String toString() {
		return "MigrateGroupNodeInfo{" +
				"groupId='" + groupId + '\'' +
				", allNode=" + allNode +
				'}';
	}
}
