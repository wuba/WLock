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
package com.wuba.wlock.client.registryclient.communication;

import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.ConnectTimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistryChannelPool {
	
	private static final Log logger = LogFactory.getLog(RegistryChannelPool.class);

	private final List<RegistryNIOChannel> channelList = new ArrayList<RegistryNIOChannel>();

	private final Object locker = new Object();
	private ServerConfig servConf;
	private RegistryServer server;
	private AtomicInteger idx = new AtomicInteger(0);

	public RegistryChannelPool(ServerConfig servConf, RegistryServer server) throws IOException {
		this.servConf = servConf;
		this.server = server;

		for (int i = 0; i < servConf.getInitConn(); i++) {
			try {
				createAndAddChannel();
			} catch (IOException ex1) {
				logger.error(Version.INFO + ", create sock error in SockPool", ex1);
			} catch (ConnectTimeoutException ex2) {
				logger.error(Version.INFO + ", create sock timeout in SockPool", ex2);
			}
		}

		if (channelList.size() == 0 && servConf.getInitConn() > 0) {
			throw new IOException(Version.INFO + ", create sockpool error:" + servConf.getIp() + ":" + servConf.getPort());
		}
	}

	public RegistryNIOChannel getChannel() throws IOException {
		if (channelList.size() == 0) {
			throw new IOException(Version.INFO + ", registry channel pool is empty");
		}

		return channelList.get(Math.abs(idx.getAndIncrement()) & (channelList.size() - 1));
	}

	public void destroy(RegistryNIOChannel socket) {
		logger.warn(Version.INFO + ", registry socket destroyed!" + socket.toString());

		synchronized (locker) {
			try {
				channelList.remove(socket);
				if (socket.isOpen()) {
					socket.close();
				}
			} catch (Throwable er) {
				logger.error(Version.INFO + ", registry socket destroy error!" + socket.toString(), er);
			}
		}
	}

	public void destroyAll() {
		logger.warn(Version.INFO + ", destroyed all socket " + this.server.toString());

		synchronized (locker) {
			for (int i = 0; i < channelList.size(); i++) {
				RegistryNIOChannel channel = null;
				try {
					channel = channelList.get(i);
					if (channel != null) {
						channelList.remove(i);
						channel.close();
					}
				} catch (Throwable er) {
					String channelToString = "";
					if (channel != null) {
						channelToString = channel.toString();
					}
					logger.error(Version.INFO + ", registry socket destroy error!" +channelToString, er);
				}
			}
		}
	}

	public int count() {
		return channelList.size();
	}

	public RegistryNIOChannel createAndAddChannel() throws IOException, ConnectTimeoutException {
		RegistryNIOChannel sock = new RegistryNIOChannel(servConf, server);
		synchronized (locker) {
			channelList.add(sock);
		}
		return sock;
	}

}
