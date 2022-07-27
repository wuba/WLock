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
package com.wuba.wlock.server.service.impl;

import com.wuba.wlock.server.constant.GroupState;
import com.wuba.wlock.server.service.KeepMasterService;
import com.wuba.wlock.server.wpaxos.SMID;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.ProposeResult;
import com.wuba.wpaxos.storemachine.SMCtx;
import com.wuba.wpaxos.config.PaxosTryCommitRet;
import com.wuba.wpaxos.storemachine.SMCtx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeepMasterServiceImpl implements KeepMasterService {

	private static final Logger logger = LogManager.getLogger(KeepMasterServiceImpl.class);
	private static KeepMasterService keepMasterService = new KeepMasterServiceImpl();

	private KeepMasterServiceImpl() {
	}

	private WpaxosService wpaxosService = WpaxosService.getInstance();

	public static KeepMasterService getInstance() {
		return keepMasterService;
	}

	@Override
	public void keepMaster(int groupIdx, long targetNodeId) {
		if (!wpaxosService.isIMMaster(groupIdx) || GroupState.isGroupKeepMaster(groupIdx)) {
			return;
		}
		GroupState.setGroupTarget(groupIdx, targetNodeId);
		logger.info("keep master,set group {} state........................", groupIdx);
		GroupState.setGroupKeepMaster(groupIdx);
		SMCtx smCtx = new SMCtx();
		smCtx.setSmId(SMID.KEEP_MASTER.getValue());
		ProposeResult propose = wpaxosService.propose(String.valueOf(targetNodeId).getBytes(), groupIdx, smCtx);
		if (PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() != propose.getResult()) {
			GroupState.clearGroupKeepMaster(groupIdx);
			logger.info("keep master,propose error.ret {},clean keep master state", propose.getResult());
		}
	}
}
