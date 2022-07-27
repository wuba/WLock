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
package com.wuba.wlock.server.dispatcher;

import com.wuba.wlock.server.collector.QpsAbandon;
import com.wuba.wlock.server.collector.QpsCounter;
import com.wuba.wlock.server.communicate.IProtocolFactory;
import com.wuba.wlock.server.communicate.ProtocolType;
import com.wuba.wlock.server.communicate.WLockRequest;
import com.wuba.wlock.server.communicate.WLockResponse;
import com.wuba.wlock.server.communicate.constant.AckContext;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.*;
import com.wuba.wlock.server.communicate.retrans.RetransConfig;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.util.ConnManager;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.worker.AckWorker;
import com.wuba.wlock.server.worker.HeartbeatWorker;
import com.wuba.wlock.server.worker.LockWorker;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.comm.NodeInfo;
import com.wuba.wpaxos.utils.ByteConverter;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContextDispatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(ContextDispatcher.class);
	private static ConnManager connManager = ConnManager.getInstance();
	private static HeartbeatWorker heartbeatWorker = HeartbeatWorker.getInstance();
	private static WpaxosService wpaxosService = WpaxosService.getInstance();
	private static AckWorker ackWorker = AckWorker.getInstance();
	private static IProtocolFactory protocolFactory = ProtocolFactoryImpl.getInstance();
	private static LockWorker lockWorker = LockWorker.getInstance();

	private ContextDispatcher() {
	}

	public static void dispatch(LockContext lockContext) {
		try {
			if (lockContext != null) {
				connManager.updateHeartBeat(lockContext.getChannel(), TimeUtil.getCurrentTimestamp());
				connManager.updateRead(lockContext.getChannel(), TimeUtil.getCurrentTimestamp());
				//第六个字节为protocol type
				byte protocolType = lockContext.getBuf()[WLockRequest.PROTOCOL_TYPE_POS];
				if (protocolType == ProtocolType.ACQUIRE_LOCK) {
					dispatchAcquireLock(lockContext);
					QpsCounter.incrServerAcquireQps();
				} else if (protocolType == ProtocolType.GET_LOCK) {
					dispatchGetLock(lockContext);
					QpsCounter.incrServerGetQps();
				} else if (protocolType == ProtocolType.RELEASE_LOCK) {
					dispatchReleaseLock(lockContext);
					QpsCounter.incrServerReleaseQps();
				} else if (protocolType == ProtocolType.RENEW_LOCK) {
					dispatchRenewLock(lockContext);
					QpsCounter.incrServerRenewQps();
				} else if (protocolType == ProtocolType.WATCH_LOCK) {
					dispatchWatchLock(lockContext);
					QpsCounter.incrServerWatchQps();
				} else if (protocolType == ProtocolType.HEARTBEAT) {
					heartbeatWorker.offer(lockContext);
				} else {
					LOGGER.error("receive message illegal, discard it.protocol type : {} from channel : {}", protocolType, lockContext.getChannel().getRemoteAddress());
				}
			}
		} catch (Exception e) {
			LOGGER.error("dispatch message error.", e);
		}
	}

	private static void dispatchWatchLock(LockContext lockContext) throws ProtocolException {
		String registryKey = getRegistryKey(lockContext.getBuf());
		String lockkey = getLockKey(lockContext.getBuf());
		int redirectTimes = getRedirectTimes(lockContext.getBuf());
		lockContext.setLockkey(lockkey);
		int group = getGroupId(lockContext.getBuf());
		if (QpsAbandon.limitSpeed(registryKey,group)) {
			QpsCounter.incrServerAbandonQps();
			QpsCounter.incrGroupAbandonQps(group);
			QpsCounter.incrKeyAnandonQps(registryKey, group);
			return;
		}
		lockContext.setRegistryKey(registryKey);
		if (wpaxosService.isNoMaster(group) || wpaxosService.isIMMaster(group) || redirectTimes > RetransConfig.CLIENT_REDIRECT_MAX_TIMES) {
			lockWorker.offer(lockContext, group);
			QpsCounter.incrGroupWatchQps(group);
			QpsCounter.incrKeyWatchQps(registryKey, group);
		} else {
			WatchLockRequest wLockRequest = new WatchLockRequest();
			wLockRequest.fromBytes(lockContext.getBuf());
			ackMasterRedirect(wLockRequest, group, lockContext.getChannel());
		}
	}

	private static void dispatchRenewLock(LockContext lockContext) throws ProtocolException {
		String registryKey = getRegistryKey(lockContext.getBuf());
		String lockkey = getLockKey(lockContext.getBuf());
		int redirectTimes = getRedirectTimes(lockContext.getBuf());
		lockContext.setLockkey(lockkey);
		int group = getGroupId(lockContext.getBuf());
		if (QpsAbandon.limitSpeed(registryKey,group)) {
			QpsCounter.incrServerAbandonQps();
			QpsCounter.incrGroupAbandonQps(group);
			QpsCounter.incrKeyAnandonQps(registryKey, group);
			return;
		}
		lockContext.setRegistryKey(registryKey);
		if (wpaxosService.isNoMaster(group) || wpaxosService.isIMMaster(group) || redirectTimes > RetransConfig.CLIENT_REDIRECT_MAX_TIMES) {
			lockWorker.offer(lockContext, group);
			QpsCounter.incrGroupRenewQps(group);
			QpsCounter.incrKeyRenewQps(registryKey, group);
		} else {
			RenewLockRequest wLockRequest = new RenewLockRequest();
			wLockRequest.fromBytes(lockContext.getBuf());
			ackMasterRedirect(wLockRequest, group, lockContext.getChannel());
		}
	}

	private static void dispatchReleaseLock(LockContext lockContext) throws ProtocolException {
		String registryKey = getRegistryKey(lockContext.getBuf());
		String lockkey = getLockKey(lockContext.getBuf());
		int redirectTimes = getRedirectTimes(lockContext.getBuf());
		lockContext.setLockkey(lockkey);
		int group = getGroupId(lockContext.getBuf());
		if (QpsAbandon.limitSpeed(registryKey,group)) {
			QpsCounter.incrServerAbandonQps();
			QpsCounter.incrGroupAbandonQps(group);
			QpsCounter.incrKeyAnandonQps(registryKey, group);
			return;
		}
		lockContext.setRegistryKey(registryKey);
		if (wpaxosService.isNoMaster(group) || wpaxosService.isIMMaster(group) || redirectTimes > RetransConfig.CLIENT_REDIRECT_MAX_TIMES) {
			lockWorker.offer(lockContext, group);
			QpsCounter.incrGroupReleaseQps(group);
			QpsCounter.incrKeyReleaseQps(registryKey, group);
		} else {
			ReleaseLockRequest wLockRequest = new ReleaseLockRequest();
			wLockRequest.fromBytes(lockContext.getBuf());
			ackMasterRedirect(wLockRequest, group, lockContext.getChannel());
		}
	}

	private static void dispatchGetLock(LockContext lockContext) throws ProtocolException {
		String registryKey = getRegistryKey(lockContext.getBuf());
		String lockkey = getLockKey(lockContext.getBuf());
		int redirectTimes = getRedirectTimes(lockContext.getBuf());
		lockContext.setLockkey(lockkey);
		int group = getGroupId(lockContext.getBuf());
		if (QpsAbandon.limitSpeed(registryKey,group)) {
			QpsCounter.incrServerAbandonQps();
			QpsCounter.incrGroupAbandonQps(group);
			QpsCounter.incrKeyAnandonQps(registryKey, group);
			return;
		}
		lockContext.setRegistryKey(registryKey);
		if (wpaxosService.isNoMaster(group) || wpaxosService.isIMMaster(group) || redirectTimes > RetransConfig.CLIENT_REDIRECT_MAX_TIMES) {
			lockWorker.offer(lockContext, group);
			QpsCounter.incrGroupGetQps(group);
			QpsCounter.incrKeyGetQps(registryKey, group);
		} else {
			GetLockRequest wLockRequest = new GetLockRequest();
			wLockRequest.fromBytes(lockContext.getBuf());
			ackMasterRedirect(wLockRequest, group, lockContext.getChannel());
		}
	}

	private static void dispatchAcquireLock(LockContext lockContext) throws ProtocolException {
		String registryKey = getRegistryKey(lockContext.getBuf());
		String lockkey = getLockKey(lockContext.getBuf());
		int redirectTimes = getRedirectTimes(lockContext.getBuf());
		lockContext.setLockkey(lockkey);
		int group = getGroupId(lockContext.getBuf());
		if (QpsAbandon.limitSpeed(registryKey,group)) {
			QpsCounter.incrServerAbandonQps();
			QpsCounter.incrGroupAbandonQps(group);
			QpsCounter.incrKeyAnandonQps(registryKey, group);
			return;
		}
		lockContext.setRegistryKey(registryKey);
		if (wpaxosService.isNoMaster(group) || wpaxosService.isIMMaster(group) || redirectTimes > RetransConfig.CLIENT_REDIRECT_MAX_TIMES) {
			lockWorker.offer(lockContext, group);
			QpsCounter.incrGroupAcquireQps(group);
			QpsCounter.incrKeyAcquireQps(registryKey, group);
		} else {
			AcquireLockRequest wLockRequest = new AcquireLockRequest();
			wLockRequest.fromBytes(lockContext.getBuf());
			ackMasterRedirect(wLockRequest, group, lockContext.getChannel());
		}
	}

	private static void ackMasterRedirect(WLockRequest wLockRequest, int groupId, Channel channel) throws ProtocolException {
		NodeInfo master = wpaxosService.getMaster(groupId);
		WLockResponse wLockResponse = protocolFactory.createMasterRedirectRes(wLockRequest, master);
		wLockResponse.setRedirectTimes(wLockRequest.getRedirectTimes());
		AckContext ackContext = new AckContext();
		ackContext.setChannel(channel);
		ackContext.setBuf(wLockResponse.toBytes());
		ackWorker.offer(ackContext);
		LOGGER.info("redirect master {}, isImMaster {}.", master, wpaxosService.isIMMaster(groupId));
	}

	private static String getRegistryKey(byte[] buf) {
		short length = ByteConverter.bytesToShortLittleEndian(buf, WLockRequest.registryKeyLenPos(buf));
		byte[] tmpBuf = new byte[length];
		System.arraycopy(buf, WLockRequest.registryKeyPos(buf), tmpBuf, 0, length);
		String key = new String(tmpBuf);
		return key;
	}

	private static String getLockKey(byte[] buf) {
		short regisKeylength = ByteConverter.bytesToShortLittleEndian(buf, WLockRequest.registryKeyLenPos(buf));
		int lockkeyLenPos = WLockRequest.registryKeyPos(buf) + regisKeylength;
		short lockKeyLength = ByteConverter.bytesToShortLittleEndian(buf, lockkeyLenPos);
		byte[] tmpBuf = new byte[lockKeyLength];
		int lockkeyPos = lockkeyLenPos + 2;
		System.arraycopy(buf, lockkeyPos, tmpBuf, 0, lockKeyLength);
		String key = new String(tmpBuf);
		return key;
	}

	private static int getRedirectTimes(byte[] buf) {
		short redirectTimes = ByteConverter.bytesToShortLittleEndian(buf, WLockRequest.REDIRECT_TIMES_POS);
		return redirectTimes;
	}
	
	public static int getGroupId(byte[] buf) throws ProtocolException {
		int groupId = ByteConverter.bytesToIntLittleEndian(buf, WLockRequest.GROUPID_POS);
		if (groupId >= PaxosConfig.getInstance().getGroupCount()) {
			throw new ProtocolException("groupid illegal, groupId ," + groupId + " group count " + PaxosConfig.getInstance().getGroupCount());
		}
		return groupId;
	}
}
