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

import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.constant.GroupState;
import com.wuba.wlock.server.constant.PaxosState;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.storemachine.SMCtx;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class KeepMasterSM extends AbstractStateMachine {
	private static final Logger LOGGER = LoggerFactory.getLogger(LockStateMachine.class);

	private final WpaxosService wpaxosService = WpaxosService.getInstance();

	private ScheduledExecutorService executorService;

	public KeepMasterSM(int groupIdx, int smID, boolean needCheckpoint) {
		super(groupIdx, smID, needCheckpoint);
		executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("keep_master_sm_worker_"+groupIdx));
	}

	@Override
	public boolean execute(final int groupIdx, long instanceID, byte[] paxosValue, SMCtx smCtx) {
		if (paxosValue == null) {
			return true;
		}
		if(!PaxosState.isStarted(groupIdx)){
			LOGGER.info("keep master group {} not start.return",groupIdx);
			return true;
		}
		long nodeId = Long.parseLong(new String(paxosValue));
		if (nodeId == PaxosConfig.getInstance().getMyNode().getNodeID() && !GroupState.isGroupKeepMaster(groupIdx)) {
			LOGGER.info("I am keepmaster target node,but not in keepmaster stat. return");
			return true;
		}

		wpaxosService.getMasterSM(groupIdx).setAbsExpireTime(TimeUtil.getCurrentTimestamp());
		if (nodeId != PaxosConfig.getInstance().getMyNode().getNodeID()) {
			LOGGER.info("group {} drop master", groupIdx);
			wpaxosService.dropMaster(groupIdx);
		}
		if (nodeId == PaxosConfig.getInstance().getMyNode().getNodeID()) {
			executorService.schedule(new Runnable() {
				@Override
				public void run() {
					if (wpaxosService.isNoMaster(groupIdx)) {
						LOGGER.info("keep master group {} has no master,to be master", groupIdx);
						wpaxosService.toBeMaster(groupIdx);
					}
				}
			}, 10, TimeUnit.MILLISECONDS);
		}
		return true;
	}

	@Override
	public byte[] beforePropose(int groupIdx, byte[] sValue) {
		return null;
	}

	@Override
	public boolean needCallBeforePropose() {
		return false;
	}

}
