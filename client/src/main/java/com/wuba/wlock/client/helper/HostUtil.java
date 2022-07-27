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
package com.wuba.wlock.client.helper;

import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HostUtil {
	
	private static final Log logger = LogFactory.getLog(HostUtil.class);

	public static HostUtil hostUtil = new HostUtil();

	public volatile List<String> ips;

	private static int rr = new Random().nextInt(1000);

	HostUtil() {
	}

	public static HostUtil getInstance() {
		return hostUtil;
	}

	public List<String> getAllServerIP() throws RegistryClientRuntimeException {
		getAllIP();
		if (ips.isEmpty()) {
			for (int i = 0; i < 3; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				getAllIP();
				if (!ips.isEmpty()) {
					return ips;
				}
			}
			throw new RegistryClientRuntimeException("get all WLock registry server ip failed.");
		} else {
			return ips;
		}
	}

	private void getAllIP() {
		InetAddress[] addresses = null;

		try {
			addresses = InetAddress.getAllByName(RegistryKeyFactory.getInsatnce().getRegistryServerIP());
		} catch (UnknownHostException e) {
			logger.warn("get WLock registry server ips failed.");
		}
		if (addresses != null && addresses.length != 0) {
			List<String> newIps = new ArrayList<String>();
			for (InetAddress address : addresses) {
				if (address.getHostAddress() != null) {
					newIps.add(address.getHostAddress());
				}
			}
			Collections.sort(newIps);
			this.ips = newIps;
		}
	}

}
