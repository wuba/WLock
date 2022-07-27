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
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.GetPaxosConfig;
import com.wuba.wlock.common.registry.protocol.response.GetPaxosConfRes;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.entity.ChannelMessage;
import com.wuba.wlock.registry.server.entity.ChannelMessageType;
import com.wuba.wlock.registry.server.entity.ServerResult;
import com.wuba.wlock.registry.server.manager.ChannelManager;
import com.wuba.wlock.registry.server.service.ServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServerGetPaxosConfigCommand implements ServerCommand{
    @Autowired
    ChannelManager channelManager;
    @Autowired
    ServerService serverService;
    
    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {
        RegistryProtocol resProtocol;
        try {
            GetPaxosConfig.ServerConfig getPaxos = JSONObject.parseObject(reqProtocol.getBody(), GetPaxosConfig.ServerConfig.class);
            ServerResult<GetPaxosConfRes> resServerResult = serverService.getPaxosConfig(getPaxos);

            channelManager.offer(new ChannelMessage(resServerResult.getResult().getClusterName(), ChannelMessageType.ServerChannel, context));
            if (resServerResult.isChanged()) {
                resProtocol = ProtocolFactory.getInstance().createPaxosConfigResponse(MessageType.SUCCESS, resServerResult.getResult(), reqProtocol.getSessionId());
            } else {
                resProtocol = ProtocolFactory.getInstance().createPaxosConfigResponse(MessageType.NO_CHANGE, resServerResult.getResult(), reqProtocol.getSessionId());
            }
        } catch (Exception e) {
            resProtocol = ProtocolFactory.getInstance().createPaxosConfigResponse(MessageType.ERROR, reqProtocol.getSessionId());
            log.error("{} get paxos config error ", context.getChannel().getRemoteIp());
            log.error(e.getMessage(), e);
        }
        context.setResponse(resProtocol.toBytes());
    }
}
