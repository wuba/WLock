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
import com.wuba.wlock.client.registryclient.protocal.ProtocolParser;
import com.wuba.wlock.client.registryclient.protocal.RegistryProtocol;
import com.wuba.wlock.client.registryclient.protocal.RequestProtocolFactory;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKey;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class VersionSendTask extends Thread {
	
	private static Log logger = LogFactory.getLog(VersionSendTask.class);
	
	private static AtomicBoolean started = new AtomicBoolean(false);
	
	public VersionSendTask() {
	}
	
	public static void startTask() {
		if (started.compareAndSet(false, true)) {
			ThreadPool.registryScheduler.scheduleAtFixedRate(new VersionSendTask(), 0, RegistryClientConfig.SEND_VERSION_PERIOD, TimeUnit.HOURS);
			logger.debug(Version.INFO + ", start send Version task success!");
		}
	}
	
	@Override
	public void run() {
		String version = Version.INFO + "&&" + Version.LANGUAGE;
		RegistryServerPool serPool = RegistryKeyFactory.getInsatnce().getSerPool();
		ConcurrentHashMap<String, RegistryKey> keyMap = RegistryKeyFactory.getInsatnce().getKeyMap();
		for (String key : keyMap.keySet()) {
			try {
				String body = key + "&&" + version;
				RegistryProtocol protocol = RequestProtocolFactory.getInstacne().getClientVersionRequest(body.getBytes());
				RegistryProtocol response = serPool.getServer(key).syncInvoke(protocol, RegistryClientConfig.WAIT_COUNT, RegistryClientConfig.GET_CONFIG_TIMEOUT);
				ProtocolParser.parse(response);
				logger.debug(Version.INFO + ", send version to key" + key + " task success!");
			} catch (Throwable e) {
				logger.warn(Version.INFO + ", run VersionSendTask of key " + key + " failed", e);
			}
		}
		logger.debug(Version.INFO + ", run VersionSendTask job success!");
	}
	
}
