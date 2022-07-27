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
			// 解析参数
			MigrateReqKeyOperateInfoReq migrateKeyOperateInfo = migrateRequestParseFactory.parse(migrateControlInfoReq.getOperateInfo(), MigrateReqKeyOperateInfoReq.class);
			ClusterDO clusterInfo = clusterRepository.getClusterByClusterName(Environment.env(), migrateKeyOperateInfo.getCluster());
			KeyDO keyByName = keyRepository.getKeyByName(Environment.env(), migrateKeyOperateInfo.getKey());
			checkParam(clusterInfo, keyByName);

			// 更新状态
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移过程版本错误,请检查版本 : " + migrateControlInfoReq.getVersion());
			}
			migrateProcessDO.setState(migrateControlInfoReq.getProcessState());

			// 逆向操作的初始化数据
			if (migrateControlInfoReq.getProcessState() == MigrateProcessState.BackWardTransfer.getValue() && migrateControlInfoReq.getMigrateState() == MigrateType.Init.getValue()) {
				log.info("start deal backward migrate process init");
				dealBackWardMigrateKeyInit(migrateControlInfoReq, clusterInfo, keyByName);
				return;
			}

			// 判断数据库中的状态 , 执行更新操作
			List<MigrateProcessDO> migrateProcesses = migrateProcessRepository.searchByCondition(Environment.env(), -1, keyByName.getHashKey(), -1L, MigrateProcessEndState.NoEnd.getValue());
			assert migrateProcesses.size() == 1 : "同时只能迁移一个秘钥";
			MigrateProcessDO migrateProcess = migrateProcesses.get(0);
			Long migrateKeyVersion = migrateProcess.getMigrateKeyVersion();
			List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateKeyVersion);

			checkState(migrateDos, MigrateType.parse(migrateControlInfoReq.getMigrateState()), CHECK_OPERATE);

			List<GroupNodeDO> needUpdateGroupInfo = new ArrayList<>();
			List<MigrateDO> needUpdateMigrateInfo = new ArrayList<>();
			getUpdateDOList(MigrateType.parse(migrateControlInfoReq.getMigrateState()), needUpdateGroupInfo, needUpdateMigrateInfo, migrateDos);

			// 更新迁移状态 :
			migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);
			if (migrateControlInfoReq.getMigrateState() == MigrateType.MigrateGroupEndMoving.getValue()) {
				// 根据单分组 ,多分组执行不同操作
				if (keyByName.getMultiGroup() == MultiGroup.UnUse.getValue()) {
					int groupId = makeNewGroup(migrateControlInfoReq.getProcessState(), clusterInfo.getGroupCount(), keyByName.getGroupIds());
					checkMaster(clusterInfo, migrateProcessDO, groupId);
					keyByName.setGroupIds(String.valueOf(groupId));
				} else {
					// 多分组
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
				redisUtil.delKey(String.format(HASHKEY_CLINTCONFONFO_MAPPING, keyByName.getHashKey()));
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
			throw new ServiceException("master 没有选举出来,请稍后再试 ,group 为 : " + groupId);
		}
	}

	private int makeNewGroup(int processState, int groupCount, String oldGroup) throws Exception {
		return getGroupIdByOldGroupId(Integer.parseInt(oldGroup), groupCount, processState);
	}

	private void dealBackWardMigrateKeyInit(MigrateControlInfoReq migrateControlInfoReq, ClusterDO clusterInfo, KeyDO keyByName) throws Exception {
		MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
		// 查询是不是重试操作 , 是否需要添加数据
		List<MigrateDO> migrateDos1 = migrateRepository.searchMigrateByCondition(Environment.env(), clusterInfo.getClusterId(), "", -1, MigrateEndState.NoEnd.getValue(), migrateProcessDO.getMigrateKeyVersion());
		if (!migrateDos1.isEmpty()) {
			throw new ServiceException("存在迁移数据 ,不允许重新开始迁移");
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
				// 逆向需要进行一个 groupId 转换
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
			// 1. 检测 参数
			MigrateProcessDO migrateProcess = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcess == null) {
				throw new ServiceException(VERSION_MIGRATE_PROCESS_LIMIT);
			}
			if (migrateProcess.getState() != MigrateProcessState.ForwardMigrate.getValue()
					&& migrateProcess.getState() != MigrateProcessState.BackWardTransfer.getValue()) {
				throw new ServiceException(ROLLBACK_MIGRATE_PROCESS_LIMIT);
			}
			// 3. 检测 数据库中表状态 (可能是第一次进行回滚 , 或者是回滚重试)
			List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateProcess.getMigrateKeyVersion());
			checkState(migrateDos, MigrateType.parse(migrateControlInfoReq.getMigrateState()), CHECK_ROLLBACK);
			// 4. 设置状态为回滚 , 此时需要 batch 操作
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
				throw new ServiceException("初始化阶段不允许重试");
			}
			// 所有节点没有执行完成 , 不能进行下一步操作 ,如果相同的命令允许重试, 回滚状态不需要进行上一个状态的检查
			if (checkType == CHECK_OPERATE) {
				if (migrateDO.getMigrateState() != prepareStart.getValue() && migrateDO.getExecuteResult() != MigrateExecuteResult.Success.getValue()) {
					log.error("迁移节点没有全部执行完上一步操作,不允许继续操作,未执行节点 : " + migrateDO.getServer());
					throw new ServiceException("迁移节点没有全部执行完上一步操作,不允许继续操作,未执行节点 : " + migrateDO.getServer());
				}
			}
			// 状态只能正向流转
			if (prepareStart.getValue() <= MigrateType.MigrateEnd.getValue() && prepareStart.getValue() > migrateDO.getMigrateState() + 1) {
				log.error("当前集群已经处于下一个迁移状态,不能回到上一迁移状态,当前状态为 : " + MigrateType.parse(migrateDO.getMigrateState()).name());
				throw new ServiceException("当前集群已经处于下一个迁移状态,不能回到上一迁移状态,当前状态为 : " + MigrateType.parse(migrateDO.getMigrateState()).name());
			}
			// 期望进行回滚操作
			if (prepareStart == MigrateType.MigrateGroupMovingSafePointRollBack || prepareStart == MigrateType.MigrateGroupStartMovingRollBack || prepareStart == MigrateType.MigratePrepareRollBack) {
				// 如果期望进行回滚操作,只能是对自己的状态进行回滚
				if (prepareStart.getValue() != migrateDO.getMigrateState() + 5 && prepareStart.getValue() != migrateDO.getMigrateState()) {
					throw new ServiceException("回滚状态错误,请检查当前处于的状态 : 当前状态 [" + MigrateType.parse(migrateDO.getMigrateState()).name() + "] , 期望操作 : [" + MigrateType.parse(prepareStart.getValue()).name() + "]");
				}
				// 如果没有拉取过配置,不允许进行回滚操作
				if (migrateDO.getExecuteResult() == MigrateExecuteResult.Init.getValue()) {
					throw new ServiceException(MigrateType.parse(migrateDO.getMigrateState()).getName() + "处理中,请稍后再试");
				}
			}
			// 当前处于回滚操作
			if (migrateDO.getMigrateState() == MigrateType.MigrateGroupMovingSafePointRollBack.getValue() || migrateDO.getMigrateState() == MigrateType.MigrateGroupStartMovingRollBack.getValue() || migrateDO.getMigrateState() == MigrateType.MigratePrepareRollBack.getValue()) {
				// 如果当前处于回滚状态只支持重试
				if (prepareStart.getValue() != migrateDO.getMigrateState()) {
					throw new ServiceException("回滚操作只支持重试: 当前状态 [" + MigrateType.parse(migrateDO.getMigrateState()) + "] , 期望操作 : [" + MigrateType.parse(prepareStart.getValue()) + "]");
				}
			}

			if (prepareStart == MigrateType.MigrateGroupMovingSafePoint && migrateDO.getMigrateState() == MigrateType.MigrateGroupStartMoving.getValue() && (migrateDO.getUpdateTime().getTime() + 5 * 60 * 1000) > System.currentTimeMillis()) {
				log.error(KEY_MIGRATE_WAIT);
				throw new ServiceException("安全迁移状态需要保证处于 10min , 才能继续进行操作,当前时间未到操作时间,请等待.");
			}
		}
	}

	private void getUpdateDOList(MigrateType prepareStart, List<GroupNodeDO> needUpdateGroupInfo, List<MigrateDO> needUpdateMigrateInfo, List<MigrateDO> migrateDos) throws Exception {
		for (MigrateDO migrateDO : migrateDos) {
			migrateDO.setExecuteResult(MigrateExecuteResult.Init.getValue());
			migrateDO.setEnd(MigrateEndState.NoEnd.getValue());
			// 如果状态是 SafeStart  ,需要开启 master 选举
			if (prepareStart == MigrateType.MigrateGroupMovingSafePointRollBack) {
				// 重试操作也不需要更新 group
				if (migrateDO.getMigrateState() != prepareStart.getValue()) {
					// 查 groupId 数量
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
		PushMessage pushMessage = new PushMessage();
		pushMessage.setCluster(clusterName);
		pushMessage.setVersion(System.currentTimeMillis());
		redisUtil.publish(RedisKeyConstant.REDIS_SUBSCRIBE_CHANNEL, JSON.toJSONString(pushMessage));
	}

	private int getGroupIdByOldGroupId(int oldGroupId, int groupCount, int migrateProcessType) throws ServiceException {
		// 支持幂等操作
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
