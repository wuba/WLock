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

import com.wuba.wlock.common.entity.VersionMessage;
import com.wuba.wlock.common.exception.ProtocolException;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.manager.ClientVersionManager;
import com.wuba.wlock.registry.server.service.ClientService;
import com.wuba.wlock.repository.domain.KeyDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClientVersionCommand implements ClientCommand{
    private static final String CLIENT_VERSION_SEPARATOR = "&&";

    @Autowired
    ClientVersionManager clientVersionManager;

    @Autowired
    ClientService clientService;

    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {
        //处理客户端版本上报
        if (reqProtocol.getMsgType() == MessageType.CLIENT_VERSION) {
            context.setAck(true);
            String versionInfo = new String(reqProtocol.getBody());
            log.info("{} :received client version message {}", context.getChannel().getRemoteIp(), versionInfo);

            String[] infoArray = versionInfo.split(CLIENT_VERSION_SEPARATOR);
            if (infoArray.length == CommonConstant.THREE) {
                KeyDO keyDO = clientService.getKeyByHashKey(Environment.env(), infoArray[0]);
                if (keyDO != null) {
                    VersionMessage version = new VersionMessage(infoArray[0], infoArray[1], infoArray[2], context.getChannel().getRemoteIp() + ":" + context.getChannel().getRemotePort(), keyDO.getName());
                    clientVersionManager.offer(version);
                } else {
                    log.warn("{} :received client version message {} keyName is null hashKey is {}", context.getChannel().getRemoteIp(), versionInfo, infoArray[0]);
                }
            } else {
                log.warn("{} :received client version message {} length is error, ignore it.", context.getChannel().getRemoteIp(), versionInfo);
            }
            RegistryProtocol resProtocol = ProtocolFactory.getInstance().createReponseAck(reqProtocol.getOpaque(), reqProtocol.getSessionId());
            context.setResponse(resProtocol.toBytes());
        } else {
            throw new ProtocolException(context.getChannel().getRemoteIp() + ": protocol msgType unmatched, the value is " + reqProtocol.getOpaque() + ":" + reqProtocol.getMsgType());
        }
    }
}
