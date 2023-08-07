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
package com.wuba.wlock.client.protocol.extend;

import com.wuba.wlock.client.InternalLockOption;
import com.wuba.wlock.client.protocol.IProtocolFactory;
import com.wuba.wlock.client.protocol.ProtocolType;
import com.wuba.wlock.client.protocol.ResponseStatus;
import com.wuba.wlock.client.util.InetAddressUtil;
import com.wuba.wlock.client.watch.WatchType;

public class ProtocolFactoryImpl implements IProtocolFactory {

	private static ProtocolFactoryImpl instance = new ProtocolFactoryImpl();

	private ProtocolFactoryImpl() {
	}

	public static ProtocolFactoryImpl getInstance() {
		return instance;
	}

	@Override
	public AcquireLockRequest createAcquireReq(String lockkey, int groupId, InternalLockOption acquireOption) {
		AcquireLockRequest acquireLockReq = new AcquireLockRequest();
		acquireLockReq.setProtocolType(ProtocolType.ACQUIRE_LOCK);
		acquireLockReq.setBlocked((byte) (acquireOption.isWaitAcquire() ? 1 : 0));
		acquireLockReq.setExpireMills((int)acquireOption.getExpireTime());
		acquireLockReq.setFencingToken(acquireOption.getLockversion());
		acquireLockReq.setHost(InetAddressUtil.getIpInt());
		acquireLockReq.setLockKey(lockkey);
		acquireLockReq.setGroupId(groupId);
		acquireLockReq.setLockKeyLen((short) lockkey.length());
		acquireLockReq.setRegistryKey(acquireOption.getRegistryKey());
		acquireLockReq.setRegistryKeyLen((short) acquireOption.getRegistryKey().length());
		acquireLockReq.setThreadID(acquireOption.getThreadID());
		acquireLockReq.setPid(acquireOption.getPID());
		acquireLockReq.setTimeout(acquireOption.getMaxWaitTime());
		acquireLockReq.setWatchID(acquireOption.getWatchID());
		acquireLockReq.setWeight(acquireOption.getWeight());
		acquireLockReq.setLockType(acquireOption.getLockType());
		acquireLockReq.setLockOpcode(acquireOption.getLockOpcode());

		return acquireLockReq;
	}

	@Override
	public WatchLockRequest createWatchLockReq(String lockkey, int groupId, WatchType watchType, InternalLockOption watchLockOption) {
		WatchLockRequest watchLockReq = new WatchLockRequest();
		watchLockReq.setProtocolType(ProtocolType.WATCH_LOCK);
		watchLockReq.setEventType(watchType.getType());
		watchLockReq.setExpireTime((int)watchLockOption.getExpireTime());
		watchLockReq.setFencingToken(watchLockOption.getLockversion());
		watchLockReq.setHost(InetAddressUtil.getIpInt());
		watchLockReq.setLockKey(lockkey);
		watchLockReq.setLockKeyLen((short) lockkey.length());
		watchLockReq.setRegistryKey(watchLockOption.getRegistryKey());
		watchLockReq.setRegistryKeyLen((short) watchLockOption.getRegistryKey().length());
		watchLockReq.setThreadID(watchLockOption.getThreadID());
		watchLockReq.setPid(watchLockOption.getPID());
		watchLockReq.setTimeout(watchLockOption.getMaxWaitTime());
		watchLockReq.setWatchID(watchLockOption.getWatchID());
		watchLockReq.setWeight(watchLockOption.getWeight());
		if (watchType == WatchType.WATCH) {
			watchLockReq.setWaitAcquire((byte) 0);
		} else if (watchType == WatchType.WATCH_AND_ACQUIRE) {
			watchLockReq.setWaitAcquire((byte) 1);
		}
		watchLockReq.setGroupId(groupId);
		watchLockReq.setLockType(watchLockOption.getLockType());
		watchLockReq.setLockOpcode(watchLockOption.getLockOpcode());
		return watchLockReq;
	}

	@Override
	public RenewLockRequest createRenewLockReq(String lockkey, int groupId, String registryKey,
			long fencingtoken, int expireTime, long threadID, int pid, int lockType, int lockOpcode) {
		RenewLockRequest renewLockReq = new RenewLockRequest();
		renewLockReq.setLockType((byte) lockType);
		renewLockReq.setLockOpcode((byte) lockOpcode);
		renewLockReq.setProtocolType(ProtocolType.RENEW_LOCK);
		renewLockReq.setExpireMills(expireTime);
		renewLockReq.setFencingToken(fencingtoken);
		renewLockReq.setHost(InetAddressUtil.getIpInt());
		renewLockReq.setLockKey(lockkey);
		renewLockReq.setLockKeyLen((short) lockkey.length());
		renewLockReq.setRegistryKey(registryKey);
		renewLockReq.setRegistryKeyLen((short) registryKey.length());
		renewLockReq.setThreadID(threadID);
		renewLockReq.setPid(pid);
		renewLockReq.setGroupId(groupId);
		return renewLockReq;
	}

	@Override
	public ReleaseLockRequest createReleaseLockReq(String lockkey, int groupId, String registryKey,
			long fencingtoken, long threadID, int pid, int lockType, int opcode) {
		ReleaseLockRequest releaseLockReq = new ReleaseLockRequest();
		releaseLockReq.setLockType((byte) lockType);
		releaseLockReq.setLockOpcode((byte) opcode);
		releaseLockReq.setProtocolType(ProtocolType.RELEASE_LOCK);
		releaseLockReq.setLockKey(lockkey);
		releaseLockReq.setLockKeyLen((short) lockkey.length());
		releaseLockReq.setRegistryKey(registryKey);
		releaseLockReq.setRegistryKeyLen((short) registryKey.length());
		releaseLockReq.setFencingToken(fencingtoken);
		releaseLockReq.setThreadID(threadID);
		releaseLockReq.setPid(pid);
		releaseLockReq.setHost(InetAddressUtil.getIpInt());
		releaseLockReq.setGroupId(groupId);
		return releaseLockReq;
	}

	@Override
	public UnWatchLockRequest createUnWatchLockReq(String lockkey, int groupId, String registryKey) {

		return null;
	}

	@Override
	public GetLockRequest createGetLockReq(String lockkey, int groupId, String registryKey) {
		GetLockRequest getLockReq = new GetLockRequest();
		getLockReq.setProtocolType(ProtocolType.GET_LOCK);
		getLockReq.setLockKey(lockkey);
		getLockReq.setLockKeyLen((short) lockkey.length());
		getLockReq.setRegistryKey(registryKey);
		getLockReq.setRegistryKeyLen((short) registryKey.length());
		getLockReq.setGroupId(groupId);
		return getLockReq;
	}

	@Override
	public EventNotifyResponse createEventNotifyResponse(
			EventNotifyRequest notifyReq) {
		EventNotifyResponse eventNotifyResp = new EventNotifyResponse();
		eventNotifyResp.setProtocolType(ProtocolType.EVENT_NOTIFY);
		eventNotifyResp.setLockKey(notifyReq.getLockKey());
		eventNotifyResp.setLockKeyLen(notifyReq.getLockKeyLen());
		eventNotifyResp.setSessionID(notifyReq.getSessionID());
		eventNotifyResp.setStatus(ResponseStatus.SUCCESS);

		return eventNotifyResp;
	}

	@Override
	public HeartbeatRequest createHeartbeatRequest() {
		HeartbeatRequest heartbeatReq = new HeartbeatRequest();
		heartbeatReq.setProtocolType(ProtocolType.HEARTBEAT);
		return heartbeatReq;
	}
}