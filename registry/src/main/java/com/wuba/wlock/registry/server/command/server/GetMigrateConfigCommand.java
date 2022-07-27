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
package com.wuba.wlock.registry.server.command.server;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.enums.MigrateExecuteResult;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.GetGroupMigrateConfig;
import com.wuba.wlock.common.registry.protocol.response.GetGroupMigrateConfigRes;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.service.ServerService;
import com.wuba.wlock.repository.domain.MigrateDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GetMigrateConfigCommand implements ServerCommand{
    @Autowired
    ServerService serverService;

    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {
        RegistryProtocol resProtocol;
        GetGroupMigrateConfig.Config serverConfig = JSONObject.parseObject(new String(reqProtocol.getBody()), GetGroupMigrateConfig.Config.class);
        log.info("dispatchGetMigrateConfig receive Config is: {}", new String(reqProtocol.getBody()));
        List<MigrateDO> migrateConfigByClusterAndIp = serverService.getMigrateConfigByClusterAndIp(serverConfig.getCluster(), serverConfig.getIp() + GetGroupMigrateConfig.SEP + serverConfig.getPort());
        try {
            Set<Integer> groups = new HashSet<>();
            if (!migrateConfigByClusterAndIp.isEmpty()) {
                List<MigrateDO> updateMigrateDOs = new ArrayList<MigrateDO>();
                Map<Integer, List<MigrateDO>> migrateDoMap = migrateConfigByClusterAndIp.stream().collect(Collectors.groupingBy(MigrateDO::getGroupId));
                for (Map.Entry<Integer, List<MigrateDO>> entry: migrateDoMap.entrySet()) {
                    Integer groupId = entry.getKey();
                    List<MigrateDO> migrateDos = entry.getValue();
                    boolean isSuccess = false;
                    for (MigrateDO migrateDO: migrateDos) {
                        if (migrateDO.getExecuteResult() == MigrateExecuteResult.Success.getValue()) {
                            isSuccess = true;
                            break;
                        }
                    }

                    if (!isSuccess) {
                        groups.add(groupId);
                        updateMigrateDOs.addAll(migrateDos);
                    }
                }

                if (!groups.isEmpty()) {
                    MigrateDO tmpDo = migrateConfigByClusterAndIp.get(0);
                    GetGroupMigrateConfigRes getGroupMigrateConfigRes = new GetGroupMigrateConfigRes();
                    getGroupMigrateConfigRes.setMigrateType(tmpDo.getMigrateState());
                    getGroupMigrateConfigRes.setRegisterKey(tmpDo.getKeyHash());
                    getGroupMigrateConfigRes.setSourceGroups(new ArrayList<>(groups));
                    getGroupMigrateConfigRes.setVersion(tmpDo.getVersion());
                    resProtocol = ProtocolFactory.getInstance().createGroupMigrateConfigResponse(MessageType.SUCCESS, reqProtocol.getSessionId(), getGroupMigrateConfigRes);
                    log.info("receive migrate config is : {}", getGroupMigrateConfigRes);
                    // 更新执行状态
                    for (MigrateDO updateMigrate: updateMigrateDOs) {
                        updateMigrate.setExecuteResult(MigrateExecuteResult.Running.getValue());
                        serverService.updateMigrateState(updateMigrate);
                    }
                } else {
                    // 没有处于迁移状态 , 服务端不作处理
                    log.info("Not in the migrated state");
                    resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_GROUP_MIGRATE_CONFIG, MessageType.ERROR, reqProtocol.getSessionId());
                }
            } else {
                // 没有处于迁移状态 , 服务端不作处理
                log.info("Not in the migrated state");
                resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_GROUP_MIGRATE_CONFIG, MessageType.ERROR, reqProtocol.getSessionId());
            }
        } catch (Exception e) {
            resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_GROUP_MIGRATE_CONFIG, MessageType.ERROR, reqProtocol.getSessionId());
            log.error(String.format("%s get migrate config error ", context.getChannel().getRemoteIp()), e);
        }
        context.setResponse(resProtocol.toBytes());
    }
}
