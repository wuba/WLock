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

import com.wuba.wlock.repository.enums.MigrateProcessState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class MigrateOperateFactory {
	private static Map<Integer, BaseMigrateOperateHandlerInterface> operateHandlerMap = new HashMap<>();

	@Autowired
	MigrateKeyOperateHandler migrateKeyOperateHandler;
	@Autowired
	ChangeNodeOperateHandler changeNodeOperateHandler;
	@Autowired
	ChangeGroupNodeOperateHandler changeGroupNodeOperateHandler;
	@Autowired
	ClusterSplitOperateHandler clusterSplitOperateHandler;
	@Autowired
	RestoreMigrationStateOperateHandler restoreMigrationStateOperateHandler;

	@PostConstruct
	public void init() {
		operateHandlerMap.put(MigrateProcessState.ForwardMigrate.getValue(), migrateKeyOperateHandler);
		operateHandlerMap.put(MigrateProcessState.ChangeNode.getValue(), changeNodeOperateHandler);
		operateHandlerMap.put(MigrateProcessState.ChangeGroupNode.getValue(), changeGroupNodeOperateHandler);
		operateHandlerMap.put(MigrateProcessState.ClusterSplit.getValue(), clusterSplitOperateHandler);
		operateHandlerMap.put(MigrateProcessState.BackWardTransfer.getValue(), migrateKeyOperateHandler);
		operateHandlerMap.put(MigrateProcessState.RestoreMigrationState.getValue(), restoreMigrationStateOperateHandler);
	}

	public BaseMigrateOperateHandlerInterface getOperateHandler(int migrateType) {
		BaseMigrateOperateHandlerInterface migrateOperateHandlerInterface = operateHandlerMap.get(migrateType);
		if (migrateOperateHandlerInterface != null) {
			return migrateOperateHandlerInterface;
		}
		throw new IllegalArgumentException("No such migrateType " + migrateType);
	}

}
