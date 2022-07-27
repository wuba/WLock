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
package com.wuba.wlock.client.lockresult;

import com.wuba.wlock.client.LockOwner;
import com.wuba.wlock.client.protocol.ResponseStatus;


public class AcquireLockResult extends LockResult {
	long lockVersion;
	LockOwner owner;
	
	public AcquireLockResult() {}
	
	public AcquireLockResult(boolean result, long lockVersion, LockOwner owner, short responseStatus) {
		super();
		this.ret = result;
		this.lockVersion = lockVersion;
		this.owner = owner;
		this.responseStatus = responseStatus;
	}

	public long getLockVersion() {
		return lockVersion;
	}

	public void setLockVersion(long lockVersion) {
		this.lockVersion = lockVersion;
	}

	public LockOwner getOwner() {
		return owner;
	}

	public void setOwner(LockOwner owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
		return "AcquireLockResult [ret=" + ret + ", lockVersion=" + lockVersion + ", owner=" + owner + ", responseStatus="
				+ ResponseStatus.toStr(responseStatus) + "]";
	}
}
