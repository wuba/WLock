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
package com.wuba.wlock.server.worker;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.server.communicate.signal.KeepMasterUdpClient;
import com.wuba.wlock.server.communicate.signal.protocol.TryBeMasterMessage;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.service.impl.KeepMasterServiceImpl;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.comm.NodeInfo;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MasterMgrWorker {
	private static final Logger logger = LogManager.getLogger(MasterMgrWorker.class);

	private static MasterMgrWorker instance = new MasterMgrWorker();

	private ExecutorService executorService = new ThreadPoolExecutor(1, 1, 1L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("master_mgr_worker"));

	private MasterMgrWorker() {
	}

	public static MasterMgrWorker getInstance() {
		return instance;
	}

	public void dropMasterHandle(final byte[] bytes) {

		executorService.execute(() -> {
			try {
				if (bytes == null) {
					return;
				}
				TryBeMasterMessage tryBeMasterMessage = JSONObject.parseObject(bytes, TryBeMasterMessage.class);
				int groupId = tryBeMasterMessage.getGroupId();
				long nodeID = tryBeMasterMessage.getNodeId();
				logger.info("groupid {} receive trybemaster form {}", groupId, nodeID);
				KeepMasterServiceImpl.getInstance().keepMaster(groupId, nodeID);
			} catch (Exception e) {
				logger.error("drop master handle execute failed.", e);
			}
		});
	}

	public void sendTryBeMasterCmd(int subGroup, long myNodeID) throws Exception {
		TryBeMasterMessage tryBeMasterMsg = new TryBeMasterMessage(subGroup, myNodeID);
		try {
			NodeInfo nodeInfo = WpaxosService.getInstance().getMaster(subGroup);
			KeepMasterUdpClient udpClient = KeepMasterUdpClient.getInstance(nodeInfo.getIp(), ServerConfig.getInstance().getUdpPort().get(nodeInfo.getNodeID()), "utf-8");
			udpClient.send(JSONObject.toJSONString(tryBeMasterMsg));
			logger.info("group {} send trybemaster to {}", subGroup, nodeInfo.getIpPort());
		} catch (Exception e) {
			logger.error("try be master handle throws exception", e);
			throw new Exception(e);
		}
	}
}
