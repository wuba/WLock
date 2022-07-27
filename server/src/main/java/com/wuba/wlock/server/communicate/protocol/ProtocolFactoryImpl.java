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
package com.wuba.wlock.server.communicate.protocol;

import com.wuba.wlock.server.communicate.*;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.util.OpaqueGenerator;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.watch.NotifyEvent;
import com.wuba.wpaxos.comm.NodeInfo;

import java.util.Map;

public class ProtocolFactoryImpl implements IProtocolFactory {

	private ProtocolFactoryImpl() {
	}

	private static IProtocolFactory protocolFactory = new ProtocolFactoryImpl();

	public static IProtocolFactory getInstance() {
		return protocolFactory;
	}

	@Override
	public AcquireLockResponse createAcquireRes(AcquireLockRequest acquireLockRequest, short status, LockOwner lockOwner) {
		AcquireLockResponse acquireLockResponse = new AcquireLockResponse();
		if (lockOwner != null && status == ResponseStatus.SUCCESS) {
			acquireLockResponse.setFencingToken(lockOwner.getLockversion());
			acquireLockResponse.setOwnerHost(lockOwner.getIp());
			acquireLockResponse.setThreadID(lockOwner.getThreadId());
			acquireLockResponse.setPid(lockOwner.getPid());
		}
		acquireLockResponse.setStatus(status);
		acquireLockResponse.setVersion(acquireLockRequest.getVersion());
		acquireLockResponse.setCommandType(acquireLockRequest.getCommandType());
		acquireLockResponse.setProtocolType(ProtocolType.ACQUIRE_LOCK);
		acquireLockResponse.setSessionID(acquireLockRequest.getSessionID());
		acquireLockResponse.setLockKeyLen(acquireLockRequest.getLockKeyLen());
		acquireLockResponse.setLockKey(acquireLockRequest.getLockKey());
		Map<String, String> propertiesMap = acquireLockResponse.getPropertiesMap();
		if (propertiesMap != null) {
			acquireLockResponse.setPropertiesMap(propertiesMap);
		}
		return acquireLockResponse;
	}

	@Override
	public WatchLockResponse createWatchLockRes(WatchLockRequest watchLockRequest, short status) {
		WatchLockResponse watchLockResponse = new WatchLockResponse();
		watchLockResponse.setStatus(status);
		watchLockResponse.setVersion(watchLockRequest.getVersion());
		watchLockResponse.setCommandType(watchLockRequest.getCommandType());
		watchLockResponse.setProtocolType(ProtocolType.WATCH_LOCK);
		watchLockResponse.setSessionID(watchLockRequest.getSessionID());
		watchLockResponse.setLockKeyLen(watchLockRequest.getLockKeyLen());
		watchLockResponse.setLockKey(watchLockRequest.getLockKey());
		Map<String, String> propertiesMap = watchLockResponse.getPropertiesMap();
		if (propertiesMap != null) {
			watchLockResponse.setPropertiesMap(propertiesMap);
		}
		return watchLockResponse;
	}

	@Override
	public RenewLockResponse createRenewLockRes(RenewLockRequest renewLockRequest, short status) {
		RenewLockResponse renewLockResponse = new RenewLockResponse();
		renewLockResponse.setStatus(status);
		renewLockResponse.setVersion(renewLockRequest.getVersion());
		renewLockResponse.setCommandType(renewLockRequest.getCommandType());
		renewLockResponse.setProtocolType(ProtocolType.RENEW_LOCK);
		renewLockResponse.setSessionID(renewLockRequest.getSessionID());
		renewLockResponse.setLockKeyLen(renewLockRequest.getLockKeyLen());
		renewLockResponse.setLockKey(renewLockRequest.getLockKey());
		Map<String, String> propertiesMap = renewLockResponse.getPropertiesMap();
		if (propertiesMap != null) {
			renewLockResponse.setPropertiesMap(propertiesMap);
		}
		return renewLockResponse;
	}

	@Override
	public ReleaseLockResponse createReleaseLockRes(ReleaseLockRequest releaseLockRequest, short status) {
		ReleaseLockResponse releaseLockResponse = new ReleaseLockResponse();
		releaseLockResponse.setStatus(status);
		releaseLockResponse.setVersion(releaseLockRequest.getVersion());
		releaseLockResponse.setCommandType(releaseLockRequest.getCommandType());
		releaseLockResponse.setProtocolType(ProtocolType.RELEASE_LOCK);
		releaseLockResponse.setSessionID(releaseLockRequest.getSessionID());
		releaseLockResponse.setLockKeyLen(releaseLockRequest.getLockKeyLen());
		releaseLockResponse.setLockKey(releaseLockRequest.getLockKey());
		Map<String, String> propertiesMap = releaseLockResponse.getPropertiesMap();
		if (propertiesMap != null) {
			releaseLockResponse.setPropertiesMap(propertiesMap);
		}
		return releaseLockResponse;
	}

	@Override
	public GetLockResponse createGetLockRes(GetLockRequest getLockRequest, short status, LockOwner lockOwner) {
		GetLockResponse getLockResponse = new GetLockResponse();
		if (lockOwner != null && status == ResponseStatus.SUCCESS) {
			getLockResponse.setFencingToken(lockOwner.getLockversion());
			getLockResponse.setOwnerHost(lockOwner.getIp());
			getLockResponse.setOwnerThreadID(lockOwner.getThreadId());
			getLockResponse.setOwnerPID(lockOwner.getPid());
		}
		getLockResponse.setStatus(status);
		getLockResponse.setVersion(getLockRequest.getVersion());
		getLockResponse.setCommandType(getLockRequest.getCommandType());
		getLockResponse.setProtocolType(ProtocolType.GET_LOCK);
		getLockResponse.setSessionID(getLockRequest.getSessionID());
		getLockResponse.setLockKeyLen(getLockRequest.getLockKeyLen());
		getLockResponse.setLockKey(getLockRequest.getLockKey());
		Map<String, String> propertiesMap = getLockResponse.getPropertiesMap();
		if (propertiesMap != null) {
			getLockResponse.setPropertiesMap(propertiesMap);
		}
		return getLockResponse;
	}

	@Override
	public HeartbeatResponse createHeartBeatRes(HeartbeatRequest heartbeatRequest, short status) {
		HeartbeatResponse heartbeatResponse = new HeartbeatResponse();
		heartbeatResponse.setStatus(status);
		heartbeatResponse.setVersion(heartbeatRequest.getVersion());
		heartbeatResponse.setCommandType(heartbeatRequest.getCommandType());
		heartbeatResponse.setProtocolType(ProtocolType.HEARTBEAT);
		heartbeatResponse.setSessionID(heartbeatRequest.getSessionID());
		heartbeatResponse.setLockKeyLen(heartbeatRequest.getLockKeyLen());
		heartbeatResponse.setLockKey(heartbeatRequest.getLockKey());
		Map<String, String> propertiesMap = heartbeatResponse.getPropertiesMap();
		if (propertiesMap != null) {
			heartbeatResponse.setPropertiesMap(propertiesMap);
		}
		return heartbeatResponse;
	}

	@Override
	public WLockResponse createMasterRedirectRes(WLockRequest wLockRequest, NodeInfo master) {
		WLockResponse wLockResponse = new MasterRedirectResponse();
		wLockResponse.setStatus(ResponseStatus.MASTER_REDIRECT);
		wLockResponse.setVersion(wLockRequest.getVersion());
		wLockResponse.setCommandType(wLockRequest.getCommandType());
		wLockResponse.setProtocolType(wLockRequest.getProtocolType());
		wLockResponse.setSessionID(wLockRequest.getSessionID());
		wLockResponse.setLockKeyLen(wLockRequest.getLockKeyLen());
		wLockResponse.setLockKey(wLockRequest.getLockKey());
		Map<String, String> propertiesMap = wLockRequest.getPropertiesMap();
		if (propertiesMap != null) {
			wLockResponse.setPropertiesMap(propertiesMap);
		}
		String masterAddress = master.getIp() + ProtocolConst.IP_PORT_SEPARATOR + master.getPort();
		wLockResponse.putProperty(ProtocolConst.PROPERTY_MASTER_ADDR, masterAddress);
		return wLockResponse;
	}

	@Override
	public EventNotifyRequest createEventNotifyRes(NotifyEvent notifyEvent) {
		EventNotifyRequest eventNotifyReq = new EventNotifyRequest();
		eventNotifyReq.setProtocolType(ProtocolType.EVENT_NOTIFY);
		eventNotifyReq.setEventType(notifyEvent.getEventType());
		eventNotifyReq.setFencingToken(notifyEvent.getLockOwner().getLockversion());
		eventNotifyReq.setHost(notifyEvent.getLockOwner().getIp());
		eventNotifyReq.setThreadID(notifyEvent.getLockOwner().getThreadId());
		eventNotifyReq.setLockKey(notifyEvent.getLockkey());
		eventNotifyReq.setLockKeyLen((short) notifyEvent.getLockkey().length());
		eventNotifyReq.setSessionID(OpaqueGenerator.getOpaque());
		eventNotifyReq.setWatchID(notifyEvent.getWatchID());
		eventNotifyReq.setTimestamp(TimeUtil.getCurrentTimestamp());
		return eventNotifyReq;
	}

	@Override
	public RebootRequest createRebootReq() {
		RebootRequest rebootReq = new RebootRequest();
		rebootReq.setProtocolType(ProtocolType.REBOOT);
		return rebootReq;
	}

	@Override
	public HeartbeatRequest createHeartBeatReq() {
		HeartbeatRequest heartbeatReq = new HeartbeatRequest();
		heartbeatReq.setProtocolType(ProtocolType.HEARTBEAT);
		return heartbeatReq;
	}
}