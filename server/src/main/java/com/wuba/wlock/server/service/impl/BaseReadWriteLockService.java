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
import com.wuba.wlock.server.communicate.*;
import com.wuba.wlock.server.communicate.constant.AckContext;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.*;
import com.wuba.wlock.server.communicate.retrans.RetransConfig;
import com.wuba.wlock.server.communicate.retrans.RetransServer;
import com.wuba.wlock.server.communicate.retrans.RetransServerManager;
import com.wuba.wlock.server.communicate.retrans.RetransServerState;
import com.wuba.wlock.server.domain.AcquireLockDO;
import com.wuba.wlock.server.domain.DeleteLockDO;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.exception.LockException;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.exception.RetransRuntimeException;
import com.wuba.wlock.server.expire.ExpireStrategyFactory;
import com.wuba.wlock.server.expire.event.LockExpireEvent;
import com.wuba.wlock.server.lock.LockResult;
import com.wuba.wlock.server.lock.protocol.*;
import com.wuba.wlock.server.lock.repository.LockRepositoryImpl;
import com.wuba.wlock.server.lock.repository.base.ILockRepository;
import com.wuba.wlock.server.lock.service.LockNotify;
import com.wuba.wlock.server.lock.service.base.ILockNotify;
import com.wuba.wlock.server.service.ILockService;
import com.wuba.wlock.server.trace.LockTrace;
import com.wuba.wlock.server.trace.TraceWorker;
import com.wuba.wlock.server.util.IPUtil;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.watch.EventType;
import com.wuba.wlock.server.watch.IWatchService;
import com.wuba.wlock.server.watch.WatchEvent;
import com.wuba.wlock.server.watch.impl.WatchServiceImpl;
import com.wuba.wlock.server.worker.AckWorker;
import com.wuba.wlock.server.wpaxos.SMID;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.ProposeResult;
import com.wuba.wpaxos.comm.NodeInfo;
import com.wuba.wpaxos.config.PaxosTryCommitRet;
import com.wuba.wpaxos.storemachine.SMCtx;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public abstract class BaseReadWriteLockService implements ILockService {
    static Logger LOGGER = LoggerFactory.getLogger(BaseReadWriteLockService.class);
    static IProtocolFactory protocolFactory = ProtocolFactoryImpl.getInstance();
    static ILockRepository lockRepository = LockRepositoryImpl.getInstance();
    static ILockNotify lockNotify = LockNotify.getInstance();
    static WpaxosService paxosService = WpaxosService.getInstance();
    static AckWorker ackWorker = AckWorker.getInstance();
    static IWatchService watchService = WatchServiceImpl.getInstance();
    static ExpireStrategyFactory expireStrategyFactory = ExpireStrategyFactory.getInstance();
    static TraceWorker traceWorker = TraceWorker.getInstance();

    protected boolean isMasterRedirect(int groupId) {
        return !paxosService.isNoMaster(groupId) && !paxosService.isIMMaster(groupId) && RetransServerManager.getInstance().isMasterNormal(groupId);
    }

    protected void expireDeleteLock(Optional<ReentrantLockValue> lock, WLockRequest wLockRequest, int groupId) {
        if (lock.isPresent()) {
            ReentrantLockValue reentrantLockValue = lock.get();
            LockOwnerInfo writeLockOwner = reentrantLockValue.getLockOwnerInfo();
            List<LockOwnerInfo> expireLockOwners = reentrantLockValue.expireLockOwners();
            if (expireLockOwners != null && !expireLockOwners.isEmpty()) {
                for (LockOwnerInfo lockOwnerInfo: expireLockOwners) {
                    LOGGER.debug("ip :{} pid {} threadid {} acquire lock key {} version {} groupid {}, lock is expire ,propose delete key.", lockOwnerInfo.getIp(), lockOwnerInfo.getPid(), lockOwnerInfo.getThreadId(), wLockRequest.getLockKey(), lockOwnerInfo.getLockVersion(), groupId);
                    proposeDeleteKey(wLockRequest, groupId, lockOwnerInfo, LockTypeEnum.readWriteReentrantLock.getValue(),
                            lockOwnerInfo == writeLockOwner ? OpcodeEnum.ReadWriteOpcode.WRITE.getValue() : OpcodeEnum.ReadWriteOpcode.READ.getValue());
                    lockNotify.lockNotifyExpired(wLockRequest.getLockKey(), new LockOwner(lockOwnerInfo.getIp(), lockOwnerInfo.getThreadId(),
                            lockOwnerInfo.getPid()), groupId, lockOwnerInfo == writeLockOwner ? EventType.WRITE_LOCK_EXPIRED: EventType.READ_LOCK_EXPIRED);
                }
            }
        }
    }

    protected void proposeDeleteKey(WLockRequest wLockRequest, int groupId, LockOwnerInfo lockOwnerInfo, int lockType, int opcode) {
        SMCtx ctx = createCtx();
        proposeDeleteKey(wLockRequest, groupId, ctx, lockOwnerInfo, lockType, opcode);
    }

    protected ProposeResult proposeDeleteKey(WLockRequest wLockRequest, int groupId, SMCtx ctx, LockOwnerInfo lockOwnerInfo, int lockType, int opcode) {
        ProposeResult result = null;
        try {
            DeleteLockDO deleteLockDO = new DeleteLockDO((byte) lockType, (byte) opcode);
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
                        lockOwnerInfo.getPid(), wLockRequest.getLockKey(), lockOwnerInfo.getLockVersion(), wLockRequest.getRegistryKey(), -1, LockCodeEnum.getBy((byte) lockType, (byte) opcode)));
            }
        } catch (ProtocolException e) {
            LOGGER.error("delete expire key error.", e);
        }
        return result;
    }

    protected void trySnatchLock(String key, int groupId, long version, String registryKey) {
        try {
            List<WatchEvent> watchEvents = watchService.getWatchEvents(key, groupId);
            if (watchEvents != null) {
                for (WatchEvent watchEvent: watchEvents) {
                    // 判断是否可以获取锁
                    if (isWakeUp(key, groupId, watchEvent)) {
                        boolean success = proposeWatchEvent(key, version, groupId, registryKey, watchEvent);
                        if (!success && watchEvent.getOpcode() == OpcodeEnum.ReadWriteOpcode.WRITE.getValue()) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("trySnatchLock error", e);
        }
    }

    private boolean isWakeUp(String key, int groupId, WatchEvent watchEvent) throws LockException {
        Optional<ReentrantLockValue> lock = lockRepository.getLock(key, groupId);
        if (!lock.isPresent()) {
            return true;
        }

        ReentrantLockValue reentrantLockValue = lock.get();
        if (!reentrantLockValue.existLock()) {
            return true;
        }

        if (watchEvent.getOpcode() == OpcodeEnum.ReadWriteOpcode.WRITE.getValue()) {
            return false;
        }

        if (watchEvent.getOpcode() == OpcodeEnum.ReadWriteOpcode.READ.getValue()) {
            if (reentrantLockValue.existWriteLock()) {
                return false;
            }

            return true;
        }

        return false;
    }

    private boolean proposeWatchEvent(String key, long version, int groupId, String registryKey, WatchEvent watchEvent) throws ProtocolException {
        AcquireLockDO acquireLockDO = AcquireLockDO.fromWatchEvent(key, watchEvent, version);
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
                    lockClient.getcPid(), acquireLockDO.getLockKey(), lockSmCtx.getFencingToken(), registryKey, lockSmCtx.getExpireTime(),  LockCodeEnum.getBy(watchEvent.getLockType(), watchEvent.getOpcode())));
            return true;
        }

        return false;
    }

    @Override
    public boolean watchLock(LockContext lockContext, int groupId) {
        if (isMasterRedirect(groupId)) {
            LOGGER.info("i am not master of group {}", groupId);

            WatchLockRequest watchLockReq = new WatchLockRequest();
            try {
                watchLockReq.fromBytes(lockContext.getBuf());
                masterRedirect(lockContext, watchLockReq, groupId, lockContext.getChannel());
            } catch (Exception e) {
                LOGGER.info("watchLock masterRedirect error.", e);
                ackWatchLock(lockContext.getChannel(), watchLockReq, ResponseStatus.ERROR);
            }
            return false;
        }

        WatchLockRequest watchLockReq = new WatchLockRequest();
        try {
            watchLockReq.fromBytes(lockContext.getBuf());
        } catch (ProtocolException e) {
            LOGGER.error("watchLock error", e);
            ackWatchLock(lockContext.getChannel(), watchLockReq, ResponseStatus.ERROR);
            return false;
        }
        String key = watchLockReq.getLockKey();
        LockClient lockClient = ClientManager.getInstance().createLockClient(key, groupId, lockContext.getChannel(), watchLockReq);
        WatchEvent watchEvent = watchService.genWatchEvent(watchLockReq, lockClient, watchLockReq.getFencingToken());
        watchService.addWatchEvent(key, watchEvent, groupId);
        ClientManager.getInstance().addLockClient(key,lockClient,groupId,lockContext.getChannel());
        ackWatchLock(lockContext.getChannel(), watchLockReq, ResponseStatus.SUCCESS);

        try {
            Optional<ReentrantLockValue> lock = lockRepository.getLock(watchLockReq.getLockKey(), groupId);
            expireDeleteLock(lock, watchLockReq, groupId);
        } catch (LockException e) {
            LOGGER.error("watchLock get lock error", e);
            return false;
        }

        trySnatchLock(watchLockReq.getLockKey(), groupId, 0, watchLockReq.getRegistryKey());
        return true;
    }

    @Override
    public boolean trySnatchLock(TrySnatchLockRequest trySnatchLockRequest, int groupId) {
        if (isMasterRedirect(groupId)) {
            LOGGER.info("i am not master of group {}", groupId);
            return false;
        }
        LOGGER.debug("trySnatchLock lockKey: {}", trySnatchLockRequest.getLockKey());
        try {
            Optional<ReentrantLockValue> lock = lockRepository.getLock(trySnatchLockRequest.getLockKey(), groupId);
            expireDeleteLock(lock, trySnatchLockRequest, groupId);
        } catch (LockException e) {
            LOGGER.error("trySnatchLock get lock error", e);
            return false;
        }

        trySnatchLock(trySnatchLockRequest.getLockKey(), groupId, 0, trySnatchLockRequest.getRegistryKey());
        return true;
    }

    protected SMCtx createCtx() {
        SMCtx ctx = new SMCtx();
        ctx.setSmId(SMID.LOCK_SMID.getValue());
        LockSmCtx lockSmCtx = new LockSmCtx();
        ctx.setpCtx(lockSmCtx);
        return ctx;
    }

    protected void ackReleaseLock(Channel channel, ReleaseLockRequest releaseLockRequest, short responseStatus) {
        ReleaseLockResponse releaseLockResponse = protocolFactory.createReleaseLockRes(releaseLockRequest, responseStatus);
        AckContext ackContext = new AckContext();
        ackContext.setChannel(channel);
        try {
            ackContext.setBuf(releaseLockResponse.toBytes());
        } catch (ProtocolException e) {
        }
        ackWorker.offer(ackContext);
    }

    protected void ackRenewLock(Channel channel, RenewLockRequest renewLockRequest, short status) {
        RenewLockResponse renewLockRes = protocolFactory.createRenewLockRes(renewLockRequest, status);
        AckContext ackContext = new AckContext();
        ackContext.setChannel(channel);
        try {
            ackContext.setBuf(renewLockRes.toBytes());
        } catch (ProtocolException e) {
        }
        ackWorker.offer(ackContext);
    }

    protected static void masterRedirect(LockContext lockContext, WLockRequest wLockRequest, int groupId, Channel channel) throws ProtocolException, RetransRuntimeException {
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

    protected static void ackMasterRedirect(WLockRequest wLockRequest, int groupId, Channel channel) throws ProtocolException {
        NodeInfo master = paxosService.getMaster(groupId);
        WLockResponse wLockResponse = protocolFactory.createMasterRedirectRes(wLockRequest, master);
        wLockResponse.setRedirectTimes(wLockRequest.getRedirectTimes());
        AckContext ackContext = new AckContext();
        ackContext.setChannel(channel);
        ackContext.setBuf(wLockResponse.toBytes());
        ackWorker.offer(ackContext);
    }

    protected void ackWatchLock(Channel channel, WatchLockRequest watchLockRequest, short status) {
        WatchLockResponse watchLockResponse = protocolFactory.createWatchLockRes(watchLockRequest, status);
        AckContext ackContext = new AckContext();
        ackContext.setChannel(channel);
        try {
            ackContext.setBuf(watchLockResponse.toBytes());
        } catch (ProtocolException e) {
        }
        ackWorker.offer(ackContext);
    }


    protected void ackAcquireLock(Channel channel, AcquireLockRequest acquireLockRequest, LockOwner lockOwner,
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

    protected void ackGetLock(Channel channel, GetLockRequest getLockRequest, LockOwner lockOwner, short status) {
        GetLockResponse getLockRes = protocolFactory.createGetLockRes(getLockRequest, status, lockOwner);
        AckContext ackContext = new AckContext();
        ackContext.setChannel(channel);
        try {
            ackContext.setBuf(getLockRes.toBytes());
        } catch (ProtocolException e) {
        }
        ackWorker.offer(ackContext);
    }
}

