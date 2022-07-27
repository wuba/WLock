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
package com.wuba.wlock.server.wpaxos.checkpoint;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wpaxos.utils.JavaOriTypeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractCheckpointManager implements ICheckpointManager{
	private static final Logger log = LoggerFactory.getLogger(AbstractCheckpointManager.class);
	protected ICheckpointGroupManager[] checkpointGroupManager;
	protected Set<Integer> checkpointSMIds;

	public AbstractCheckpointManager() {
	}

	public abstract ICheckpointGroupManager createICheckpointGroupManager(int group);

	@Override
	public void start() {
		log.info("AbstractCheckpointManager.start start");
		int groupCount = PaxosConfig.getInstance().getGroupCount();
		checkpointGroupManager = new ICheckpointGroupManager[groupCount];
		for (int group = 0; group < groupCount; group++) {
			checkpointGroupManager[group] = createICheckpointGroupManager(group);
			checkpointGroupManager[group].start();
		}
		log.info("AbstractCheckpointManager.start end");
	}

	@Override
	public void shutdown() {
		log.info("AbstractCheckpointManager.shutdown start");
		int groupCount = PaxosConfig.getInstance().getGroupCount();
		for (int group = 0; group < groupCount; group++) {
			checkpointGroupManager[group].shutdown();
		}
		log.info("AbstractCheckpointManager.shutdown end");
	}

	@Override
	public boolean executeForCheckpoint(int groupIdx, int smID, long instanceID, byte[] paxosValue) {
		if (checkpointSMIds.contains(smID)) {
			return checkpointGroupManager[groupIdx].executeForCheckpoint(smID, instanceID, paxosValue);
		}
		return true;
	}

	@Override
	public long getCheckpointInstanceID(int groupIdx, int smID) {
		if (checkpointSMIds.contains(smID)) {
			return checkpointGroupManager[groupIdx].getCheckpointInstanceID(smID);
		}
		return -1;
	}

	@Override
	public int lockCheckpointState(int groupIdx, int smID) {
		if (checkpointSMIds.contains(smID)) {
			return checkpointGroupManager[groupIdx].lockCheckpointState(smID);
		}
		return 0;
	}

	@Override
	public int getCheckpointState(int groupIdx, int smID, JavaOriTypeWrapper<String> dirPath, List<String> fileList) {
		if (checkpointSMIds.contains(smID)) {
			return checkpointGroupManager[groupIdx].getCheckpointState(smID, dirPath, fileList);
		}
		return 0;
	}

	@Override
	public void unLockCheckpointState(int groupIdx, int smID) {
		if (checkpointSMIds.contains(smID)) {
			checkpointGroupManager[groupIdx].unLockCheckpointState(smID);
		}
	}

	@Override
	public int loadCheckpointState(int groupIdx, int smID, String checkpointTmpFileDirPath, List<String> fileList,
								   long checkpointInstanceID) {
		log.info("AbstractCheckpointManager.loadCheckpointState start! groupId: {}, smId: {}, checkpointTmpFileDirPath: {}, checkpointInstanceID: {}, fileList: {}",
				groupIdx, smID, checkpointTmpFileDirPath, checkpointInstanceID, JSON.toJSONString(fileList));
		if (checkpointSMIds.contains(smID)) {
			return checkpointGroupManager[groupIdx].loadCheckpointState(smID, checkpointTmpFileDirPath, fileList, checkpointInstanceID);
		}
		return 0;
	}

	@Override
	public void fixCheckpointByMinChosenInstanceId(int groupIdx, int smID, long minChosenInstanceID) {
		if (checkpointSMIds.contains(smID)) {
			checkpointGroupManager[groupIdx].fixCheckpointByMinChosenInstanceId(smID, minChosenInstanceID);
		}
	}

	@Override
	public void registerCheckpoint(int smID) {
		if (checkpointSMIds == null) {
			checkpointSMIds = new HashSet<>();
		}
		checkpointSMIds.add(smID);
		log.info("AbstractCheckpointManager.registerCheckpoint simId: {}", smID);
	}
}
