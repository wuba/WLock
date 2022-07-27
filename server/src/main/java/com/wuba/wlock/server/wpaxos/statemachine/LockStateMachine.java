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
package com.wuba.wlock.server.wpaxos.statemachine;

import com.wuba.wlock.server.communicate.ProtocolType;
import com.wuba.wlock.server.lock.LockResult;
import com.wuba.wlock.server.lock.protocol.LockSmCtx;
import com.wuba.wlock.server.lock.protocol.LockTypeEnum;
import com.wuba.wlock.server.lock.service.ReadWriteLock;
import com.wuba.wlock.server.lock.service.ReentrantLock;
import com.wuba.wlock.server.lock.service.base.IReentrantLock;
import com.wuba.wlock.server.migrate.service.MigrateService;
import com.wuba.wlock.server.service.GroupMetaService;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.wpaxos.checkpoint.CheckpointManager;
import com.wuba.wlock.server.domain.*;
import com.wuba.wpaxos.storemachine.SMCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockStateMachine extends AbstractStateMachine {
	private static final Logger LOGGER = LoggerFactory.getLogger(LockStateMachine.class);
	private static CheckpointManager checkpointManager = CheckpointManager.getInstance();

	private IReentrantLock reentrantLock = ReentrantLock.getInstance();
	private IReentrantLock readWriteLock = ReadWriteLock.getInstance();

	public LockStateMachine(int groupIdx, int smID, boolean needCheckpoint) {
		super(groupIdx, smID, needCheckpoint);
	}

	@Override
	public boolean execute(int groupIdx, long instanceID, byte[] paxosValue, SMCtx smCtx) {
		if(!MigrateService.getInstance().isSyncToMigratePoint(groupIdx)) {
			LOGGER.error("LockStateMachine not Sync To Migrate Point. groupId: {} instanceId: {}", groupIdx, instanceID);
			return false;
		}

		long start = TimeUtil.getCurrentTimestamp();
		byte protocolType = paxosValue[BaseLockDO.PROYOCOL_TYPE_OFFSET];
		LockSmCtx lockSmCtx = null;
		if (smCtx != null && smCtx.getpCtx() != null) {
			lockSmCtx = (LockSmCtx) smCtx.getpCtx();
		}
		if (lockSmCtx != null) {
			lockSmCtx.setLockRet(LockResult.SUCCESS);
		}
		boolean result = false;
		if (ProtocolType.ACQUIRE_LOCK == protocolType) {
			AcquireLockDO acquireLockDO;
			acquireLockDO = AcquireLockDO.fromBytes(paxosValue);
			long groupVersion = GroupMetaService.getInstance().getGroupVersion(groupIdx);
			long lockVersion = (groupVersion << 48) + instanceID;
			result = reentrantLock(acquireLockDO.getLockType()).tryAcquireLock(acquireLockDO, lockVersion, groupIdx, lockSmCtx);
		} else if (ProtocolType.RENEW_LOCK == protocolType) {
			RenewLockDO renewLockDO;
			renewLockDO = RenewLockDO.fromBytes(paxosValue);
			result = reentrantLock(renewLockDO.getLockType()).renewLock(renewLockDO, groupIdx, lockSmCtx);
		} else if (ProtocolType.RELEASE_LOCK == protocolType) {
			ReleaseLockDO releaseLockDO;
			releaseLockDO = ReleaseLockDO.fromBytes(paxosValue);
			result = reentrantLock(releaseLockDO.getLockType()).releaseLock(releaseLockDO, groupIdx, lockSmCtx);
		} else if (ProtocolType.DELETE_LOCK == protocolType) {
			DeleteLockDO deleteLockDO;
			deleteLockDO = DeleteLockDO.fromBytes(paxosValue);
			result = reentrantLock(deleteLockDO.getLockType()).deleteLock(deleteLockDO, groupIdx, lockSmCtx);
		} else if (lockSmCtx != null) {
			LOGGER.error("unknow protocol {}", protocolType);
			lockSmCtx.setLockRet(LockResult.PROTOCOL_TYPE_ERROR);
		}
		checkpointManager.executeForCheckpoint(groupIdx, super.getSMID(), instanceID, paxosValue);
		long cost = TimeUtil.getCurrentTimestamp()-start;
		if(cost>200){
			LOGGER.error("lock sm groupid {} execute cost {}",groupIdx,cost);
		}
		return result;
	}

	private IReentrantLock reentrantLock(byte lockType) {
		if (LockTypeEnum.reentrantLock.getValue() == lockType) {
			return reentrantLock;
		} else if (LockTypeEnum.readWriteReentrantLock.getValue() == lockType) {
			return readWriteLock;
		}
		throw new IllegalArgumentException("lock type not eixt");
	}

	@Override
	public byte[] beforePropose(int groupIdx, byte[] sValue) {
		return new byte[0];
	}

	@Override
	public boolean needCallBeforePropose() {
		return false;
	}
}
