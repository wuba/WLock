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
import com.wuba.wlock.server.migrate.handler.CommandHandler;
import com.wuba.wlock.server.migrate.handler.MigrateCommandHandlerFactory;
import com.wuba.wlock.server.migrate.protocol.MigrateCommandDO;
import com.wuba.wlock.server.migrate.protocol.MigrateSmCtx;
import com.wuba.wpaxos.storemachine.SMCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateCommandSM extends AbstractStateMachine{
    private static final Logger log = LoggerFactory.getLogger(MigrateCommandSM.class);

    public MigrateCommandSM(int groupIdx, int smID, boolean needCheckpoint) {
        super(groupIdx, smID, needCheckpoint);
    }

    @Override
    public boolean execute(int groupIdx, long instanceID, byte[] paxosValue, SMCtx smCtx) {
        MigrateCommandDO migrateCommandDO = MigrateCommandDO.fromBytes(paxosValue);
        log.error("MigrateCommandSM MigrateCommandDO: {}", JSON.toJSONString(migrateCommandDO));
        MigrateSmCtx migrateSmCtx = null;
        if (smCtx != null) {
            migrateSmCtx = (MigrateSmCtx) smCtx.getpCtx();
        }

        CommandHandler commandHandler = MigrateCommandHandlerFactory.getInstance().getCommandHandler(migrateCommandDO.getMigrateType());
        if (commandHandler == null) {
            log.error("MigrateCommandSM CommandHandler not exit! MigrateType: {}", migrateCommandDO.getMigrateType());
            return true;
        }
        return commandHandler.handle(migrateCommandDO, migrateSmCtx);
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
