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

import com.wuba.wlock.server.communicate.WLockResponse;
import com.wuba.wpaxos.utils.ByteConverter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RetransHandler extends SimpleChannelHandler {
	private static final Logger logger = LoggerFactory.getLogger(RetransHandler.class);

	public RetransHandler() {}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ByteBuffer buffer = ((ChannelBuffer) e.getMessage()).toByteBuffer();
		byte[] reciveByte = buffer.array();
		dispatchMsg(reciveByte, ctx.getChannel());
	}

	private void dispatchMsg(byte[] reciveByte, Channel channel) throws IOException {
		long sid = ByteConverter.bytesToLongLittleEndian(reciveByte, WLockResponse.SESSION_ID_POS);	
		RetransChannel retransChannel = RetransChannel.getChannelById(channel.getId());
		if (retransChannel != null) {
			retransChannel.notify(sid, reciveByte);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		logger.error("unexpected exception from registry remoteAddress {},{}",
				e.getChannel() != null ? e.getChannel().getRemoteAddress() : "null", e.getCause());
		RetransChannel retransChannel = RetransChannel.getChannelById(ctx.getChannel().getId());
		if (retransChannel != null) {
			retransChannel.getRetransServer().check();
			RetransChannel.removeRecordChannel(ctx.getChannel().getId());
		}
		ctx.getChannel().close();
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		RetransChannel retransChannel = RetransChannel.getChannelById(ctx.getChannel().getId());
		RetransChannel.removeRecordChannel(ctx.getChannel().getId());
		retransChannel.getRetransServer().check();
	}
}
