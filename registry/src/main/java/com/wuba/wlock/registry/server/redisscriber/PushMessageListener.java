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
package com.wuba.wlock.registry.server.redisscriber;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.entity.ClientKeyEntity;
import com.wuba.wlock.common.entity.PushMessage;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.ProtocolFactory;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.server.entity.ClientConfInfo;
import com.wuba.wlock.registry.server.entity.ClusterMasterGroupDistribute;
import com.wuba.wlock.registry.server.manager.ChannelManager;
import com.wuba.wlock.registry.server.service.ClientService;
import com.wuba.wlock.repository.domain.ClusterDO;
import com.wuba.wlock.repository.domain.KeyDO;
import com.wuba.wlock.repository.repository.ClusterRepository;
import com.wuba.wlock.repository.repository.KeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

@Slf4j
@Component
public class PushMessageListener extends JedisPubSub {
	@Autowired
	ChannelManager channelManager;

	@Autowired
	ClusterRepository clusterRepository;
	@Autowired
	KeyRepository keyRepository;
	@Autowired
	ClientService clientService;

	@Override
	public void onMessage(String channel, String messageString) {
		log.info("receive cluster config change message: {}", messageString);
		try {
			PushMessage message = JSON.parseObject(messageString, PushMessage.class);
			if (message != null) {
				if (message.getCluster() == null) {
					return;
				}

				ClusterDO clusterDO = clusterRepository.getClusterByClusterName(Environment.env(), message.getCluster());
				if (clusterDO == null) {
					log.warn("current environment not exist this cluster {}, please ignore.", message.getCluster());
					return;
				}

				if (clusterDO.getUpdateTime() > message.getVersion()) {
					return;
				}

				List<KeyDO> keyList = keyRepository.getKeyByClusterId(Environment.env(), message.getCluster());
				if (CollectionUtils.isEmpty(keyList)) {
					return;
				}

				ClusterMasterGroupDistribute clusterConf = clientService.getClusterMasterGroupDistribute(message.getCluster(), true);
				// 对比版本,如果从redis获取的是旧配置,则再从DB获取一次
				if (clusterConf == null || clusterConf.getVersion() < clusterDO.getUpdateTime()) {
					clusterConf = clientService.getClusterMasterGroupDistribute(message.getCluster(), false);
				}

				for (KeyDO keyDO: keyList) {
					try {
						String hashKey = keyDO.getHashKey();
						ClientConfInfo clientConfInfo = clientService.getClientConfInfoByMapping(hashKey, true);
						ClientKeyEntity clientKeyEntity = clientService.getClientKeyEntityFromClusterConf(hashKey, clusterConf, clientConfInfo);
						RegistryProtocol pushProtocol = ProtocolFactory.getInstance().createClientConfigPush(MessageType.PUSH_ALL_DATAS, JSON.toJSONString(clientKeyEntity).getBytes());
						channelManager.sendMessage(hashKey, pushProtocol, clientKeyEntity);
					} catch (Exception e) {
						log.info("push conf to client error", e);
					}
				}
			}
		} catch (Exception e) {
			log.error("PushMessageListener error", e);
		}
	}
}
