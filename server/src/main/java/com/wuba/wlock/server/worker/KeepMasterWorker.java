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

import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.constant.GroupState;
import com.wuba.wlock.server.constant.PaxosState;
import com.wuba.wlock.server.keepmaster.GroupMasterStrategy;
import com.wuba.wlock.server.keepmaster.HashStrategy;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class KeepMasterWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(KeepMasterWorker.class);

	private static KeepMasterWorker keepMasterWorker = new KeepMasterWorker();
	private static final long INTERVAL = 20;
	private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("keep_master_worker"));

	private WpaxosService wpaxosService = WpaxosService.getInstance();
	private GroupMasterStrategy groupMasterStrategy;


	private KeepMasterWorker() {
		groupMasterStrategy = new HashStrategy();
	}

	public static KeepMasterWorker getInstance() {
		return keepMasterWorker;
	}

	public void start() {
		LOGGER.info("start keepmaster {}", ServerConfig.getInstance().isEnableKeepMaster());
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try{
					if (ServerConfig.getInstance().isEnableKeepMaster() && PaxosState.isStarted()) {
						keepMaster();
					}
				}catch (Throwable e){
					LOGGER.error("keepmaster error.",e);
				}

			}
		}, 60, INTERVAL, TimeUnit.SECONDS);
	}

	private void keepMaster() {
		int groupCount = PaxosConfig.getInstance().getGroupCount();

		Map<Integer, List<Integer>> groupNodeList = makeGroupNodeList();
		Map<Integer, Long> groupIds = groupMasterStrategy.getBalancedGroupMaster(groupCount, PaxosConfig.getInstance().getNodeMap(), groupNodeList);
		for (int group = 0; group < groupCount; group++) {

			if (!PaxosConfig.getInstance().isEnableBalance(group)) {
				LOGGER.debug("try keep master of group :{}, no enable balance", group);
				continue;
			}

			if (wpaxosService.isNoMaster(group) || GroupState.isGroupKeepMaster(group) || WpaxosService.getInstance().isLearn(group)) {
				LOGGER.warn("try keep master of group :{},but is in keepMaster state {}, isLearn {}, stop.", group, GroupState.isGroupKeepMaster(group), WpaxosService.getInstance().isLearn(group));
				return;
			}
			long factNodeID = wpaxosService.getMaster(group).getNodeID();
			Long idealNode = groupIds.get(group);
			/**
			 * 找到group序号最小的master错乱group
			 */
			if (factNodeID != idealNode) {
				if (idealNode == wpaxosService.getPaxosNode().getMyNodeID()) {
					/**
					 * 最小的错乱group是自身，则发起夺回master
					 */
					if (!GroupState.isGroupKeepMaster(group)) {
						LOGGER.info("self should be master of group {},but is not", group);
						GroupState.setGroupKeepMaster(group);
						LOGGER.info("keep master,set group {} state........................", group);
						try {
							MasterMgrWorker.getInstance().sendTryBeMasterCmd(group, idealNode);
						} catch(Exception e) {
							GroupState.clearGroupKeepMaster(group);
						}
					}
				}
				break;
			}
		}
	}

	private Map<Integer, List<Integer>> makeGroupNodeList() {
		Map<Integer, Set<Integer>> groupNodeMap = PaxosConfig.getInstance().getGroupNodeMap();
		Map<Integer, List<Integer>> result = new HashMap<>(30);
		for (Map.Entry<Integer, Set<Integer>> data : groupNodeMap.entrySet()) {
			result.put(data.getKey(), new ArrayList<>(data.getValue()));
		}
		return result;
	}

	public void shutdown() {
		scheduledExecutorService.shutdown();
		try {
			if (!scheduledExecutorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				scheduledExecutorService.shutdownNow();
			}
		} catch(InterruptedException e) {
		} finally {
			scheduledExecutorService.shutdownNow();
		}
		LOGGER.info("keep master worker shutdown");
	}
}
