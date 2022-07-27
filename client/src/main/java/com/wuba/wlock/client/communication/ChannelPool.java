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
package com.wuba.wlock.client.communication;

import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.ConnectTimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ChannelPool {

	private static final Log logger = LogFactory.getLog(ChannelPool.class);
	private static final ChannelPool INSTANCE = new ChannelPool();

	final ReentrantLock lock = new ReentrantLock();
	static Map<String, NIOChannel> nodeNIOChannelMap = new ConcurrentHashMap<String, NIOChannel>();

	private ChannelPool() {
	}

	public static ChannelPool getInstance() {
		return INSTANCE;
	}

	public NIOChannel getChannel(Server server) throws Exception {
		NIOChannel nioChannel = nodeNIOChannelMap.get(server.node());
		if(nioChannel == null){
			throw new IOException(Version.INFO + "NioChannelPool is empty");
		}
		return nioChannel;
	}


	public void destroy(Server server) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try{
			NIOChannel nioChannel = nodeNIOChannelMap.get(server.node());
			if (nioChannel != null) {
				nioChannel.destroy(server);
			}
		} catch (Exception e) {
			logger.error("ChannelPool destroy error", e);
		}finally{
			lock.unlock();
		}
	}

	public int count(Server server){
		return nodeNIOChannelMap.containsKey(server.node()) ? 1 : 0;
	}

	public void add(Server server, ServerConfig servConf) throws IOException, ConnectTimeoutException {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try{
			NIOChannel nioChannel = nodeNIOChannelMap.get(server.node());
			if (nioChannel == null) {
				nioChannel = new NIOChannel(servConf);
				nodeNIOChannelMap.put(server.node(), nioChannel);
			}

			nioChannel.add(server);
		}finally{
			lock.unlock();
		}
	}

	public void remove(String node) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			nodeNIOChannelMap.remove(node);
		} finally {
			lock.unlock();
		}
	}

	public Collection<NIOChannel> allNioChannel() {
		return nodeNIOChannelMap.values();
	}
}
