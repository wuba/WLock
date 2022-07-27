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
package com.wuba.wlock.client.util;

import com.wuba.wlock.client.helper.ByteConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class InetAddressUtil {

	private static final Log logger = LogFactory.getLog(InetAddressUtil.class);
	private static String localIP;
	private static int localHostIP;

	static {
		try {
			localIP = InetAddressUtil.getIpMixed();
			logger.info("HostAddress is " + localIP);
			localHostIP = ByteConverter.ipToInt(localIP);
		} catch (UnknownHostException e) {
			logger.error("InetAddressUtil get Ip error", e);
		} catch (Exception e) {
			logger.error("InetAddressUtil get Ip error", e);
		}
	}

	public static String getLocalHostName() throws UnknownHostException {
		try {
			return (InetAddress.getLocalHost()).getHostAddress();
		} catch (UnknownHostException uhe) {
			String host = uhe.getMessage();
			if (host != null) {
				int colon = host.indexOf(':');
				if (colon > 0) {
					return host.substring(0, colon);
				}
			}
			throw uhe;
		}
	}

	/**
	 * 返回本机IP INT表示类型
	 * 
	 * @return
	 * @throws Exception
	 */
	public static int getIpInt() {
		try {
			if (localHostIP == 0) {
				localHostIP = ByteConverter.ipToInt(localIP);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return localHostIP;
	}

	/**
	 * 根据主题产生注册key 规则：ip+subject
	 * 
	 * @return
	 * @throws UnknownHostException
	 */
	public static String generatorRegistryKey(String subject)
			throws UnknownHostException {
		return localIP + ":" + subject;
	}

	public static String getHostAddress() throws UnknownHostException {
		try {
			return (InetAddress.getLocalHost()).getHostAddress();
		} catch (UnknownHostException uhe) {
			String host = uhe.getMessage();
			if (host != null) {
				int colon = host.indexOf(':');
				if (colon > 0) {
					return host.substring(0, colon);
				}
			}
			throw uhe;
		}
	}

	public static int getHashCode() throws UnknownHostException {
		return (String.valueOf(localIP)).hashCode();
	}

	/**
	 * 获取本机IP
	 * @return
	 */
	public static String getIpMixed() {
		Enumeration<NetworkInterface> netInterfaces = null;
		try {
			netInterfaces = NetworkInterface.getNetworkInterfaces();
			while (netInterfaces.hasMoreElements()) {
				NetworkInterface ni = netInterfaces.nextElement();
				Enumeration<InetAddress> ips = ni.getInetAddresses();
				if ("eth0".equals(ni.getName()) || "eth1".equals(ni.getName()) || "em1".equals(ni.getName())) {
					while (ips.hasMoreElements()) {
						String strIp = ips.nextElement().getHostAddress();
						if (strIp.split(Pattern.quote(".")).length > 3) {
							return strIp;
						}
					}
				}
			}
			System.err.println("wlock client get default ip is :" + "127.0.0.1");
			return "127.0.0.1";
		} catch (Exception e) {
			logger.error("InetAddressUtil getIpMixed error", e);
			System.err.println("wlock client get default ip :" + "127.0.0.1");
			return "127.0.0.1";
		}
	}

}
