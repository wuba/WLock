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
package com.wuba.wlock.server.service.impl;

import com.wuba.wlock.server.client.ClientManager;
import com.wuba.wlock.server.client.LockClient;
import com.wuba.wlock.server.communicate.ProtocolType;
import com.wuba.wlock.server.communicate.ResponseStatus;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.*;
import com.wuba.wlock.server.domain.AcquireLockDO;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.domain.ReleaseLockDO;
import com.wuba.wlock.server.domain.RenewLockDO;
import com.wuba.wlock.server.exception.LockException;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.expire.event.LockExpireEvent;
import com.wuba.wlock.server.lock.LockResult;
import com.wuba.wlock.server.lock.protocol.*;
import com.wuba.wlock.server.trace.LockTrace;
import com.wuba.wlock.server.util.IPUtil;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.watch.EventType;
import com.wuba.wlock.server.watch.WatchEvent;
import com.wuba.wpaxos.ProposeResult;
import com.wuba.wpaxos.storemachine.SMCtx;
import com.wuba.wpaxos.config.PaxosTryCommitRet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ReadLockService extends BaseReadWriteLockService {
	private static Logger LOGGER = LoggerFactory.getLogger(ReadLockService.class);

	@Override
	public boolean tryAcquireLock(LockContext lockContext, int groupId) {
		if (isMasterRedirect(groupId)) {
			LOGGER.info("i am not master of group {}", groupId);
			AcquireLockRequest acquireLockRequest = new AcquireLockRequest();
			try {
				acquireLockRequest.fromBytes(lockContext.getBuf());
				masterRedirect(lockContext, acquireLockRequest, groupId, lockContext.getChannel());
			} catch (Exception e) {
				LOGGER.info("tryAcquireLock masterRedirect error.", e);
				ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
			}
			return false;
		}
		AcquireLockRequest acquireLockRequest = new AcquireLockRequest();
		try {
			acquireLockRequest.fromBytes(lockContext.getBuf());
		} catch (ProtocolException e) {
			LOGGER.error("try acquire lock error", e);
			ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
			return false;
		}

		long version = 0L;
		String key = acquireLockRequest.getLockKey();
		Optional<ReentrantLockValue> lock = null;
		try {
			lock = lockRepository.getLock(key, groupId);
		} catch (LockException e) {
			LOGGER.error("{} acquire lock key : {} error.", acquireLockRequest.getHost(), key, e);
			ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
			return false;
		}

		/**
		 * 写锁不存在，队列方式获取锁
		 * 写锁存在，如果是自己持有，可以获取读锁
		 *
		 */
		ReentrantLockValue reentrantLockValue;

		if (lock.isPresent() && lock.get().existWriteLock()) {
			reentrantLockValue = lock.get();
			LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
			version = reentrantLockValue.getLockVersion();
			if (!lockOwnerInfo.isExpire()) {
				if (lockOwnerInfo.equals(acquireLockRequest.getHost(), acquireLockRequest.getThreadID(), acquireLockRequest.getPid())) {
					AcquireLockDO acquireLockDO = AcquireLockDO.fromRequest(acquireLockRequest, version);
					SMCtx ctx = createCtx();
					byte[] proposeMsg;
					try {
						proposeMsg = acquireLockDO.toBytes();
					} catch (ProtocolException e) {
						LOGGER.error("try acquire lock error", e);
						ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
						return false;
					}
					ProposeResult propose = paxosService.batchPropose(proposeMsg, groupId, ctx, acquireLockRequest.getRegistryKey());
					LockSmCtx lockSmCtx = (LockSmCtx) ctx.getpCtx();
					if (propose.getResult() == PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && lockSmCtx.getLockRet() == LockResult.SUCCESS) {
						LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} version {} groupid {}, acquire lock success ", acquireLockDO.getHost(), acquireLockDO.getPid(), acquireLockDO.getThreadID(), key, lockSmCtx.getFencingToken(), groupId);
						LockOwner lockOwner = new LockOwner(acquireLockRequest.getHost(), acquireLockRequest.getThreadID(), acquireLockRequest.getPid(), lockSmCtx.getFencingToken());
						LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), acquireLockRequest);
						ClientManager.getInstance().addLockClient(key, lockClient, groupId, lockContext.getChannel());
						ackAcquireLock(lockContext.getChannel(), acquireLockRequest, lockOwner, ResponseStatus.SUCCESS);
						expireStrategyFactory.addExpireEvent(new LockExpireEvent(lockSmCtx.getExpireTime(), acquireLockDO.getLockKey(),
								groupId, lockSmCtx.getFencingToken(), acquireLockDO.getLockType(), acquireLockDO.getOpcode(), acquireLockDO.getHost(), acquireLockDO.getThreadID(), acquireLockDO.getPid()));
						traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.ACQUIRE_LOCK, IPUtil.getIpStr(acquireLockDO.getHost()), acquireLockDO.getThreadID(),
								acquireLockRequest.getPid(), acquireLockRequest.getLockKey(), lockSmCtx.getFencingToken(), acquireLockRequest.getRegistryKey(), lockSmCtx.getExpireTime(), LockCodeEnum.Read_Lock));

						return true;
					}
					ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
					return false;
				}

				if (acquireLockRequest.isBlocked()) {
					LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {}  groupid {}, lock exist wait.", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
					LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), acquireLockRequest);
					WatchEvent watchEvent = watchService.genWatchEvent(acquireLockRequest, lockClient, reentrantLockValue.getLockVersion());
					watchService.addWatchEvent(key, watchEvent, groupId);
					ClientManager.getInstance().addLockClient(key,lockClient,groupId,lockContext.getChannel());
					ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.LOCK_WAIT);
				} else {
					LOGGER.debug("ip :{} pid {} threadid {} acquire get lock key {} groupid {}, lock exist return occupied.", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
					ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.LOCK_OCCUPIED);
				}
				return false;
			}
		}

		expireDeleteLock(lock, acquireLockRequest, groupId);

		boolean success = false;
		if (canGetReadLock(lock, acquireLockRequest, groupId)) {
			SMCtx ctx = createCtx();
			byte[] proposeMsg;
			try {
				AcquireLockDO acquireLockDO = AcquireLockDO.fromRequest(acquireLockRequest, version);
				proposeMsg = acquireLockDO.toBytes();
			} catch (ProtocolException e) {
				LOGGER.error("try acquire lock error", e);
				ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
				return false;
			}

			ProposeResult propose = paxosService.batchPropose(proposeMsg, groupId, ctx, acquireLockRequest.getRegistryKey());
			LockSmCtx lockSmCtx = (LockSmCtx) ctx.getpCtx();
			if (propose.getResult() == PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && lockSmCtx.getLockRet() == LockResult.SUCCESS) {
				LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} version {} groupid {}, acquire lock success ", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, lockSmCtx.getFencingToken(), groupId);
				LockOwner lockOwner = new LockOwner(acquireLockRequest.getHost(), acquireLockRequest.getThreadID(), acquireLockRequest.getPid(), lockSmCtx.getFencingToken());
				lockNotify.lockNotifyUpdate(key, lockOwner, groupId);
				LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), acquireLockRequest);
				ClientManager.getInstance().addLockClient(key, lockClient, groupId, lockContext.getChannel());
				ackAcquireLock(lockContext.getChannel(), acquireLockRequest, lockOwner, ResponseStatus.SUCCESS);
				traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.ACQUIRE_LOCK, IPUtil.getIpStr(acquireLockRequest.getHost()), acquireLockRequest.getThreadID(),
						acquireLockRequest.getPid(), acquireLockRequest.getLockKey(), lockSmCtx.getFencingToken(), acquireLockRequest.getRegistryKey(), lockSmCtx.getExpireTime(), LockCodeEnum.Read_Lock));
				success = true;
			} else {
				LOGGER.error("ip :{} pid {} threadid {} acquire lock key {} groupid {}, acquire lock failed ", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
				ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
				success = false;
			}
		} else {
			if (acquireLockRequest.isBlocked()) {
				LockClient newLockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), acquireLockRequest);
				WatchEvent newWatchEvent = watchService.genWatchEvent(acquireLockRequest, newLockClient, 0);
				watchService.addWatchEvent(key, newWatchEvent, groupId);
				ClientManager.getInstance().addLockClient(key, newLockClient, groupId, lockContext.getChannel());
				LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} groupid {}, lock wait", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
				ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.LOCK_WAIT);
			} else {
				LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} groupid {}, lock occupied", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
				ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.LOCK_OCCUPIED);
			}

			success = false;
		}

		trySnatchLock(key, groupId, version, acquireLockRequest.getRegistryKey());
		return success;
	}

	private boolean canGetReadLock(Optional<ReentrantLockValue> lock, AcquireLockRequest acquireLockRequest, int groupId) {
		List<WatchEvent> watchEvents = watchService.getWatchEvents(acquireLockRequest.getLockKey(), groupId);
		if (watchEvents != null && !watchEvents.isEmpty()) {
			for (WatchEvent watchEvent: watchEvents) {
				if (watchEvent.getOpcode() == OpcodeEnum.ReadWriteOpcode.WRITE.getValue()) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean tryReleaseLock(LockContext lockContext, int groupId) {
		if (isMasterRedirect(groupId)) {
			LOGGER.info("i am not master of group {}", groupId);
			ReleaseLockRequest releaseLockRequest = new ReleaseLockRequest();
			try {
				releaseLockRequest.fromBytes(lockContext.getBuf());
				masterRedirect(lockContext, releaseLockRequest, groupId, lockContext.getChannel());
			} catch (Exception e) {
				LOGGER.error("try release lock masterRedirect error", e);
				ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.ERROR);
			}
			return false;
		}
		ReleaseLockRequest releaseLockRequest = new ReleaseLockRequest();
		try {
			releaseLockRequest.fromBytes(lockContext.getBuf());
		} catch (ProtocolException e) {
			LOGGER.error("try release lock error", e);
			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.ERROR);
			return false;
		}
		String key = releaseLockRequest.getLockKey();
		Optional<ReentrantLockValue> lock = null;
		long version = 0L;
		try {
			lock = lockRepository.getLock(key, groupId);
		} catch (LockException e) {
			LOGGER.error("{} release lock key : {} error.", releaseLockRequest.getHost(), key, e);
			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.ERROR);
			return false;
		}
		if (!lock.isPresent() || !lock.get().existReadLock(releaseLockRequest.getHost(), releaseLockRequest.getThreadID(), releaseLockRequest.getPid())) {
			LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, key not exist return lock deleted ", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.LOCK_DELETED);
			trySnatchLock(key, groupId, version, releaseLockRequest.getRegistryKey());
			return false;
		}
		ReentrantLockValue reentrantLockValue = lock.get();
		LockOwnerInfo lockOwnerInfo = reentrantLockValue.getReadLockOwner(releaseLockRequest.getHost(), releaseLockRequest.getThreadID(), releaseLockRequest.getPid());
		if (!lockOwnerInfo.isExpire()) {
			version = lockOwnerInfo.getLockVersion();
			if (version != releaseLockRequest.getFencingToken()) {
				LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, key exist and verison {} {} error", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId, releaseLockRequest.getFencingToken(), reentrantLockValue.getLockVersion());
				ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.TOKEN_ERROR);
				return false;
			}
			SMCtx ctx = createCtx();
			ReleaseLockDO releaseLockDO = ReleaseLockDO.fromRequest(releaseLockRequest);
			byte[] proposeMsg;
			try {
				proposeMsg = releaseLockDO.toBytes();
			} catch (ProtocolException e) {
				LOGGER.error("try release lock error", e);
				ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.ERROR);
				return false;
			}
			ProposeResult propose = paxosService.batchPropose(proposeMsg, groupId, ctx, releaseLockRequest.getRegistryKey());
			LockSmCtx lockSmCtx = (LockSmCtx) ctx.getpCtx();
			if (propose.getResult() == PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && lockSmCtx.getLockRet() == LockResult.SUCCESS) {
				LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, success", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
				ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.SUCCESS);
				LockClient lockClient = ClientManager.getInstance().getLockOwnerClient(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid()), groupId);
				ClientManager.getInstance().removeLockClient(key, lockClient, groupId);
				trySnatchLock(key, groupId, version, releaseLockRequest.getRegistryKey());
				traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.RELEASE_LOCK, IPUtil.getIpStr(lockOwnerInfo.getIp()), lockOwnerInfo.getThreadId(),
						lockOwnerInfo.getPid(), releaseLockRequest.getLockKey(), version, releaseLockRequest.getRegistryKey(), -1, LockCodeEnum.Read_Lock));

				return true;
			}
			LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, failed", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.ERROR);
			return false;
		} else {
			LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, key delete", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
			short responseStatus = ResponseStatus.LOCK_DELETED;
			if (lockOwnerInfo.getLockVersion() == releaseLockRequest.getFencingToken()) {
				LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, lock expire version correct. version: {}", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId, releaseLockRequest.getFencingToken());
				responseStatus = ResponseStatus.SUCCESS;
			}

			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, responseStatus);
			lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid()), groupId, EventType.READ_LOCK_EXPIRED);

			expireDeleteLock(lock, releaseLockRequest, groupId);

			trySnatchLock(key, groupId, version, releaseLockRequest.getRegistryKey());
			return false;
		}
	}



	@Override
	public boolean tryRenewLock(LockContext lockContext, int groupId) {
		if (isMasterRedirect(groupId)) {
			LOGGER.info("i am not master of group {}", groupId);
			RenewLockRequest renewLockRequest = new RenewLockRequest();
			try {
				renewLockRequest.fromBytes(lockContext.getBuf());
				masterRedirect(lockContext, renewLockRequest, groupId, lockContext.getChannel());
			} catch (Exception e) {
				LOGGER.error("try renew lock masterRedirect error.", e);
				ackRenewLock(lockContext.getChannel(), renewLockRequest,  ResponseStatus.ERROR);
			}
			return false;
		}
		RenewLockRequest renewLockRequest = new RenewLockRequest();
		try {
			renewLockRequest.fromBytes(lockContext.getBuf());
		} catch (ProtocolException e) {
			LOGGER.error("try renew lock error", e);
			ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.ERROR);
			return false;
		}
		String key = renewLockRequest.getLockKey();
		Optional<ReentrantLockValue> lock = null;
		try {
			lock = lockRepository.getLock(key, groupId);
		} catch (LockException e) {
			LOGGER.error("{} get lock key : {} error.", renewLockRequest.getHost(), key, e);
			ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.ERROR);
			return false;
		}
		long version = 0L;
		if (!lock.isPresent() || !lock.get().existReadLock(renewLockRequest.getHost(), renewLockRequest.getThreadID(), renewLockRequest.getPid())) {
			LOGGER.debug("ip :{} pid {} threadid {} renew lock key {}  groupid {}, key not exist,return key deleted", renewLockRequest.getHost(), renewLockRequest.getPid(), renewLockRequest.getThreadID(), key, groupId);
			ackRenewLock(lockContext.getChannel(), renewLockRequest,  ResponseStatus.LOCK_DELETED);
			trySnatchLock(key, groupId, version, renewLockRequest.getRegistryKey());
			return false;
		}
		ReentrantLockValue reentrantLockValue = lock.get();
		LockOwnerInfo lockOwnerInfo = reentrantLockValue.getReadLockOwner(renewLockRequest.getHost(), renewLockRequest.getThreadID(), renewLockRequest.getPid());
		version = lockOwnerInfo.getLockVersion();
		if (!lockOwnerInfo.isExpire()) {
			if (version != renewLockRequest.getFencingToken()) {
				LOGGER.debug("ip :{} pid {} threadid {} renew lock key {}  groupid {}, key exist and version {} {} error", renewLockRequest.getHost(), renewLockRequest.getPid(), renewLockRequest.getThreadID(), key, groupId, renewLockRequest.getFencingToken(), reentrantLockValue.getLockVersion());
				ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.TOKEN_ERROR);
				return false;
			}
			SMCtx ctx = createCtx();
			RenewLockDO renewLockDO = RenewLockDO.fromRequest(renewLockRequest);
			byte[] proposeMsg;
			try {
				proposeMsg = renewLockDO.toBytes();
			} catch (ProtocolException e) {
				LOGGER.error("try renew lock error", e);
				ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.ERROR);
				return false;
			}

			if (renewLockRequest.getExpireMills() == 0) {
				// master切换时，acquire event补全
				LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), renewLockRequest);
				ClientManager.getInstance().addLockClient(key, lockClient, groupId, lockContext.getChannel());
				ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.SUCCESS);
				return true;
			}

			ProposeResult propose = paxosService.batchPropose(proposeMsg, groupId, ctx, renewLockRequest.getRegistryKey());
			LockSmCtx lockSmCtx = (LockSmCtx) ctx.getpCtx();
			if (propose.getResult() == PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && lockSmCtx.getLockRet() == LockResult.SUCCESS) {
				LOGGER.debug("ip :{} pid {} threadid {} renew lock key {}  groupid {}, success. next expiretime is {}.", renewLockRequest.getHost(), renewLockRequest.getPid(), renewLockRequest.getThreadID(), key, groupId, lockSmCtx.getExpireTime());
				ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.SUCCESS);
				expireStrategyFactory.addExpireEvent(new LockExpireEvent(lockSmCtx.getExpireTime(), renewLockDO.getLockKey(),
						groupId, lockSmCtx.getFencingToken(), renewLockDO.getLockType(), renewLockDO.getOpcode(), renewLockDO.getHost(), renewLockDO.getThreadID(), renewLockDO.getPid()));
				LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), renewLockRequest);
				ClientManager.getInstance().addLockClient(key, lockClient, groupId, lockContext.getChannel());
				traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.RENEW_LOCK, IPUtil.getIpStr(lockOwnerInfo.getIp()), lockOwnerInfo.getThreadId(),
						lockOwnerInfo.getPid(), renewLockRequest.getLockKey(), version, renewLockRequest.getRegistryKey(), lockSmCtx.getExpireTime(), LockCodeEnum.Read_Lock));

				return true;
			}
			ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.ERROR);
			return false;
		} else {
			LOGGER.debug("ip :{} pid {} threadid {} renew lock key {}  groupid {}, key is expire , return delete", renewLockRequest.getHost(), renewLockRequest.getPid(), renewLockRequest.getThreadID(), key, groupId);
			ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.LOCK_DELETED);
			lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid()), groupId, EventType.READ_LOCK_EXPIRED);
			expireDeleteLock(lock, renewLockRequest, groupId);
			trySnatchLock(key, groupId, version, renewLockRequest.getRegistryKey());
			return false;
		}
	}

	@Override
	public boolean tryDeleteLock(DeleteLockRequest deleteLockRequest, int groupId) {
		if (isMasterRedirect(groupId)) {
			LOGGER.info("i am not master of group {}", groupId);
			return false;
		}
		String key = deleteLockRequest.getLockKey();
		Optional<ReentrantLockValue> lock = null;
		try {
			lock = lockRepository.getLock(key, groupId);
		} catch (LockException e) {
			LOGGER.error("{} get lock key : {} error.", deleteLockRequest.getHost(), key, e);
			return false;
		}
		long version = 0L;
		if (!lock.isPresent() || !lock.get().existReadLock(deleteLockRequest.getHost(), deleteLockRequest.getThreadID(), deleteLockRequest.getPid())) {
			LOGGER.debug("delete key {} is not exist", key);
			trySnatchLock(key, groupId, version, deleteLockRequest.getRegistryKey());
			return false;
		}
		ReentrantLockValue reentrantLockValue = lock.get();
		LockOwnerInfo lockOwnerInfo = reentrantLockValue.getReadLockOwner(deleteLockRequest.getHost(), deleteLockRequest.getThreadID(), deleteLockRequest.getPid());
		version = lockOwnerInfo.getLockVersion();
		if (lockOwnerInfo.isExpire()) {
			lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version), groupId, EventType.READ_LOCK_EXPIRED);
			SMCtx ctx = createCtx();
			ProposeResult result = proposeDeleteKey(deleteLockRequest, groupId, ctx, lockOwnerInfo, LockTypeEnum.readWriteReentrantLock.getValue(), OpcodeEnum.ReadWriteOpcode.READ.getValue());
			trySnatchLock(key, groupId, version, deleteLockRequest.getRegistryKey());
			LockSmCtx lockSmCtx = (LockSmCtx) ctx.getpCtx();
			if (result != null && result.getResult() == PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet()
					&& lockSmCtx != null && lockSmCtx.getLockRet() == LockResult.SUCCESS) {
				return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean tryGetLock(LockContext lockContext, int groupId) {
		if (isMasterRedirect(groupId)) {
			LOGGER.info("i am not master of group {}", groupId);
			GetLockRequest getLockRequest = new GetLockRequest();
			try {
				getLockRequest.fromBytes(lockContext.getBuf());
				masterRedirect(lockContext, getLockRequest, groupId, lockContext.getChannel());
			} catch (Exception e) {
				LOGGER.error("try get lock masterRedirect error.", e);
				ackGetLock(lockContext.getChannel(), getLockRequest, null, ResponseStatus.ERROR);
			}
			return false;
		}
		GetLockRequest getLockRequest = new GetLockRequest();
		try {
			getLockRequest.fromBytes(lockContext.getBuf());
		} catch (ProtocolException e) {
			LOGGER.error("try get lock error", e);
			ackGetLock(lockContext.getChannel(), getLockRequest, null, ResponseStatus.ERROR);
			return false;
		}
		long version = 0L;
		String key = getLockRequest.getLockKey();
		Optional<ReentrantLockValue> lock = null;
		try {
			lock = lockRepository.getLock(key, groupId);
			if (!lock.isPresent() || !lock.get().existReadLock(getLockRequest.getHost(), getLockRequest.getThreadID(), getLockRequest.getPid())) {
				ackGetLock(lockContext.getChannel(), getLockRequest, null, ResponseStatus.LOCK_DELETED);
				trySnatchLock(key, groupId, version, getLockRequest.getRegistryKey());
				return false;
			}
			ReentrantLockValue reentrantLockValue = lock.get();
			LockOwnerInfo lockOwnerInfo = reentrantLockValue.getReadLockOwner(getLockRequest.getHost(), getLockRequest.getThreadID(), getLockRequest.getPid());
			version = lockOwnerInfo.getLockVersion();
			if (!lockOwnerInfo.isExpire()) {
				LockOwner lockOwner = new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version);
				ackGetLock(lockContext.getChannel(), getLockRequest, lockOwner, ResponseStatus.SUCCESS);
				return true;
			} else {
				ackGetLock(lockContext.getChannel(), getLockRequest, null, ResponseStatus.LOCK_DELETED);
				lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version), groupId, EventType.READ_LOCK_EXPIRED);
				proposeDeleteKey(getLockRequest, groupId, lockOwnerInfo, LockTypeEnum.readWriteReentrantLock.getValue(), OpcodeEnum.ReadWriteOpcode.READ.getValue());
				trySnatchLock(key, groupId, version, getLockRequest.getRegistryKey());
				return false;
			}
		} catch (LockException e) {
			LOGGER.error("{} get lock key : {} error.", getLockRequest.getHost(), key);
			LOGGER.error(e.getMessage(), e);
			ackGetLock(lockContext.getChannel(), getLockRequest, null, ResponseStatus.ERROR);
			return false;
		}
	}

}

