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
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.registry.admin.constant.ExceptionConstant;
import com.wuba.wlock.registry.admin.domain.request.ClusterSplitOperateInfoReq;
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.admin.validators.ParamValidateUtil;
import com.wuba.wlock.registry.admin.validators.ValidateResult;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.repository.domain.*;
import com.wuba.wlock.repository.enums.ClusterState;
import com.wuba.wlock.repository.enums.MasterLoadBalance;
import com.wuba.wlock.repository.enums.MigrateProcessState;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.*;


@Slf4j
@Component
public class ClusterSplitOperateHandler extends BaseMigrateOperateHandlerInterface {

	@Override
	public void checkOperate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		try {
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移版本不存在,请确认迁移版本 : " + migrateControlInfoReq.getVersion());
			}
			if (migrateProcessDO.getState() == MigrateProcessState.ClusterSplit.getValue() || migrateProcessDO.getState() == MigrateProcessState.ChangeGroupNode.getValue()) {
				log.info("ClusterSplitOperateHandler check operator success");
				return;
			}

			throw new ServiceException("迁移过程大状态流转错误,目前处于 [" + MigrateProcessState.parse(migrateProcessDO.getState()) + "] , 期望状态是 :[" + MigrateProcessState.ChangeNode.name() + "]");
		} catch (Exception e) {
			log.error("", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	@Override
	public void operate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		// 1. 验证要拆分的集群的分组下是不是只有迁移的秘钥
		// 2. 进行 t_clusterqps 表变更  , 秘钥数据先写入到新集群下 , t_group_node 表变更 ,修改对应集群 ,t_server 表变更 , 修改归属集群
		try {
			log.info(this.getClass().getName() + "start operate");
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移过程版本错误,请检查版本 : " + migrateControlInfoReq.getVersion());
			}
			migrateProcessDO.setState(migrateControlInfoReq.getProcessState());
			KeyDO keyDO = keyRepository.getKeyByHashKey(Environment.env(), migrateProcessDO.getKayHash());
			if (keyDO == null) {
				throw new ServiceException(ExceptionConstant.KEY_NOT_EXIST);
			}

			for (String groupId : keyDO.getGroupIds().split(CommonConstant.COMMA)) {
				if (!isGroupChangeEnd(Integer.parseInt(groupId))) {
					throw new ServiceException("分组扩缩容没有完成,请把老节点移除,新节点移入在进行集群拆分操作");
				}
			}

			// 更新迁移状态
			migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);
			if (Strings.isEmpty(migrateControlInfoReq.getOperateInfo())) {
				log.info("ClusterSplitOperateHandler operator info is null , only update migrate process state");
				return;
			}
			ClusterSplitOperateInfoReq changeGroupNodeOperateInfo = migrateRequestParseFactory.parse(migrateControlInfoReq.getOperateInfo(), ClusterSplitOperateInfoReq.class);
			ValidateResult valid = ParamValidateUtil.valid(changeGroupNodeOperateInfo);
			if (!valid.isPass()) {
				throw new ServiceException(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg());
			}
			ClusterDO targetClusterInfo = clusterRepository.getClusterByClusterName(Environment.env(), changeGroupNodeOperateInfo.getTargetCluster());
			if (targetClusterInfo != null) {
				throw new ServiceException(MIGRATE_CLUSTER_SPLIT_MUST_NEW_CLUSTER);
			}
			// 校验新老集群是不是同一集群
			if (changeGroupNodeOperateInfo.getSourceCluster().equals(changeGroupNodeOperateInfo.getTargetCluster())) {
				throw new ServiceException("拆分集群和原集群不能是同一集群");
			}
			// 更新状态
			if (Strings.isEmpty(changeGroupNodeOperateInfo.getServerIds())) {
				throw new ServiceException(MIGRATE_CLUSTER_SPLIT_NODE_LIST_EMPTY);
			}
			ClusterDO sourceClusterInfo = clusterRepository.getClusterByClusterName(Environment.env(), changeGroupNodeOperateInfo.getSourceCluster());
			if (sourceClusterInfo == null) {
				throw new ServiceException(CLUSTER_NOT_EXIST);
			}
			String[] serverIds = changeGroupNodeOperateInfo.getServerIds().split(",");
			List<GroupNodeDO> needUpdateGroupNode = new ArrayList<>();
			for (String id : serverIds) {
				ServerDO serverInfo = serverRepository.getServerById(Environment.env(), Long.parseLong(id));
				// t_groupNode 更新集群信息
				List<GroupNodeDO> oldGroupNode = groupNodeRepository.searchByCondition(Environment.env(), -1, serverInfo.getServerAddr(), changeGroupNodeOperateInfo.getSourceCluster());
				for (GroupNodeDO groupNodeDO : oldGroupNode) {
					groupNodeDO.setClusterId(changeGroupNodeOperateInfo.getTargetCluster());
				}
				needUpdateGroupNode.addAll(oldGroupNode);
			}
			clusterSplitDbChange(changeGroupNodeOperateInfo, sourceClusterInfo, keyDO, serverIds, needUpdateGroupNode);
			log.info("notify old client config change");
			notifyRegistryClusterChange(changeGroupNodeOperateInfo.getSourceCluster(), Environment.env());
			log.info("notify new client config change");
			notifyRegistryClusterChange(changeGroupNodeOperateInfo.getTargetCluster(), Environment.env());
		} catch (Exception e) {
			log.error("", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	private boolean checkMasterState(KeyDO keyDO) throws Exception {
		Optional<GroupServerRefDO> masterInfo = groupServerRefRepository.getGroupServerRefByClusterId(Environment.env(), keyDO.getClusterId()).stream().filter(serverGroup -> serverGroup.getGroupId().equals(keyDO.getGroupId())).findAny();
		Optional<GroupNodeDO> anyGroupInfo = groupNodeRepository.searchByCondition(Environment.env(), keyDO.getGroupId(), "", keyDO.getClusterId()).stream().findAny();
		return anyGroupInfo.isPresent() && masterInfo.isPresent() && Sets.newHashSet(anyGroupInfo.get().getNodes().split(",")).contains(masterInfo.get().getServerAddr());
	}

	private void clusterSplitDbChange(ClusterSplitOperateInfoReq changeGroupNodeOperateInfo, ClusterDO sourceClusterInfo, KeyDO keyDO, String[] serverIds, List<GroupNodeDO> needUpdateGroupNode) throws Exception {
		if (!needUpdateGroupNode.isEmpty()) {
			log.info("batch update group node info : {}", needUpdateGroupNode);
			groupNodeRepository.batchUpdateById(Environment.env(), needUpdateGroupNode);
		}
		keyDO.setClusterId(changeGroupNodeOperateInfo.getTargetCluster());
		// 更新秘钥所属集群
		keyRepository.updateKeyDO(Environment.env(), keyDO);
		ClusterDO targetClusterInfo = clusterRepository.getClusterByClusterName(Environment.env(), changeGroupNodeOperateInfo.getTargetCluster());
		if (targetClusterInfo == null) {
			// 拆分集群是新集群,才需要进行处理
			// server 变更集群
			for (String id : serverIds) {
				log.info("batch update server cluster info : {}", serverIds);
				serverRepository.updateClusterById(Environment.env(), changeGroupNodeOperateInfo.getTargetCluster(), Long.parseLong(id));
			}
			ClusterDO clusterDO = new ClusterDO();
			clusterDO.setClusterId(changeGroupNodeOperateInfo.getTargetCluster());
			clusterDO.setHashCode(changeGroupNodeOperateInfo.getTargetCluster().hashCode());
			clusterDO.setUpdateTime(System.currentTimeMillis());
			clusterDO.setStatus(ClusterState.online.getValue());
			clusterDO.setGroupCount(sourceClusterInfo.getGroupCount());
			clusterDO.setQps(keyDO.getQps());
			clusterRepository.insertCluster(Environment.env(), clusterDO);
		}
		// 通知集群配置变更
		clusterRepository.updateClusterVersionByClusterName(Environment.env(), System.currentTimeMillis(), changeGroupNodeOperateInfo.getSourceCluster());
	}


	@Override
	public void rollback(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		throw new ServiceException(ROLLBACK_MIGRATE_PROCESS_LIMIT);
	}

	private void notifyRegistryClusterChange(String clusterName, String env) {
		if (redisUtil.isUseRedis()) {
			PushMessage pushMessage = new PushMessage();
			pushMessage.setCluster(clusterName);
			pushMessage.setVersion(System.currentTimeMillis());
			redisUtil.publish(RedisKeyConstant.REDIS_SUBSCRIBE_CHANNEL, JSON.toJSONString(pushMessage));
		}
	}

	/**
	 * 基于迁移的 group 查询老节点信息
	 *
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	private Set<String> makeOldNodes(int groupId) throws Exception {
		// 查询老节点,判断状态 :
		int oldGroupId = groupId == 0 ? 1 : 0;
		List<GroupNodeDO> oldGroupNode = groupNodeRepository.searchByCondition(Environment.env(), oldGroupId, "", "");
		Set<String> oldNodes = new HashSet<>();
		for (GroupNodeDO groupNodeDO : oldGroupNode) {
			if (groupNodeDO.getLoadBalance() == MasterLoadBalance.use.getValue()) {
				oldNodes = Sets.newHashSet(groupNodeDO.getNodes().split(","));
			}
		}
		return oldNodes;
	}

	private Set<String> makeCurrentGroupNodeInfo(int groupId) throws Exception {
		List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), groupId, "", "");
		Set<String> currGroupNodeInfo = new HashSet<>();
		for (GroupNodeDO groupNodeDO : groupNodeDos) {
			// 节点只有自己的跳过
			if (groupNodeDO.getNodes().equals(groupNodeDO.getServer())) {
				continue;
			}
			currGroupNodeInfo.addAll(Sets.newHashSet(groupNodeDO.getNodes().split(",")));
		}
		return currGroupNodeInfo;
	}

	private Map<String, ServerDO> makeAllNode() throws Exception {
		List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), -1, "", "");
		Set<String> allNodeString = groupNodeDos.stream().map(GroupNodeDO::getServer).collect(Collectors.toSet());
		Map<String, ServerDO> result = new HashMap<>();
		for (String node : allNodeString) {
			ServerDO serverDO = serverRepository.getByServer(Environment.env(), node);
			result.put(serverDO.getServerAddr(), serverDO);
		}
		return result;
	}

	private boolean isGroupChangeEnd(int groupId) throws Exception {
		Set<String> currGroupNodeInfo = makeCurrentGroupNodeInfo(groupId);
		Map<String, ServerDO> allNode = makeAllNode();
		Set<String> oldNodes = makeOldNodes(groupId);
		for (Map.Entry<String, ServerDO> node : allNode.entrySet()) {
			boolean oldNode = oldNodes.contains(node.getKey());
			boolean isOffline = !currGroupNodeInfo.contains(node.getKey());
			if (oldNode != isOffline) {
				return false;
			}
		}
		return true;
	}

}
