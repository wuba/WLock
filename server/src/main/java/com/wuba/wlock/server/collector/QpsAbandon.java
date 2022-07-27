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
package com.wuba.wlock.server.collector;

import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class QpsAbandon {

	private static final Logger LOGGER = LoggerFactory.getLogger(QpsAbandon.class);

	private static final String SEP = "_";
	private static long nowSecond;
	private static long nowMinute;
	private static AtomicInteger serverSecondQps;
	private static final Map<Integer, AtomicInteger> GROUP_SECOND_QPS = new ConcurrentHashMap<>();
	//实际申请的key qps
	private static Map<String, Integer> keyQps = new ConcurrentHashMap<>();

	private static Map<String, Integer> keyGroup = new ConcurrentHashMap<>();
	//按秒统计的qps:hashcode
	private static final Map<String, AtomicInteger> KEY_SECOND_QPS = new ConcurrentHashMap<>();
	//按分钟统计的qps:hashcode
	private static final Map<String, AtomicInteger> KEY_MINUTE_QPS = new ConcurrentHashMap<>();

	private static int limitServerOverQps;
	private static int limitServerTopQps;
	private static int limitGroupOverQps;
	private static int limitGroupTopQps;
	private static int limitAllServerQps;
	private static int limitAllGroupQps;
	public static boolean limitEnable = false;

	//超速限制
	private static final Map<String, Long> LIMIT_OVER = new ConcurrentHashMap<>();
	//限制所有
	private static final Map<String, Long> LIMIT = new ConcurrentHashMap<>();

	private static final Map<Long, Set<String>> LIMIT_KEYS_MAP = new ConcurrentHashMap<Long, Set<String>>();

	private static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("speed_limit_worker"));

	private static final Set EMPTY_SET = new HashSet();

	public static void init() {
		if (ServerConfig.getInstance().getServerQps() == 0 || ServerConfig.getInstance().getGroupQps() == 0) {
			limitEnable = false;
			return;
		}
		serverSecondQps = new AtomicInteger();
		limitServerOverQps = (int) (ServerConfig.getInstance().getServerQps() * 0.5);
		limitServerTopQps = (int) (ServerConfig.getInstance().getServerQps() * 0.8);
		limitGroupOverQps = (int) (ServerConfig.getInstance().getGroupQps() * 0.5);
		limitGroupTopQps = (int) (ServerConfig.getInstance().getGroupQps() * 0.8);
		limitAllServerQps = (int) (ServerConfig.getInstance().getServerQps() * 0.95);
		limitAllGroupQps = (int) (ServerConfig.getInstance().getGroupQps() * 0.95);
		LOGGER.info("init limit qps limitServerOverQps {} limitServerTopQps {} limitGroupOverQps {} limitGroupTopQps {} limitAllServerQps {} limitAllGroupQps {}",
				limitServerOverQps, limitServerTopQps, limitGroupOverQps, limitGroupTopQps, limitAllServerQps, limitAllGroupQps);
		EXECUTOR.scheduleAtFixedRate(QpsAbandon::checkSpeed, 0, 10, TimeUnit.MILLISECONDS);
		EXECUTOR.scheduleAtFixedRate(QpsAbandon::removeMap, 1, 2, TimeUnit.MINUTES);
	}

	public synchronized static void addLimitKey(String key, long minute) {
		LOGGER.info("QpsAbandon addLimitKey key: {}, minute: {}", key, minute);
		Set<String> limitKeys = LIMIT_KEYS_MAP.get(minute);
		if (limitKeys == null) {
			limitKeys = new CopyOnWriteArraySet();
			LIMIT_KEYS_MAP.put(minute, limitKeys);
		}

		limitKeys.add(key);
	}

	private static void removeMap() {
		try {
			long delete = TimeUtil.getCurrentSecond() - 10 * 1000;
			LIMIT_OVER.entrySet().removeIf(stringLongEntry -> stringLongEntry.getValue() < delete);
			LIMIT.entrySet().removeIf(stringLongEntry -> stringLongEntry.getValue() < delete);
			LOGGER.info("remove map {} {}", LIMIT_OVER.size(), LIMIT.size());

			long nowMinute = QpsAbandon.nowMinute;
			LIMIT_KEYS_MAP.entrySet().removeIf(entry -> entry.getKey() != nowMinute);
		} catch (Throwable e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private static void checkSpeed() {
		try {
			long currentSecond = TimeUtil.getCurrentSecond();
			if (currentSecond != nowSecond) {
				nowSecond = currentSecond;
				serverSecondQps = new AtomicInteger();
				GROUP_SECOND_QPS.clear();
				KEY_SECOND_QPS.clear();
			}
			long currentMinute = TimeUtil.getCurrentMinute();
			if (currentMinute != nowMinute) {
				nowMinute = currentMinute;
				KEY_MINUTE_QPS.clear();
			}

			long nextSecond = TimeUtil.getNextSecond();
			Map<String, AtomicInteger> keyQpsSortMap = sortAtomicMap(KEY_SECOND_QPS);
			Set<String> limitKeys = LIMIT_KEYS_MAP.getOrDefault(currentMinute, EMPTY_SET);
			Map<Integer, Integer> afterLimitGroupQps = limitServer(currentSecond, nextSecond, keyQpsSortMap, limitKeys);
			limitGroup(currentSecond, nextSecond, keyQpsSortMap, afterLimitGroupQps, limitKeys);
		} catch (Throwable e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private static void limitGroup(long currentSecond, long nextSecond, Map<String, AtomicInteger> keyQpsSortMap, Map<Integer, Integer> afterLimitGroupQps, Set<String> limitKeys) {
		for (Map.Entry<Integer, Integer> groupEntry : afterLimitGroupQps.entrySet()) {
			if (groupEntry.getValue() > limitGroupTopQps) {
				LOGGER.debug("group {} qps {} > limit top {}", groupEntry.getKey(), groupEntry.getValue(), limitGroupTopQps);
				int groupSecond = groupEntry.getValue();
				boolean needDropTop = true;
				for (String limitKey : limitKeys) {
					if (groupEntry.getKey().intValue() != keyGroup.getOrDefault(limitKey, -1).intValue()) {
						continue;
					}
					if (!LIMIT.containsKey(limitKey + SEP + currentSecond)) {
						groupSecond -= KEY_SECOND_QPS.getOrDefault(limitKey, new AtomicInteger(0)).get();
						LOGGER.debug("key {} over speed {} limit 2 second ", limitKey, KEY_SECOND_QPS.get(limitKey));
						LIMIT.put(limitKey + SEP + currentSecond, currentSecond);
						LIMIT.put(limitKey + SEP + nextSecond, nextSecond);
						if (groupSecond <= limitGroupTopQps) {
							needDropTop = false;
							break;
						}
					}
				}
				if (needDropTop) {
					for (Map.Entry<String, AtomicInteger> entry : keyQpsSortMap.entrySet()) {
						if (groupEntry.getKey().intValue() != keyGroup.getOrDefault(entry.getKey(), -1).intValue()) {
							continue;
						}
						if (!LIMIT.containsKey(entry.getKey() + SEP + currentSecond)) {
							groupSecond -= entry.getValue().get();
							LOGGER.debug("key {} {} limit 2 second ", entry.getKey(), entry.getValue());
							LIMIT.put(entry.getKey() + SEP + currentSecond, currentSecond);
							LIMIT.put(entry.getKey() + SEP + nextSecond, nextSecond);
							if (groupSecond <= limitGroupTopQps) {
								break;
							}
						}
					}
				}
			} else if (groupEntry.getValue() > limitGroupOverQps) {
				int groupSecond = groupEntry.getValue();
				LOGGER.debug("group {} qps {} > limit {}", groupEntry.getKey(), groupEntry.getValue(), limitGroupOverQps);
				for (String limitKey : limitKeys) {
					if (groupEntry.getKey().intValue() != keyGroup.getOrDefault(limitKey, -1).intValue()) {
						continue;
					}
					if (!LIMIT_OVER.containsKey(limitKey + SEP + currentSecond)) {
						groupSecond -= KEY_SECOND_QPS.getOrDefault(limitKey, new AtomicInteger(0)).get();
						LOGGER.debug("key {} {} limit 2 second ", limitKey, KEY_SECOND_QPS.get(limitKey));
						LIMIT_OVER.put(limitKey + SEP + currentSecond, currentSecond);
						LIMIT_OVER.put(limitKey + SEP + nextSecond, nextSecond);
						if (groupSecond <= limitGroupOverQps) {
							break;
						}
					}
				}
			}
		}
	}


	private static Map<Integer, Integer> limitServer(long currentSecond, long nextSecond, Map<String, AtomicInteger> keyQpsSortMap, Set<String> limitKeys) {
		Map<Integer, Integer> groupQps = GROUP_SECOND_QPS.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
		if (serverSecondQps.get() > limitServerTopQps) {
			LOGGER.debug("server qps {} > limit top {}", serverSecondQps.get(), limitServerTopQps);
			int serverSecond = serverSecondQps.get();
			boolean needDropTop = true;
			for (String limitKey : limitKeys) {
				int keyQps = KEY_SECOND_QPS.getOrDefault(limitKey, new AtomicInteger(0)).get();
				LOGGER.debug("key {} over speed {} limit 2 second ", limitKey, keyQps);
				serverSecond -= keyQps;
				LIMIT.put(limitKey + SEP + currentSecond, currentSecond);
				LIMIT.put(limitKey + SEP + nextSecond, nextSecond);
				int group = keyGroup.getOrDefault(limitKey, -1);
				groupQps.put(group, groupQps.getOrDefault(group, -1) - keyQps);
				if (serverSecond <= limitServerTopQps) {
					needDropTop = false;
					break;
				}
			}
			if (needDropTop) {
				for (Map.Entry<String, AtomicInteger> entry : keyQpsSortMap.entrySet()) {
					if (!LIMIT.containsKey(entry.getKey() + SEP + currentSecond)) {
						int keyQps = entry.getValue().get();
						LOGGER.debug("key {}  {} limit 2 second ", entry.getKey(), keyQps);
						serverSecond -= keyQps;
						LIMIT.put(entry.getKey() + SEP + currentSecond, currentSecond);
						LIMIT.put(entry.getKey() + SEP + nextSecond, nextSecond);
						int group = keyGroup.getOrDefault(entry.getKey(), -1);
						groupQps.put(group, groupQps.getOrDefault(group, -1) - keyQps);
						if (serverSecond <= limitServerTopQps) {
							break;
						}
					}
				}
			}
		} else if (serverSecondQps.get() > limitServerOverQps) {
			int serverSecond = serverSecondQps.get();
			LOGGER.debug("server qps {} > limit top {}", serverSecondQps.get(), limitServerOverQps);
			for (String limitKey : limitKeys) {
				int keyQps = KEY_SECOND_QPS.getOrDefault(limitKey, new AtomicInteger(0)).get();
				LOGGER.debug("key {}  {} limit 2 second ", limitKey, keyQps);
				serverSecond -= keyQps;
				LIMIT_OVER.put(limitKey + SEP + currentSecond, currentSecond);
				LIMIT_OVER.put(limitKey + SEP + nextSecond, nextSecond);
				int group = keyGroup.getOrDefault(limitKey, -1);
				groupQps.put(group, groupQps.getOrDefault(group, -1) - keyQps);
				if (serverSecond <= limitServerOverQps) {
					break;
				}
			}
		}
		return groupQps;
	}

	private static Map<String, AtomicInteger> sortAtomicMap(Map<String, AtomicInteger> map) {
		return map.entrySet().stream().
				sorted((o1, o2) -> o2.getValue().get() - o1.getValue().get())
				.collect(
						Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue,
								(oldVal, newVal) -> oldVal,
								LinkedHashMap::new
						)
				);
	}

	public static void renewQps(Map<String, Integer> qps) {
		keyQps.clear();
		keyQps.putAll(qps);
	}


	public static boolean limitSpeed(String key, int group) {
		if (!limitEnable) {
			return false;
		}
		if (limitServerOverQps <= 0 || limitServerTopQps <= 0 || limitGroupOverQps <= 0 || limitGroupTopQps <= 0 || limitAllServerQps <= 0 || limitAllGroupQps <= 0) {
			return false;
		}
		long currentSecond = TimeUtil.getCurrentSecond();
		incrKeyQps(key);
		keyGroup.put(key, group);
		if (!GROUP_SECOND_QPS.containsKey(group)) {
			GROUP_SECOND_QPS.put(group, new AtomicInteger());
		}
		if (GROUP_SECOND_QPS.get(group).get() > limitAllGroupQps || serverSecondQps.get() > limitAllServerQps) {
			LOGGER.warn("group {} qps {} or server qps {} over 95% , limit all.", group, GROUP_SECOND_QPS.get(group).get(), serverSecondQps.get());
			return true;
		}
		if (GROUP_SECOND_QPS.get(group).get() > limitGroupOverQps && GROUP_SECOND_QPS.get(group).get() <= limitGroupTopQps) {
			if (LIMIT_OVER.containsKey(key + SEP + currentSecond)) {
				LOGGER.warn("group {} qps {} over 50% and key {} is over , limit .", group, GROUP_SECOND_QPS.get(group).get(), key);
				return true;
			}
		}
		if (GROUP_SECOND_QPS.get(group).get() > limitGroupTopQps) {
			if (LIMIT.containsKey(key + SEP + currentSecond)) {
				LOGGER.warn("group {} qps {} over 80% and key {} is over , limit .", group, GROUP_SECOND_QPS.get(group).get(), key);
				return true;
			}
		}
		if (serverSecondQps.get() > limitServerOverQps && serverSecondQps.get() <= limitServerTopQps) {
 			if (LIMIT_OVER.containsKey(key + SEP + currentSecond)) {
				LOGGER.warn("server qps over 50% and key {} is over , limit .", key);
				return true;
			}
		}
		if (serverSecondQps.get() > limitServerTopQps) {
			if (LIMIT.containsKey(key + SEP + currentSecond)) {
				LOGGER.warn("server qps over 80% and key {} is over , limit .", key);
				return true;
			}
		}
		incrServerQps(group);
		return false;
	}


	private static void incrKeyQps(String key) {
		AtomicInteger keySecondCount = KEY_SECOND_QPS.get(key);
		if (keySecondCount == null) {
			synchronized (KEY_SECOND_QPS) {
				keySecondCount = KEY_SECOND_QPS.get(key);
				if (keySecondCount == null) {
					keySecondCount = new AtomicInteger();
					KEY_SECOND_QPS.put(key, keySecondCount);
				}
			}
		}
		int i = keySecondCount.incrementAndGet();
		LOGGER.debug("incre key {} second qps {}", key, i);
		AtomicInteger keyMinuteCount = KEY_MINUTE_QPS.get(key);
		if (keyMinuteCount == null) {
			synchronized (KEY_MINUTE_QPS) {
				keyMinuteCount = KEY_MINUTE_QPS.get(key);
				if (keyMinuteCount == null) {
					keyMinuteCount = new AtomicInteger();
					KEY_MINUTE_QPS.put(key, keyMinuteCount);
				}
			}
		}
		int i1 = keyMinuteCount.incrementAndGet();
		LOGGER.debug("incre key {} minute qps {}", key, i1);
	}

	private static void incrServerQps(int group) {
		AtomicInteger groupCount = GROUP_SECOND_QPS.get(group);
		if (groupCount == null) {
			synchronized (GROUP_SECOND_QPS) {
				groupCount = GROUP_SECOND_QPS.get(group);
				if (groupCount == null) {
					groupCount = new AtomicInteger();
					GROUP_SECOND_QPS.put(group, groupCount);
				}
			}
		}
		int i = groupCount.incrementAndGet();
		LOGGER.debug("incre group {} second qps {}", group, i);
		int i1 = serverSecondQps.incrementAndGet();
		LOGGER.debug("incre server {} second qps {}", group, i1);
	}
}
