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
package com.wuba.wlock.registry.server.worker;

import com.wuba.wlock.registry.server.redisscriber.PushMessageListener;
import com.wuba.wlock.registry.server.redisscriber.SubscribeClient;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.registry.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

@Slf4j
@Component
public class RedisClusterConfSubscribeWorker implements Runnable {
	@Autowired
	RedisUtil redisUtil;

	@Autowired
	PushMessageListener pushMessageListener;

	@Override
	public void run() {
		try {
			if (redisUtil.isUseRedis()) {
				Jedis jedisLink = redisUtil.getJedisLink();
				if (jedisLink != null) {
					SubscribeClient subClient = new SubscribeClient(jedisLink);
					log.info("redis subscriber will start!");
					subClient.clusterConfSubscribe(pushMessageListener, RedisKeyConstant.REDIS_SUBSCRIBE_CHANNEL);
				} else {
					log.info("no use redis .");
				}
			}
		} catch (Exception e) {
			log.info("RedisClusterConfSubscribeTask error", e);
		}

	}

}
