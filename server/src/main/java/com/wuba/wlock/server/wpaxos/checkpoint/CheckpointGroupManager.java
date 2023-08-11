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

import com.wuba.wlock.server.constant.PaxosState;
import com.wuba.wlock.server.wpaxos.SMID;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.utils.JavaOriTypeWrapper;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CheckpointGroupManager implements ICheckpointGroupManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckpointGroupManager.class);
	private final int group;
	private final String path;
	private final int intervalMin;
	private Map<Integer, Long> checkpointInstanceIdMap = new ConcurrentHashMap<>();
	private Map<Integer, CheckPoint> checkpointMap = new ConcurrentHashMap<>();
	private Map<Integer, Byte> checkPointLockFlag = new ConcurrentHashMap<>();
	private final ScheduledExecutorService executorService;


	public CheckpointGroupManager(int group, String dir, int intervalMin, Set<Integer> checkpointSMIds) {
		this.group = group;
		this.path = dir + File.separator + group;
		this.intervalMin = intervalMin;
		File checkpointDir = new File(path);
		String[] list = checkpointDir.list();
		if (list != null) {
			for (String smid : list) {
				try {
					if (!checkpointSMIds.contains(Integer.parseInt(smid))) {
						continue;
					}
					CheckPoint checkPoint = new CheckPoint(path, smid, group);
					checkpointInstanceIdMap.put(Integer.valueOf(smid), checkPoint.getNowCheckpointInstanceId());
					checkpointMap.put(Integer.valueOf(smid), checkPoint);
				} catch(IOException e) {
					LOGGER.error("init checkpoint file error.group {} smid {}", group, smid, e);
				}
			}
		}
		executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("checkpoint_worker_" + group));
	}

	@Override
	public void start() {
		executorService.scheduleAtFixedRate(this::presistCheckpoint, 0, intervalMin, TimeUnit.MINUTES);
	}

	@Override
	public void shutdown() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException ignored) {
		} finally {
			executorService.shutdownNow();
		}
		checkpointMap.values().forEach(CheckPoint::shutdown);
	}

	private void presistCheckpoint() {
		try{
			checkpointInstanceIdMap.forEach(this::presistCheckpoint);
		}catch (Throwable e){
			LOGGER.error("save checkpoint error.",e);
		}
	}


	private void presistCheckpoint(int smid, long instanceId) {
		if (checkPointLockFlag.getOrDefault(smid, LockCheckpointFlag.UNLOCK.getValue())
				!= LockCheckpointFlag.LOCK.getValue()) {
			CheckPoint checkPoint = checkpointMap.get(smid);
			try {
				if (checkPoint == null) {
					checkPoint = new CheckPoint(path, smid, group);
					checkpointMap.put(smid, checkPoint);
				}
				if (checkPoint.getNowCheckpointInstanceId() > instanceId) {
					LOGGER.info("presist checkpoint schedual ,groupid {} smis {} nowinstanceid {} < instanceid {}.return", group, smid, checkPoint.getNowCheckpointInstanceId(), instanceId);
					return;
				}
				LOGGER.info("presist checkpoint schedual ,groupid {} smis {} instanceid {}", group, smid, instanceId);
				checkPoint.presistInstanceId(instanceId);

				long lastCheckpointInstanceId = checkPoint.getLastCheckpointInstanceId();
				if (PaxosState.isStarted(group) && WpaxosService.getInstance().getPaxosNode().getLogStorage().getMinChosenInstanceID(group) < lastCheckpointInstanceId) {
					WpaxosService.getInstance().setMinChosenInstanceId(group, lastCheckpointInstanceId);
				}
			} catch (IOException e) {
				LOGGER.error("presistCheckpoint error.groupid {} smid {}", group, smid, e);
			}
		}
	}


	private boolean learnCheckpoint(int smid, long lastInstanceId, long nowInstanceId) {
		if (checkPointLockFlag.getOrDefault(smid, LockCheckpointFlag.UNLOCK.getValue())
				!= LockCheckpointFlag.LOCK.getValue()) {
			CheckPoint checkPoint = checkpointMap.get(smid);
			try {
				if (checkPoint == null) {
					checkPoint = new CheckPoint(path, smid, group);
					checkpointMap.put(smid, checkPoint);
				}
				checkPoint.setLastCheckpointInstanceId(lastInstanceId);
				checkPoint.setNowCheckpointInstanceId(nowInstanceId);
				WpaxosService.getInstance().setMinChosenInstanceId(group,lastInstanceId);
				checkPoint.flush();
				return true;
			} catch(IOException e) {
				LOGGER.error("learnCheckpoint error.groupid {} smid {}", group, smid, e);
				return false;
			}
		}
		return false;
	}


	@Override
	public boolean executeForCheckpoint(int smID, long instanceID, byte[] paxosValue) {
		Long checkpoint = checkpointInstanceIdMap.get(smID);
		LOGGER.debug("execute for checkpoint groupid {} smid {} instanceid {} checkpoint {}", group, smID, instanceID, checkpoint);
		if (checkpoint != null && checkpoint > instanceID) {
			return true;
		}
		checkpointInstanceIdMap.put(smID, instanceID);
		return true;
	}

	@Override
	public long getCheckpointInstanceID(int smID) {
		Long instanceId = checkpointInstanceIdMap.get(smID);
		LOGGER.debug("get checkpoint groupid {} smid {} instanceid {}", group, smID, instanceId);
		if (instanceId == null) {
			return -1;
		}
		return instanceId.longValue();
	}

	@Override
	public int lockCheckpointState(int smID) {
		checkPointLockFlag.put(smID, LockCheckpointFlag.LOCK.getValue());
		return 0;
	}

	@Override
	public int getCheckpointState(int smID, JavaOriTypeWrapper<String> dirPath, List<String> fileList) {
		if (smID != SMID.LOCK_SMID.getValue()) {
			return 0;
		}
		dirPath.setValue(path);
		if (!checkpointMap.containsKey(smID)) {
			return 0;
		}
		File file = new File(path);
		if (!file.exists() || !file.isDirectory()) {
			return 0;
		}
		String[] list = file.list();
		if (list == null || list.length == 0) {
			return 0;
		}
		fileList.add(checkpointMap.get(smID).filePath());
		return 0;
	}


	@Override
	public void unLockCheckpointState(int smID) {
		checkPointLockFlag.put(smID, LockCheckpointFlag.UNLOCK.getValue());
	}

	@Override
	public int loadCheckpointState(int smID, String checkpointTmpFileDirPath, List<String> fileList,
								   long checkpointInstanceID) {
		String tmpFile = fileList.get(0);
		File file = new File(checkpointTmpFileDirPath + File.separator + tmpFile);
		try (FileChannel fileChannel = new RandomAccessFile(file,"rw").getChannel()) {
			MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
			long lastInstanceId = mappedByteBuffer.getLong();
			/**
			 * 这里需要设置checkpoint为十分钟前的
			 */
			LOGGER.info("load checkpoint state group {} smid {} instanceid {}", group, smID, lastInstanceId);
			checkpointInstanceIdMap.put(smID, lastInstanceId);
			learnCheckpoint(smID, lastInstanceId, lastInstanceId);
			return 0;
		} catch (IOException e) {
			LOGGER.error("load checkpoint state throws exception.groupid {} smid {}", group, smID, e);
			return -1;
		}
	}

	@Override
	public void fixCheckpointByMinChosenInstanceId(int smID, long minChosenInstanceID) {
		CheckPoint checkPoint = checkpointMap.get(smID);
		try {
			if (checkPoint == null) {
				checkPoint = new CheckPoint(path, smID, group);
				checkpointMap.put(smID, checkPoint);
			}
			LOGGER.info("fix checkpoint by minChosenInstanceId,groupid {} smid {} minInstanceId {}", group, smID, minChosenInstanceID);
			if (!checkpointInstanceIdMap.containsKey(smID) || checkpointInstanceIdMap.get(smID) < minChosenInstanceID) {
				LOGGER.info("now instanceid {} < min chosen instnceid {},groupid {} smid {}.set instanceid = min chosen", checkPoint.getLastCheckpointInstanceId(), minChosenInstanceID, group, smID);
				checkpointInstanceIdMap.put(smID,minChosenInstanceID);
				checkPoint.flush();
			}
		} catch (IOException e) {
			LOGGER.error("fixCheckpointByMinChosenInstanceId  error.groupid {} smid {}", group, smID, e);
		}
	}

}
