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
package com.wuba.wlock.server.communicate.registry;

import com.wuba.wlock.common.exception.ProtocolException;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.server.communicate.TcpPipelineFactory;
import com.wuba.wlock.server.config.RegistryConfig;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.exception.CommunicationException;
import com.wuba.wlock.server.exception.OperationCanceledException;
import com.wuba.wlock.server.exception.RegistryClientRuntimeException;
import com.wuba.wlock.server.util.ThreadPoolUtil;
import com.wuba.wlock.server.util.ThreadRenameFactory;
import com.wuba.wlock.server.exception.ConfigException;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RegisterChannel {
	private static final Logger logger = LoggerFactory.getLogger(RegistryClient.class);

	private ClientBootstrap client;
	private Channel channel;
	private AtomicLong sessionId;
	private String ip;
	private int port;
	private Map<Long, WindowData> waitWindows;
	private AtomicInteger errorTimes = new AtomicInteger(0);
	private static final Object LOCK = new Object();

	public RegisterChannel(String ip, int port) throws ConfigException, RegistryClientRuntimeException {
		this.ip = ip;
		this.port = port;
		sessionId = new AtomicLong(0L);
		waitWindows = new ConcurrentHashMap<>(1024);
		ExecutorService boss = ThreadPoolUtil.newCachedThreadPool(new ThreadRenameFactory("RegisterChannel-boos"));
		ExecutorService worker = ThreadPoolUtil.newCachedThreadPool(new ThreadRenameFactory("RegisterChannel-work"));
		client = new ClientBootstrap();
		client.setFactory(new NioClientSocketChannelFactory(boss, worker));
		client.setPipelineFactory(new TcpPipelineFactory(new RegistryHandler(this), ServerConfig.getInstance().getFrameMaxLength()));
		client.setOption("tcpNoDelay", true);
		client.setOption("keepAlive", true);
		client.setOption("receiveBufferSize", RegistryConfig.getInstance().getRecvBufferSize());
		client.setOption("connectTimeoutMillis", RegistryConfig.getInstance().getConnectTimeOut());
		client.setOption("sendBufferSize", RegistryConfig.getInstance().getSendBufferSize());
		client.setOption("writeBufferHighWaterMark", RegistryConfig.getInstance().getMaxPakageSize());
		channel = createChannel();
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public RegistryProtocol syncSend(RegistryProtocol registryProtocol, int timeout) throws ProtocolException, RegistryClientRuntimeException {
		if (channel == null || !channel.isOpen()) {
			throw new RegistryClientRuntimeException("channel is null or close " + ip);
		}
		long sid = sessionId.getAndIncrement();
		WindowData windowData = new WindowData(sid, registryProtocol);
		registryProtocol.setSessionId(sid);
		waitWindows.put(sid, windowData);
		channel.write(ChannelBuffers.copiedBuffer(registryProtocol.toBytes()));
		byte[] data;
		try {
			data = receive(sid, timeout);
			RegistryProtocol res = RegistryProtocol.fromBytes(data);
			errorTimes.set(0);
			return res;
		} catch (Exception e) {
			errorTimes.incrementAndGet();
			throw new RegistryClientRuntimeException(e);
		} finally {
			waitWindows.remove(sid);
		}
	}

	public void notify(long sid, byte[] response) {
		WindowData cd = waitWindows.get(sid);
		if (cd != null) {
			cd.setData(response);
			cd.countDown();
		}
	}

	public void cancelWd(long sessionId) {
		WindowData wd = waitWindows.get(sessionId);
		if (wd != null && !wd.isCanceled()) {
			for (int i = 0; i < wd.getEvent().getWaitCount(); i++) {
				wd.countDown();
			}
			wd.setCanceled(true);
		}
	}

	public void cancelAllWd() {
		for (WindowData wd : waitWindows.values()) {
			if (wd != null && !wd.isCanceled()) {
				for (int i = 0; i < wd.getEvent().getWaitCount(); i++) {
					wd.countDown();
				}
				wd.setCanceled(true);
			}
		}
		waitWindows.clear();
	}

	private byte[] receive(long sessionId, int timeout) throws CommunicationException, OperationCanceledException, TimeoutException, InterruptedException {
		WindowData cd = waitWindows.get(sessionId);
		if (cd == null) {
			throw new CommunicationException("sessionID:" + sessionId + " need invoke 'registerRec' method before invoke 'receive' method!");
		}
		if (!cd.getEvent().waitOne(timeout)) {
			throw new TimeoutException("receive data from esb registry server timeout:" + timeout + "ms sessionID:" + sessionId
					+ "[" + ip + ":" + port + "]");
		}
		if (cd.isCanceled()) {
			throw new OperationCanceledException("canceled waitting data due to server dead:" + ip + ":" + port);
		}
		return cd.getData();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RegisterChannel that = (RegisterChannel) o;
		return port == that.port &&
				Objects.equals(ip, that.ip);
	}

	@Override
	public int hashCode() {

		return Objects.hash(ip, port);
	}

	@Override
	public String toString() {
		return "RegisterChannel{" +
				"ip='" + ip + '\'' +
				", port=" + port +
				'}';
	}

	public AtomicLong getSessionId() {
		return sessionId;
	}

	public void setSessionId(AtomicLong sessionId) {
		this.sessionId = sessionId;
	}

	public Map<Long, WindowData> getWaitWindows() {
		return waitWindows;
	}

	public void setWaitWindows(Map<Long, WindowData> waitWindows) {
		this.waitWindows = waitWindows;
	}

	public void asyncSend(RegistryProtocol registryProtocol) throws IOException, ProtocolException {
		if (channel == null || !channel.isOpen()) {
			throw new IOException("channel is closed : " + ip);
		}
		channel.write(ChannelBuffers.copiedBuffer(registryProtocol.toBytes()));
	}

	public int getErrorTimes() {
		return errorTimes.get();
	}

	public void setErrorTimes(int errorTimes) {
		this.errorTimes.set(errorTimes);
	}

	public void close() {
		ChannelFuture close = channel.close();
		close.awaitUninterruptibly();
		client.getFactory().releaseExternalResources();
	}


	private Channel createChannel() throws  RegistryClientRuntimeException {
		synchronized (LOCK) {
			ChannelFuture future = client.connect(new InetSocketAddress(ip, port));
			try {
				future.sync();
			} catch (Exception e) {
				logger.error("sync to registry error .ip " + ip);
			}
			if (future.isSuccess()) {
				return future.getChannel();
			} else {
				client.getFactory().releaseExternalResources();
				logger.error("connect to registry error .ip " + ip);
				throw new RegistryClientRuntimeException("connect to registry error .ip " + ip);
			}
		}
	}

}
