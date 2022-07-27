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
package com.wuba.wlock.server.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IpTablesFactory {

	private static final Logger logger = LoggerFactory.getLogger(IpTablesFactory.class);
	private static final Lock IPTABLES_LOCK = new ReentrantLock();
	private static ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, String> patternMap = new ConcurrentHashMap<String, String>();

	public static boolean isForbid(String ip) {
		if (map.get(ip) != null) {
			return true;
		}
		Iterator<String> iter = patternMap.keySet().iterator();
		while (iter.hasNext()) {
			String ptr = iter.next();
			Pattern ptn = Pattern.compile(ptr);
			Matcher m = ptn.matcher(ip);
			if (m.find()) {
				return true;
			}
		}
		return false;
	}

	public static void load(String iplist) throws Exception {
		try {
			if (null != iplist) {
				String[] iparray = iplist.split(",");
				if (null != iparray && iparray.length > 0) {
					ConcurrentHashMap<String, String> ipmap = new ConcurrentHashMap<String, String>();
					for (String str : iparray) {
						if (null != str) {
							ipmap.put(str.trim(), str.trim());
						}
					}
					if (ipmap.size() > 0) {
						IPTABLES_LOCK.lock();
						try {
							map = ipmap;
							logger.info("iptables properties is load. context is " + ipmap);
							ipmap = null;
						} finally {
							IPTABLES_LOCK.unlock();
						}
					}
				}
			} else {
				IPTABLES_LOCK.lock();
				try {
					map.clear();
					logger.info("iptables properties is load. context is " + map);
				} finally {
					IPTABLES_LOCK.unlock();
				}
			}
		} catch(Exception ex) {
			logger.warn("load iptables properties failed: " + iplist);
			logger.error(ex.getMessage(), ex);
		}
	}

	public static void loadPattern(String ipPatterns) throws Exception {
		try {
			if (null != ipPatterns) {
				String[] iparray = ipPatterns.split(",");
				if (null != iparray && iparray.length > 0) {
					ConcurrentHashMap<String, String> ipPatternMap = new ConcurrentHashMap<String, String>();
					for (String str : iparray) {
						if (null != str) {
							ipPatternMap.put(str.trim(), str.trim());
						}
					}
					if (ipPatternMap.size() > 0) {
						IPTABLES_LOCK.lock();
						try {
							patternMap = ipPatternMap;
							logger.info("forbid ip pattern properties is load. context is " + patternMap);
							ipPatternMap = null;
						} finally {
							IPTABLES_LOCK.unlock();
						}
					}
				}
			} else {
				IPTABLES_LOCK.lock();
				try {
					patternMap.clear();
					logger.info("forbid ip pattern properties is load. context is " + patternMap);
				} finally {
					IPTABLES_LOCK.unlock();
				}
			}
		} catch(Exception ex) {
			logger.warn("load forbid ip pattern properties failed: " + ipPatterns);
			logger.error(ex.getMessage(), ex);
		}
	}
}

