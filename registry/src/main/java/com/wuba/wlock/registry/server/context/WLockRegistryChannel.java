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
package com.wuba.wlock.registry.server.context;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class WLockRegistryChannel {

	private static Logger logger = LogManager.getLogger(WLockRegistryChannel.class);
	
	private Channel nettyChannel;
	
	private String remoteIp;
	
	private int remotePort;

	public WLockRegistryChannel() {
	}
	
	public WLockRegistryChannel(Channel nettyChannel) {
		this.nettyChannel = nettyChannel;
		SocketAddress remoteAddress = nettyChannel.remoteAddress();
		this.remoteIp = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
		this.remotePort = ((InetSocketAddress) remoteAddress).getPort();
	}
	
	public void write(byte[] buffer) {
		if (null == buffer) {
			logger.warn("buffer before send is null, so ignore. channel is:" + remoteIp + ":" + remotePort);
		}
		if (this.nettyChannel.isWritable()) {
			this.nettyChannel.writeAndFlush(Unpooled.copiedBuffer(buffer));
		} else {
			close();
			logger.info("channel can not writable, close channel" + remoteIp + ":" + remotePort);
		}
	}
	
	public void close() {
		nettyChannel.close();
	}
	
	public Channel getNettyChannel() {
		return nettyChannel;
	}

	public void setNettyChannel(Channel nettyChannel) {
		this.nettyChannel = nettyChannel;
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	/**
	 * 判断 channel 是否可以使用
	 * @return
	 */
	public boolean channelCanUse(){
		return nettyChannel != null && nettyChannel.isOpen();
	}
}
