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

import com.wuba.wlock.server.communicate.registry.RegistryClient;
import com.wuba.wlock.server.communicate.retrans.RetransServerManager;
import com.wuba.wlock.server.constant.GroupState;
import com.wuba.wlock.server.constant.PaxosState;
import com.wuba.wlock.server.expire.ExpireStrategyFactory;
import com.wuba.wpaxos.comm.MasterChangeCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterChangeService implements MasterChangeCallback {

	private static Logger logger = LoggerFactory.getLogger(MasterChangeService.class);

	private MasterChangeService() {
	}

	private static MasterChangeCallback masterChangeService = new MasterChangeService();


	public static MasterChangeCallback getInstance() {
		return masterChangeService;
	}


	@Override
	public void callback(int groupIdx, boolean masterChange, boolean isImMaster, boolean isCheckpoint) {
		if (isCheckpoint) {
			return;
		}
		PaxosState.setGroupStart(groupIdx);
		logger.debug("master sm callback.not checkpoint.group {} start OK", groupIdx);
		if (masterChange) {
			GroupState.setGroupChangeState(groupIdx, masterChange);
			GroupState.setStopPush(true);
			GroupState.clearGroupKeepMaster(groupIdx);

			if (isImMaster) {
				ExpireStrategyFactory.getInstance().learnMaster(groupIdx);
			} else {
				ExpireStrategyFactory.getInstance().paused(groupIdx);
			}
			RetransServerManager.getInstance().masterChanged(groupIdx);
			logger.info("group {} master changed.clear keep master state", groupIdx);
		} else if (GroupState.isGroupKeepMaster(groupIdx)) {
			//needDropKeepMaster???true???????????????keepmaster????????????????????????master??????????????????keepmaster???drop??????
			if (GroupState.needDropKeepMaster(groupIdx)) {
				//??????keepmaster????????????????????????drop???????????????????????????
				GroupState.clearGroupKeepMaster(groupIdx);
				ExpireStrategyFactory.getInstance().resume(groupIdx);

			} else {
				//needDropKeepMaster???false???????????????keepmaster?????????????????????????????????
				//??????needDropKeepMaster??????????????????????????????master??????????????????keepmaster??????
				GroupState.setDropKeepMaster(groupIdx);
			}
		} else if (GroupState.getGroupChangeState(groupIdx)) {
			GroupState.setStopPush(false);
			GroupState.setGroupChangeState(groupIdx, masterChange);
			logger.info("to be master twice,group {},start push.notify to client ,push to registry", groupIdx);
			RegistryClient.getInstance().asyncUploadConfig();
		}
	}
}
