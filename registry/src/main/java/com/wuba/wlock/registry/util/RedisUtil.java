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
package com.wuba.wlock.registry.util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * redis util
 */
@Slf4j
@Component
public class RedisUtil {

	private static int MAX_ACTIVE = 1000;

	private static int MAX_IDLE = 32;

	private static int MAX_WAIT = 1000;

	private static int TIMEOUT = 1000;

	private static boolean TEST_ON_BORROW = true;

	private static JedisPool jedisPool = null;

	@Value("${use_redis}")
	boolean isUseRedis = true;
	@Value("${redis_ip}")
	String host;
	@Value("${redis_port}")
	int port;
	@Value("${redis_auth}")
	String auth;

	@PostConstruct
	public void init() throws Exception {
		try {
			createJedisPool();
		} catch (Exception e) {
			log.error("init redis error , no use redis.");
		}
	}

	private synchronized void createJedisPool() throws Exception {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(MAX_ACTIVE);
		config.setMaxIdle(MAX_IDLE);
		config.setMaxWait(Duration.ofMillis(MAX_WAIT));
		config.setTestOnBorrow(TEST_ON_BORROW);
		if (auth != null && !"".equals(auth)) {
			jedisPool = new JedisPool(config, host, port, TIMEOUT, auth);
		} else {
			jedisPool = new JedisPool(host, port);
		}
	}

	private static synchronized Jedis getJedis() {
		try {
			if (null != jedisPool) {
				return jedisPool.getResource();
			}
		} catch (Exception e) {
			log.error("get redis error : ", e);
		}
		return null;
	}

	public Jedis getJedisLink() {
		return getJedis();
	}

	public List<String> getRedisMap(String mapName, String... fields) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				return jedis.hmget(mapName, fields);
			}
		} catch (Exception e) {
			log.error("getRedisMap error : ", e);
		}
		return null;
	}

	public String hGet(String mapName, String field) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				return jedis.hget(mapName, field);
			}
		} catch (Exception e) {
			log.error("hget error : ", e);
		}
		return null;
	}

	public String getValue(String key) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				return jedis.get(key);
			}
		} catch (Exception e) {
			log.error("getValue error : ", e);
		}
		return null;
	}

	public void setValue(String key, String value) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				jedis.set(key, value);
			}
		} catch (Exception e) {
			log.error("setValue error : ", e);
		}
	}

	public void setValueAndExpire(String key, String value, int minute) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				jedis.set(key, value);
				jedis.expire(key, minute * 60);
			}
		} catch (Exception e) {
			log.error("setValueAndExpire error : ", e);
		}
	}

	public long delKey(String key) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				return jedis.del(key);
			}
		} catch (Exception e) {
			log.error("delKey error : ", e);
		}
		return 0;
	}

	public <T> List<T> getMessagesFromRedis(String mapName, String field, Class<T> targetClass) {
		List<String> result = getRedisMap(mapName, field);
		if (result != null && !result.isEmpty()) {
			return JSON.parseArray(result.get(0), targetClass);
		}
		return new ArrayList<T>();
	}

	public void publish(String channel, String message) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				jedis.publish(channel, message);
			}
		} catch (Exception e) {
			log.error("publish error : ", e);
		}
	}

	public void hset(String key, String field, String value) {
		try (Jedis jedis = getJedis()) {
			if (jedis != null) {
				jedis.hset(key, field, value);
			}
		} catch (Exception e) {
			log.error("hset error : ", e);
		}
	}

	public boolean isUseRedis() {
		return isUseRedis;
	}
}
