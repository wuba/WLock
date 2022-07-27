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
import com.wuba.wlock.registry.admin.domain.request.ChangeGroupNodeOperateInfoReq;
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.admin.utils.SetUtil;
import com.wuba.wlock.registry.admin.validators.ParamValidateUtil;
import com.wuba.wlock.registry.admin.validators.ValidateResult;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.repository.domain.*;
import com.wuba.wlock.repository.enums.MasterLoadBalance;
import com.wuba.wlock.repository.enums.MigrateProcessState;
import com.wuba.wlock.repository.enums.ServerState;
import com.wuba.wlock.repository.enums.UseMasterState;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.*;

@Slf4j
@Component
public class ChangeGroupNodeOperateHandler extends BaseMigrateOperateHandlerInterface {
	private static final long TIMEOUT = 5 * 60 * 1000;

	@Override
	public void checkOperate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		try {
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移版本不存在,请确认迁移版本 : " + migrateControlInfoReq.getVersion());
			}
			if (migrateProcessDO.getState() == MigrateProcessState.ChangeNode.getValue() || migrateProcessDO.getState() == MigrateProcessState.ChangeGroupNode.getValue()) {
				log.info("ChangeGroupNodeOperateHandler check operator success");
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
		// 分组上下线 (对于下线节点修改为上线 , 对于上线节点不变)
		// 一次最多变更 2 个节点,扩容缩容都可以 , 扩缩容过程中,把要扩容的节点设置为 online
		try {
			// 更新状态
			log.info(this.getClass().getName() + "start operate");
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移过程版本错误,请检查版本 : " + migrateControlInfoReq.getVersion());
			}
			migrateProcessDO.setState(migrateControlInfoReq.getProcessState());

			if (!checkNodeCount(migrateProcessDO.getId())) {
				throw new ServiceException("新节点数量不到 3 个,不允许进行扩缩容!");
			}

			migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);

			if (Strings.isEmpty(migrateControlInfoReq.getOperateInfo())) {
				log.info("ChangeGroupNodeOperateHandler operator info is null , only update migrate process state");
				dealBackProcessNewNodeList(migrateProcessDO);
				return;
			}
			ChangeGroupNodeOperateInfoReq changeGroupNodeOperateInfoReq = migrateRequestParseFactory.parse(migrateControlInfoReq.getOperateInfo(), ChangeGroupNodeOperateInfoReq.class);
			ValidateResult valid = ParamValidateUtil.valid(changeGroupNodeOperateInfoReq);
			if (!valid.isPass()) {
				throw new ServiceException(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg());
			}
			String serviceIds = changeGroupNodeOperateInfoReq.getNodes();
			List<Long> updateIds = new ArrayList<>();
			KeyDO keyByHashKey = keyRepository.getKeyByHashKey(Environment.env(), migrateProcessDO.getKayHash());
			String cluster = keyByHashKey.getClusterId();
			HashSet<String> newNodes = new HashSet<>();
			for (String serviceId : serviceIds.split(CommonConstant.COMMA)) {
				ServerDO serverById = serverRepository.getServerById(Environment.env(), Long.parseLong(serviceId));
				if (serverById == null) {
					throw new ServiceException(SERVER_UN_EXISTED);
				}
				if (serverById.getState() == ServerState.offline.getValue() && serverById.getLastUpdateTime() != null) {
					throw new ServiceException("节点 : " + serverById.getServerAddr() + " 已经处于运行状态,请先关闭节点并清空 db 后在进行扩容操作");
				}
				if (serverById.getState() == ServerState.offline.getValue()) {
					serverById.setState(ServerState.online.getValue());
					updateIds.add(serverById.getId());
				}
				newNodes.add(serverById.getServerAddr());
			}

			ClusterDO clusterByClusterName = clusterRepository.getClusterByClusterName(Environment.env(), keyByHashKey.getClusterId());
			int groupCount = clusterByClusterName.getGroupCount();
			List<GroupNodeDO> groupNodeDos = new ArrayList<GroupNodeDO>();
			for (Integer group: migrateProcessDO.groupIds()) {
				groupNodeDos.addAll(groupNodeRepository.searchByCondition(Environment.env(), group + groupCount, "", ""));
			}
			String[] nodeSplit = groupNodeDos.get(0).getNodes().split(",");
			Set<String> oldNodes = Sets.newHashSet(nodeSplit);
			checkOldNodeRunState(oldNodes);
			// 比较是否只改变了 1 个节点
			Set<String> diff = SetUtil.diffSet(oldNodes, newNodes);
			if (diff.size() != 1) {
				throw new ServiceException(MIGRATE_GROUP_NODE_CHANGE_COUNT_CHECK);
			}

			// 分组节点变更间隔需要 大于 5min,保证数据学习完成 (新节点个数是 4 个 , 老节点个数 3 个时候不做限制)
			int nodeSize = groupNodeDos.size() / 2;
			// 缩容时候进行等待
			if (newNodes.size() == nodeSize) {
				checkUpdateTimeNeedMoreThan5Min(groupNodeDos);
			}
			if (newNodes.size() > nodeSize + 1) {
				throw new ServiceException("迁移操作,请按照添加节点,删除节点的步骤执行,不要添加太多节点");
			}

			if (!updateIds.isEmpty()) {
				log.info("online server by id : cluster is {} , ids is {}", cluster, updateIds);
				serverRepository.onlineServerByIds(Environment.env(), cluster, updateIds);
			}
			String newNodeString = String.join(",", newNodes);
			for (GroupNodeDO groupNodeDO : groupNodeDos) {
				// 迁移分组需要打开 use master 开关,进行 master 选举
				groupNodeDO.setUseMaster(UseMasterState.use.getValue());
				groupNodeDO.setNodes(newNodeString);
			}
			log.info("update group node ,info is {}", groupNodeDos);
			groupNodeRepository.batchUpdateById(Environment.env(), groupNodeDos);
			clusterRepository.updateClusterVersionByClusterName(Environment.env(), System.currentTimeMillis(), cluster);
			notifyRegistryClusterChange(cluster);
		} catch (Exception e) {
			log.error("", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	private void checkUpdateTimeNeedMoreThan5Min(List<GroupNodeDO> groupNodeDos) throws ServiceException {
		Optional<Date> lastTime = groupNodeDos.stream().map(GroupNodeDO::getUpdateTime).max(Date::compareTo);
		if (lastTime.isPresent()) {
			if (System.currentTimeMillis() - lastTime.get().getTime() < TIMEOUT) {
				// 保证数据可以全部学习过来
				throw new ServiceException("节点变更间隔需要大于 5 min");
			}
		}
	}

	/**
	 * 变更之前检测老节点状态是否都是运行中
	 *
	 * @param oldNodes
	 * @throws Exception
	 */
	private void checkOldNodeRunState(Set<String> oldNodes) throws Exception {
		for (String oldNode : oldNodes) {
			ServerDO byServer = serverRepository.getByServer(Environment.env(), oldNode);
			// 数据更新时间距离现在超过了 1min,说明节点状态不正常,需要确认 server 是否存在
			if (byServer.getLastUpdateTime() == null || byServer.getLastUpdateTime().getTime() - System.currentTimeMillis() > 1 * 60 * 1000) {
				throw new ServiceException("服务节点可能启动失败,请检查,节点 ip 为 : " + byServer.getServerAddr());
			}
		}
	}

	private void dealBackProcessNewNodeList(MigrateProcessDO migrateProcessDO) throws Exception {
		// 找到所有拉取节点是自己的节点,之后将其放在一个集群中,让他们拉取到的 group 列表包含彼此
		List<GroupNodeDO> allGroupNode = groupNodeRepository.searchByCondition(Environment.env(), -1, "", "");

		// 如果迁移到已存在的集群 ,此时不需要处理
		long count = allGroupNode.parallelStream().map(GroupNodeDO::getClusterId).collect(Collectors.toSet()).size();
		if (count > 1) {
			log.info("migrate cluster already exist");
			return;
		}

		Set<String> newNodeSet = new HashSet<String>();
		for (GroupNodeDO groupNodeDO : allGroupNode) {
			if (groupNodeDO.getServer().equals(groupNodeDO.getNodes())) {
				newNodeSet.add(groupNodeDO.getServer());
			}
		}
		List<GroupNodeDO> needUpdateGroupNode = new ArrayList<>();
		Set<Integer> groupIdSet = migrateProcessDO.groupIds();
		String newNode = StringUtils.join(newNodeSet, ",");
		for (GroupNodeDO groupNodeDO : allGroupNode) {
			// 迁移的新分组不更新
			if (groupNodeDO.getServer().equals(groupNodeDO.getNodes()) &&  groupIdSet.contains(groupNodeDO.getGroupId())) {
				groupNodeDO.setNodes(newNode);
				needUpdateGroupNode.add(groupNodeDO);
			}
		}
		if (!needUpdateGroupNode.isEmpty()) {
			groupNodeRepository.batchUpdateById(Environment.env(), needUpdateGroupNode);
		}
	}

	private void checkMasterNodeOffline(int group, String cluster, Set<String> diff, Set<String> currOldNodes) throws Exception {
		List<GroupServerRefDO> groupServerRefByClusterId = groupServerRefRepository.getGroupServerRefByClusterId(Environment.env(), cluster);
		String currentMaster = "";
		for (GroupServerRefDO groupServerRefDO : groupServerRefByClusterId) {
			if (groupServerRefDO.getGroupId() == group) {
				currentMaster = groupServerRefDO.getServerAddr();
				break;
			}
		}
		// 要删除的节点是 master 节点 ,确认当前的节点列表是不是只有老的 master 节点
		if (diff.iterator().next().equals(currentMaster)) {
			// 找到其他分组的 server 节点
			int otherGroup = 0 == group ? 1 : 0;
			// 查看 master 其他分组节点的数量
			List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), otherGroup, currentMaster, "");
			// 其他分组节点的数量
			Set<String> oldNodes = Sets.newHashSet(groupNodeDos.get(0).getNodes().split(","));
			// 取交集
			currOldNodes.retainAll(oldNodes);
			if (currOldNodes.size() != 1 || !currOldNodes.iterator().next().equals(currentMaster)) {
				throw new ServiceException("master 节点请最后下线");
			}
		}
	}


	@Override
	public void rollback(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		throw new ServiceException(ROLLBACK_MIGRATE_PROCESS_LIMIT);
	}

	private void notifyRegistryClusterChange(String clusterName) {
		PushMessage pushMessage = new PushMessage();
		pushMessage.setCluster(clusterName);
		pushMessage.setVersion(System.currentTimeMillis());
		redisUtil.publish(RedisKeyConstant.REDIS_SUBSCRIBE_CHANNEL, JSON.toJSONString(pushMessage));
	}

	public boolean checkNodeCount(Long version) throws ServiceException {
		try {
			MigrateProcessDO migrateProcess = migrateProcessRepository.searchById(Environment.env(), version);
			// 查询老节点,判断状态 :
			Set<String> oldNodes = migrateProcess.nodes();
			Map<String, ServerDO> allNode = makeAllNode();
			return makeMigrateNewNodeCount(allNode, oldNodes);
		} catch (Exception e) {
			throw new ServiceException(SERVER_EXCEPTION);
		}
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

	private boolean makeMigrateNewNodeCount(Map<String, ServerDO> allNode, Set<String> oldNodes) throws Exception {
		int count = 0;
		for (Map.Entry<String, ServerDO> node : allNode.entrySet()) {
			if (!oldNodes.contains(node.getKey())) {
				// 新节点
				count++;
			}
		}
		return count >= 3;
	}

}
