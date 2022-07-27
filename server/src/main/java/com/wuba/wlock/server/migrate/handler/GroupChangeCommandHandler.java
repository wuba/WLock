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
package com.wuba.wlock.server.migrate.handler;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.enums.MigrateType;
import com.wuba.wlock.server.migrate.domain.GroupMigrateState;
import com.wuba.wlock.server.migrate.protocol.MigrateCommandDO;
import com.wuba.wlock.server.migrate.protocol.MigrateResult;
import com.wuba.wlock.server.migrate.protocol.MigrateSmCtx;
import com.wuba.wlock.server.migrate.service.MigrateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupChangeCommandHandler extends BaseCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(GroupChangeCommandHandler.class);

    @Override
    public boolean handle(MigrateCommandDO migrateCommandDO, MigrateSmCtx migrateSmCtx) {
        try {
            GroupMigrateState groupMigrateState = MigrateService.getInstance().getGroupMigrateState(migrateCommandDO.getSourceGroupId());
            if (groupMigrateState == null) {
                setMigrateResult(migrateSmCtx, MigrateResult.STATE_CHECK_FAIL);
                log.error("GroupChangeCommandHandler group: {} groupMigrateState is null", migrateCommandDO.getSourceGroupId());
                return true;
            }

            int state = groupMigrateState.getState();
            if (MigrateType.MigratePrepare.getValue() != state && MigrateType.MigrateGroupStartMoving.getValue() != state) {
                setMigrateResult(migrateSmCtx, MigrateResult.STATE_CHECK_FAIL);
                log.error("GroupChangeCommandHandler group: {} state not right. state: {}", migrateCommandDO.getSourceGroupId(), state);
                return true;
            }

            long version = groupMigrateState.getVersion();
            if (version != migrateCommandDO.getVersion()) {
                setMigrateResult(migrateSmCtx, MigrateResult.VERSION_CHECK_FAIL);
                log.error("GroupChangeCommandHandler group: {} version not equals. version: {} commandVersion: {}",
                        migrateCommandDO.getSourceGroupId(), version, migrateCommandDO.getVersion());
                return true;
            }

            String registerKey = groupMigrateState.getRegistryKey();
            if (!registerKey.equals(migrateCommandDO.getRegistryKey())) {
                setMigrateResult(migrateSmCtx, MigrateResult.REGISTER_KEY_CHECK_FAIL);
                log.error("GroupChangeCommandHandler group: {} registerKey not equals. registerKey: {} commandRegisterKey: {}",
                        migrateCommandDO.getSourceGroupId(), registerKey, migrateCommandDO.getRegistryKey());
                return true;
            }

            if (MigrateType.MigratePrepare.getValue() == state) {
                MigrateService.getInstance().saveGroupMirateState(migrateCommandDO.getSourceGroupId(), migrateCommandDO.getMigrateType(), migrateCommandDO.getVersion(), registerKey);
            }

            // 变更分组
            MigrateService.getInstance().changeGroup(migrateCommandDO.getSourceGroupId());

            uploadMigrateState(migrateCommandDO);

        } catch (Exception e) {
            setMigrateResult(migrateSmCtx, MigrateResult.EXCEPTION);
            log.error("GroupChangeCommandHandler error", e);
            return false;
        }

        setMigrateResult(migrateSmCtx, MigrateResult.SUCCESS);
        log.error("GroupChangeCommandHandler execute success! migrateCommandDO: {}", JSON.toJSONString(migrateCommandDO));
        return true;
    }
}
