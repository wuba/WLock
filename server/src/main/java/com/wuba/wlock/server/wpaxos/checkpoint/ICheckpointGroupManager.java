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


import com.wuba.wpaxos.utils.JavaOriTypeWrapper;
import java.util.List;

public interface ICheckpointGroupManager {
	void start();

	void shutdown();

	boolean executeForCheckpoint(int smID, long instanceID, byte[] paxosValue);

	long getCheckpointInstanceID(int smID);

	int lockCheckpointState(int smID);

	int getCheckpointState(int smID, JavaOriTypeWrapper<String> dirPath, List<String> fileList);


	void unLockCheckpointState(int smID);

	int loadCheckpointState(int smID, String checkpointTmpFileDirPath, List<String> fileList,
								   long checkpointInstanceID);

	void fixCheckpointByMinChosenInstanceId(int smID, long minChosenInstanceID);
}
