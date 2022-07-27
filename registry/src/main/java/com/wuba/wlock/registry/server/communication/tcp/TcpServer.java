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
package com.wuba.wlock.registry.server.communication.tcp;

import com.wuba.wlock.common.registry.protocol.ProtocolConstant;
import com.wuba.wlock.registry.server.communication.IServer;
import com.wuba.wlock.registry.server.config.Configuration;
import com.wuba.wlock.registry.server.config.TCPServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class TcpServer implements IServer {
	
	private static final Logger logger = LogManager.getLogger(TcpServer.class);

	TCPServerConfig config;

	TcpUpstreamHandler tcpUpstreamHandler;
	
	public TcpServer(Configuration config, TcpUpstreamHandler tcpUpstreamHandler) {
		this.config = (TCPServerConfig) config;
		this.tcpUpstreamHandler = tcpUpstreamHandler;
	}
	
	static final ServerBootstrap BOOTSTRAP = new ServerBootstrap();
	
	static final ChannelGroup ALL_CHANNELS = new DefaultChannelGroup("WLock.Registry.Server.TCP", GlobalEventExecutor.INSTANCE);
	
	static final EventLoopGroup BOSS_GROUP = new NioEventLoopGroup(1);
	
	static EventLoopGroup workGroup = null;

	
	@Override
	public void start() throws Exception {
		workGroup = new NioEventLoopGroup(config.getWorkerCount());
		initSocketServer();
	}

	@Override
	public void stop() throws Exception {
		ChannelGroupFuture future = ALL_CHANNELS.close();
		future.awaitUninterruptibly();
		BOSS_GROUP.shutdownGracefully();
		workGroup.shutdownGracefully();
	}
	
	private void initSocketServer() throws Exception {
		BOOTSTRAP.group(BOSS_GROUP, workGroup);
		BOOTSTRAP.channel(NioServerSocketChannel.class);
		BOOTSTRAP.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		BOOTSTRAP.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

		BOOTSTRAP.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline cp = ch.pipeline();
				cp.addLast("decoder", new RegistryFrameDecoder(config.getMaxPakageSize()));
				cp.addLast("encoder", new RegistryPrepender(ProtocolConstant.P_END_TAG));
				cp.addLast("registryReceiveHandler", tcpUpstreamHandler);
			}
		});
		
		BOOTSTRAP.option(ChannelOption.TCP_NODELAY, config.isTcpNoDelay());
		BOOTSTRAP.option(ChannelOption.SO_KEEPALIVE, config.isKeepAlive());
		BOOTSTRAP.option(ChannelOption.SO_RCVBUF, config.getRecvBufferSize());
		BOOTSTRAP.option(ChannelOption.SO_SNDBUF, config.getSendBufferSize());
		
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(config.getLocal(), config.getPort());
			ChannelFuture future = BOOTSTRAP.bind(socketAddress);
			ALL_CHANNELS.add(future.channel());
			logger.info("TcpServer listening is start! local:" + config.getLocal() + " port:" + config.getPort());
		} catch (Exception e) {
			logger.info("initSocketServer error", e);
			System.exit(1);
		}
	}
	
}
