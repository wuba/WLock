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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.Arrays;
import java.util.List;

public class RegistryPrepender extends MessageToMessageEncoder<ByteBuf> {
	
	private static final int TOTAL_LEN_SIZE = 4;
	
	private byte[] endTag;

	private boolean lengthIncludeLengthFieldLength;
	
	private boolean lengthIncludeEndTagLength;
	
	public RegistryPrepender(byte[] endTag) {
		this(endTag, false, false);
		
	}

	public RegistryPrepender(byte[] endTag, boolean lengthIncludeLengthFieldLength, boolean lengthIncludeEndTagLength) {
		super();
		this.endTag = Arrays.copyOf(endTag, endTag.length);
		this.lengthIncludeEndTagLength = lengthIncludeEndTagLength;
		this.lengthIncludeLengthFieldLength = lengthIncludeLengthFieldLength;
	}
	
	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		
		int length = msg.readableBytes();
		
		ByteBuf data = Unpooled.buffer(TOTAL_LEN_SIZE + length + this.endTag.length);
		
		if (lengthIncludeLengthFieldLength) {
			length += TOTAL_LEN_SIZE;
		}
		if (lengthIncludeEndTagLength) {
			length += this.endTag.length;
		}
		
		data.writeInt(length);
		data.writeBytes(msg.array(), 0, length);
		data.writeBytes(this.endTag, 0, this.endTag.length);
		
		out.add(data);
	}

}
