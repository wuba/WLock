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
import com.wuba.wlock.server.communicate.constant.AckContext;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.retrans.RetransConfig;
import com.wuba.wlock.server.communicate.retrans.RetransServer;
import com.wuba.wlock.server.communicate.retrans.RetransServerManager;
import com.wuba.wlock.server.communicate.retrans.RetransServerState;
import com.wuba.wlock.server.exception.LockException;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.exception.RetransRuntimeException;
import com.wuba.wlock.server.expire.ExpireStrategyFactory;
import com.wuba.wlock.server.expire.event.LockExpireEvent;
import com.wuba.wlock.server.lock.LockResult;
import com.wuba.wlock.server.lock.protocol.LockCodeEnum;
import com.wuba.wlock.server.lock.protocol.LockOwnerInfo;
import com.wuba.wlock.server.lock.protocol.LockSmCtx;
import com.wuba.wlock.server.lock.protocol.ReentrantLockValue;
import com.wuba.wlock.server.lock.repository.LockRepositoryImpl;
import com.wuba.wlock.server.lock.repository.base.ILockRepository;
import com.wuba.wlock.server.lock.service.LockNotify;
import com.wuba.wlock.server.lock.service.base.ILockNotify;
import com.wuba.wlock.server.service.ILockService;
import com.wuba.wlock.server.trace.LockTrace;
import com.wuba.wlock.server.trace.TraceWorker;
import com.wuba.wlock.server.util.IPUtil;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.watch.IWatchService;
import com.wuba.wlock.server.watch.WatchEvent;
import com.wuba.wlock.server.watch.impl.WatchServiceImpl;
import com.wuba.wlock.server.worker.AckWorker;
import com.wuba.wlock.server.wpaxos.SMID;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.ProposeResult;
import com.wuba.wpaxos.storemachine.SMCtx;
import com.wuba.wpaxos.comm.NodeInfo;
import com.wuba.wpaxos.config.PaxosTryCommitRet;
import com.wuba.wlock.server.communicate.*;
import com.wuba.wlock.server.communicate.protocol.*;
import com.wuba.wlock.server.domain.*;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ReentrantLockService implements ILockService {
	private static Logger LOGGER = LoggerFactory.getLogger(ReentrantLockService.class);
	private static IProtocolFactory protocolFactory = ProtocolFactoryImpl.getInstance();
	private static ILockRepository lockRepository = LockRepositoryImpl.getInstance();
	private static ILockNotify lockNotify = LockNotify.getInstance();
	private static WpaxosService paxosService = WpaxosService.getInstance();
	private static AckWorker ackWorker = AckWorker.getInstance();
	private static IWatchService watchService = WatchServiceImpl.getInstance();
	private static ExpireStrategyFactory expireStrategyFactory = ExpireStrategyFactory.getInstance();
	private static TraceWorker traceWorker = TraceWorker.getInstance();

	private boolean isMasterRedirect(int groupId) {
		return !paxosService.isNoMaster(groupId) && !paxosService.isIMMaster(groupId) && RetransServerManager.getInstance().isMasterNormal(groupId);
	}

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

		ReentrantLockValue reentrantLockValue;
		if (lock.isPresent()) {
			reentrantLockValue = lock.get();
			LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
			version = reentrantLockValue.getLockVersion();
			if (!lockOwnerInfo.isExpire()) {
				if (lock.get().getLockOwnerInfo().equals(acquireLockRequest.getHost(), acquireLockRequest.getThreadID(), acquireLockRequest.getPid())) {
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
								acquireLockRequest.getPid(), acquireLockRequest.getLockKey(), lockSmCtx.getFencingToken(), acquireLockRequest.getRegistryKey(), lockSmCtx.getExpireTime(), LockCodeEnum.Reentrant_Lock));

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
			LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} version {} groupid {}, lock is expire ,propose delete key.", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, reentrantLockValue.getLockVersion(), groupId);
			proposeDeleteKey(acquireLockRequest, groupId, lockOwnerInfo);

			lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid()), groupId);
		}

		// 权重比较,选择优先级高的client获取到锁
		AcquireLockDO acquireLockDO = null;
		boolean isFirstWatch;
		WatchEvent watchEvent = watchService.fetchFirstAcquiredWatchEvent(key, groupId);
		if (watchEvent != null && watchEvent.getWeight() >= acquireLockRequest.getWeight()) {
			acquireLockDO = AcquireLockDO.fromWatchEvent(key, watchEvent, version);
			isFirstWatch = true;
			LOGGER.info("acquire lock by watch events : {}.", watchEvent.getLockClient());
		} else {
			acquireLockDO = AcquireLockDO.fromRequest(acquireLockRequest, version);
			isFirstWatch = false;
		}
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
			if (isFirstWatch) {
				LockClient lockClient = watchEvent.getLockClient();
				LockOwner lockOwner = new LockOwner(lockClient.getcHost(), lockClient.getcThreadID(), lockClient.getcPid(), lockSmCtx.getFencingToken());
				lockNotify.lockNotifyUpdate(key, lockOwner, groupId);
				watchService.removeWatchEvent(key,groupId,watchEvent);
				if (acquireLockRequest.isBlocked()) {
					LockClient newLockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), acquireLockRequest);
					WatchEvent newWatchEvent = watchService.genWatchEvent(acquireLockRequest, newLockClient, lockSmCtx.getFencingToken());
					watchService.addWatchEvent(key, newWatchEvent, groupId);
					ClientManager.getInstance().addLockClient(key, newLockClient, groupId, lockContext.getChannel());
					LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} groupid {}, lock wait", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
					ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.LOCK_WAIT);
				} else {
					LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} groupid {}, lock occupied", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
					ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.LOCK_OCCUPIED);
				}
				traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.ACQUIRE_LOCK, IPUtil.getIpStr(lockClient.getcHost()), lockClient.getcThreadID(),
						lockClient.getcPid(), acquireLockRequest.getLockKey(), lockSmCtx.getFencingToken(), acquireLockRequest.getRegistryKey(), lockSmCtx.getExpireTime(), LockCodeEnum.Reentrant_Lock));
			} else {
				LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} version {} groupid {}, acquire lock success ", acquireLockDO.getHost(), acquireLockDO.getPid(), acquireLockDO.getThreadID(), key, lockSmCtx.getFencingToken(), groupId);
				LockOwner lockOwner = new LockOwner(acquireLockRequest.getHost(), acquireLockRequest.getThreadID(), acquireLockRequest.getPid(), lockSmCtx.getFencingToken());
				lockNotify.lockNotifyUpdate(key, lockOwner, groupId);
				LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), acquireLockRequest);
				ClientManager.getInstance().addLockClient(key, lockClient, groupId, lockContext.getChannel());
				ackAcquireLock(lockContext.getChannel(), acquireLockRequest, lockOwner, ResponseStatus.SUCCESS);
				traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.ACQUIRE_LOCK, IPUtil.getIpStr(acquireLockDO.getHost()), acquireLockDO.getThreadID(),
						acquireLockRequest.getPid(), acquireLockRequest.getLockKey(), lockSmCtx.getFencingToken(), acquireLockRequest.getRegistryKey(), lockSmCtx.getExpireTime(), LockCodeEnum.Reentrant_Lock));
			}
			expireStrategyFactory.addExpireEvent(new LockExpireEvent(lockSmCtx.getExpireTime(), acquireLockDO.getLockKey(),
					groupId, lockSmCtx.getFencingToken(), acquireLockDO.getLockType(), acquireLockDO.getOpcode(), acquireLockDO.getHost(), acquireLockDO.getThreadID(), acquireLockDO.getPid()));
			return true;
		}

		LOGGER.error("ip :{} pid {} threadid {} acquire lock key {} groupid {}, acquire lock failed ", acquireLockRequest.getHost(), acquireLockRequest.getPid(), acquireLockRequest.getThreadID(), key, groupId);
		ackAcquireLock(lockContext.getChannel(), acquireLockRequest, null, ResponseStatus.ERROR);
		return false;
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
		if (!lock.isPresent()) {
			LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, key not exist return lock deleted ", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.LOCK_DELETED);
			trySnatchLock(key, groupId, version, releaseLockRequest.getRegistryKey());
			return false;
		}
		ReentrantLockValue reentrantLockValue = lock.get();
		LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
		version = reentrantLockValue.getLockVersion();
		if (!lockOwnerInfo.isExpire()) {
			if (!lockOwnerInfo.equals(releaseLockRequest.getHost(), releaseLockRequest.getThreadID(), releaseLockRequest.getPid())) {
				LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, key exist and not itself return owner changed", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
				LOGGER.debug("owner is ip :{} pid {} threadid {}", lockOwnerInfo.getIp(), lockOwnerInfo.getPid(), lockOwnerInfo.getThreadId());
				ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.LOCK_CHANGED_OWNER);
				return false;
			}
			if (reentrantLockValue.getLockVersion() != releaseLockRequest.getFencingToken()) {
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
						lockOwnerInfo.getPid(), releaseLockRequest.getLockKey(), reentrantLockValue.getLockVersion(), releaseLockRequest.getRegistryKey(), -1, LockCodeEnum.Reentrant_Lock));

				return true;
			}
			LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, failed", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, ResponseStatus.ERROR);
			return false;
		} else {
			LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, key delete", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId);
			short responseStatus = ResponseStatus.LOCK_DELETED;
			if (reentrantLockValue.getLockVersion() == releaseLockRequest.getFencingToken()) {
				LOGGER.debug("ip :{} pid {} threadid {} release lock key {}  groupid {}, lock expire version correct. version: {}", releaseLockRequest.getHost(), releaseLockRequest.getPid(), releaseLockRequest.getThreadID(), key, groupId, releaseLockRequest.getFencingToken());
				responseStatus = ResponseStatus.SUCCESS;
			}

			ackReleaseLock(lockContext.getChannel(), releaseLockRequest, responseStatus);
			lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid()), groupId);

			proposeDeleteKey(releaseLockRequest, groupId, lockOwnerInfo);

			trySnatchLock(key, groupId, version, releaseLockRequest.getRegistryKey());
			return false;
		}
	}

	private void trySnatchLock(String key, int groupId, long version, String registryKey) {
		WatchEvent watchEvent = watchService.fetchFirstAcquiredWatchEvent(key, groupId);
		if (watchEvent != null) {
			LOGGER.debug("snatch lock,find first watchevent {}, lockClient {}.", watchEvent.getWatchID(), watchEvent.getLockClient());
			AcquireLockDO acquireLockDO = AcquireLockDO.fromWatchEvent(key, watchEvent, version);
			try {
				SMCtx ctx1 = createCtx();
				ProposeResult proposeRes = paxosService.batchPropose(acquireLockDO.toBytes(), groupId, ctx1, registryKey);
				LockSmCtx lockSmCtx = (LockSmCtx) ctx1.getpCtx();
				if (proposeRes.getResult() == PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && lockSmCtx.getLockRet() == LockResult.SUCCESS) {
					LockClient lockClient = watchEvent.getLockClient();
					LockOwner lockOwner = new LockOwner(lockClient.getcHost(), lockClient.getcThreadID(), lockClient.getcPid(), lockSmCtx.getFencingToken());
					lockNotify.lockNotifyUpdate(key, lockOwner, groupId);
					expireStrategyFactory.addExpireEvent(new LockExpireEvent(lockSmCtx.getExpireTime(), acquireLockDO.getLockKey(),
							groupId, lockSmCtx.getFencingToken(), acquireLockDO.getLockType(), acquireLockDO.getOpcode(), acquireLockDO.getHost(), acquireLockDO.getThreadID(), acquireLockDO.getPid()));
					watchService.removeWatchEvent(key, groupId, watchEvent);
					traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.WATCH_LOCK, IPUtil.getIpStr(lockClient.getcHost()), lockClient.getcThreadID(),
							lockClient.getcPid(), acquireLockDO.getLockKey(), lockSmCtx.getFencingToken(), registryKey, lockSmCtx.getExpireTime(), LockCodeEnum.Reentrant_Lock));
				}
			} catch (ProtocolException e) {
				LOGGER.error("trySnatchLock error.", e);
			}
		} else {
			lockNotify.lockNotifyDelete(key, groupId);
		}
	}

	private void ackReleaseLock(Channel channel, ReleaseLockRequest releaseLockRequest, short responseStatus) {
		ReleaseLockResponse releaseLockResponse = protocolFactory.createReleaseLockRes(releaseLockRequest, responseStatus);
		AckContext ackContext = new AckContext();
		ackContext.setChannel(channel);
		try {
			ackContext.setBuf(releaseLockResponse.toBytes());
		} catch (ProtocolException e) {

		}
		ackWorker.offer(ackContext);
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
				ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.ERROR);
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
		if (!lock.isPresent()) {
			LOGGER.debug("ip :{} pid {} threadid {} renew lock key {}  groupid {}, key not exist,return key deleted", renewLockRequest.getHost(), renewLockRequest.getPid(), renewLockRequest.getThreadID(), key, groupId);
			ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.LOCK_DELETED);
			trySnatchLock(key, groupId, version, renewLockRequest.getRegistryKey());
			return false;
		}
		ReentrantLockValue reentrantLockValue = lock.get();
		LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
		version = reentrantLockValue.getLockVersion();
		if (!lockOwnerInfo.isExpire()) {
			if (!lockOwnerInfo.equals(renewLockRequest.getHost(), renewLockRequest.getThreadID(), renewLockRequest.getPid())) {
				LOGGER.debug("ip :{} pid {} threadid {} renew lock key {}  groupid {}, key exist and not itself return owner changed", renewLockRequest.getHost(), renewLockRequest.getPid(), renewLockRequest.getThreadID(), key, groupId);
				LOGGER.debug("owner is ip :{} pid {} threadid {}", lockOwnerInfo.getIp(), lockOwnerInfo.getPid(), lockOwnerInfo.getThreadId());
				ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.LOCK_CHANGED_OWNER);
				return false;
			}
			if (reentrantLockValue.getLockVersion() != renewLockRequest.getFencingToken()) {
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
				ackRenewLock(lockContext.getChannel(), renewLockRequest,  ResponseStatus.ERROR);
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
				expireStrategyFactory.addExpireEvent(new LockExpireEvent(lockSmCtx.getExpireTime(), renewLockDO.getLockKey(), groupId, lockSmCtx.getFencingToken(), renewLockDO.getLockType(), renewLockDO.getOpcode(), renewLockDO.getHost(), renewLockDO.getThreadID(), renewLockDO.getPid()));
				LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), renewLockRequest);
				ClientManager.getInstance().addLockClient(key, lockClient, groupId, lockContext.getChannel());
				traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.RENEW_LOCK, IPUtil.getIpStr(lockOwnerInfo.getIp()), lockOwnerInfo.getThreadId(),
						lockOwnerInfo.getPid(), renewLockRequest.getLockKey(), reentrantLockValue.getLockVersion(), renewLockRequest.getRegistryKey(), lockSmCtx.getExpireTime(), LockCodeEnum.Reentrant_Lock));

				return true;
			}
			ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.ERROR);
			return false;
		} else {
			LOGGER.debug("ip :{} pid {} threadid {} renew lock key {}  groupid {}, key is expire , return delete", renewLockRequest.getHost(), renewLockRequest.getPid(), renewLockRequest.getThreadID(), key, groupId);
			ackRenewLock(lockContext.getChannel(), renewLockRequest, ResponseStatus.LOCK_DELETED);
			lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid()), groupId);

			proposeDeleteKey(renewLockRequest, groupId, lockOwnerInfo);

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
		if (!lock.isPresent()) {
			LOGGER.debug("delete key {} is not exist", key);
			trySnatchLock(key, groupId, version, deleteLockRequest.getRegistryKey());
			return false;
		}
		ReentrantLockValue reentrantLockValue = lock.get();
		LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
		version = reentrantLockValue.getLockVersion();
		if (lockOwnerInfo.isExpire()) {
			lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version), groupId);
			SMCtx ctx = createCtx();
			ProposeResult result = proposeDeleteKey(deleteLockRequest, groupId, ctx, lockOwnerInfo);
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
			if (!lock.isPresent() || lock.get().getLockOwnerInfo() == null) {
				ackGetLock(lockContext.getChannel(), getLockRequest, null, ResponseStatus.LOCK_DELETED);
				trySnatchLock(key, groupId, version, getLockRequest.getRegistryKey());
				return false;
			}
			ReentrantLockValue reentrantLockValue = lock.get();
			LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
			version = reentrantLockValue.getLockVersion();
			if (!lockOwnerInfo.isExpire()) {
				LockOwner lockOwner = new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version);
				ackGetLock(lockContext.getChannel(), getLockRequest, lockOwner, ResponseStatus.SUCCESS);
				return true;
			} else {
				ackGetLock(lockContext.getChannel(), getLockRequest, null, ResponseStatus.LOCK_DELETED);
				lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version), groupId);
				proposeDeleteKey(getLockRequest, groupId, lockOwnerInfo);
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

	private void ackGetLock(Channel channel, GetLockRequest getLockRequest, LockOwner lockOwner, short status) {
		GetLockResponse getLockRes = protocolFactory.createGetLockRes(getLockRequest, status, lockOwner);
		AckContext ackContext = new AckContext();
		ackContext.setChannel(channel);
		try {
			ackContext.setBuf(getLockRes.toBytes());
		} catch (ProtocolException e) {

		}
		ackWorker.offer(ackContext);
	}

	@Override
	public boolean watchLock(LockContext lockContext, int groupId) {
		WatchLockRequest watchLockReq = new WatchLockRequest();
		try {
			watchLockReq.fromBytes(lockContext.getBuf());
		} catch (ProtocolException e) {
			LOGGER.error("watch lock error", e);
			ackWatchLock(lockContext.getChannel(), watchLockReq, ResponseStatus.ERROR);
			return false;
		}
		String key = watchLockReq.getLockKey();
		LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), watchLockReq);
		WatchEvent watchEvent = watchService.genWatchEvent(watchLockReq, lockClient, watchLockReq.getFencingToken());
		watchService.addWatchEvent(key, watchEvent, groupId);
		ClientManager.getInstance().addLockClient(key,lockClient,groupId,lockContext.getChannel());
		ackWatchLock(lockContext.getChannel(), watchLockReq, ResponseStatus.SUCCESS);

		long version = 0L;
		Optional<ReentrantLockValue> lock = null;
		try {
			lock = lockRepository.getLock(key, groupId);
			if (!lock.isPresent()) {
				trySnatchLock(key, groupId, version, watchLockReq.getRegistryKey());
				return true;
			}
			ReentrantLockValue reentrantLockValue = lock.get();
			LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
			version = reentrantLockValue.getLockVersion();
			if (lockOwnerInfo.isExpire()) {
				lockNotify.lockNotifyExpired(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version), groupId);
				proposeDeleteKey(watchLockReq, groupId, lockOwnerInfo);
				trySnatchLock(key, groupId, version, watchLockReq.getRegistryKey());
				return true;
			} else if (version > watchLockReq.getFencingToken()) {
				lockNotify.lockNotifyUpdate2(key, new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(), lockOwnerInfo.getPid(), version), groupId);
			}
		} catch (LockException e) {
			LOGGER.error("{} watch lock key : {} error.", watchLockReq.getHost(), key, e);
			return false;
		}
		return true;
	}

	@Override
	public boolean trySnatchLock(TrySnatchLockRequest trySnatchLockRequest, int groupId) {
		return false;
	}

	private void ackWatchLock(Channel channel, WatchLockRequest watchLockRequest, short status) {
		WatchLockResponse watchLockResponse = protocolFactory.createWatchLockRes(watchLockRequest, status);
		AckContext ackContext = new AckContext();
		ackContext.setChannel(channel);
		try {
			ackContext.setBuf(watchLockResponse.toBytes());
		} catch (ProtocolException e) {
		}
		ackWorker.offer(ackContext);
	}

	private void ackAcquireLock(Channel channel, AcquireLockRequest acquireLockRequest, LockOwner lockOwner,
	                            short responseStatus) {
		AcquireLockResponse acquireRes = protocolFactory.createAcquireRes(acquireLockRequest, responseStatus, lockOwner);
		AckContext ackContext = new AckContext();
		ackContext.setChannel(channel);
		try {
			ackContext.setBuf(acquireRes.toBytes());
		} catch (ProtocolException e) {
		}
		ackWorker.offer(ackContext);
	}

	private void proposeDeleteKey(WLockRequest wLockRequest, int groupId, LockOwnerInfo lockOwnerInfo) {
		SMCtx ctx = createCtx();
		proposeDeleteKey(wLockRequest, groupId, ctx, lockOwnerInfo);
	}

	private ProposeResult proposeDeleteKey(WLockRequest wLockRequest, int groupId, SMCtx ctx, LockOwnerInfo lockOwnerInfo) {
		ProposeResult result = null;
		try {
			DeleteLockDO deleteLockDO = new DeleteLockDO(wLockRequest.getLockType(), wLockRequest.getOpcode());
			deleteLockDO.setFencingToken(lockOwnerInfo.getLockVersion());
			deleteLockDO.setProtocolType(ProtocolType.DELETE_LOCK);
			deleteLockDO.setLockKeyLen(wLockRequest.getLockKeyLen());
			deleteLockDO.setLockKey(wLockRequest.getLockKey());
			deleteLockDO.setHost(lockOwnerInfo.getIp());
			deleteLockDO.setPid(lockOwnerInfo.getPid());
			deleteLockDO.setThreadID(lockOwnerInfo.getThreadId());
			result = paxosService.batchPropose(deleteLockDO.toBytes(), groupId, ctx, wLockRequest.getRegistryKey());
			if (result != null && result.getResult() == PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet()) {
				traceWorker.offer(new LockTrace(TimeUtil.getCurrentTimestamp(), ProtocolType.DELETE_LOCK, IPUtil.getIpStr(lockOwnerInfo.getIp()), lockOwnerInfo.getThreadId(),
						lockOwnerInfo.getPid(), wLockRequest.getLockKey(), lockOwnerInfo.getLockVersion(), wLockRequest.getRegistryKey(), -1, LockCodeEnum.Reentrant_Lock));
			}
		} catch (ProtocolException e) {
			LOGGER.error("delete expire key error.", e);
		}
		return result;
	}

	private SMCtx createCtx() {
		SMCtx ctx = new SMCtx();
		ctx.setSmId(SMID.LOCK_SMID.getValue());
		LockSmCtx lockSmCtx = new LockSmCtx();
		ctx.setpCtx(lockSmCtx);
		return ctx;
	}


	private void ackRenewLock(Channel channel, RenewLockRequest renewLockRequest, short status) {
		RenewLockResponse renewLockRes = protocolFactory.createRenewLockRes(renewLockRequest, status);
		AckContext ackContext = new AckContext();
		ackContext.setChannel(channel);
		try {
			ackContext.setBuf(renewLockRes.toBytes());
		} catch (ProtocolException e) {
		}
		ackWorker.offer(ackContext);
	}

	private static void masterRedirect(LockContext lockContext, WLockRequest wLockRequest, int groupId, Channel channel) throws ProtocolException, RetransRuntimeException {
		if (wLockRequest.getRedirectTimes() > RetransConfig.CLIENT_REDIRECT_MAX_TIMES) {
			//多次转发不成功，有可能是客户端与master网络问题，直接转发请求
			RetransServer retranServer = RetransServerManager.getInstance().getRetransServerByGroup(groupId);
			if (retranServer != null && retranServer.getState().equals(RetransServerState.Normal)) {
				lockContext.setSessionId(wLockRequest.getSessionID());
				retranServer.retransRequest(lockContext, wLockRequest);
			} else {
				throw new RetransRuntimeException("retranServer is null, current master : " + WpaxosService.getInstance().getMaster(groupId));
			}
		} else {
			ackMasterRedirect(wLockRequest, groupId, channel);
		}
	}

	private static void ackMasterRedirect(WLockRequest wLockRequest, int groupId, Channel channel) throws ProtocolException {
		NodeInfo master = paxosService.getMaster(groupId);
		WLockResponse wLockResponse = protocolFactory.createMasterRedirectRes(wLockRequest, master);
		wLockResponse.setRedirectTimes(wLockRequest.getRedirectTimes());
		AckContext ackContext = new AckContext();
		ackContext.setChannel(channel);
		ackContext.setBuf(wLockResponse.toBytes());
		ackWorker.offer(ackContext);
	}
}

