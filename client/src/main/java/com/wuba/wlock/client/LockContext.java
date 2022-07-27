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
package com.wuba.wlock.client;

import com.wuba.wlock.client.util.TimeUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LockContext implements Comparable<LockContext>{
	private String lockkey;
	private String lockContextkey;
	private long lockVersion;
	private AtomicInteger aquiredCount = new AtomicInteger(0);
	private InternalLockOption lockOption;
	private int expireMills;
	private long expireTimestamp = Long.MAX_VALUE;
	private AtomicBoolean released = new AtomicBoolean(false);

	public LockContext(String lockkey, String lockContextkey, long lockVersion, int aquiredCount, InternalLockOption lockOption) {
		super();
		this.lockContextkey = lockContextkey;
		this.lockkey = lockkey;
		this.lockVersion = lockVersion;
		this.aquiredCount.set(aquiredCount);
		this.lockOption = lockOption;
	}
	
	public String getLockContextkey() {
		return lockContextkey;
	}

	public void setLockContextkey(String lockContextkey) {
		this.lockContextkey = lockContextkey;
	}

	public String getLockkey() {
		return lockkey;
	}

	public void setLockkey(String lockkey) {
		this.lockkey = lockkey;
	}

	public long getLockVersion() {
		return lockVersion;
	}

	public void setLockVersion(long lockVersion) {
		this.lockVersion = lockVersion;
	}

	public InternalLockOption getLockOption() {
		return lockOption;
	}

	public void setLockOption(InternalLockOption lockOption) {
		this.lockOption = lockOption;
	}

	public int getAquiredCount() {
		return aquiredCount.get();
	}

	public void incrAquiredCount() {
		this.aquiredCount.incrementAndGet();
	}
	public int decrAquiredCount() {
		return this.aquiredCount.decrementAndGet();
	}

	public void setAquiredCount(AtomicInteger aquiredCount) {
		this.aquiredCount = aquiredCount;
	}

	public int getExpireMills() {
		return expireMills;
	}

	public void setExpireMills(int expireMills) {
		this.expireMills = expireMills;
	}

	public long getExpireTimestamp() {
		return expireTimestamp;
	}

	public void setExpireTimestamp(long expireTimestamp) {
		this.expireTimestamp = expireTimestamp;
	}
	
	public void updateExpireTime(int expireTime) {
		this.expireTimestamp = TimeUtil.getCurrentMills() + expireTime;
	}
	
	public boolean isExpired() {
		return this.expireTimestamp < TimeUtil.getCurrentMills() ? true : false;
	}

	public AtomicBoolean getReleased() {
		return released;
	}

	public void setReleased(AtomicBoolean released) {
		this.released = released;
	}
	
	public boolean isReleased() {
		return this.released.get();
	}

	public void updateRealExpireTime() {
		this.lockOption.updateRealExpireTime();
	}

	@Override
	public int compareTo(LockContext context) {
		if (this.equals(context)) {
			return 0;
		}
		long cmp = this.expireTimestamp - context.expireTimestamp;
		if (cmp > 0) {
			return 1;
		} else if (cmp < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lockContextkey == null) ? 0 : lockContextkey.hashCode());
		result = prime * result + (int) (lockVersion ^ (lockVersion >>> 32));
		result = prime * result + ((lockkey == null) ? 0 : lockkey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LockContext other = (LockContext) obj;
		if (lockContextkey == null) {
			if (other.lockContextkey != null) {
				return false;
			}
		} else if (!lockContextkey.equals(other.lockContextkey)) {
			return false;
		}
		if (lockVersion != other.lockVersion) {
			return false;
		}
		if (lockkey == null) {
			if (other.lockkey != null) {
				return false;
			}
		} else if (!lockkey.equals(other.lockkey)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "LockContext [lockkey=" + lockkey + ", lockContextkey=" + lockContextkey + ", lockVersion=" + lockVersion
				+ ", expireTimestamp : " + expireTimestamp + "]";
	}
}
