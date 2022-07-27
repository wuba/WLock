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
package com.wuba.wlock.client.service;

import com.wuba.wlock.client.communication.ChannelPool;
import com.wuba.wlock.client.communication.NIOChannel;
import com.wuba.wlock.client.communication.WindowData;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.helper.OpaqueGenerator;
import com.wuba.wlock.client.protocol.extend.HeartbeatRequest;
import com.wuba.wlock.client.protocol.extend.ProtocolFactoryImpl;
import com.wuba.wlock.client.util.TimeUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatService {
	private static final Log logger = LogFactory.getLog(HeartbeatService.class);

	public AtomicBoolean started = new AtomicBoolean(true);
	private int initialDelay = 30;
	private int period = (new Random().nextInt(4) + 2);
	private ConcurrentHashMap<String, Integer> serverDetectMap = new ConcurrentHashMap<String, Integer>(128);
	private ConcurrentHashMap<Long, WindowData> awaitWindow = new ConcurrentHashMap<Long, WindowData>(128);
	private AtomicInteger clearCount = new AtomicInteger(0);
	private int heartbeatErrorMax = 5;
	private int heartbeatMaxTimes = 512;
	private int heartbeatTimeout = 2;
	public static HeartbeatService hbService = new HeartbeatService();

	private HeartbeatService() {
		Thread thread = new Thread(new HeartbeatWorker());
		thread.setName("WLock HeartbeatDetectThread");
		thread.setDaemon(true);
		thread.start();

		logger.info(Version.INFO + ", start heartbeat detect job success.");
	}

	public static HeartbeatService getHbService() {
		return hbService;
	}

	private void sendHeartbeat() {
		// 清理脏数据
		if (clearCount.incrementAndGet() > heartbeatMaxTimes) {
			clearCount.set(0);
			clearDetectMap();
		}

		clearAwaitMap();

		// 异步发送
		asyncSendHeartbeat();
		// 等待
		TimeUtil.secondSleep(heartbeatTimeout);
		// 处理结果
		handleResults();

	}

	private void asyncSendHeartbeat() {
		Collection<NIOChannel> allNioChannel = ChannelPool.getInstance().allNioChannel();
		for (NIOChannel channel: allNioChannel) {
			HeartbeatRequest request = ProtocolFactoryImpl.getInstance().createHeartbeatRequest();
			request.setSessionID(OpaqueGenerator.getOpaque());
			try {
				if (!channel.isOpen()) {
					throw new IOException("channel is close:" + channel.toString());
				}
				WindowData wd = new WindowData(request.getSessionID(), request, channel);
				addWData(wd);

				channel.asyncSend(wd);
			} catch (Exception e) {
				logger.error(Version.INFO + ", asyncSendHeartbeat failed.", e);
			}
		}
	}

	private void handleResults() {
		for (WindowData wd : awaitWindow.values()) {
			if (wd.getResult()) {
				serverErrorDecreace(wd.getChannel());
			} else {
				serverErrorIncreace(wd.getChannel());
			}
		}

	}

	public void addWData(WindowData wd) {
		awaitWindow.put(wd.getSessionId(), wd);
	}

	private void serverErrorDecreace(NIOChannel channel) {
		try {
			if (null != channel && channel.isOpen()) {
				String localAddr = channel.getSockChannel().socket().getLocalSocketAddress().toString();
				this.serverDetectMap.put(localAddr, 0);
				logger.debug(Version.INFO + channel.getSockChannel().socket().getRemoteSocketAddress().toString() + ": send heartbeat success.");
			}
		} catch (Throwable e) {
			logger.error("serverErrorDecreace error", e);
		}
	}

	private void serverErrorIncreace(NIOChannel channel) {
		try {
			if (null != channel && channel.isOpen()) {
				String localAddr = channel.getSockChannel().socket().getLocalSocketAddress().toString();
				Integer errCount = this.serverDetectMap.get(localAddr);
				if (errCount == null) {
					errCount = 0;
				}
				errCount++;
				if (errCount < this.heartbeatErrorMax) {
					this.serverDetectMap.put(localAddr, errCount);
				} else {
					channel.replaceChannel();
					this.serverDetectMap.remove(localAddr);
					logger.info(Version.INFO + localAddr + " connect to server " + channel.getServerConfig().getIp() + " is bad, so disconnect it.");
				}
			}
		} catch (Throwable e) {
			logger.error("HeartbeatService serverErrorIncreace error", e);
		}
	}

	private void clearDetectMap() {
		this.serverDetectMap.clear();
	}

	private void clearAwaitMap() {
		this.awaitWindow.clear();
	}

	public void handleResponse(long opaque, byte[] result) {
		WindowData wd = awaitWindow.get(opaque);
		if (wd != null) {
			wd.setResult(true);
			wd.appendData(result);
		}
	}

	public class HeartbeatWorker implements Runnable {

		@Override
		public void run() {
			TimeUtil.secondSleep(initialDelay);
			while (started.get()) {
				try {
					sendHeartbeat();
				} catch (Throwable e) {
					logger.warn(Version.INFO + " wlock send heartbeat caught unexpected exception: " + e);
				}
				logger.debug(Version.INFO + " heartbeat detect job run success.");
				TimeUtil.secondSleep(period);
			}
		}

	}

}
