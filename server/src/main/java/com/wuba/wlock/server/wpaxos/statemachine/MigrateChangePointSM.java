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
package com.wuba.wlock.server.wpaxos.statemachine;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.server.domain.GroupMeta;
import com.wuba.wlock.server.migrate.protocol.MigrateChangePointDO;
import com.wuba.wlock.server.migrate.service.MigrateService;
import com.wuba.wlock.server.service.GroupMetaService;
import com.wuba.wpaxos.storemachine.SMCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 迁移指令的执行，注意：没有保存到checkpoint，因为迁移指令需要从头顺序执行
 */
public class MigrateChangePointSM extends AbstractMetaStateMachine {
    private static final Logger log = LoggerFactory.getLogger(MigrateChangePointSM.class);

    public MigrateChangePointSM(int groupIdx, int smID, boolean needCheckpoint) {
        super(groupIdx, smID, needCheckpoint);
    }

    @Override
    public boolean execute(int groupIdx, long instanceID, byte[] paxosValue, SMCtx smCtx) {
        try {
            MigrateChangePointDO migrateChangePointDO = MigrateChangePointDO.fromBytes(paxosValue);
            log.error("MigrateChangePointSM migrateChangePointDO: {} ,current instance id is  {}", JSON.toJSONString(migrateChangePointDO),instanceID);
            MigrateService.getInstance().setMigratePoint(migrateChangePointDO);

            GroupMeta groupMeta = GroupMetaService.getInstance().get(groupIdx);
            return this.executeForCheckpoint(groupIdx, instanceID, groupMeta.toBytes());
        } catch (Exception e) {
            log.error("MigratePointInfoSM error", e);
            return false;
        }
    }

    @Override
    public byte[] beforePropose(int groupIdx, byte[] sValue) {
        return new byte[0];
    }

    @Override
    public boolean needCallBeforePropose() {
        return false;
    }
}
