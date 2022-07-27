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

import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wpaxos.utils.ByteConverter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class RegistryHandler extends SimpleChannelHandler {
	private static final Logger logger = LoggerFactory.getLogger(RegistryHandler.class);

	private RegisterChannel registerChannel;

	public RegistryHandler(RegisterChannel registerChannel){
		this.registerChannel = registerChannel;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		ByteBuffer buffer = ((ChannelBuffer) e.getMessage()).toByteBuffer();
		byte[] receiveByte = buffer.array();
		dispatchMsg(receiveByte);
	}

	private void dispatchMsg(byte[] receiveByte) {
		long sid = ByteConverter.bytesToLongLittleEndian(receiveByte, RegistryProtocol.SESSION_POS);
		registerChannel.notify(sid, receiveByte);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.error(
				"unexpected exception from registry remoteAddress {},{}",
				e.getChannel() != null ? e.getChannel().getRemoteAddress() : "null",
				e.getCause());
		ctx.getChannel().close();
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		logger.error("registry channel close {}",ctx.getChannel().getRemoteAddress());
		ctx.getChannel().close();
	}
}
