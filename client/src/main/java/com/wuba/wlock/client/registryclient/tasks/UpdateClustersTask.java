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
package com.wuba.wlock.client.registryclient.tasks;

import com.wuba.wlock.client.config.RegistryClientConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.helper.ThreadPool;
import com.wuba.wlock.client.registryclient.communication.RegistryServerPool;
import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;
import com.wuba.wlock.client.registryclient.protocal.ProtocolParser;
import com.wuba.wlock.client.registryclient.protocal.RegistryProtocol;
import com.wuba.wlock.client.registryclient.protocal.RequestProtocolFactory;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKey;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class UpdateClustersTask extends Thread {
	
	private static final Log logger = LogFactory.getLog(UpdateClustersTask.class);
	
	private static AtomicBoolean started = new AtomicBoolean(false);
	
	public UpdateClustersTask() {
	}
	
	public static void startTask() {
		if (started.compareAndSet(false, true)) {
			ThreadPool.registryScheduler.scheduleAtFixedRate(new UpdateClustersTask(), RegistryClientConfig.GET_CONFIG_PERIOD, 
					RegistryClientConfig.GET_CONFIG_PERIOD, TimeUnit.SECONDS);
			logger.debug(Version.INFO + ", start update clusters task success!");
		}
	}
	
	@Override
	public void run() {
		RegistryServerPool serPool = RegistryKeyFactory.getInsatnce().getSerPool();
		ConcurrentHashMap<String, RegistryKey> keyMap = RegistryKeyFactory.getInsatnce().getKeyMap();
		for (Entry<String, RegistryKey> entry : keyMap.entrySet()) {
			try {
				ClientKeyEntity keyObj = new ClientKeyEntity(entry.getKey());
				keyObj.setVersion(entry.getValue().getClusterConf().getVersion());
				RegistryProtocol protocol = RequestProtocolFactory.getInstacne().getCongigValidatorRequest(ClientKeyEntity.toJsonString(keyObj).getBytes());
				RegistryProtocol response = serPool.getServer(entry.getKey()).syncInvoke(protocol, RegistryClientConfig.WAIT_COUNT, RegistryClientConfig.GET_CONFIG_TIMEOUT);
				ProtocolParser.parse(response);
				logger.debug(Version.INFO + ", update key " + entry.getKey() + "task send success!");
			} catch (Throwable e) {
				logger.warn(Version.INFO + ", run updateClusterTask: key " + entry.getKey() + " update failed!", e);
			}
		}
	}
	
}
