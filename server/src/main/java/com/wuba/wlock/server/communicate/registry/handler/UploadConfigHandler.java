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
package com.wuba.wlock.server.communicate.registry.handler;

import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.UploadGroupMaster;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.comm.NodeInfo;
import com.wuba.wlock.server.exception.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UploadConfigHandler extends AbstractPaxosHandler implements IPaxosHandler {
	private static final Logger logger = LoggerFactory.getLogger(UploadConfigHandler.class);

	@Override
	protected boolean checkProtocol(byte opaque) {
		return true;
	}

	@Override
	public boolean doSuccess(RegistryProtocol registryProtocol) throws Exception {
		return true;
	}

	@Override
	public boolean doError(RegistryProtocol registryProtocol) {
		logger.error("upload master group config error.");
		return false;
	}

	@Override
	public boolean doElse(RegistryProtocol registryProtocol) {
		return true;
	}

	@Override
	public RegistryProtocol buildMessage() throws ConfigException {
		int groupCount = PaxosConfig.getInstance().getGroupCount();
		List<UploadGroupMaster.GroupMasterVersion> groupMasterVersions = new ArrayList<>();
		UploadGroupMaster.GroupMasterVersion groupMasterVersion;
		for (int group = 0; group < groupCount; group++) {
			NodeInfo nodeInfo = WpaxosService.getInstance().getMaster(group);
			if (nodeInfo == null) {
				continue;
			}
			long masterVersion = WpaxosService.getInstance().getMasterVersion(group);
			groupMasterVersion = new UploadGroupMaster.GroupMasterVersion();
			groupMasterVersion.setMaster(nodeInfo.getIp() + UploadGroupMaster.SEP + nodeInfo.getPort());
			groupMasterVersion.setGroup(group);
			groupMasterVersion.setVersion(masterVersion);
			groupMasterVersions.add(groupMasterVersion);
		}
		UploadGroupMaster.GroupMaster groupMaster = new UploadGroupMaster.GroupMaster();
		groupMaster.setGroupMasterVersions(groupMasterVersions);
		groupMaster.setClusterName(ServerConfig.getInstance().getCluster());
		groupMaster.setIp(ServerConfig.getInstance().getServerListenIP());
		groupMaster.setPort(ServerConfig.getInstance().getServerListenPort());
		return new UploadGroupMaster(groupMaster);
	}

	@Override
	public boolean handleResponse(RegistryProtocol registryProtocol) throws Exception {
		return super.doHandler(registryProtocol);
	}
}
