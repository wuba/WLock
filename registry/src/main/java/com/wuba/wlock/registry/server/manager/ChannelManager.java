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

import com.wuba.wlock.common.entity.ClientKeyEntity;
import com.wuba.wlock.common.exception.ProtocolException;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.registry.server.context.WLockRegistryChannel;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.entity.ChannelMessage;
import com.wuba.wlock.registry.server.entity.ChannelMessageType;
import com.wuba.wlock.registry.server.entity.RemoveVersion;
import com.wuba.wlock.registry.util.ThreadPool;
import com.wuba.wlock.registry.util.ThreadRenameFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class ChannelManager {
    private static final int CHANNEL_SIZE_LIMIT = 2048;

    ExecutorService threadPool = new ThreadPoolExecutor(2, 2,0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadRenameFactory("ChannelManager-Thread"));

    ChannelKeyPool channelKeyPool = ChannelKeyPool.getInstance();

    @Autowired
    ClientVersionManager clientVersionManager;

    private final Object locker = new Object();

    public void offer(ChannelMessage message) {

        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (message != null) {
                        if (message.getChannelType() == ChannelMessageType.ClientChannel || message.getChannelType() == ChannelMessageType.ServerChannel) {
                            addVerifyChannel(message.getKeyName(), message.getContext());
                        } else {
                            log.warn("unsupported channel message type: {}", message.getChannelType().getValue());
                        }
                    }
                } catch (Exception e) {
                    log.error("ChannelManager error", e);
                }
            }
        });
    }

    public void addVerifyChannel(String key, WLockRegistryContext context) {
        synchronized (locker) {
            boolean exist = false;
            ArrayList<WLockRegistryChannel> cList = channelKeyPool.getChannelList(key);
            if (cList != null && cList.size() > CHANNEL_SIZE_LIMIT) {
                log.error("key {} : channel list has been greater than 2048, so add ip {} to it failed.", key, context.getChannel().getRemoteIp());
                return;
            }

            if (cList == null) {
                cList = new ArrayList<WLockRegistryChannel>();
            } else {
                for (WLockRegistryChannel c : cList) {
                    if (c.getRemoteIp().equalsIgnoreCase(context.getChannel().getRemoteIp()) && c.getRemotePort() == context.getChannel().getRemotePort()) {
                        exist = true;
                        break;
                    }
                }
            }

            channelKeyPool.addChannelHashKeyMapping(context.getChannel().getNettyChannel(), key);
            if (!exist) {
                WLockRegistryChannel registryChannel = context.getChannel();
                if (!registryChannel.getNettyChannel().isOpen()) {
                    log.info("key {} ,add new channel not open,return {}:{}", key, context.getChannel().getRemoteIp(), context.getChannel().getRemotePort());
                    return;
                }
                cList.add(registryChannel);
                channelKeyPool.setChannelList(key, cList);
                log.info("key {} ,add new channel {}:{}", key, context.getChannel().getRemoteIp(), context.getChannel().getRemotePort());
            }
        }
    }

    public void removeChannel(Channel channel) {
        synchronized (locker) {
            String hashKey = channelKeyPool.getHashKeyByChannel(channel);
            if (hashKey == null) {
                return;
            }
            List<WLockRegistryChannel> channelList = channelKeyPool.getChannelList(hashKey);
            Iterator<WLockRegistryChannel> iter = channelList.iterator();
            while (iter.hasNext()) {
                WLockRegistryChannel tempChannel = iter.next();
                if (tempChannel.getNettyChannel().equals(channel)) {
                    RemoveVersion removeVersion = new RemoveVersion(hashKey, tempChannel.getRemoteIp(), tempChannel.getRemotePort());
                    clientVersionManager.remove(removeVersion);
                    iter.remove();
                    channelKeyPool.removeChannelHashKeyMapping(channel);
                    return;
                }
            }
        }
    }

    public void cleanErrorChannel(String key, List<WLockRegistryChannel> channels) {
        ArrayList<WLockRegistryChannel> channelList = channelKeyPool.getChannelList(key);
        synchronized(locker){
            for (WLockRegistryChannel channel : channels) {
                log.warn(channel.getRemoteIp() + " link is error, so clean it.");
                channelList.remove(channel);
            }
        }
    }

    public void sendMessage(String hashKey, RegistryProtocol protocol, ClientKeyEntity clientKeyEntity) throws ProtocolException {
        List<WLockRegistryChannel> channelList = ChannelKeyPool.getInstance().getChannelList(hashKey);
        if (CollectionUtils.isNotEmpty(channelList)) {
            List<WLockRegistryChannel> toDeleteChannels = new ArrayList<WLockRegistryChannel>();
            for (WLockRegistryChannel channel : channelList) {
                if (channel == null || !channel.getNettyChannel().isOpen()) {
                    toDeleteChannels.add(channel);
                    continue;
                }

                ThreadPool.EXECUTORS.execute(new SendRunnable(hashKey, channel, protocol.toBytes()));
                log.info("key {}: master group changed send new cluster config to {} content is {}", hashKey, channel.getRemoteIp(), clientKeyEntity.toString());
            }
            if (!toDeleteChannels.isEmpty()) {
                cleanErrorChannel(hashKey, toDeleteChannels);
            }
        }
    }

    private class SendRunnable implements Runnable {
        private String hashKey;

        private WLockRegistryChannel channel;

        private byte[] buf;

        public SendRunnable(String hashKey, WLockRegistryChannel channel, byte[] buf) {
            this.hashKey = hashKey;
            this.channel = channel;
            this.buf = buf;
        }

        @Override
        public void run() {
            try {
                Channel nettyChannel = channel.getNettyChannel();
                if (nettyChannel != null && nettyChannel.isOpen()) {
                    nettyChannel.writeAndFlush(Unpooled.copiedBuffer(buf));
                    log.debug("{} :push config: {}", channel.getRemoteIp(), hashKey);
                } else {
                    log.warn( "{} :link is error!", channel.getRemoteIp());
                }
            } catch (Exception e) {
                log.info("SendWorker error:", e);
            }
        }

    }
}
