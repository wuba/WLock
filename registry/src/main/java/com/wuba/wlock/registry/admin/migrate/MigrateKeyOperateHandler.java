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
package com.wuba.wlock.registry.admin.migrate;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.entity.PushMessage;
import com.wuba.wlock.common.enums.MigrateEndState;
import com.wuba.wlock.common.enums.MigrateExecuteResult;
import com.wuba.wlock.common.enums.MigrateProcessEndState;
import com.wuba.wlock.common.enums.MigrateType;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.domain.request.MigrateReqKeyOperateInfoReq;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.registry.util.IDHelper;
import com.wuba.wlock.repository.domain.*;
import com.wuba.wlock.repository.enums.MigrateProcessState;
import com.wuba.wlock.repository.enums.MultiGroup;
import com.wuba.wlock.repository.enums.ServerState;
import com.wuba.wlock.repository.enums.UseMasterState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.*;

@Slf4j
@Component
public class MigrateKeyOperateHandler extends BaseMigrateOperateHandlerInterface {

	private static final int CHECK_ROLLBACK = 1;
	private static final int CHECK_OPERATE = 0;


	private static final String HASHKEY_CLINTCONFONFO_MAPPING = "hashkey:client:conf:info:mapping:%s";

	@Override
	public void checkOperate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
	}

	@Override
	public void operate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		try {
			log.info(this.getClass().getName() + "start operate");
			// ????????????
			MigrateReqKeyOperateInfoReq migrateKeyOperateInfo = migrateRequestParseFactory.parse(migrateControlInfoReq.getOperateInfo(), MigrateReqKeyOperateInfoReq.class);
			ClusterDO clusterInfo = clusterRepository.getClusterByClusterName(Environment.env(), migrateKeyOperateInfo.getCluster());
			KeyDO keyByName = keyRepository.getKeyByName(Environment.env(), migrateKeyOperateInfo.getKey());
			checkParam(clusterInfo, keyByName);

			// ????????????
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("????????????????????????,??????????????? : " + migrateControlInfoReq.getVersion());
			}
			migrateProcessDO.setState(migrateControlInfoReq.getProcessState());

			// ??????????????????????????????
			if (migrateControlInfoReq.getProcessState() == MigrateProcessState.BackWardTransfer.getValue() && migrateControlInfoReq.getMigrateState() == MigrateType.Init.getValue()) {
				log.info("start deal backward migrate process init");
				dealBackWardMigrateKeyInit(migrateControlInfoReq, clusterInfo, keyByName);
				return;
			}

			// ??????????????????????????? , ??????????????????
			List<MigrateProcessDO> migrateProcesses = migrateProcessRepository.searchByCondition(Environment.env(), -1, keyByName.getHashKey(), -1L, MigrateProcessEndState.NoEnd.getValue());
			assert migrateProcesses.size() == 1 : "??????????????????????????????";
			MigrateProcessDO migrateProcess = migrateProcesses.get(0);
			Long migrateKeyVersion = migrateProcess.getMigrateKeyVersion();
			List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateKeyVersion);

			checkState(migrateDos, MigrateType.parse(migrateControlInfoReq.getMigrateState()), CHECK_OPERATE);

			List<GroupNodeDO> needUpdateGroupInfo = new ArrayList<>();
			List<MigrateDO> needUpdateMigrateInfo = new ArrayList<>();
			getUpdateDOList(MigrateType.parse(migrateControlInfoReq.getMigrateState()), needUpdateGroupInfo, needUpdateMigrateInfo, migrateDos);

			// ?????????????????? :
			migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);
			if (migrateControlInfoReq.getMigrateState() == MigrateType.MigrateGroupEndMoving.getValue()) {
				// ??????????????? ,???????????????????????????
				if (keyByName.getMultiGroup() == MultiGroup.UnUse.getValue()) {
					int groupId = makeNewGroup(migrateControlInfoReq.getProcessState(), clusterInfo.getGroupCount(), keyByName.getGroupIds());
					checkMaster(clusterInfo, migrateProcessDO, groupId);
					keyByName.setGroupIds(String.valueOf(groupId));
				} else {
					// ?????????
					StringBuilder groupIds = new StringBuilder();
					for (String oldGroupId : keyByName.getGroupIds().split(CommonConstant.COMMA)) {
						int groupId = makeNewGroup(migrateControlInfoReq.getProcessState(), clusterInfo.getGroupCount(), oldGroupId);
						checkMaster(clusterInfo, migrateProcessDO, groupId);
						groupIds.append(groupId).append(",");
					}
					groupIds.deleteCharAt(groupIds.length() - 1);
					keyByName.setGroupIds(groupIds.toString());
				}
				keyRepository.updateKeyDO(Environment.env(), keyByName);
				clusterRepository.updateClusterVersionByClusterName(Environment.env(), System.currentTimeMillis(), clusterInfo.getClusterId());
				if (redisUtil.isUseRedis()) {
					redisUtil.delKey(String.format(HASHKEY_CLINTCONFONFO_MAPPING, keyByName.getHashKey()));
				}
				notifyMigrateGroup(clusterInfo.getClusterId());
			}
			if (!needUpdateGroupInfo.isEmpty()) {
				groupNodeRepository.batchUpdateById(Environment.env(), needUpdateGroupInfo);
			}
			if (!needUpdateMigrateInfo.isEmpty()) {
				migrateRepository.batchUpdateById(Environment.env(), needUpdateMigrateInfo);
			}
		} catch (Exception e) {
			log.error("", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	private void checkMaster(ClusterDO clusterInfo, MigrateProcessDO migrateProcessDO, int groupId) throws Exception {
		List<GroupServerRefDO> groupServerRefByClusterId = groupServerRefRepository.getGroupServerRefByClusterId(Environment.env(), clusterInfo.getClusterId());
		List<GroupServerRefDO> masterGroupInfo = groupServerRefByClusterId.stream().filter(v -> v.getGroupId() == groupId).collect(Collectors.toList());
		if (masterGroupInfo.isEmpty() || masterGroupInfo.get(0).getUpdateTime().before(migrateProcessDO.getCreateTime())) {
			throw new ServiceException("master ??????????????????,??????????????? ,group ??? : " + groupId);
		}
	}

	private int makeNewGroup(int processState, int groupCount, String oldGroup) throws Exception {
		return getGroupIdByOldGroupId(Integer.parseInt(oldGroup), groupCount, processState);
	}

	private void dealBackWardMigrateKeyInit(MigrateControlInfoReq migrateControlInfoReq, ClusterDO clusterInfo, KeyDO keyByName) throws Exception {
		MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
		// ??????????????????????????? , ????????????????????????
		List<MigrateDO> migrateDos1 = migrateRepository.searchMigrateByCondition(Environment.env(), clusterInfo.getClusterId(), "", -1, MigrateEndState.NoEnd.getValue(), migrateProcessDO.getMigrateKeyVersion());
		if (!migrateDos1.isEmpty()) {
			throw new ServiceException("?????????????????? ,???????????????????????????");
		}
		migrateProcessDO.setState(migrateControlInfoReq.getProcessState());
		Long version = IDHelper.getUniqueId();
		migrateProcessDO.setMigrateKeyVersion(version);
		List<MigrateDO> migrateDos = new ArrayList<>();
		List<ServerDO> serviceInfos = serverRepository.getServerByClusterIdAndState(Environment.env(), clusterInfo.getClusterId(), ServerState.online.getValue());
		for (String groupId : migrateProcessDO.getGroups().split(CommonConstant.COMMA)) {
			for (ServerDO serviceInfo : serviceInfos) {
				MigrateDO migrateDO = new MigrateDO();
				migrateDO.setVersion(version);
				// ???????????????????????? groupId ??????
				migrateDO.setGroupId(Integer.parseInt(groupId) + clusterInfo.getGroupCount());
				migrateDO.setCluster(clusterInfo.getClusterId());
				migrateDO.setMigrateState(MigrateType.Init.getValue());
				migrateDO.setServer(serviceInfo.getServerAddr());
				migrateDO.setExecuteResult(MigrateExecuteResult.Success.getValue());
				migrateDO.setKeyHash(keyByName.getHashKey());
				migrateDO.setCreateTime(new Date());
				migrateDO.setEnd(MigrateEndState.NoEnd.getValue());
				migrateDos.add(migrateDO);
			}
		}
		migrateRepository.batchSaveMigrate(Environment.env(), migrateDos);
		migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);
	}

	@Override
	public void rollback(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		try {
			log.info(this.getClass().getName() + "start rollback");
			// 1. ?????? ??????
			MigrateProcessDO migrateProcess = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcess == null) {
				throw new ServiceException(VERSION_MIGRATE_PROCESS_LIMIT);
			}
			if (migrateProcess.getState() != MigrateProcessState.ForwardMigrate.getValue()
					&& migrateProcess.getState() != MigrateProcessState.BackWardTransfer.getValue()) {
				throw new ServiceException(ROLLBACK_MIGRATE_PROCESS_LIMIT);
			}
			// 3. ?????? ????????????????????? (?????????????????????????????? , ?????????????????????)
			List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateProcess.getMigrateKeyVersion());
			checkState(migrateDos, MigrateType.parse(migrateControlInfoReq.getMigrateState()), CHECK_ROLLBACK);
			// 4. ????????????????????? , ???????????? batch ??????
			List<GroupNodeDO> needUpdateGroupInfo = new ArrayList<>();
			List<MigrateDO> needUpdateMigrateInfo = new ArrayList<>();
			getUpdateDOList(MigrateType.parse(migrateControlInfoReq.getMigrateState()), needUpdateGroupInfo, needUpdateMigrateInfo, migrateDos);
			if (!needUpdateGroupInfo.isEmpty()) {
				groupNodeRepository.batchUpdateById(Environment.env(), needUpdateGroupInfo);
			}
			if (!needUpdateMigrateInfo.isEmpty()) {
				migrateRepository.batchUpdateById(Environment.env(), needUpdateMigrateInfo);
			}
		} catch (Exception e) {
			log.error("", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}


	private void checkState(List<MigrateDO> migrateDos, MigrateType prepareStart, int checkType) throws ServiceException {
		if (migrateDos.isEmpty()) {
			throw new ServiceException(KEY_MIGRATE_EMPTY);
		}
		for (MigrateDO migrateDO : migrateDos) {
			if (migrateDO.getMigrateState() == MigrateType.Init.getValue() && prepareStart.getValue() == MigrateType.Init.getValue()) {
				throw new ServiceException("??????????????????????????????");
			}
			// ?????????????????????????????? , ??????????????????????????? ,?????????????????????????????????, ???????????????????????????????????????????????????
			if (checkType == CHECK_OPERATE) {
				if (migrateDO.getMigrateState() != prepareStart.getValue() && migrateDO.getExecuteResult() != MigrateExecuteResult.Success.getValue()) {
					log.error("????????????????????????????????????????????????,?????????????????????,??????????????? : " + migrateDO.getServer());
					throw new ServiceException("????????????????????????????????????????????????,?????????????????????,??????????????? : " + migrateDO.getServer());
				}
			}
			// ????????????????????????
			if (prepareStart.getValue() <= MigrateType.MigrateEnd.getValue() && prepareStart.getValue() > migrateDO.getMigrateState() + 1) {
				log.error("?????????????????????????????????????????????,??????????????????????????????,??????????????? : " + MigrateType.parse(migrateDO.getMigrateState()).name());
				throw new ServiceException("?????????????????????????????????????????????,??????????????????????????????,??????????????? : " + MigrateType.parse(migrateDO.getMigrateState()).name());
			}
			// ????????????????????????
			if (prepareStart == MigrateType.MigrateGroupMovingSafePointRollBack || prepareStart == MigrateType.MigrateGroupStartMovingRollBack || prepareStart == MigrateType.MigratePrepareRollBack) {
				// ??????????????????????????????,???????????????????????????????????????
				if (prepareStart.getValue() != migrateDO.getMigrateState() + 5 && prepareStart.getValue() != migrateDO.getMigrateState()) {
					throw new ServiceException("??????????????????,?????????????????????????????? : ???????????? [" + MigrateType.parse(migrateDO.getMigrateState()).name() + "] , ???????????? : [" + MigrateType.parse(prepareStart.getValue()).name() + "]");
				}
				// ???????????????????????????,???????????????????????????
				if (migrateDO.getExecuteResult() == MigrateExecuteResult.Init.getValue()) {
					throw new ServiceException(MigrateType.parse(migrateDO.getMigrateState()).getName() + "?????????,???????????????");
				}
			}
			// ????????????????????????
			if (migrateDO.getMigrateState() == MigrateType.MigrateGroupMovingSafePointRollBack.getValue() || migrateDO.getMigrateState() == MigrateType.MigrateGroupStartMovingRollBack.getValue() || migrateDO.getMigrateState() == MigrateType.MigratePrepareRollBack.getValue()) {
				// ?????????????????????????????????????????????
				if (prepareStart.getValue() != migrateDO.getMigrateState()) {
					throw new ServiceException("???????????????????????????: ???????????? [" + MigrateType.parse(migrateDO.getMigrateState()) + "] , ???????????? : [" + MigrateType.parse(prepareStart.getValue()) + "]");
				}
			}

			if (prepareStart == MigrateType.MigrateGroupMovingSafePoint && migrateDO.getMigrateState() == MigrateType.MigrateGroupStartMoving.getValue() && (migrateDO.getUpdateTime().getTime() + 5 * 60 * 1000) > System.currentTimeMillis()) {
				log.error(KEY_MIGRATE_WAIT);
				throw new ServiceException("???????????????????????????????????? 10min , ????????????????????????,??????????????????????????????,?????????.");
			}
		}
	}

	private void getUpdateDOList(MigrateType prepareStart, List<GroupNodeDO> needUpdateGroupInfo, List<MigrateDO> needUpdateMigrateInfo, List<MigrateDO> migrateDos) throws Exception {
		for (MigrateDO migrateDO : migrateDos) {
			migrateDO.setExecuteResult(MigrateExecuteResult.Init.getValue());
			migrateDO.setEnd(MigrateEndState.NoEnd.getValue());
			// ??????????????? SafeStart  ,???????????? master ??????
			if (prepareStart == MigrateType.MigrateGroupMovingSafePointRollBack) {
				// ?????????????????????????????? group
				if (migrateDO.getMigrateState() != prepareStart.getValue()) {
					// ??? groupId ??????
					ClusterDO clusterByClusterName = clusterRepository.getClusterByClusterName(Environment.env(), migrateDO.getCluster());
					int groupCount = clusterByClusterName.getGroupCount();
					int realGroup = migrateDO.getGroupId() >= groupCount ? migrateDO.getGroupId() - groupCount : migrateDO.getGroupId() + groupCount;
					List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), realGroup, migrateDO.getServer(), migrateDO.getCluster());
					GroupNodeDO groupNodeDO = groupNodeDos.get(0);
					groupNodeDO.setUseMaster(UseMasterState.noUse.getValue());
					needUpdateGroupInfo.add(groupNodeDO);
				}
			} else if (prepareStart == MigrateType.MigrateGroupEndMoving) {
				migrateDO.setExecuteResult(MigrateExecuteResult.Success.getValue());
			}
			migrateDO.setMigrateState(prepareStart.getValue());
			needUpdateMigrateInfo.add(migrateDO);
		}
	}

	private void checkParam(ClusterDO clusterInfo, KeyDO keyByName) throws ServiceException {
		if (clusterInfo == null) {
			throw new ServiceException(CLUSTER_NOT_EXIST);
		}
		if (keyByName == null) {
			throw new ServiceException(KEY_NOT_EXIST);
		}
		if (!keyByName.getClusterId().equals(clusterInfo.getClusterId())) {
			throw new ServiceException(LOCK_KEY_CLUSTER_ERROR);
		}
	}

	private void notifyMigrateGroup(String clusterName) {
		if (redisUtil.isUseRedis()) {
			PushMessage pushMessage = new PushMessage();
			pushMessage.setCluster(clusterName);
			pushMessage.setVersion(System.currentTimeMillis());
			redisUtil.publish(RedisKeyConstant.REDIS_SUBSCRIBE_CHANNEL, JSON.toJSONString(pushMessage));
		}
	}

	private int getGroupIdByOldGroupId(int oldGroupId, int groupCount, int migrateProcessType) throws ServiceException {
		// ??????????????????
		if (migrateProcessType == MigrateProcessState.ForwardMigrate.getValue() && oldGroupId < groupCount) {
			return oldGroupId + groupCount;
		} else if (migrateProcessType == MigrateProcessState.BackWardTransfer.getValue() && oldGroupId >= groupCount) {
			return oldGroupId - groupCount;
		} else {
			log.warn(String.format("group id already change : oldGroupId %s,groupCount  %s", oldGroupId, groupCount));
			return oldGroupId;
		}
	}

}
