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
package com.wuba.wlock.common.registry.protocol.request;

import com.alibaba.fastjson.JSONArray;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;

import java.util.List;


public class UploadGroupMaster extends RegistryProtocol {
	public static final String SEP = ":";

	public UploadGroupMaster(GroupMaster groupMaster) {
		this.setOpaque(OptionCode.UPLOAD_MASTER_CONFIG);
		this.setBody(JSONArray.toJSONString(groupMaster).getBytes());
	}

	public static class GroupMaster {
		
		private String clusterName;
		
		private String ip;
		
		private int port;
		
		private List<GroupMasterVersion> groupMasterVersions;

		public String getClusterName() {
			return clusterName;
		}

		public void setClusterName(String clusterName) {
			this.clusterName = clusterName;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}
		
		public List<GroupMasterVersion> getGroupMasterVersions() {
			return groupMasterVersions;
		}

		public void setGroupMasterVersions(List<GroupMasterVersion> groupMasterVersions) {
			this.groupMasterVersions = groupMasterVersions;
		}
	}


	public static class GroupMasterVersion {
		private String master; // IP + PaxosPort
		private int group;
		private long version;

		public String getMaster() {
			return master;
		}

		public void setMaster(String master) {
			this.master = master;
		}

		public int getGroup() {
			return group;
		}

		public void setGroup(int group) {
			this.group = group;
		}

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}
	}

}
