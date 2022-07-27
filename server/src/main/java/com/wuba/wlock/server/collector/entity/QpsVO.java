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
package com.wuba.wlock.server.collector.entity;


import com.wuba.wlock.common.collector.protocol.GroupQps;
import com.wuba.wlock.common.collector.protocol.KeyQps;
import com.wuba.wlock.common.collector.protocol.QpsEntity;
import com.wuba.wlock.common.collector.protocol.ServerQps;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QpsVO{
	/**
	 * 服务器的qps
	 */
	private static volatile QpsLockStatCounter serverQps = new QpsLockStatCounter();
	private static volatile QpsLockStatCounter lastServerQps = new QpsLockStatCounter();

	/**
	 * 密钥的QPS
	 */
	private static volatile Map<String, Map<Integer, QpsLockStatCounter>> keyQps = new ConcurrentHashMap<String, Map<Integer, QpsLockStatCounter>>();
	private static volatile Map<String, Map<Integer, QpsLockStatCounter>> lastKeyQps = new ConcurrentHashMap<String, Map<Integer, QpsLockStatCounter>>();

	/**
	 * lockkey的QPS
	 */
	private static volatile Map<String, ConcurrentHashMap<String, QpsLockStatCounter>> lockKeyQps = new ConcurrentHashMap<>();
	private static volatile Map<String, ConcurrentHashMap<String, QpsLockStatCounter>> lastLockKeyQps = new ConcurrentHashMap<>();

	/**
	 * group qps
	 */
	private static volatile Map<Integer, QpsLockStatCounter> groupQps = new ConcurrentHashMap<>();
	private static volatile Map<Integer, QpsLockStatCounter> lastGroupQps = new ConcurrentHashMap<>();


	private QpsVO() {
	}

	public static QpsVO qpsEntity = new QpsVO();

	public static QpsVO getInstance() {
		return qpsEntity;
	}

	public void incrServerOtherQps() {
		serverQps.incrOtherQps();
	}

	public void incrServerAcquireQps() {
		serverQps.incrAcquireQps();
	}

	public void incrServerRenewQps() {
		serverQps.incrRenewQps();
	}

	public void incrServerReleaseQps() {
		serverQps.incrReleaseQps();
	}

	public void incrServerWatchQps() {
		serverQps.incrWatchQps();
	}

	public void incrServerGetQps() {
		serverQps.incrGetQps();
	}

	public void incrServerAcquireFailQps() {
		serverQps.incrAcquireFailQps();
	}

	public void incrServerRenewFailQps() {
		serverQps.incrRenewFailQps();
	}

	public void incrServerReleaseFailQps() {
		serverQps.incrReleaseFailQps();
	}

	public void incrServerWatchFailQps() {
		serverQps.incrWatchFailQps();
	}

	public void incrServerGetFailQps() {
		serverQps.incrGetFailQps();
	}

	public void incrServerDeleteQps(){
		serverQps.incrDeleteQps();
	}

	public void incrServerDeleteFailQps(){
		serverQps.incrDeleteFailQps();
	}

	public void incrServerAbandonQps(){
		serverQps.incrAbandonQps();
	}


	public void incrGroupOtherQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrOtherQps();
	}

	public void incrGroupAcquireQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrAcquireQps();
	}

	public void incrGroupRenewQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrRenewQps();
	}

	public void incrGroupReleaseQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrReleaseQps();
	}

	public void incrGroupWatchQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrWatchQps();
	}

	public void incrGroupGetQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrGetQps();
	}

	public void incrGroupAcquireFailQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrAcquireFailQps();
	}

	public void incrGroupRenewFailQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrRenewFailQps();
	}

	public void incrGroupReleaseFailQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrReleaseFailQps();
	}

	public void incrGroupWatchFailQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrWatchFailQps();
	}

	public void incrGroupGetFailQps(int group) {
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrGetFailQps();
	}

	public void incrGroupDeleteQps(int group){
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrDeleteQps();
	}

	public void incrGroupDeleteFailQps(int group){
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrDeleteFailQps();
	}

	public void incrGroupAbandonQps(int group){
		QpsLockStatCounter qpsLockStatCounter = groupQps.get(group);
		if (qpsLockStatCounter == null) {
			synchronized (groupQps) {
				qpsLockStatCounter = groupQps.get(group);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					groupQps.put(group, qpsLockStatCounter);
				}
			}
		}
		qpsLockStatCounter.incrAbandonQps();
	}

	private QpsLockStatCounter getOrCreate(String key, int groupId) {
		Map<Integer, QpsLockStatCounter> qpsLockStatCounterMap = keyQps.get(key);
		if (qpsLockStatCounterMap == null) {
			synchronized (keyQps) {
				qpsLockStatCounterMap = keyQps.get(key);
				if (qpsLockStatCounterMap == null) {
					qpsLockStatCounterMap = new ConcurrentHashMap<Integer, QpsLockStatCounter>();
					keyQps.put(key, qpsLockStatCounterMap);
				}
			}
		}

		QpsLockStatCounter qpsLockStatCounter = qpsLockStatCounterMap.get(groupId);
		if (qpsLockStatCounter == null) {
			synchronized (keyQps) {
				qpsLockStatCounter = qpsLockStatCounterMap.get(groupId);
				if (qpsLockStatCounter == null) {
					qpsLockStatCounter = new QpsLockStatCounter();
					qpsLockStatCounterMap.put(groupId, qpsLockStatCounter);
				}
			}
		}

		return qpsLockStatCounter;
	}

	public void incrKeyOtherQps(String key, int groupId) {
		getOrCreate(key, groupId).incrOtherQps();
	}

	public void incrKeyAcquireQps(String key, int groupId) {
		getOrCreate(key, groupId).incrAcquireQps();
	}

	public void incrKeyRenewQps(String key, int groupId) {
		getOrCreate(key, groupId).incrRenewQps();
	}

	public void incrKeyReleaseQps(String key, int groupId) {
		getOrCreate(key, groupId).incrReleaseQps();
	}

	public void incrKeyWatchQps(String key, int groupId) {
		getOrCreate(key, groupId).incrWatchQps();
	}

	public void incrKeyGetQps(String key, int groupId) {
		getOrCreate(key, groupId).incrGetQps();
	}

	public void incrKeyAcquireFailQps(String key, int groupId) {
		getOrCreate(key, groupId).incrAcquireFailQps();
	}

	public void incrKeyRenewFailQps(String key, int groupId) {
		getOrCreate(key, groupId).incrRenewFailQps();
	}

	public void incrKeyReleaseFailQps(String key, int groupId) {
		getOrCreate(key, groupId).incrReleaseFailQps();
	}

	public void incrKeyWatchFailQps(String key, int groupId) {
		getOrCreate(key, groupId).incrWatchFailQps();
	}

	public void incrKeyGetFailQps(String key, int groupId) {
		getOrCreate(key, groupId).incrGetFailQps();
	}

	public void incrKeyDeleteQps(String key, int groupId){
		getOrCreate(key, groupId).incrDeleteQps();
	}

	public void incrKeyDeleteFailQps(String key, int groupId){
		getOrCreate(key, groupId).incrDeleteFailQps();
	}

	public void incrKeyAbandonQps(String key, int groupId){
		getOrCreate(key, groupId).incrAbandonQps();
	}


	public ServerQps calculateServerQps() {
		QpsLockStatCounter tmpCounter = new QpsLockStatCounter(serverQps);
		QpsEntity qpsByType = tmpCounter.getRealTimeQps(lastServerQps);
		lastServerQps.replaceBy(tmpCounter);
		ServerQps serverQps = new ServerQps();
		serverQps.setQps(qpsByType);
		return serverQps;
	}

	public GroupQps calcutateGroupQps() {
		GroupQps result = new GroupQps();
		Map<Integer, QpsEntity> qps = new HashMap<>();
		groupQps.forEach((key, value) -> {
			int group = key;
			QpsLockStatCounter tmpCounter = new QpsLockStatCounter(value);
			QpsLockStatCounter lastQps = lastGroupQps.get(group);
			if (lastQps == null) {
				lastQps = new QpsLockStatCounter();
				lastGroupQps.put(group, lastQps);
			}
			QpsEntity qpsEntity = tmpCounter.getRealTimeQps(lastQps);
			lastQps.replaceBy(tmpCounter);
			qps.put(group, qpsEntity);
		});
		result.setQps(qps);
		return result;
	}

	public KeyQps calculateKeyQps() {
		KeyQps result = new KeyQps();
		Map<String, Map<Integer, QpsEntity>> keyGroupQps = new HashMap<String, Map<Integer, QpsEntity>>();
		keyQps.forEach((key, value) -> {
			HashMap<Integer, QpsEntity> qpsEntityHashMap = new HashMap<>();
			keyGroupQps.put(key, qpsEntityHashMap);

			Map<Integer, QpsLockStatCounter> qpsLockStatCounterMap = value;
			qpsLockStatCounterMap.forEach((groupId, counter) -> {
				QpsLockStatCounter tmpCounter = new QpsLockStatCounter(counter);
				Map<Integer, QpsLockStatCounter> lastQpsLockStatCounterMap = lastKeyQps.get(key);
				if (lastQpsLockStatCounterMap == null) {
					lastQpsLockStatCounterMap = new ConcurrentHashMap<Integer, QpsLockStatCounter>();
					lastKeyQps.put(key, lastQpsLockStatCounterMap);
				}

				QpsLockStatCounter lastQps = lastQpsLockStatCounterMap.get(groupId);
				if (lastQps == null) {
					lastQps = new QpsLockStatCounter();
					lastQpsLockStatCounterMap.put(groupId, lastQps);
				}

				QpsEntity qpsEntity = tmpCounter.getRealTimeQps(lastQps);
				lastQps.replaceBy(tmpCounter);

				qpsEntityHashMap.put(groupId, qpsEntity);
			});

		});
		result.setKeyGroupQps(keyGroupQps);
		return result;
	}

}
