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

import com.wuba.wlock.common.enums.MigrateExecuteResult;
import com.wuba.wlock.common.enums.MigrateProcessEndState;
import com.wuba.wlock.common.enums.MigrateType;
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.repository.domain.GroupNodeDO;
import com.wuba.wlock.repository.domain.MigrateDO;
import com.wuba.wlock.repository.domain.MigrateProcessDO;
import com.wuba.wlock.repository.enums.MigrateProcessState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.ROLLBACK_MIGRATE_PROCESS_LIMIT;
import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.SERVER_EXCEPTION;

@Slf4j
@Component
public class RestoreMigrationStateOperateHandler extends BaseMigrateOperateHandlerInterface {


	@Override
	public void checkOperate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		try {
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			if (migrateProcessDO == null) {
				throw new ServiceException("迁移版本不存在,请确认迁移版本 : " + migrateControlInfoReq.getVersion());
			}
			if (migrateProcessDO.getState() == MigrateProcessState.RestoreMigrationState.getValue() || migrateProcessDO.getState() == MigrateProcessState.BackWardTransfer.getValue()) {
				// 遍历查看 t_migrate 表中数据是否都迁移完成
				Long migrateKeyVersion = migrateProcessDO.getMigrateKeyVersion();
				List<MigrateDO> migrateDos = migrateRepository.searchMigrateByCondition(Environment.env(), "", "", -1, -1, migrateKeyVersion);
				for (MigrateDO migrateDO : migrateDos) {
					if (migrateDO.getMigrateState() != MigrateType.MigrateEnd.getValue() || migrateDO.getExecuteResult() != MigrateExecuteResult.Success.getValue()) {
						throw new ServiceException("秘钥迁移没有执行完成,不允许进行迁移结束操作");
					}
				}
				log.info("RestoreMigrationStateOperateHandler check operator success");
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
		try {
			log.info(this.getClass().getName() + "start operate");
			// 清空 group node 数据
			List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), -1, "", "");
			List<Long> ids = new ArrayList<>();
			for (GroupNodeDO groupNodeDO : groupNodeDos) {
				ids.add(groupNodeDO.getId());
			}
			if (!ids.isEmpty()) {
				groupNodeRepository.deleteByIds(Environment.env(), ids);
			}
			MigrateProcessDO migrateProcessDO = migrateProcessRepository.searchById(Environment.env(), migrateControlInfoReq.getVersion());
			migrateProcessDO.setState(migrateControlInfoReq.getProcessState());
			migrateProcessDO.setIsEnd(MigrateProcessEndState.End.getValue());
			migrateProcessRepository.updateMigrateStateByKeyHash(Environment.env(), migrateProcessDO);
		} catch (Exception e) {
			log.error("RestoreMigrationState error", e);
			if (e instanceof ServiceException) {
				throw (ServiceException) e;
			}
			throw new ServiceException(SERVER_EXCEPTION);
		}
	}


	@Override
	public void rollback(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException {
		throw new ServiceException(ROLLBACK_MIGRATE_PROCESS_LIMIT);
	}
}
