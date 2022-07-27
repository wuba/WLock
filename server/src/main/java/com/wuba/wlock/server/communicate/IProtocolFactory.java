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
package com.wuba.wlock.server.communicate;

import com.wuba.wlock.server.communicate.protocol.*;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.watch.NotifyEvent;
import com.wuba.wpaxos.comm.NodeInfo;

public interface IProtocolFactory {

	AcquireLockResponse createAcquireRes(AcquireLockRequest acquireLockRequest, short status, LockOwner lockOwner);

	WatchLockResponse createWatchLockRes(WatchLockRequest watchLockRequest, short status);

	RenewLockResponse createRenewLockRes(RenewLockRequest renewLockRequest, short status);

	ReleaseLockResponse createReleaseLockRes(ReleaseLockRequest releaseLockRequest, short status);

	GetLockResponse createGetLockRes(GetLockRequest getLockRequest, short status, LockOwner lockOwner);

	HeartbeatResponse createHeartBeatRes(HeartbeatRequest heartbeatRequest, short status);
	
	HeartbeatRequest createHeartBeatReq();

	WLockResponse createMasterRedirectRes(WLockRequest wLockRequest, NodeInfo master);

	EventNotifyRequest createEventNotifyRes(NotifyEvent notifyEvent);
	
	RebootRequest createRebootReq();
}
