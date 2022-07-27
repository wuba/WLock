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
import com.wuba.wlock.server.service.GroupMetaService;
import com.wuba.wlock.server.wpaxos.checkpoint.ICheckPoint;
import com.wuba.wpaxos.store.pagecache.MapedFile;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantLock;

public class MetaCheckPoint implements ICheckPoint {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetaCheckPoint.class);
	private final RandomAccessFile randomAccessFile;
	private final FileChannel fileChannel;
	private final MappedByteBuffer mappedByteBuffer;
	private volatile GroupMeta groupMeta;
	private ReentrantLock checkPointlock = new ReentrantLock();
	private final int groupId;
	private final int smId;

	public MetaCheckPoint(final String path, final int smId, final int groupId) throws IOException, RocksDBException, ProtocolException {
		this(path, String.valueOf(smId), groupId);
	}

	public MetaCheckPoint(final String path, final String smId, final int groupId) throws IOException, RocksDBException, ProtocolException {
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
			LOGGER.info("store MetaCheckpoint file {} exists, ", filePath);
			this.groupMeta = GroupMeta.fromByteBuffer(mappedByteBuffer);
			GroupMetaService.getInstance().save(this.groupMeta);

			LOGGER.info("store MetaCheckpoint file groupid {} smid {} groupMeta {}", groupId, smId, JSON.toJSONString(this.groupMeta));
		} else {
			LOGGER.info("store MetaCheckpoint file {} not exists, ", filePath);
		}
	}

	public void shutdown() {
		// unmap mappedByteBuffer
		MapedFile.clean(this.mappedByteBuffer);
		try {
			this.fileChannel.close();
			LOGGER.info("MetaCheckpoint shutdown.groupid {} smid {}", groupId, smId);
		} catch(IOException e) {
			LOGGER.error("MetaCheckpoint shutdown throws exception.groupid {} smid {}", groupId, smId, e);
		}
	}

	public void flush() throws Exception{
		checkPointlock.lock();
		try {
			LOGGER.info("presist MetaCheckpoint group {} smid {} groupMeta: {}", groupId, smId, JSON.toJSONString(groupMeta));
			if (groupMeta == null) {
				return;
			}

			this.mappedByteBuffer.put(this.groupMeta.toBytes());
			this.mappedByteBuffer.force();

			GroupMetaService.getInstance().save(this.groupMeta);
		} catch(Exception e) {
			LOGGER.error("MetaCheckpoint flush throws exception.groupid {} smid {}", groupId, smId, e);
			throw e;
		} finally {
			checkPointlock.unlock();
		}
	}

	public void presist(byte[] paxosValue) throws Exception {
		this.presist(GroupMeta.fromBytes(paxosValue));
	}

	public void presist(GroupMeta groupMeta) throws Exception {
		if (this.groupMeta != null && this.groupMeta.getGroupVersion() >= groupMeta.getGroupVersion()) {
			LOGGER.error("MetaCheckPoint.presist this.groupVersion: {} >= groupVersion: {}", this.groupMeta.getGroupVersion(), groupMeta.getGroupVersion());
			return;
		}

		this.groupMeta = groupMeta;
		flush();
	}

	@Override
	public String filePath() {
		return String.valueOf(smId);
	}
}
