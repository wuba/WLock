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

import com.wuba.wlock.server.client.ClientManager;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.constant.ServerState;
import com.wuba.wlock.server.dispatcher.ContextDispatcher;
import com.wuba.wlock.server.filter.IptablesFilter;
import com.wuba.wlock.server.util.ConnManager;
import com.wuba.wlock.server.util.TimeUtil;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class TcpHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(TcpHandler.class);

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		ByteBuffer buffer = ((ChannelBuffer) e.getMessage()).toByteBuffer();
		byte[] reciveByte = buffer.array();
		Channel channel = e.getChannel();
		ContextDispatcher.dispatch(new LockContext(channel, reciveByte, TimeUtil.getCurrentTimestamp()));
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.debug(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (!IptablesFilter.getInstance().filter(ctx.getChannel().getRemoteAddress())) {
			if (ServerState.isRebooting()) {
				e.getChannel().close();
				logger.warn("this server will reboot " + e.getChannel() + " is close");
			} else {
				if (!ConnManager.getInstance().updateChannelOpen(e.getChannel())) {
					e.getChannel().close();
					logger.warn("channels from the same ip {} maybe too max.", e.getChannel());
					return;
				}
				TcpServer.ALL_CHANNELS.add(e.getChannel());
				logger.info("new channel open : {}.", e.getChannel());
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.error(
				"unexpected exception from downstream remoteAddress {},{}",
				e.getChannel() != null ? e.getChannel().getRemoteAddress() : "null",
				e.getCause());
		logger.error("",e.getCause());
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		logger.info("channel is closed:" + e.getChannel().getRemoteAddress().toString());
		ClientManager.getInstance().channelClosed(e.getChannel());
		TcpServer.ALL_CHANNELS.remove(e.getChannel());
		ConnManager.getInstance().removeConn(e.getChannel());
	}
}
