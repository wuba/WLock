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

import com.wuba.wlock.server.collector.QpsCounter;
import com.wuba.wlock.server.communicate.ProtocolType;
import com.wuba.wlock.server.communicate.WLockRequest;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.DeleteLockRequest;
import com.wuba.wlock.server.communicate.protocol.TrySnatchLockRequest;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.lock.protocol.LockTypeEnum;
import com.wuba.wlock.server.lock.protocol.OpcodeEnum;
import com.wuba.wlock.server.service.ILockService;
import com.wuba.wlock.server.service.impl.ReadLockService;
import com.wuba.wlock.server.service.impl.ReentrantLockService;
import com.wuba.wlock.server.service.impl.WriteLockService;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class LockWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(LockWorker.class);

	private static LockWorker lockWorker = new LockWorker();
	private static LockGroupWorker[] lockGroupWorkers;
	private static ILockService reentrantLockService = new ReentrantLockService();
	private static ILockService readLockService = new ReadLockService();
	private static ILockService writeLockService = new WriteLockService();

	private LockWorker() {
		int groupCount = PaxosConfig.getInstance().getGroupCount();
		lockGroupWorkers = new LockGroupWorker[groupCount];
		for (int i = 0; i < groupCount; i++) {
			lockGroupWorkers[i] = new LockGroupWorker(i);
		}
	}

	public static LockWorker getInstance() {
		return lockWorker;
	}

	public void offer(final LockContext lockContext, int group) {
		lockGroupWorkers[group].offer(lockContext);
	}


	public void shutdown() {
		for (LockGroupWorker lockGroupWorker : lockGroupWorkers) {
			lockGroupWorker.shutdown();
		}
	}

	public Map<Integer,Integer> getQueueSize(){
		Map<Integer, Integer> result = new HashMap<>();
		for (LockGroupWorker lockGroupWorker : lockGroupWorkers) {
			result.put(lockGroupWorker.groupId,lockGroupWorker.getQueueSize());
		}
		return result;
	}

	class LockGroupWorker {
		private final int groupId;
		private int exeCount = 32;
		private ExecutorService[] executors = new ThreadPoolExecutor[exeCount];
		private boolean isShutdown = true;

		public LockGroupWorker(int groupId) {
			this.groupId = groupId;
			isShutdown = false;
			for (int i = 0; i < exeCount; i++) {
				executors[i] = new ThreadPoolExecutor(1, 1, 1500L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryImpl("lock_group_worker_" + groupId + "_" + i));
			}
		}

		private ILockService lockService(LockContext lockContext) {
			byte[] buf = lockContext.getBuf();
			if (buf[WLockRequest.LOCK_TYPE] == LockTypeEnum.reentrantLock.getValue()) {
				return reentrantLockService;
			}

			if (buf[WLockRequest.LOCK_TYPE] == LockTypeEnum.readWriteReentrantLock.getValue()) {
				if (buf[WLockRequest.OPCODE] == OpcodeEnum.ReadWriteOpcode.READ.getValue()) {
					return readLockService;
				}

				if (buf[WLockRequest.OPCODE] == OpcodeEnum.ReadWriteOpcode.WRITE.getValue()) {
					return writeLockService;
				}
			}
			return null;
		}
		private void doLock(LockContext lockContext) throws ProtocolException {
			if (lockContext != null) {
				byte protocolType = lockContext.getBuf()[WLockRequest.PROTOCOL_TYPE_POS];
				ILockService lockService = lockService(lockContext);
				if (protocolType == ProtocolType.ACQUIRE_LOCK) {
					if (!lockService.tryAcquireLock(lockContext, groupId)) {
						QpsCounter.incrServerAcquireFailQps();
						QpsCounter.incrGroupAcquireFailQps(groupId);
						QpsCounter.incrKeyAcquireFailQps(lockContext.getRegistryKey(), groupId);
					}
					return;
				}
				if (protocolType == ProtocolType.RELEASE_LOCK) {
					if (!lockService.tryReleaseLock(lockContext, groupId)) {
						QpsCounter.incrServerReleaseFailQps();
						QpsCounter.incrGroupReleaseFailQps(groupId);
						QpsCounter.incrKeyReleaseFailQps(lockContext.getRegistryKey(), groupId);
					}
					return;
				}
				if (protocolType == ProtocolType.RENEW_LOCK) {
					if (!lockService.tryRenewLock(lockContext, groupId)) {
						QpsCounter.incrServerRenewFailQps();
						QpsCounter.incrGroupRenewFailQps(groupId);
						QpsCounter.incrKeyRenewFailQps(lockContext.getRegistryKey(), groupId);
					}
					return;
				}
				if (protocolType == ProtocolType.DELETE_LOCK) {
					DeleteLockRequest deleteLockRequest = new DeleteLockRequest();
					deleteLockRequest.fromBytes(lockContext.getBuf());
					QpsCounter.incrServerDeleteQps();
					QpsCounter.incrGroupDeleteQps(groupId);
					if (lockService.tryDeleteLock(deleteLockRequest, groupId)) {
						QpsCounter.incrServerDeleteFailQps();
						QpsCounter.incrGroupDeleteFailQps(groupId);
					}
					return;
				}
				if (protocolType == ProtocolType.GET_LOCK) {
					if (!lockService.tryGetLock(lockContext, groupId)) {
						QpsCounter.incrServerGetFailQps();
						QpsCounter.incrGroupGetFailQps(groupId);
						QpsCounter.incrKeyGetFailQps(lockContext.getRegistryKey(), groupId);
					}
					return;
				}
				if (protocolType == ProtocolType.WATCH_LOCK) {
					if (!lockService.watchLock(lockContext, groupId)) {
						QpsCounter.incrServerWatchFailQps();
						QpsCounter.incrGroupWatchFailQps(groupId);
						QpsCounter.incrKeyWatchFailQps(lockContext.getRegistryKey(), groupId);
					}
					return;
				}
				if (protocolType == ProtocolType.TRY_SNATCH_LOCK) {
					TrySnatchLockRequest trySnatchLockRequest = new TrySnatchLockRequest();
					trySnatchLockRequest.fromBytes(lockContext.getBuf());
					lockService.trySnatchLock(trySnatchLockRequest, groupId);
					return;
				}

				LOGGER.error("receive message illegal, discard it.protocol type : {} from channel : {}", protocolType, lockContext.getChannel().getRemoteAddress());
			}

		}

		public void offer(final LockContext lockContext) {
			String key = lockContext.getLockkey();
			int exeIdx = getExecutorByKey(key);
			executors[exeIdx].execute(() -> {
				try {
					doLock(lockContext);
				} catch(ProtocolException e) {
					LOGGER.error(e.getMessage(), e);
				}
			});
		}

		public void shutdown() {
			isShutdown = true;
			for (int i = 0; i < exeCount; i++) {
				executors[i].shutdown();
				try {
					if (executors[i].awaitTermination(3000, TimeUnit.MILLISECONDS)) {
						executors[i].shutdownNow();
					}
				} catch(InterruptedException e) {
				} finally {
					executors[i].shutdownNow();
				}
				LOGGER.info("shutdown lock worker, {}_{}", groupId, i);
			}
		}
		
		
		public int getExecutorByKey(String key) {
			return Math.abs(key.hashCode() % exeCount);
		}
		
		public int getQueueSize() {
			int queueSize = 0;
			for (int i = 0; i < exeCount; i++) {
				queueSize += ((ThreadPoolExecutor)executors[i]).getQueue().size();
			}
			
			return queueSize;
		}

		public boolean isShutdown() {
			return isShutdown;
		}

		public void setShutdown(boolean isShutdown) {
			this.isShutdown = isShutdown;
		}
	}
}
