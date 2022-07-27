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

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.GetRegistryKeyQps;
import com.wuba.wlock.common.registry.protocol.response.GetRegistryKeyQpsRes;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.entity.ServerResult;
import com.wuba.wlock.registry.server.service.ServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GetKeyQpsCommand implements ServerCommand {
    @Autowired
    ServerService serverService;

    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {
        RegistryProtocol resProtocol;
        try {
            String cluster = GetRegistryKeyQps.getCluster(reqProtocol.getBody());
            long version = GetRegistryKeyQps.getKeyVersion(reqProtocol.getBody());
            log.info("get key qps cluster {} version {}", cluster, version);

            ServerResult<GetRegistryKeyQpsRes> result = serverService.getKeyQps(cluster, version);
            if (result.isChanged()) {
                log.info("get key qps cluster {} version {} response {}", cluster, version, JSON.toJSONString(result.getResult()));
                resProtocol = ProtocolFactory.getInstance().createKeyQpsResponse(MessageType.SUCCESS, reqProtocol.getSessionId(), result.getResult());
            } else {
                log.info("get key qps cluster {} version {} response not change", cluster, version);
                resProtocol = ProtocolFactory.getInstance().createKeyQpsResponse(MessageType.NO_CHANGE, reqProtocol.getSessionId(), result.getResult());
            }
        } catch (Exception e) {
            resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_REGISTRY_KEY_QPS, MessageType.ERROR, reqProtocol.getSessionId());
            log.error(String.format("%s get key qps error ", context.getChannel().getRemoteIp()), e);
        }
        context.setResponse(resProtocol.toBytes());
    }
}
