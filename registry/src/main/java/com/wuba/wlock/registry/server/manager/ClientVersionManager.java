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
package com.wuba.wlock.registry.server.manager;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.entity.VersionMessage;
import com.wuba.wlock.registry.server.entity.RemoveVersion;
import com.wuba.wlock.registry.util.ThreadRenameFactory;
import com.wuba.wlock.registry.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class ClientVersionManager {
    private static final long DEADLINE_MILLISECOND = 10800000; // 3小时
    private static final String MAP_NAME = "wlock_client_version_map";
    
    ExecutorService threadPool = new ThreadPoolExecutor(2, 2,0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadRenameFactory("ClientVersionManager-Thread"));

    @Autowired
    RedisUtil redisUtil;

    public void offer(VersionMessage message) {
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                if (message != null) {
                    try {
                        List<VersionMessage> messages = redisUtil.getMessagesFromRedis(MAP_NAME, message.getKey(), VersionMessage.class);
                        messages = updateVersion(messages, message);
                        String jsonString = JSON.toJSONString(messages);
                        redisUtil.hset(MAP_NAME, message.getKey(), jsonString);
                    } catch (Exception e) {
                        log.error("put message {} to redis failed, {}", message.toString(), e);
                    }
                }
            }
        });
    }

    public void remove(RemoveVersion removeVersion) {
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    removeVersion(removeVersion.getHashKey(), removeVersion.getIp(), removeVersion.getPort());
                } catch (Exception e) {
                    log.error(String.format("put message %s to redis failed, {}", removeVersion.toString()), e);
                }
            }
        });
    }


    private void removeVersion(final String key, final String ip, final int port) {
        String address = ip + ":" + port;
        String version = redisUtil.hget(MAP_NAME, key);
        if (version == null) {
            return;
        }

        boolean needUpdate = false;
        List<VersionMessage> messages = JSON.parseArray(version, VersionMessage.class);
        List<VersionMessage> tmp = new ArrayList<>();
        for (VersionMessage message : messages) {
            tmp = new ArrayList<>(messages.size());
            if (message.getAddr().equalsIgnoreCase(address)) {
                needUpdate = true;
            } else {
                tmp.add(message);
            }
        }
        if (needUpdate) {
            log.info("client {} close, remove version", address);
            redisUtil.hset(MAP_NAME, key, JSON.toJSONString(tmp));
        }
    }

    private List<VersionMessage> updateVersion(List<VersionMessage> messagesInRedis, VersionMessage message) {
        List<VersionMessage> messages = new ArrayList<VersionMessage>();
        messages.add(message);
        if (messagesInRedis != null) {
            Iterator<VersionMessage> iter = messagesInRedis.iterator();
            while (iter.hasNext()) {
                VersionMessage mess = iter.next();
                if (mess.getAddr().equalsIgnoreCase(message.getAddr())) {
                    continue;
                }
                if (message.getDate() - mess.getDate() > DEADLINE_MILLISECOND) {
                    continue;
                }
                messages.add(mess);
            }
        }
        return messages;
    }
}
