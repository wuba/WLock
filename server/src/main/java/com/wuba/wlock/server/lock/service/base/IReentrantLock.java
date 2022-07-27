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
package com.wuba.wlock.server.lock.service.base;

import com.wuba.wlock.server.domain.AcquireLockDO;
import com.wuba.wlock.server.domain.DeleteLockDO;
import com.wuba.wlock.server.domain.ReleaseLockDO;
import com.wuba.wlock.server.domain.RenewLockDO;
import com.wuba.wlock.server.lock.protocol.LockSmCtx;

public interface IReentrantLock extends ILock {


	/**
	 * 加锁
	 *
	 * @param instanceID
	 * @param groupIdx
	 * @param smCtx
	 * @return
	 */
	boolean tryAcquireLock(AcquireLockDO acquireLockDO, long instanceID, int groupIdx, LockSmCtx smCtx);

	/**
	 * 刷新锁
	 * @param renewLockDO
	 * @param groupIdx
	 * @param smCtx
	 * @return
	 */
	boolean renewLock(RenewLockDO renewLockDO, int groupIdx, LockSmCtx smCtx);

	/**
	 * 释放锁
	 *
	 * @param releaseLockDO
	 * @param groupIdx
	 * @param smCtx
	 * @return
	 */
	boolean releaseLock(ReleaseLockDO releaseLockDO, int groupIdx, LockSmCtx smCtx);

	boolean deleteLock(DeleteLockDO deleteLockDO, int groupIdx, LockSmCtx smCtx);
}
