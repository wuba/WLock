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
package com.wuba.wlock.client.protocol;

import com.wuba.wlock.client.InternalLockOption;
import com.wuba.wlock.client.watch.WatchType;
import com.wuba.wlock.client.protocol.extend.*;

public interface IProtocolFactory {
	
	AcquireLockRequest createAcquireReq(String lockkey, int groupId, InternalLockOption acquireOption);
	
	WatchLockRequest createWatchLockReq(String lockkey, int groupId, WatchType watchType, InternalLockOption watchLockOption);
	
	RenewLockRequest createRenewLockReq(String lockkey, int groupId, String registryKey, long fencingtoken, int expireTime, long threadID, int pid, int lockType, int lockOpcode);
	
	ReleaseLockRequest createReleaseLockReq(String lockkey, int groupId, String registryKey, long fencingtoken, long threadID, int pid, int lockType, int opcode);
	
	UnWatchLockRequest createUnWatchLockReq(String lockkey, int groupId, String registryKey);
	
	GetLockRequest createGetLockReq(String lockkey, int groupId, String registryKey);
	
	EventNotifyResponse createEventNotifyResponse(EventNotifyRequest notifyReq);
	
	HeartbeatRequest createHeartbeatRequest();
}