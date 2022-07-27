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
package com.wuba.wlock.server.config;

import com.wuba.wlock.server.expire.ExpireStrategyFactory;
import com.wuba.wlock.server.exception.ConfigException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public final class ServerConfig extends IConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);
	private static final long OUT_TIME = 30000;
	private static final int MAX_CONN_COUNT_PER_IP = 500;
	private static final int CONN_MAX_UNLIVE_MILLS = 2 * 60 * 1000;
	private static ServerConfig SERVER_CONFIG = new ServerConfig();
	private static final boolean ENABLE_KEEP_MASTER = true;
	private static final int LOCK_INIT_EXPIRETIME = 10000;
	private static final int NOTIFY_TIMEOUT_MILLS = 10000;
	private static final int WATCHEVENT_MAX_UNTOUCH_TIME = 60000;
	private static final int MAX_SERVER_QPS = 80000;
	private static final int MAX_GROUP_QPS = 16000;
	private String cluster;
	private Map<Long, Integer> udpPort = new HashMap<>();
	private Map<Long, String> tcpIpPorts = new HashMap<>();
	private int myUdpPort;

	private static final int MAX_DELAY_TIME = 6000; // 6000 * 50 ms = 5分钟

	private static final long WHEEL_START_TIME = 0L;

	private static final boolean ENABLE_REGISTRY = true;

	private static double STORE_LIMIT_QPS = 100000;
	private static long CACHE_EXPIREEVENT_LIMIT = 50000;
	private static long WARMUP_TIME_MILLSECOND = 500;
	private static boolean EXPIRE_LIMIT_START = false;

	private ServerConfig() {
	}

	public static ServerConfig getInstance() {
		return SERVER_CONFIG;
	}

	@Override
	public void init(String path, boolean mustExist) throws ConfigException {
		super.initConfig(path, mustExist);
	}

	@Override
	public void loadSpecial() {

	}

	public int getGroupQps() {
		return super.getInt("group.qps", MAX_GROUP_QPS);
	}

	public int getServerQps() {
		return super.getInt("server.qps", MAX_SERVER_QPS);
	}

	public String getServerListenIP() throws ConfigException {
		String ip;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new ConfigException(e.getMessage());
		}
		if (Strings.isEmpty(ip)) {
			return super.getString("lock.server.listenIP");
		}
		return ip;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public int getServerListenPort() throws ConfigException {
		// 由于接入 usp 部署 ,所以端口号从配置文件中获取
		String port = System.getProperty("port");
		if (Strings.isEmpty(port)) {
			return super.getInt("lock.server.listenPort");
		}
		return Integer.parseInt(port);
	}

	public int getReceiveBufferSize() throws ConfigException {
		return super.getInt("lock.server.receiveBufferSize");
	}

	public int getSendBufferSize() throws ConfigException {
		return super.getInt("lock.server.sendBufferSize");
	}

	public int getFrameMaxLength() throws ConfigException {
		return super.getInt("lock.server.frameMaxLength");
	}

	public int getWorkerCount() {
		return super.getInt("lock.server.workerCount", 1);
	}

	public int getWriteBufferHighWaterMark() throws ConfigException {
		return super.getInt("lock.server.writeBufferHighWaterMark");
	}

	public int getWriteBufferLowWaterMark() throws Exception {
		return super.getInt("lock.server.writeBufferLowWaterMark");
	}

	public long getConnMaxUnliveTime() {
		return super.getInt("lock.server.connMaxUnliveMills", CONN_MAX_UNLIVE_MILLS);
	}

	public int getMaxConnCountPerIp() {
		return super.getInt("lock.server.maxConnCountPerIp", MAX_CONN_COUNT_PER_IP);
	}

	public boolean isEnableKeepMaster() {
		return super.getBoolean("enable.keep.master", ENABLE_KEEP_MASTER);
	}

	public int getLockInitExpireTime() {
		return super.getInt("lock.init.expiretime", LOCK_INIT_EXPIRETIME);
	}

	public int getMaxDelayMinute() {
		return super.getInt("lock.wheel.maxDelayMinute", MAX_DELAY_TIME);
	}

	public long getTimerWheelStartTime() {
		return super.getLong("lock.wheel.starttime", WHEEL_START_TIME);
	}

	public boolean isEnableRegistry() {
		return super.getBoolean("enable.registry", ENABLE_REGISTRY);
	}

	public int getNotifyTimeoutMills() {
		return super.getInt("lock.notify.timeoutmills", NOTIFY_TIMEOUT_MILLS);
	}

	public int getWatchMaxUntouchMills() {
		return super.getInt("lock.watch.maxUntouchTime", WATCHEVENT_MAX_UNTOUCH_TIME);
	}

	public int getCollectorUdpPort() throws ConfigException {
		return super.getInt("collector.udp.port");
	}

	public String getCollectorUdpIp() throws ConfigException {
		return super.getString("collector.udp.ip");
	}

	public String getExpirePattern() {
		return super.getString("expire.pattern", ExpireStrategyFactory.QUEUE_ALL_PATTERN);
	}

	public double getStoreLimitQps() {
		return super.getDouble("store.limit.qps", STORE_LIMIT_QPS);
	}

	public long getCacheExpireEvent() {
		return super.getLong("cache.expire.event", CACHE_EXPIREEVENT_LIMIT);
	}

	public long getWarmUpTimeMillSecond() {
		return super.getLong("warmup.time.millsecond", WARMUP_TIME_MILLSECOND);
	}

	public int getMyUdpPort() {
		return myUdpPort;
	}

	public void setMyUdpPort(int myUdpPort) {
		this.myUdpPort = myUdpPort;
	}

	public Map<Long, Integer> getUdpPort() {
		return udpPort;
	}

	public void setUdpPort(Map<Long, Integer> udpPort) {
		this.udpPort = udpPort;
	}

	public Map<Long, String> getTcpIpPorts() {
		return tcpIpPorts;
	}

	public void setTcpIpPorts(Map<Long, String> tcpIpPorts) {
		this.tcpIpPorts = tcpIpPorts;
	}

	public boolean getExpireLimitStart() {
		return EXPIRE_LIMIT_START;
	}

	public synchronized void setExpireLimitStart(boolean expireLimitStart) {
		EXPIRE_LIMIT_START = expireLimitStart;
	}
}
