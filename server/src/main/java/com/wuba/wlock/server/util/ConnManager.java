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

import com.wuba.wlock.server.communicate.TcpServer;
import com.wuba.wlock.server.config.ServerConfig;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnManager {
	private static final Logger logger = LoggerFactory.getLogger(ConnManager.class);
	private ConcurrentHashMap<Integer, Long> heartbeatMap = new ConcurrentHashMap<Integer, Long>();
	private ConcurrentHashMap<Integer, Long> readStateMap = new ConcurrentHashMap<Integer, Long>();
	private ConcurrentHashMap<String, AtomicInteger> connCountMap = new ConcurrentHashMap<String, AtomicInteger>();
	private static final long MAX_UNLIVE_TIME = ServerConfig.getInstance().getConnMaxUnliveTime();
	private static final long CLOSE_MAX_WAIT_TIME = 60 * 1000;
	private static final long CLOSE_MIN_WAIT_TIME = 3 * 1000;
	private static final int MAX_CONN_COUNT_PER_IP = ServerConfig.getInstance().getMaxConnCountPerIp();
	private static ConnManager instance = new ConnManager();

	private ConnManager() {
	}

	public synchronized static ConnManager getInstance() {
		return instance;
	}

	public boolean updateChannelOpen(Channel channel) {
		if (!checkConnCount(channel)) {
			return false;
		}
		updateHeartBeat(channel, TimeUtil.getCurrentTimestamp());
		updateRead(channel, TimeUtil.getCurrentTimestamp());
		logger.debug("update heartbeat from channel : {}, map item : {}", channel.getRemoteAddress().toString(), heartbeatMap.get(getKey(channel)));
		return true;
	}

	public void updateHeartBeat(Channel channel, long timestamp) {
		heartbeatMap.put(getKey(channel), timestamp);
	}

	public void updateRead(Channel channel, long timestamp) {
		readStateMap.put(getKey(channel), timestamp);
	}

	public boolean checkConnCount(Channel channel) {
		try {
			InetSocketAddress inetAddr = (InetSocketAddress) (channel.getRemoteAddress());
			String ip = inetAddr.getAddress().getHostAddress();
			AtomicInteger count = connCountMap.get(ip);
			if (count == null) {
				connCountMap.putIfAbsent(ip, new AtomicInteger(1));
			}
			count = connCountMap.get(ip);
			if (count.incrementAndGet() <= MAX_CONN_COUNT_PER_IP) {
				logger.debug("the connections from ip {} is count {}.", ip, count.get());
				return true;
			} else {
				logger.warn("the connections from ip {} is too Max {}.", ip, count.get());
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	public void decreConnCount(Channel channel) {
		try {
			InetSocketAddress inetAddr = (InetSocketAddress) (channel.getRemoteAddress());
			String ip = inetAddr.getAddress().getHostAddress();
			AtomicInteger count = connCountMap.get(ip);
			if (count != null && count.get() > 0) {
				count.decrementAndGet();
			} else {
				connCountMap.remove(ip);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("the connections from ip {} is count {}.", ip, count.get());
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public int getKey(Channel channel) {
		return channel.getId();
	}

	public void removeConn(Channel channel) {
		heartbeatMap.remove(getKey(channel));
		readStateMap.remove(getKey(channel));
		decreConnCount(channel);
	}

	public boolean isAlive(Channel channel) {
		if (channel.getRemoteAddress() == null) {
			return true;
		}
		if (this.heartbeatMap.containsKey(getKey(channel))) {
			long lastHeartbeat = this.heartbeatMap.get(getKey(channel));
			long current = TimeUtil.getCurrentTimestamp();
			if (current - lastHeartbeat <= MAX_UNLIVE_TIME) {
				return true;
			} else {
				logger.info("channel : {} has no msg in {} mills, so close it.", channel, MAX_UNLIVE_TIME);
				return false;
			}
		}
		logger.info("channel : {} is illegal, as has no heartbeart.", channel);
		return false;
	}

	private boolean hasRead(Channel channel) {
		if (channel.getRemoteAddress() == null) {
			return false;
		}
		if (readStateMap.containsKey(getKey(channel))) {
			long lastHeartbeat = readStateMap.get(getKey(channel));
			long current = TimeUtil.getCurrentTimestamp();
			if (current - lastHeartbeat <= CLOSE_MIN_WAIT_TIME) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	private boolean hasRead() {
		try {
			boolean flag = false;
			Iterator<Channel> iter = TcpServer.ALL_CHANNELS.iterator();
			while (iter.hasNext()) {
				Channel channel = iter.next();
				flag = flag || hasRead(channel);
			}
			return flag;
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	public void beforeCloseCheck() {
		long start = TimeUtil.getCurrentTimestamp();
		long current = TimeUtil.getCurrentTimestamp();
		boolean hasRead = true;
		while ((((current - start) < CLOSE_MAX_WAIT_TIME)) && hasRead) {
			hasRead = hasRead();
			if (hasRead) {
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {
				}
			}
			current = TimeUtil.getCurrentTimestamp();
		}
		if (!hasRead) {
			logger.info("has no msg to read......");
		}
	}
}
