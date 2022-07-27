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
package com.wuba.wlock.server.communicate;

import com.wuba.wlock.server.bootstrap.base.IServer;
import com.wuba.wlock.server.bootstrap.signal.RebootSignalHandle;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.util.ConnManager;
import com.wuba.wlock.server.util.ThreadPoolUtil;
import com.wuba.wlock.server.util.ThreadRenameFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.net.InetSocketAddress;

public final class TcpServer implements IServer {
	private static final ServerBootstrap BOOTSTRAP = new ServerBootstrap();
	public static final ChannelGroup ALL_CHANNELS = new DefaultChannelGroup("WLOCK");
	private static TcpServer tcpServer = new TcpServer();

	private static final Logger LOGGER = LoggerFactory.getLogger(TcpServer.class);

	public static TcpServer getInstance() {
		return tcpServer;
	}

	@Override
	public void start() {
		try {
			final boolean tcpNoDelay = true;
			BOOTSTRAP.setFactory(new NioServerSocketChannelFactory(ThreadPoolUtil.newCachedThreadPool(new ThreadRenameFactory("TcpServer-boos")),
					ThreadPoolUtil.newCachedThreadPool(new ThreadRenameFactory("TcpServer-work")), ServerConfig.getInstance().getWorkerCount()));
			BOOTSTRAP.setPipelineFactory(new TcpPipelineFactory(new TcpHandler(), ServerConfig.getInstance().getFrameMaxLength()));
			BOOTSTRAP.setOption("child.tcpNoDelay", tcpNoDelay);
			BOOTSTRAP.setOption("child.receiveBufferSize", ServerConfig.getInstance().getReceiveBufferSize());
			BOOTSTRAP.setOption("child.sendBufferSize", ServerConfig.getInstance().getSendBufferSize());
			BOOTSTRAP.setOption("child.writeBufferHighWaterMark", ServerConfig.getInstance().getWriteBufferHighWaterMark());
			BOOTSTRAP.setOption("child.writeBufferLowWaterMark", ServerConfig.getInstance().getWriteBufferLowWaterMark());

			InetSocketAddress socketAddress = null;
			socketAddress = new InetSocketAddress(ServerConfig.getInstance().getServerListenIP(), ServerConfig.getInstance().getServerListenPort());
			Channel channel = BOOTSTRAP.bind(socketAddress);
			ALL_CHANNELS.add(channel);
		} catch(Exception e) {
			LOGGER.error("init socket server error", e);
			System.exit(0);
		}

		LOGGER.info("------------------signal register start---------------------");
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName != null && osName.indexOf("window") == -1) {
			RebootSignalHandle rebootHandler = new RebootSignalHandle();
			Signal sig = new Signal("USR2");
			Signal.handle(sig, rebootHandler);
		}
		LOGGER.info("------------------signal register success----------------------\n");
	}

	@Override
	public void stop() {
		ConnManager.getInstance().beforeCloseCheck();
		LOGGER.info("----------------------------------------------------");
		LOGGER.info("-- socket server closing...");
		LOGGER.info("-- channels count : " + ALL_CHANNELS.size());
		ChannelGroupFuture future = ALL_CHANNELS.close();
		LOGGER.info("-- closing all channels...");
		future.awaitUninterruptibly();
		LOGGER.info("-- closed all channels...");
		BOOTSTRAP.getFactory().releaseExternalResources();
		LOGGER.info("-- released external resources");
		LOGGER.info("-- close success !");
		LOGGER.info("----------------------------------------------------");
	}
}
