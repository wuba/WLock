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
package com.wuba.wlock.server.wpaxos.checkpoint.meta;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.server.domain.GroupMeta;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.wpaxos.checkpoint.ICheckpointGroupManager;
import com.wuba.wpaxos.utils.JavaOriTypeWrapper;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MetaCheckpointGroupManager implements ICheckpointGroupManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetaCheckpointGroupManager.class);
	private final int group;
	private final String path;
	private Map<Integer, MetaCheckPoint> checkpointMap = new ConcurrentHashMap<Integer, MetaCheckPoint>();


	public MetaCheckpointGroupManager(int group, String dir, Set<Integer> checkpointSMIds) {
		this.group = group;
		this.path = dir + File.separator + group;
		File checkpointDir = new File(path);
		String[] list = checkpointDir.list();
		if (list != null) {
			for (String smid : list) {
				try {
					if (!checkpointSMIds.contains(Integer.parseInt(smid))) {
						continue;
					}
					MetaCheckPoint checkPoint = new MetaCheckPoint(path, smid, group);
					checkpointMap.put(Integer.valueOf(smid), checkPoint);
				} catch(Exception e) {
					LOGGER.error("init checkpoint file error.group {} smid {}", group, smid, e);
				}
			}
		}
	}

	@Override
	public void start() {
	}

	@Override
	public void shutdown() {
		checkpointMap.values().forEach(MetaCheckPoint::shutdown);
	}

	@Override
	public boolean executeForCheckpoint(int smID, long instanceID, byte[] paxosValue) {
		try {
			MetaCheckPoint metaCheckPoint = getMetaCheckPoint(smID);
			metaCheckPoint.presist(paxosValue);
		} catch (Exception e) {
			LOGGER.error("MetaCheckpointGroupManager.executeForCheckpoint error", e);
			return false;
		}
		return true;
	}

	@Override
	public long getCheckpointInstanceID(int smID) {
		return -1;
	}

	@Override
	public int lockCheckpointState(int smID) {
		return 0;
	}

	@Override
	public int getCheckpointState(int smID, JavaOriTypeWrapper<String> dirPath, List<String> fileList) {
		MetaCheckPoint metaCheckPoint = checkpointMap.get(smID);
		if (metaCheckPoint != null) {
			dirPath.setValue(this.path);
			fileList.add(metaCheckPoint.filePath());
		}

		return 0;
	}


	@Override
	public void unLockCheckpointState(int smID) {
	}

	@Override
	public int loadCheckpointState(int smID, String checkpointTmpFileDirPath, List<String> fileList,
								   long checkpointInstanceID) {
		String tmpFile = fileList.get(0);
		File file = new File(checkpointTmpFileDirPath + File.separator + tmpFile);
		try (FileChannel fileChannel = new RandomAccessFile(file,"rw").getChannel()) {
			MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
			MetaCheckPoint metaCheckPoint = getMetaCheckPoint(smID);
			GroupMeta groupMeta = GroupMeta.fromByteBuffer(mappedByteBuffer);
			LOGGER.info("MetaCheckpointGroupManager.loadCheckpointState groupMeta: {}", JSON.toJSONString(groupMeta));
			metaCheckPoint.presist(groupMeta);

			return 0;
		} catch (Exception e) {
			LOGGER.error("load MetaCheckpoint state throws exception.groupid {} smid {}", group, smID, e);
			return -1;
		}
	}

	private MetaCheckPoint getMetaCheckPoint(int smId) throws RocksDBException, ProtocolException, IOException {
		MetaCheckPoint metaCheckPoint = checkpointMap.get(smId);
		if (metaCheckPoint == null) {
			metaCheckPoint = new MetaCheckPoint(path, smId, group);
			checkpointMap.put(smId, metaCheckPoint);
		}

		return metaCheckPoint;
	}

	@Override
	public void fixCheckpointByMinChosenInstanceId(int smID, long minChosenInstanceID) {
	}

}
