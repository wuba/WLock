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
package com.wuba.wlock.server.communicate.retrans;

import com.wuba.wlock.server.communicate.ResponseStatus;
import com.wuba.wlock.server.communicate.TcpPipelineFactory;
import com.wuba.wlock.server.communicate.WLockRequest;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.HeartbeatRequest;
import com.wuba.wlock.server.communicate.protocol.HeartbeatResponse;
import com.wuba.wlock.server.communicate.protocol.ProtocolFactoryImpl;
import com.wuba.wlock.server.config.RegistryConfig;
import com.wuba.wlock.server.exception.RetransRuntimeException;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RetransServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetransServer.class);
	private final RetransServerConfig retransServeConfig;
	private RetransChannel retransChannel;
	private RetransServerState state = RetransServerState.Normal;
	private AtomicBoolean isDelete = new AtomicBoolean(false);
	private static ClientBootstrap client = new ClientBootstrap();
	private static ScheduledExecutorService heartBeatExecute = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("retrans_heartbeat"));
	
	static {
		ExecutorService boss = Executors.newCachedThreadPool();
		ExecutorService worker = Executors.newCachedThreadPool();
		client.setFactory(new NioClientSocketChannelFactory(boss, worker));
		client.setPipelineFactory(new TcpPipelineFactory(new RetransHandler(), RetransServerConfig.getFrameMaxLength()));
		client.setOption("tcpNoDelay", true);
		client.setOption("keepAlive", true);
		client.setOption("receiveBufferSize", RegistryConfig.getInstance().getRecvBufferSize());
		client.setOption("connectTimeoutMillis", RegistryConfig.getInstance().getConnectTimeOut());
		client.setOption("sendBufferSize", RegistryConfig.getInstance().getSendBufferSize());
		client.setOption("writeBufferHighWaterMark", RegistryConfig.getInstance().getMaxPakageSize());
	}
	
	public RetransServer(RetransServerConfig retransServeConfig) {
		this.retransServeConfig = retransServeConfig;
		this.retransChannel = new RetransChannel(retransServeConfig.getIp(), retransServeConfig.getPort(), this);
		init();
	}
	
	public void init() {
		try {
			retransChannel = createChannel();
		} catch (InterruptedException e) {
			LOGGER.error("create retrans channel failed.", e);
		}
		heartBeatExecute.scheduleAtFixedRate(new HeartBeatTimer(this), 5, 5, TimeUnit.SECONDS);
	}
	
	public RetransChannel createChannel() throws InterruptedException {
		String ip = retransServeConfig.getIp();
		int port = retransServeConfig.getPort();
		try {
			ChannelFuture future = client.connect(new InetSocketAddress(ip, port));
			future.addListener(future1 -> {
				if (future1.isSuccess()) {
					retransChannel.setChannel(future1.getChannel());
				} else {
					LOGGER.error("connect to retrans server {}:{} error", ip, port);
				}
			});
			future.sync();
		} catch(Exception e) {
			LOGGER.error("connect to retrans server {}:{} error", ip, port, e);
		} 
		
		Channel channel = retransChannel.getChannel();
		if (channel != null && channel.isOpen()) {
			RetransChannel.channelMapper.put(retransChannel.getChannel().getId(), retransChannel);
			LOGGER.info("init retrans server : {}:{}", ip, port);
		} else {
			this.check();
		}
		return retransChannel;
	}
	
	public void retransRequest(LockContext lockContext, WLockRequest wlockRequest) throws RetransRuntimeException {
		try {
			this.retransChannel.asyncSend(lockContext, wlockRequest);
		} catch (IOException e) {
			this.check();
			throw new RetransRuntimeException("retrans request io error.", e);
		}
	}

	public RetransChannel getRetransChannel() {
		return retransChannel;
	}

	public void setRetransChannel(RetransChannel retransChannel) {
		this.retransChannel = retransChannel;
	}

	public RetransServerState getState() {
		return state;
	}

	public void setState(RetransServerState state) {
		this.state = state;
	}

	public boolean isDelete() {
		return isDelete.get();
	}

	public void setDelelte(boolean isDelete) {
		this.isDelete.set(isDelete);
	}
	
	public void check() {
		retransChannel.setErrorTimes(0);
		RetransDaemonChecker.check(this);
	}

	public void delete() {
		this.setDelelte(true);
		this.retransChannel.closed();
	}
	
	public RetransServerConfig getRetransServeConfig() {
		return retransServeConfig;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((retransServeConfig == null) ? 0 : retransServeConfig.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RetransServer other = (RetransServer) obj;
		if (retransServeConfig == null) {
			if (other.retransServeConfig != null) {
				return false;
			}
		} else if (!retransServeConfig.equals(other.retransServeConfig)) {
			return false;
		}
		return true;
	}

	class HeartBeatTimer implements Runnable {
		RetransServer retransServer;
		
		public HeartBeatTimer(RetransServer retransServer) {
			this.retransServer = retransServer;
		}

		@Override
		public void run() {
			RetransChannel retransChannel = retransServer.getRetransChannel();
			Channel channel = retransChannel.getChannel();
			
			if (channel == null || !channel.isOpen()) {
				retransServer.check();
				return;
			}
			
			if (retransChannel.getErrorTimes() > RetransConfig.CHANNEL_MAX_ERROR_TIMES) {
				retransChannel.closed();
				retransServer.check();
				return;
			}
			
			if (channel != null && channel.isOpen()) {
				HeartbeatRequest heartbeatReq = ProtocolFactoryImpl.getInstance().createHeartBeatReq();
				try {
					HeartbeatResponse heartbeatResp = ProtocolFactoryImpl.getInstance().createHeartBeatRes(heartbeatReq, ResponseStatus.SUCCESS);
					byte[] data = retransChannel.syncSend(heartbeatReq, 3000);
					heartbeatResp.fromBytes(data);
					if (heartbeatResp.getStatus() == ResponseStatus.SUCCESS) {
						retransChannel.setErrorTimes(0);						
					}
				} catch (Exception e) {
					LOGGER.error("heartbeat send error.", e);
				}
			}
		}	
	}
}
