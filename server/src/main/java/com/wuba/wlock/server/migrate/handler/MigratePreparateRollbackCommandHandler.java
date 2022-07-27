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

public class MigratePreparateRollbackCommandHandler extends BaseCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(MigratePreparateRollbackCommandHandler.class);

    @Override
    public boolean handle(MigrateCommandDO migrateCommandDO, MigrateSmCtx migrateSmCtx) {

        try {
            GroupMigrateState groupMigrateState = MigrateService.getInstance().getGroupMigrateState(migrateCommandDO.getSourceGroupId());
            if (groupMigrateState == null) {
                setMigrateResult(migrateSmCtx, MigrateResult.STATE_CHECK_FAIL);
                uploadMigrateState(migrateCommandDO);
                log.error("MigratePreparateRollbackCommandHandler group: {} groupMigrateState is null", migrateCommandDO.getSourceGroupId());
                return true;
            }

            int state = groupMigrateState.getState();
            if (MigrateType.MigratePrepare.getValue() != state) {
                setMigrateResult(migrateSmCtx, MigrateResult.STATE_CHECK_FAIL);
                log.error("MigratePreparateRollbackCommandHandler group: {} state not PrepareStart! state: {}",
                        migrateCommandDO.getSourceGroupId(), state);
                return true;
            }

            long version = groupMigrateState.getVersion();
            if (version != migrateCommandDO.getVersion()) {
                setMigrateResult(migrateSmCtx, MigrateResult.VERSION_CHECK_FAIL);
                log.error("MigratePreparateRollbackCommandHandler group: {} version not equals! version: {}, commandVersion: {}",
                        migrateCommandDO.getSourceGroupId(), version, migrateCommandDO.getVersion());
                return true;
            }

            MigrateService.getInstance().deleteGroupMirateState(migrateCommandDO.getSourceGroupId(), false);

            uploadMigrateState(migrateCommandDO);

        } catch (Exception e) {
            setMigrateResult(migrateSmCtx, MigrateResult.EXCEPTION);
            log.error("MigratePreparateRollbackCommandHandler error", e);
            return false;
        }

        setMigrateResult(migrateSmCtx, MigrateResult.SUCCESS);
        log.error("MigratePreparateRollbackCommandHandler execute success! migrateCommandDO: {}", JSON.toJSONString(migrateCommandDO));
        return true;
    }
}
