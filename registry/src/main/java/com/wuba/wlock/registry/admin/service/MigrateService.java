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
package com.wuba.wlock.registry.admin.service;

import com.wuba.wlock.common.enums.MigrateEndState;
import com.wuba.wlock.common.enums.MigrateExecuteResult;
import com.wuba.wlock.common.enums.MigrateProcessEndState;
import com.wuba.wlock.common.enums.MigrateType;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.domain.request.MigrateKeyInfoReq;
import com.wuba.wlock.registry.admin.domain.response.MigrateBaseResp;
import com.wuba.wlock.registry.admin.domain.response.MigrateGroupNodeInfoResp;
import com.wuba.wlock.registry.admin.domain.response.MigrateResp;
import com.wuba.wlock.registry.admin.domain.response.ServerResp;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.admin.migrate.MigrateOperateFactory;
import com.wuba.wlock.registry.util.IDHelper;
import com.google.common.collect.Sets;
import com.wuba.wlock.repository.domain.*;
import com.wuba.wlock.repository.enums.*;
import com.wuba.wlock.repository.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.*;

@Service
@Slf4j
public class MigrateService {
	@Autowired
    MigrateRepository migrateRepository;
	@Autowired
    ClusterRepository clusterRepository;
	@Autowired
    ServerRepository serverRepository;
	@Autowired
    KeyRepository keyRepository;
	@Autowired
	GroupNodeRepository groupNodeRepository;
	@Autowired
    MigrateProcessRepository migrateProcessRepository;
	@Autowired
	GroupServerRefRepository groupServerRefRepository;
	@Autowired
	MigrateOperateFactory migrateOperateFactory;


	public Long migrateProcessStart(MigrateKeyInfoReq migrateKeyInfoReq) throws ServiceException {
		try {
			log.info("start migrateProcessStart : migrate info is {}", migrateKeyInfoReq);
			ClusterDO clusterInfo = clusterRepository.getClusterByClusterName(Environment.env(), migrateKeyInfoReq.getClusterName());
			KeyDO keyDO = keyRepository.getKeyByName(Environment.env(), migrateKeyInfoReq.getKeyName());
			checkParam(migrateKeyInfoReq, clusterInfo, keyDO);
			// 1. ?????????????????????????????????
			List<MigrateProcessDO> migrateProcesses = migrateProcessRepository.searchByCondition(Environment.env(), -1, keyDO.getHashKey(), -1L, MigrateProcessEndState.NoEnd.getValue());
			Long version = IDHelper.getUniqueId();
			boolean needSaveGroupNode = false;
			MigrateProcessDO migrateProcess = null;
			if (!migrateProcesses.isEmpty()) {
				// ?????????????????????????????????,???????????????????????????
				assert migrateProcesses.size() == 1 : "??????????????????????????????";
				migrateProcess = migrateProcesses.get(0);
				// ?????? migrate_key_version ,??? t_migrate ????????????????????? ????????????,???????????????????????? ,?????????????????????????????????
				Long migrateKeyVersion = migrateProcess.getMigrateKeyVersion();
				List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateKeyVersion);
				for (MigrateDO migrateDO : migrateDos) {
					// ?????????????????????????????????????????????????????????
					if (migrateDO.getMigrateState() < MigrateType.MigratePrepareRollBack.getValue()
							|| migrateDO.getMigrateState() > MigrateType.MigrateGroupMovingSafePointRollBack.getValue()
							|| migrateDO.getExecuteResult() != MigrateExecuteResult.Success.getValue()) {
						log.error("migrateProcessStart error : info is  " + KEY_MIGRATE_PROCESS_LIMIT);
						throw new ServiceException(KEY_MIGRATE_PROCESS_LIMIT);
					}
				}
				// ??????????????? ,?????? update ?????? , ?????? version
				migrateProcess.setMigrateKeyVersion(version);
				migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcess);
			} else {
				// ???????????? t_migrate_process
				migrateProcess = new MigrateProcessDO();
				migrateProcess.setState(MigrateProcessState.ForwardMigrate.getValue());
				migrateProcess.setKayHash(keyDO.getHashKey());
				migrateProcess.setMigrateKeyVersion(version);
				migrateProcess.setIsEnd(MigrateProcessEndState.NoEnd.getValue());
				migrateProcess.setCreateTime(new Date());
				migrateProcess.setGroups(keyDO.getGroupIds());
				// group node ????????????????????????????????????
				needSaveGroupNode = true;
			}
			// ???????????? t_migrate
			List<MigrateDO> migrateDos = new ArrayList<>();
			List<GroupNodeDO> groupNodeDos = new ArrayList<>();
			String nodes = getMigrateSaveData(migrateKeyInfoReq, migrateProcess.getGroups(), clusterInfo, keyDO, version, migrateDos, groupNodeDos, needSaveGroupNode);
			if (needSaveGroupNode) {
				migrateProcess.setNodes(nodes);
				migrateProcessRepository.saveMigrateProcess(Environment.env(), migrateProcess);
			}

			if (!migrateDos.isEmpty()) {
				migrateRepository.batchSaveMigrate(Environment.env(), migrateDos);
			}
			if (!groupNodeDos.isEmpty()) {
				groupNodeRepository.batchSave(Environment.env(), groupNodeDos);
			}
			// ???????????? id ??????????????????????????? id
			List<MigrateProcessDO> resultProcess = migrateProcessRepository.searchByCondition(Environment.env(), -1, keyDO.getHashKey(), -1L, MigrateProcessEndState.NoEnd.getValue());
			long result = resultProcess.get(0).getId();
			log.info("start migrate success, id is : {}", result);
			return result;
		} catch (Exception e) {
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			log.error("", e);
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	private String getMigrateSaveData(MigrateKeyInfoReq migrateKeyInfoReq, String groupIds, ClusterDO clusterInfo, KeyDO keyByName, Long version, List<MigrateDO> migrateDos, List<GroupNodeDO> groupNodeDos, boolean needSaveGroupNode) throws Exception {
		// ????????? group ?????????????????????
		List<ServerDO> serviceInfos = serverRepository.getServerByClusterIdAndState(Environment.env(), clusterInfo.getClusterId(), ServerState.online.getValue());
		for (String groupId : groupIds.split(CommonConstant.COMMA)) {
			for (ServerDO serviceInfo : serviceInfos) {
				MigrateDO migrateDO = new MigrateDO();
				migrateDO.setVersion(version);
				migrateDO.setGroupId(Integer.parseInt(groupId));
				migrateDO.setCluster(migrateKeyInfoReq.getClusterName());
				migrateDO.setMigrateState(MigrateType.Init.getValue());
				migrateDO.setServer(serviceInfo.getServerAddr());
				// ??????????????????????????? ???????????????
				migrateDO.setExecuteResult(MigrateExecuteResult.Success.getValue());
				migrateDO.setKeyHash(keyByName.getHashKey());
				migrateDO.setCreateTime(new Date());
				migrateDO.setEnd(MigrateEndState.NoEnd.getValue());
				migrateDos.add(migrateDO);
			}
		}
		if (!needSaveGroupNode) {
			return null;
		}
		//  t_groupnode ???????????????????????? : ??????????????????????????????????????????,????????????????????????,???????????? master ??????,batch ??????
		StringBuilder defaultNodes = new StringBuilder();
		for (ServerDO serviceInfo : serviceInfos) {
			defaultNodes.append(serviceInfo.getServerAddr()).append(",");
		}
		defaultNodes.deleteCharAt(defaultNodes.length() - 1);
		// 	??????????????? : ??????????????????????????????,?????????????????? t_groupNode ??????,????????????,???????????????????????????????????????????????????????????????????????????.
		for (int i = 0; i < clusterInfo.getGroupCount() * CommonConstant.TWO; i++) {
			for (ServerDO serviceInfo : serviceInfos) {
				GroupNodeDO groupNodeDO = new GroupNodeDO();
				groupNodeDO.setClusterId(serviceInfo.getClusterId());
				groupNodeDO.setGroupId(i);
				groupNodeDO.setServer(serviceInfo.getServerAddr());
				groupNodeDO.setNodes(defaultNodes.toString());
				groupNodeDO.setCreateTime(new Date());
				// ??????????????? ??????????????? ?????????????????? ??????????????????????????????????????????
				if (i < clusterInfo.getGroupCount()) {
					// ??????
					groupNodeDO.setLoadBalance(MasterLoadBalance.use.getValue());
					groupNodeDO.setUseMaster(UseMasterState.use.getValue());
				} else {
					groupNodeDO.setLoadBalance(MasterLoadBalance.noUse.getValue());
					groupNodeDO.setUseMaster(UseMasterState.noUse.getValue());
				}
				groupNodeDos.add(groupNodeDO);
			}
		}

		return defaultNodes.toString();
	}


	public List<MigrateResp> migrateList(Long processVersion) throws ServiceException {
		try {
			// 1. ??????????????? t_migrate_process ????????? t_migrate ??? migrate_version
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), processVersion);
			Long migrateKeyVersion = migrateProcessDO.getMigrateKeyVersion();
			List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateKeyVersion);
			// 2. ?????? migrate_version ??????????????????
			List<MigrateResp> migrateResps = new ArrayList<>();
			for (MigrateDO migrateDO : migrateDos) {
				MigrateResp migrateResp = new MigrateResp();
				migrateResp.setId(migrateDO.getId());
				migrateResp.setServer(migrateDO.getServer());
				// ????????????????????????????????? group ????????????????????????
				migrateResp.setGroupId(migrateDO.getGroupId());
				migrateResp.setMigrate(migrateDO.getMigrateState());
				migrateResp.setExecuteResult(migrateDO.getExecuteResult());
				migrateResp.setIsEnd(migrateDO.getEnd());
				migrateResps.add(migrateResp);
			}
			return migrateResps;
		} catch (Exception e) {
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			log.error("", e);
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}


	public void rollback(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		log.info("migrate rollback info is  {}", migrateControlInfoReq);
		migrateOperateFactory.getOperateHandler(migrateControlInfoReq.getProcessState()).checkOperate(migrateControlInfoReq);
		migrateOperateFactory.getOperateHandler(migrateControlInfoReq.getProcessState()).rollback(migrateControlInfoReq);
	}


	public void migrateOperate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		log.info("migrate operate info is  {}", migrateControlInfoReq);
		migrateOperateFactory.getOperateHandler(migrateControlInfoReq.getProcessState()).checkOperate(migrateControlInfoReq);
		migrateOperateFactory.getOperateHandler(migrateControlInfoReq.getProcessState()).operate(migrateControlInfoReq);
	}


	public MigrateGroupNodeInfoResp groupNodeInfo(Long version) throws ServiceException {
		try {
			MigrateProcessDO migrateProcess = migrateProcessRepository.searchById(Environment.env(), version);

			KeyDO keyByHashKey = keyRepository.getKeyByHashKey(Environment.env(), migrateProcess.getKayHash());
			ClusterDO clusterByClusterName = clusterRepository.getClusterByClusterName(Environment.env(), keyByHashKey.getClusterId());
			int groupId = migrateProcess.groupIds().iterator().next() + clusterByClusterName.getGroupCount();
			Set<String> oldNodes = migrateProcess.nodes();
			Set<String> currGroupNodeInfo = makeCurrentGroupNodeInfo(groupId);
			Map<String, ServerDO> allNode = makeAllNode();
			List<MigrateGroupNodeInfoResp.MigrateNodeInfo> migrateNodeInfos = makeMigrateNodeInfos(currGroupNodeInfo, allNode, oldNodes, null);
			MigrateGroupNodeInfoResp result = new MigrateGroupNodeInfoResp();
			result.setAllNode(migrateNodeInfos);
			return result;
		} catch (Exception e) {
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	private String makeMasterAddress(ClusterDO clusterByClusterName, int groupId) throws Exception {
		List<GroupServerRefDO> groupServerRefByClusterAndServer = groupServerRefRepository.getGroupServerRefByClusterId(Environment.env(), clusterByClusterName.getClusterId());
		Optional<String> masterNode = groupServerRefByClusterAndServer.stream().filter(groupServerRefDO -> groupServerRefDO.getGroupId() == groupId).map(GroupServerRefDO::getServerAddr).findFirst();
		return masterNode.orElse("");
	}

	/**
	 * ??????????????? group ?????????????????????
	 *
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	private Set<String> makeOldNodes(int groupId) throws Exception {
		// ???????????????,???????????? :
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

	private List<MigrateGroupNodeInfoResp.MigrateNodeInfo> makeMigrateNodeInfos(Set<String> currGroupNodeInfo, Map<String, ServerDO> allNode, Set<String> oldNodes, String masterAddress) {
		List<MigrateGroupNodeInfoResp.MigrateNodeInfo> migrateNodeInfos = new ArrayList<>();
		for (Map.Entry<String, ServerDO> node : allNode.entrySet()) {
			MigrateGroupNodeInfoResp.MigrateNodeInfo migrateNodeInfo = new MigrateGroupNodeInfoResp.MigrateNodeInfo();
			migrateNodeInfo.setNode(node.getKey());
			// ??????????????????????????????
			migrateNodeInfo.setState(currGroupNodeInfo.contains(node.getKey()) ? ServerState.online.getValue() : ServerState.offline.getValue());
			migrateNodeInfo.setId(node.getValue().getId());
			// 1 :??????????????? , 0 ???????????????
			migrateNodeInfo.setIsNewNode(oldNodes.contains(node.getKey()) ? 1 : 0);
			migrateNodeInfos.add(migrateNodeInfo);
		}
		return migrateNodeInfos;
	}

	private List<ServerResp> makeMigrateNewNodeInfos(Map<String, ServerDO> allNode, Set<String> oldNodes) throws Exception {
		List<ServerResp> result = new ArrayList<>();
		for (Map.Entry<String, ServerDO> node : allNode.entrySet()) {
			if (!oldNodes.contains(node.getKey())) {
				// ?????????
				ServerDO serverDO = serverRepository.getByServer(Environment.env(), node.getKey());
				ServerResp serverResp = new ServerResp();
				serverResp.setId(String.valueOf(serverDO.getId()));
				serverResp.setServer(serverDO.getServerAddr());
				serverResp.setPaxosPort(serverDO.getPaxosPort());
				serverResp.setClusterName(serverDO.getClusterId());
				serverResp.setState(String.valueOf(serverDO.getState()));
				serverResp.setSequenceId(serverDO.getSequenceId());
				serverResp.setUdpPort(serverDO.getUdpPort());
				result.add(serverResp);
			}
		}
		return result;
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

	private Set<String> makeCurrentGroupNodeInfo(int groupId) throws Exception {
		List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), groupId, "", "");
		Set<String> currGroupNodeInfo = new HashSet<>();
		for (GroupNodeDO groupNodeDO : groupNodeDos) {
			/*
				???????????????????????????
				???????????? : ?????????????????????????????????????????????????????????????????????????????????
			 */
			if (groupNodeDO.getNodes().equals(groupNodeDO.getServer())) {
				continue;
			}
			currGroupNodeInfo.addAll(Sets.newHashSet(groupNodeDO.getNodes().split(",")));
		}
		return currGroupNodeInfo;
	}

	private void checkParam(MigrateKeyInfoReq migrateKeyInfoReq, ClusterDO clusterInfo, KeyDO keyByName) throws
			ServiceException {
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


	public void mandatoryEnd(Long version) throws ServiceException {
		try {
			// ????????????
			// 1. ?????????????????????????????????????????????????????????
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), version);

			// check state
			if (migrateProcessDO.getIsEnd() == MigrateProcessEndState.End.getValue()) {
				throw new ServiceException("???????????????????????????,?????????????????????");
			}
			if (migrateProcessDO.getState() != MigrateProcessState.ForwardMigrate.getValue()) {
				throw new ServiceException("????????????????????????????????????????????????");
			}
			Long migrateKeyVersion = migrateProcessDO.getMigrateKeyVersion();
			List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateKeyVersion);
			for (MigrateDO migrateDO : migrateDos) {
				if (migrateDO.getMigrateState() < MigrateType.MigratePrepareRollBack.getValue()
						|| migrateDO.getMigrateState() > MigrateType.MigrateGroupMovingSafePointRollBack.getValue()
						|| migrateDO.getExecuteResult() != MigrateExecuteResult.Success.getValue()) {
					throw new ServiceException("???????????????????????????????????????,????????????????????????");
				}
			}

			migrateProcessDO.setIsEnd(MigrateProcessEndState.End.getValue());
			migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);

			// 2. ?????? group node ???????????? , ?????? t_migrate_process ?????????
			List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), -1, "", "");
			List<Long> ids = new ArrayList<>();
			for (GroupNodeDO groupNodeDO : groupNodeDos) {
				ids.add(groupNodeDO.getId());
			}
			groupNodeRepository.deleteByIds(Environment.env(), ids);
		} catch (Exception e) {
			log.error("", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	public Set<Integer> clusterGroupIdsSearch(String clusterName) throws ServiceException {
		try {
			ClusterDO clusterByClusterName = clusterRepository.getClusterByClusterName(Environment.env(), clusterName);
			if (clusterByClusterName == null) {
				throw new ServiceException(CLUSTER_NOT_EXIST);
			}
			Set<Integer> groupIds = new HashSet<>();
			for (int i = 0; i < clusterByClusterName.getGroupCount() * CommonConstant.TWO; i++) {
				groupIds.add(i);
			}
			return groupIds;
		} catch (Exception e) {
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	public MigrateBaseResp searchMigrateVersion() throws ServiceException {
		try {
			List<MigrateProcessDO> migrateProcesses = migrateProcessRepository.searchByCondition(Environment.env(), -1, "", -1L, MigrateProcessEndState.NoEnd.getValue());
			if (!migrateProcesses.isEmpty()) {
				MigrateProcessDO migrateProcessDO = migrateProcesses.get(0);
				KeyDO keyByHashKey = keyRepository.getKeyByHashKey(Environment.env(), migrateProcessDO.getKayHash());
				MigrateBaseResp migrateBaseResp = new MigrateBaseResp();
				migrateBaseResp.setVersion(migrateProcessDO.getId());
				migrateBaseResp.setKey(keyByHashKey.getName());
				migrateBaseResp.setState(migrateProcessDO.getState());
				migrateBaseResp.setCluster(keyByHashKey.getClusterId());
				migrateBaseResp.setGroup(keyByHashKey.getMultiGroup()== MultiGroup.UnUse.getValue() ? keyByHashKey.getGroupIds(): "All");
				return migrateBaseResp;
			}
			return new MigrateBaseResp();
		} catch (Exception e) {
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	public List<ServerResp> migrateOfflineNodeList(Long version) throws ServiceException {
		try {
			MigrateProcessDO migrateProcess = migrateProcessRepository.searchById(Environment.env(), version);
			assert migrateProcess.getGroups().split(",").length == 1 : "????????????????????????";
			Set<String> oldNodes = migrateProcess.nodes();
			Map<String, ServerDO> allNode = makeAllNode();
			return makeMigrateNewNodeInfos(allNode, oldNodes);
		} catch (Exception e) {
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

}
