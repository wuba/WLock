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

import com.wuba.wlock.common.enums.ChangeNodeOperateType;
import com.wuba.wlock.common.enums.MigrateExecuteResult;
import com.wuba.wlock.common.enums.MigrateType;
import com.wuba.wlock.registry.admin.constant.ExceptionConstant;
import com.wuba.wlock.registry.admin.domain.request.ChangeNodeOperateInfoReq;
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.admin.validators.ParamValidateUtil;
import com.wuba.wlock.registry.admin.validators.ValidateResult;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.repository.domain.*;
import com.wuba.wlock.repository.enums.MasterLoadBalance;
import com.wuba.wlock.repository.enums.MigrateProcessState;
import com.wuba.wlock.repository.enums.ServerState;
import com.wuba.wlock.repository.enums.UseMasterState;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.*;


@Slf4j
@Component
public class ChangeNodeOperateHandler extends BaseMigrateOperateHandlerInterface {
	private static final String COLON_SEPARATOR = ":";

	@Override
	public void checkOperate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		try {
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移版本不存在,请确认迁移版本 : " + migrateControlInfoReq.getVersion());
			}
			if (migrateProcessDO.getState() == MigrateProcessState.ChangeNode.getValue() || migrateProcessDO.getState() == MigrateProcessState.ForwardMigrate.getValue()) {
				// 遍历查看 t_migrate 表中数据是否都迁移完成
				Long migrateKeyVersion = migrateProcessDO.getMigrateKeyVersion();
				List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateKeyVersion);
				for (MigrateDO migrateDO : migrateDos) {
					if (migrateDO.getMigrateState() != MigrateType.MigrateEnd.getValue() || migrateDO.getExecuteResult() != MigrateExecuteResult.Success.getValue()) {
						throw new ServiceException(MIGRATE_GROUP_NODE_ADD_CHECK);
					}
				}
				log.info("ChangeNodeOperateHandler : check operator success");
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
		// 查看操作是添加还是删除 : 操作的意义是需要区分出来 是添加错节点了要删除 , 还是下线老节点
		try {
			log.info(this.getClass().getName() + "start operate");
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移过程版本错误,请检查版本 : " + migrateControlInfoReq.getVersion());
			}
			migrateProcessDO.setState(migrateControlInfoReq.getProcessState());
			migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);
			if (Strings.isEmpty(migrateControlInfoReq.getOperateInfo())) {
				log.info("ChangeGroupNodeOperateHandler operator info is null , only update migrate process state");
				return;
			}
			ChangeNodeOperateInfoReq changeGroupNodeOperateInfo = migrateRequestParseFactory.parse(migrateControlInfoReq.getOperateInfo(), ChangeNodeOperateInfoReq.class);
			ValidateResult valid = ParamValidateUtil.valid(changeGroupNodeOperateInfo);
			if (!valid.isPass()) {
				throw new ServiceException(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg());
			}
			// 清空前后空格
			String server = changeGroupNodeOperateInfo.getIp().trim() + COLON_SEPARATOR + changeGroupNodeOperateInfo.getTcpPort();
			ServerDO serverDO = serverRepository.getByServer(Environment.env(), server);
			List<GroupNodeDO> groupNodeDos = new ArrayList<>();
			if (changeGroupNodeOperateInfo.getOperator() == ChangeNodeOperateType.Add.getValue()) {
				dealAddNode(changeGroupNodeOperateInfo, server, serverDO, groupNodeDos);
			} else {
				dealDelNode(server, serverDO);
			}
		} catch (Exception e) {
			log.error("", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}

	private void dealDelNode(String server, ServerDO serverDO) throws Exception {
		// 删除节点
		if (serverDO == null) {
			throw new ServiceException(SERVER_UN_EXISTED);
		}
		if (serverDO.getState() == ServerState.offline.getValue()) {
			serverRepository.deleteServerById(Environment.env(), serverDO.getId());
		}
		log.info("del node : {}", serverDO);
		List<GroupNodeDO> needDel = groupNodeRepository.searchByCondition(Environment.env(), -1, server, "");
		if (!needDel.isEmpty()) {
			log.info("del group node info  : {}", needDel);
			List<Long> delIds = new ArrayList<>();
			for (GroupNodeDO groupNodeDO : needDel) {
				delIds.add(groupNodeDO.getId());
			}
			groupNodeRepository.deleteByIds(Environment.env(), delIds);
		}
	}

	private void dealAddNode(ChangeNodeOperateInfoReq changeGroupNodeOperateInfo, String server, ServerDO serverDO, List<GroupNodeDO> groupNodeDos) throws Exception {
		// 参数检验
		// 判断是添加还是更新
		boolean hasSameSequenceIdFlag;
		String serverAddress = "";
		if (serverDO == null) {
			// 添加
			hasSameSequenceIdFlag = serverRepository.isHasSameSequenceId(Environment.env(), changeGroupNodeOperateInfo.getClusterName(), changeGroupNodeOperateInfo.getSequenceId());
			if (hasSameSequenceIdFlag) {
				throw new ServiceException(ExceptionConstant.SERVER_SEQID_EXIST);
			}
			serverDO = new ServerDO();
			serverDO.setSequenceId(changeGroupNodeOperateInfo.getSequenceId());
			serverDO.setServerAddr(server);
			serverDO.setPaxosPort(changeGroupNodeOperateInfo.getPaxosPort());
			serverDO.setClusterId(changeGroupNodeOperateInfo.getClusterName());
			serverDO.setUdpPort(changeGroupNodeOperateInfo.getUdpPort());
			serverDO.setState(ServerState.offline.getValue());
			serverAddress = serverDO.getServerAddr();
			serverRepository.saveServer(Environment.env(), serverDO);
		} else {
			throw new ServiceException(SERVER_EXISTED);
		}
		// t_groupnode 表中也要添加数据,这里拉取到的 server 列表只有自己
		// 取出集群 , 找到 group count
		ClusterDO clusterInfo = clusterRepository.getClusterByClusterName(Environment.env(), changeGroupNodeOperateInfo.getClusterName());
		for (int i = 0; i < clusterInfo.getGroupCount() * CommonConstant.TWO; i++) {
			GroupNodeDO groupNodeDO = new GroupNodeDO();
			groupNodeDO.setClusterId(serverDO.getClusterId());
			groupNodeDO.setGroupId(i);
			groupNodeDO.setServer(serverDO.getServerAddr());
			// 前一半分组保持不变,后一半分组使用自己
			groupNodeDO.setNodes(i >= clusterInfo.getGroupCount() ? serverDO.getServerAddr() : serverAddress);
			groupNodeDO.setCreateTime(new Date());
			// 新节点 开启 master 选举 和 负载均衡不开启
			groupNodeDO.setLoadBalance(MasterLoadBalance.noUse.getValue());
			groupNodeDO.setUseMaster(UseMasterState.noUse.getValue());
			groupNodeDos.add(groupNodeDO);
		}
		if (!groupNodeDos.isEmpty()) {
			log.info("batch save node info is {}", groupNodeDos);
			groupNodeRepository.batchSave(Environment.env(), groupNodeDos);
		}
	}

	@Override
	public void rollback(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		throw new ServiceException(ROLLBACK_MIGRATE_PROCESS_LIMIT);
	}
}
