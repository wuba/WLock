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

import com.wuba.wpaxos.store.pagecache.MapedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantLock;

public class CheckPoint implements ICheckPoint{

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckPoint.class);
	private final RandomAccessFile randomAccessFile;
	private final FileChannel fileChannel;
	private final MappedByteBuffer mappedByteBuffer;
	private volatile long lastCheckpointInstanceId;
	private volatile long nowCheckpointInstanceId;
	private ReentrantLock checkPointlock = new ReentrantLock();
	private final int groupId;
	private final int smId;

	public CheckPoint(final String path, final int smId, final int groupId) throws IOException {
		this(path, String.valueOf(smId), groupId);
	}

	public CheckPoint(final String path, final String smId, final int groupId) throws IOException {
		this.smId = Integer.parseInt(smId);
		this.groupId = groupId;
		String filePath = path + File.separator + smId;
		File file = new File(filePath);
		MapedFile.ensureDirOK(file.getParent());
		boolean fileExists = file.exists();
		if (!fileExists) {
			file.createNewFile();
		}
		this.randomAccessFile = new RandomAccessFile(file, "rw");
		this.fileChannel = this.randomAccessFile.getChannel();
		this.mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MapedFile.OS_PAGE_SIZE);

		if (fileExists) {
			LOGGER.info("store checkpoint file {} exists, ", filePath);
			this.lastCheckpointInstanceId = this.mappedByteBuffer.getLong(0);
			this.nowCheckpointInstanceId = this.mappedByteBuffer.getLong(8);
			LOGGER.info("store checkpoint file groupid {} smid {} lastCheckpointInstanceId [{}]  nowCheckpointInstanceId [{}]", groupId, smId, lastCheckpointInstanceId, nowCheckpointInstanceId);
		} else {
			LOGGER.info("store checkpoint file {} not exists, ", filePath);
		}
	}

	public void shutdown() {
		this.flush();
		// unmap mappedByteBuffer
		MapedFile.clean(this.mappedByteBuffer);
		try {
			this.fileChannel.close();
			LOGGER.info("checkpoint shutdown.groupid {} smid {}", groupId, smId);
		} catch(IOException e) {
			LOGGER.error("Checkpoint shutdown throws exception.groupid {} smid {}", groupId, smId, e);
		}
	}

	public void flush() {
		checkPointlock.lock();
		try {
			LOGGER.info("presist checkpoint group {} smid {} lastcheckpoint {} nowcheckpoint {}", groupId, smId, lastCheckpointInstanceId, nowCheckpointInstanceId);
			this.mappedByteBuffer.putLong(0, this.lastCheckpointInstanceId);
			this.mappedByteBuffer.putLong(8, this.nowCheckpointInstanceId);
			this.mappedByteBuffer.force();
		} catch(Exception e) {
			LOGGER.error("Checkpoint flush throws exception.groupid {} smid {}", groupId, smId, e);
		} finally {
			checkPointlock.unlock();
		}
	}


	public void presistInstanceId(long nowInstanceId) {
		this.lastCheckpointInstanceId = nowCheckpointInstanceId;
		this.nowCheckpointInstanceId = nowInstanceId;
		flush();
	}

	public void setLastCheckpointInstanceId(long lastCheckpointInstanceId) {
		this.lastCheckpointInstanceId = lastCheckpointInstanceId;
	}

	public void setNowCheckpointInstanceId(long nowCheckpointInstanceId) {
		this.nowCheckpointInstanceId = nowCheckpointInstanceId;
	}

	public long getLastCheckpointInstanceId() {
		return lastCheckpointInstanceId;
	}

	public long getNowCheckpointInstanceId() {
		return nowCheckpointInstanceId;
	}

	@Override
	public String filePath() {
		return String.valueOf(smId);
	}
}
