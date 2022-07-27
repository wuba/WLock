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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

public class WlockPrepender extends OneToOneEncoder {

	private static final int TOTAL_LENGTH_SIZE = 4;

	private final ChannelBuffer ENG_TAG;
	private final int ENDTAG_LENGTH;
	private boolean lengthIncludeLengthFieldLength;
	private boolean lengthIncludeEndTagLength;

	public WlockPrepender(byte[] endTag) {
		this(endTag, false, false);
	}


	public WlockPrepender(byte[] endTag, boolean lengthIncludeLengthFieldLength, boolean lengthIncludeEndTagLength) {
		super();
		this.ENG_TAG = ChannelBuffers.copiedBuffer(endTag);
		this.lengthIncludeEndTagLength = lengthIncludeEndTagLength;
		this.lengthIncludeLengthFieldLength = lengthIncludeLengthFieldLength;
		this.ENDTAG_LENGTH = endTag.length;
	}

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (!(msg instanceof ChannelBuffer)) {
			return msg;
		}
		ChannelBuffer body = (ChannelBuffer) msg;
		ChannelBuffer header = ChannelBuffers.buffer(TOTAL_LENGTH_SIZE);
		int length = body.readableBytes();
		if (lengthIncludeLengthFieldLength) {
			length += TOTAL_LENGTH_SIZE;
		}
		if (lengthIncludeEndTagLength) {
			length += ENDTAG_LENGTH;
		}
		header.writeInt(length);
		return wrappedBuffer(header, body, ENG_TAG);
	}
}
