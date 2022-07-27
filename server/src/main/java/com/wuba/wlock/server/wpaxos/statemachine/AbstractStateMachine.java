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
package com.wuba.wlock.server.wpaxos.statemachine;

import com.wuba.wlock.server.wpaxos.checkpoint.CheckpointManager;

import com.wuba.wpaxos.storemachine.StateMachine;
import com.wuba.wpaxos.utils.JavaOriTypeWrapper;
import java.util.List;

public abstract class AbstractStateMachine implements StateMachine {
	private static CheckpointManager checkpointManager = CheckpointManager.getInstance();

	protected int groupIdx;
	private int smID;

	public AbstractStateMachine(int groupIdx, int smID, boolean needCheckpoint) {
		this.groupIdx = groupIdx;
		this.smID = smID;
		if (needCheckpoint) {
			registerCheckpoint(smID);
		}
	}

	public void registerCheckpoint(int smID) {
		checkpointManager.registerCheckpoint(smID);
	}


	@Override
	public int getSMID() {
		return smID;
	}

	@Override
	public boolean executeForCheckpoint(int groupIdx, long instanceID, byte[] paxosValue) {
		return checkpointManager.executeForCheckpoint(groupIdx, smID, instanceID, paxosValue);
	}

	@Override
	public long getCheckpointInstanceID(int groupIdx) {
		return checkpointManager.getCheckpointInstanceID(groupIdx, smID);
	}

	@Override
	public int lockCheckpointState() {
		return checkpointManager.lockCheckpointState(groupIdx, smID);
	}

	@Override
	public int getCheckpointState(int groupIdx, JavaOriTypeWrapper<String> dirPath, List<String> fileList) {
		return checkpointManager.getCheckpointState(groupIdx, smID, dirPath, fileList);
	}

	@Override
	public void unLockCheckpointState() {
		checkpointManager.unLockCheckpointState(groupIdx, smID);
	}

	@Override
	public int loadCheckpointState(int groupIdx, String checkpointTmpFileDirPath, List<String> fileList, long checkpointInstanceID) {
		return checkpointManager.loadCheckpointState(groupIdx, smID, checkpointTmpFileDirPath, fileList, checkpointInstanceID);
	}

	@Override
	public void fixCheckpointByMinChosenInstanceId(long minChosenInstanceID) {
		checkpointManager.fixCheckpointByMinChosenInstanceId(groupIdx, smID, minChosenInstanceID);
	}
}
