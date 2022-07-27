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
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.UploadGroupMaster;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.entity.ChannelMessage;
import com.wuba.wlock.registry.server.entity.ChannelMessageType;
import com.wuba.wlock.registry.server.manager.ChannelManager;
import com.wuba.wlock.registry.server.service.ServerService;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.registry.util.ThreadRenameFactory;
import com.wuba.wlock.registry.util.RedisUtil;
import com.wuba.wlock.repository.domain.GroupServerRefDO;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class ServerUploadMasterCommand implements ServerCommand{
    ExecutorService threadPool = new ThreadPoolExecutor(2, 2,0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadRenameFactory("ServerUploadMasterCommand-Thread"));

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ChannelManager channelManager;

    @Autowired
    ServerService serverService;



    @Override
    public void execute(WLockRegistryContext context, RegistryProtocol reqProtocol) throws Exception {


        RegistryProtocol resProtocol;
        try {
            UploadGroupMaster.GroupMaster groupMaster = JSONObject.parseObject(new String(reqProtocol.getBody()), UploadGroupMaster.GroupMaster.class);
            // 开源去掉，限流需要
            channelManager.offer(new ChannelMessage(groupMaster.getClusterName(), ChannelMessageType.ServerChannel, context));
            resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_UPLOAD_MASTER_CONFIG, MessageType.SUCCESS, reqProtocol.getSessionId());
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    serverService.handleGroupMaster(groupMaster);
                }
            });
        } catch (Exception e) {
            resProtocol = ProtocolFactory.getInstance().createCommonAck(OptionCode.RES_UPLOAD_MASTER_CONFIG, MessageType.ERROR, reqProtocol.getSessionId());
            log.error("{} upload paxos master config error ", context.getChannel().getRemoteIp());
        }
        context.setResponse(resProtocol.toBytes());
    }




    private long getRedisVersion(String cluster, int group) {
        String value = redisUtil.getValue(RedisKeyConstant.getGroupVersionKey(cluster, group));
        if (Strings.isNullOrEmpty(value)) {
            return -1;
        }
        return Long.parseLong(value);
    }

    private GroupServerRefDO buildGroupServerRefDO(String clusterId, int groupId, String serverAddr, long version) {
        GroupServerRefDO groupServerRefDO = new GroupServerRefDO();
        groupServerRefDO.setClusterId(clusterId);
        groupServerRefDO.setGroupId(groupId);
        groupServerRefDO.setServerAddr(serverAddr);
        groupServerRefDO.setVersion(version);
        groupServerRefDO.setCreateTime(new Date());
        groupServerRefDO.setUpdateTime(new Date());
        return groupServerRefDO;
    }
}
