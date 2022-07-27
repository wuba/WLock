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

import com.wuba.wlock.registry.server.communication.IServerHandler;
import com.wuba.wlock.registry.server.context.WLockRegistryChannel;
import com.wuba.wlock.registry.server.context.WLockRegistryContext;
import com.wuba.wlock.registry.server.handler.AsyncInvokerHandler;
import com.wuba.wlock.registry.server.manager.ChannelManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ChannelHandler.Sharable
public class TcpUpstreamHandler extends SimpleChannelInboundHandler<Object> implements IServerHandler {
	@Autowired
	ChannelManager channelManager;

	@Autowired
	AsyncInvokerHandler invokerHandler;


	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (msg instanceof ByteBuf) {
				ByteBuf buf = (ByteBuf) msg;
				byte[] receiveByte = new byte[buf.readableBytes()];
				buf.readBytes(receiveByte);
				WLockRegistryContext context = new WLockRegistryContext(receiveByte, new WLockRegistryChannel(ctx.channel()), this);
				invokerHandler.invoke(context);
			}
		} catch (Exception e) {
			log.error(String.format("%s: TcpUpstreamHandler receive handle request error", ctx.channel().remoteAddress()), e);
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		TcpServer.ALL_CHANNELS.add(ctx.channel());
		log.debug("WLock Rgistry: new channel open: {}", ctx.channel().remoteAddress().toString());
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error(String.format("unexpected exception from downstream remoteAddress(%s)", ctx.channel().remoteAddress().toString()), cause.getCause());
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		log.info("WLock Registry: channel is closed: {}", ctx.channel().remoteAddress().toString());
		channelManager.removeChannel(ctx.channel());
		ctx.channel().close();
	}
	
	@Override
	public void writeResponse(WLockRegistryContext context) {
		if (null != context && null != context.getResponse()) {
			context.getChannel().write(context.getResponse());
		}
	}

}