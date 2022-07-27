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

import com.wuba.wlock.common.exception.ProtocolException;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClientConfigPushCommand implements ClientCommand{
    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {
        if (reqProtocol.getMsgType() == MessageType.RESPONSE_ACK) {
            context.setAck(true);
            log.info("server push configs to {} success.", context.getChannel().getRemoteIp());
        } else {
            throw new ProtocolException(context.getChannel().getRemoteIp() + ": protocol msgType unmatched, the value is " + reqProtocol.getOpaque() + ":" + reqProtocol.getMsgType());
        }
    }
}
