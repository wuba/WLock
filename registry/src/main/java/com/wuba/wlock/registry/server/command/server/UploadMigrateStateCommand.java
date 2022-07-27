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
import com.wuba.wlock.common.enums.MigrateEndState;
import com.wuba.wlock.common.enums.MigrateExecuteResult;
import com.wuba.wlock.common.enums.MigrateType;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.UploadGroupMigrateState;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.service.ServerService;
import com.wuba.wlock.repository.domain.GroupNodeDO;
import com.wuba.wlock.repository.domain.MigrateDO;
import com.wuba.wlock.repository.enums.UseMasterState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UploadMigrateStateCommand implements ServerCommand{
    @Autowired
    ServerService serverService;

    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {
        RegistryProtocol resProtocol;
        UploadGroupMigrateState.State state = JSONObject.parseObject(new String(reqProtocol.getBody()), UploadGroupMigrateState.State.class);
        log.info("dispatchUploadMigrateState receive state is: {}", new String(reqProtocol.getBody()));
        MigrateDO migrateDO = serverService.getMigrateConfigByClusterAndIpAndGroup(state.getCluster(), state.getIp() + UploadGroupMigrateState.SEP + state.getPort(), state.getGroupId());
        boolean isError = true;
        try {
            // 只有成功状态上报
            if (migrateDO != null) {
                // 原状态和新状态是否一致, 不应该出现不一致的情况
                if (migrateDO.getMigrateState().intValue() == state.getState()) {
                    migrateDO.setMigrateState(state.getState());
                    migrateDO.setExecuteResult(MigrateExecuteResult.Success.getValue());
                    if (state.getState() == MigrateType.MigrateEnd.getValue() ||
                            state.getState() == MigrateType.MigratePrepareRollBack.getValue() ||
                            state.getState() == MigrateType.MigrateGroupStartMovingRollBack.getValue() ||
                            state.getState() == MigrateType.MigrateGroupMovingSafePointRollBack.getValue()) {
                        migrateDO.setEnd(MigrateEndState.End.getValue());
                    }
                    if (state.getState() == MigrateType.MigrateGroupMovingSafePoint.getValue()){
                        // 判断是否所有节点都执行完迁移安全点状态
                        List<MigrateDO> migrateConfigByCluster = serverService.getMigrateConfigByCluster(state.getCluster());
                        long count = migrateConfigByCluster.stream().filter(v -> !v.getExecuteResult().equals(MigrateExecuteResult.Success.getValue())).count();
                        if (count == 0) {
                            // 设置开启 master 选举
                            List<GroupNodeDO> needUpdate = new ArrayList<>();
                            for (MigrateDO migrate : migrateConfigByCluster) {
                                GroupNodeDO groupNodeInfo = serverService.getGroupDOByClusterGroupServer(migrate.getCluster(), migrate.getGroupId(), migrate.getServer());
                                if (groupNodeInfo != null) {
                                    groupNodeInfo.setUseMaster(UseMasterState.use.getValue());
                                    needUpdate.add(groupNodeInfo);
                                }
                            }
                            if (!needUpdate.isEmpty()) {
                                serverService.updateGroupNode(needUpdate);
                            }
                        }
                    }
                    if (serverService.updateMigrateState(migrateDO)) {
                        isError = false;
                        log.info("update migrate success : current state is {}", MigrateType.parse(state.getState()).name());
                    }
                }
            } else {
                log.warn("dispatchUploadMigrateState migrateDO is null body: {}", new String(reqProtocol.getBody()));
            }
        } catch (Exception e) {
            log.error(String.format("%s update migrate state error ", context.getChannel().getRemoteIp()), e);
        }
        if (isError) {
            resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_UPLOAD_MIGRATE_STATE, MessageType.ERROR, reqProtocol.getSessionId());
        } else {
            resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_UPLOAD_MIGRATE_STATE, MessageType.SUCCESS, reqProtocol.getSessionId());
        }
        context.setResponse(resProtocol.toBytes());
    }
}
