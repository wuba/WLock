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

import com.wuba.wlock.common.enums.MigrateType;

import java.util.HashMap;
import java.util.Map;

public class MigrateCommandHandlerFactory {
    private static final MigrateCommandHandlerFactory INSTANCE = new MigrateCommandHandlerFactory();
    private static Map<Integer, CommandHandler> commandHandlerMap = new HashMap<Integer, CommandHandler>();
    static {
        commandHandlerMap.put(MigrateType.MigratePrepare.getValue(), new MigratePreparateCommandHandler());
        commandHandlerMap.put(MigrateType.MigratePrepareRollBack.getValue(), new MigratePreparateRollbackCommandHandler());

        commandHandlerMap.put(MigrateType.MigrateGroupStartMoving.getValue(), new GroupChangeCommandHandler());
        commandHandlerMap.put(MigrateType.MigrateGroupStartMovingRollBack.getValue(), new GroupChangeRollbackCommandHandler());

        commandHandlerMap.put(MigrateType.MigrateGroupMovingSafePoint.getValue(), new GroupChangeSafetypointCommandHandler());
        commandHandlerMap.put(MigrateType.MigrateGroupMovingSafePointRollBack.getValue(), new GroupChangeSafetypointRollbackCommandHandler());

        commandHandlerMap.put(MigrateType.MigrateEnd.getValue(), new MigrateEndCommandHandler());
    }

    private MigrateCommandHandlerFactory() {
    }

    public static MigrateCommandHandlerFactory getInstance() {
        return INSTANCE;
    }


    public CommandHandler getCommandHandler(int migrateType) {

        return commandHandlerMap.get(migrateType);
    }
}
