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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异常打印
 */
@Component
public class RedisUtil {
	
	private static int MAX_ACTIVE = 1000;
	
	private static int MAX_IDLE = 32;
	
	private static int MAX_WAIT = 1000;
	
	private static int TIMEOUT = 1000;
	
	private static boolean TEST_ON_BORROW = true; 
	
	private static JedisPool jedisPool = null;

	@Value("${redis_ip}")
	String host;
	@Value("${redis_port}")
	int port;
	@Value("${redis_auth}")
	String auth;

	@PostConstruct
	public void init() throws Exception {
		createJedisPool();
	}
	
	public synchronized void createJedisPool() throws Exception {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(MAX_ACTIVE);
		config.setMaxIdle(MAX_IDLE);
		config.setMaxWaitMillis(MAX_WAIT);
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
			e.printStackTrace();
		}
		return null;
	} 
	
	public Jedis getJedisLink() {
		return getJedis();
	}
	
	private static void releaseJedis(final Jedis jedis) {
		if (null != jedis) {
			jedis.close();
		}
	}
	
	public void releaseJedisLink(final Jedis jedis) {
		if (null != jedis) {
			jedis.close();
		}
	}
	
	public List<String> getRedisMap(String mapName, String... fields) {
		Jedis jedis = getJedis();
		try {
			return jedis.hmget(mapName, fields);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
		return null;
	}

	public String hget(String mapName, String field) {
		Jedis jedis = getJedis();
		try {
			return jedis.hget(mapName, field);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
		return null;
	}

	public void addKeyToMap(String mapName, String key, String value) {
		Jedis jedis = getJedis();
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put(key, value);
			jedis.hmset(mapName, map);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}

	public Map<String, String> getAllMaps(String mapName) {
		Jedis jedis = getJedis();
		try {
			return jedis.hgetAll(mapName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
		return new HashMap<String, String>();
	}

	public String getValue(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.get(key);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
		return null;
	}

	public void setValue(String key, String value) {
		Jedis jedis = getJedis();
		try {
			jedis.set(key, value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}

	public void delKeyToMap(String mapName, String key) {
		Jedis jedis = getJedis();
		try {
			jedis.hdel(mapName, key);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}

	public void addMapToRedis(String mapName, Map<String, String> map) {
		Jedis jedis = getJedis();
		try {
			jedis.hmset(mapName, map);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}

	public void setExpire(String mapName, int timeout) {
		Jedis jedis = getJedis();
		try {
			jedis.expire(mapName.getBytes(), timeout);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}

	public void setValueAndExpire(String key, String value, int minute) {
		Jedis jedis = getJedis();
		try {
			jedis.set(key, value);
			jedis.expire(key, minute * 60);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}

	public long incr(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.incr(key);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
		return 0;
	}

	public long delKey(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.del(key);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
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

	public long incrby(String key, long value) {
		Jedis jedis = getJedis();
		try {
			return jedis.incrBy(key.getBytes(), value);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			releaseJedis(jedis);
		}
	}

	public long setNx(String key, String value) {
		Jedis jedis = getJedis();
		try {
			return jedis.setnx(key.getBytes(), value.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			releaseJedis(jedis);
		}
	}

	public void publish(String channel, String message) {
		Jedis jedis = getJedis();
		try {
			jedis.publish(channel, message);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}

	public void hset(String key, String field, String value) {
		Jedis jedis = getJedis();
		try {
			jedis.hset(key, field, value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			releaseJedis(jedis);
		}
	}
	
}
