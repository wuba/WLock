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
package com.wuba.wlock.registry.server.command.client;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.entity.ClientKeyEntity;
import com.wuba.wlock.common.exception.ProtocolException;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.entity.ChannelMessage;
import com.wuba.wlock.registry.server.entity.ChannelMessageType;
import com.wuba.wlock.registry.server.entity.ServerResult;
import com.wuba.wlock.registry.server.manager.ChannelManager;
import com.wuba.wlock.registry.server.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClientConfigGetCommand implements ClientCommand {
    @Autowired
    ClientService clientService;

    @Autowired
    ChannelManager channelManager;


    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {
        if (reqProtocol.getMsgType() == MessageType.REQUEST_ALL_DATAS) {
            try {
                ClientKeyEntity reqClientKey = getReqClientKeyEntity(reqProtocol);
                log.info("{}:client pull config request content:{}", context.getChannel().getRemoteIp(), reqClientKey.toString());
                channelManager.offer(new ChannelMessage(reqClientKey.getKey(), ChannelMessageType.ClientChannel, context));

                ClientKeyEntity resClientKey = clientService.getClientKeyEntity(reqClientKey.getKey());

                String resString = clientKeyToString(resClientKey);

                log.info("{}:client pull config response content: {}", context.getChannel().getRemoteIp(), resClientKey.toString());
                RegistryProtocol resProtocol = ProtocolFactory.getInstance().createClientConfigResponse(reqProtocol.getOpaque(), MessageType.RESPONSE_ALL_DATAS_SUCCESS, resString.getBytes(), reqProtocol.getSessionId());
                context.setResponse(resProtocol.toBytes());
            } catch (Exception e) {
                RegistryProtocol errProtocol = ProtocolFactory.getInstance().createClientConfigResponse(reqProtocol.getOpaque(), MessageType.RESPONSE_ALL_DATAS_ERROR,
                        "client get all config error".getBytes(), reqProtocol.getSessionId());
                context.setResponse(errProtocol.toBytes());
                log.error(String.format("%s:client get all configuration failed!", context.getChannel().getRemoteIp()), e);
            }
        } else if (reqProtocol.getMsgType() == MessageType.REQUEST_VALIDATION) {
            try {
                ClientKeyEntity reqClientKey = getReqClientKeyEntity(reqProtocol);
                log.info("{}:client validate request content:{}", context.getChannel().getRemoteIp(), reqClientKey.toString());

                channelManager.offer(new ChannelMessage(reqClientKey.getKey(), ChannelMessageType.ClientChannel, context));
                ServerResult<ClientKeyEntity> resObj = clientService.getChangedClusterConf(reqClientKey);
                log.info("{}:client validate response content:{}", context.getChannel().getRemoteIp(), JSON.toJSONString(resObj.getResult()));

                RegistryProtocol resProtocol = null;
                String resStr = clientKeyToString(resObj.getResult());
                if (!resObj.isChanged()) {
                    resProtocol = ProtocolFactory.getInstance().createClientConfigResponse(reqProtocol.getOpaque(), MessageType.RESPONSE_NO_CHANGE, resStr.getBytes(), reqProtocol.getSessionId());
                } else {
                    resProtocol = ProtocolFactory.getInstance().createClientConfigResponse(reqProtocol.getOpaque(), MessageType.RESPONSE_HAS_CHANGE, resStr.getBytes(), reqProtocol.getSessionId());
                }
                context.setResponse(resProtocol.toBytes());
            } catch (Exception e) {
                RegistryProtocol errProtocol = ProtocolFactory.getInstance().createClientConfigResponse(reqProtocol.getOpaque(), MessageType.RESPONSE_ERROR,
                        "client get changed config error".getBytes(), reqProtocol.getSessionId());
                context.setResponse(errProtocol.toBytes());
                log.error(String.format("{}: client validate configuration failed!", context.getChannel().getRemoteIp()), e);
            }
        } else {
            throw new ProtocolException(String.format("%s protocol message type unmatch, value is %d", context.getChannel().getRemoteIp(), reqProtocol.getMsgType()));
        }
    }

    private ClientKeyEntity getReqClientKeyEntity(RegistryProtocol reqProtocol) {
        return JSON.parseObject(new String(reqProtocol.getBody()), ClientKeyEntity.class);
    }

    private String clientKeyToString(ClientKeyEntity clientKey) {
        return JSON.toJSONString(clientKey);
    }



}
