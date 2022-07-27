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

public class MigratePreparateCommandHandler extends BaseCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(MigratePreparateCommandHandler.class);

    @Override
    public boolean handle(MigrateCommandDO migrateCommandDO, MigrateSmCtx migrateSmCtx) {

        try {
            GroupMigrateState groupMigrateState = MigrateService.getInstance().getGroupMigrateState(migrateCommandDO.getSourceGroupId());
            if (groupMigrateState != null && groupMigrateState.isDel()) {
                log.info("MigratePreparateCommandHandler del state! groupMigrateState: {}", JSON.toJSONString(groupMigrateState));
                MigrateService.getInstance().deleteGroupMirateState(migrateCommandDO.getSourceGroupId(), false);
                groupMigrateState = null;
            }

            if (groupMigrateState == null) {
                MigrateService.getInstance().saveGroupMirateState(migrateCommandDO.getSourceGroupId(), migrateCommandDO.getMigrateType(), migrateCommandDO.getVersion(), migrateCommandDO.getRegistryKey());
                uploadMigrateState(migrateCommandDO);
            } else {
                int state = groupMigrateState.getState();
                if (MigrateType.MigratePrepare.getValue() != state) {
                    setMigrateResult(migrateSmCtx, MigrateResult.STATE_CHECK_FAIL);
                    log.error("MigratePreparateCommandHandler group: {} state not PrepareStart! state: {}",
                            migrateCommandDO.getSourceGroupId(), state);
                    return true;
                }

                long version = groupMigrateState.getVersion();
                if (version != migrateCommandDO.getVersion()) {
                    setMigrateResult(migrateSmCtx, MigrateResult.STATE_CHECK_FAIL);
                    log.error("MigratePreparateCommandHandler group: {} version not equals! version: {}, commandVersion: {}",
                            migrateCommandDO.getSourceGroupId(), version, migrateCommandDO.getVersion());
                    return true;
                }

                String registerKey = groupMigrateState.getRegistryKey();
                if (!registerKey.equals(migrateCommandDO.getRegistryKey())) {
                    setMigrateResult(migrateSmCtx, MigrateResult.REGISTER_KEY_CHECK_FAIL);
                    log.error("MigratePreparateCommandHandler group: {} registerKey not equals! registerKey: {}, commandRegisterKey: {}",
                            migrateCommandDO.getSourceGroupId(), registerKey, migrateCommandDO.getRegistryKey());
                    return true;
                }

                uploadMigrateState(migrateCommandDO);
            }
        } catch (Exception e) {
            setMigrateResult(migrateSmCtx, MigrateResult.EXCEPTION);
            log.error("MigratePreparateCommandHandler error", e);
            return false;
        }

        setMigrateResult(migrateSmCtx, MigrateResult.SUCCESS);
        log.error("MigratePreparateCommandHandler execute success! migrateCommandDO: {}", JSON.toJSONString(migrateCommandDO));
        return true;
    }
}
