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

import com.wuba.wlock.server.exception.RegistryClientRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HostUtil {
	private static final Logger logger = LogManager.getLogger(HostUtil.class);

	public static HostUtil hostUtil = new HostUtil();

	public volatile List<String> ips;

	private static int rr = new Random().nextInt(1000);

	HostUtil() {
	}

	public static HostUtil getInstance() {
		return hostUtil;
	}

	public List<String> getAllServerIP(String addr) throws RegistryClientRuntimeException {
		getAllIP(addr);
		if (ips.isEmpty()) {
			for (int i = 0; i < 3; i++) {
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {
				}
				getAllIP(addr);
				if (!ips.isEmpty()) {
					return ips;
				}
			}
			throw new RegistryClientRuntimeException("get all registry server ip failed.");
		} else {
			return ips;
		}
	}

	private void getAllIP(String addr) {
		InetAddress[] addresses = null;

		try {
			addresses = InetAddress.getAllByName(addr);
		} catch(UnknownHostException e) {
			logger.warn("get registry server ips failed.", e);
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
