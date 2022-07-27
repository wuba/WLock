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
import org.jboss.netty.handler.codec.frame.CorruptedFrameException;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class WlockFrameDecoder extends FrameDecoder {

	private static final int TOTAL_LENGTH_SIZE = 4;
	private final int maxFrameLength;
	private final int ENDTAG_LENGTH;
	private boolean lengthIncludeLengthFieldLength;
	private boolean lengthIncludeEndTagLength;
	private boolean discardingTooLongFrame;
	private final ChannelBuffer END_TAG;


	public WlockFrameDecoder(byte[] endTag, int maxFrameLength, boolean lengthIncludeLengthFieldLength, boolean lengthIncludeEndTagLength) {
		super();
		this.maxFrameLength = maxFrameLength;
		this.END_TAG = ChannelBuffers.copiedBuffer(endTag);
		this.ENDTAG_LENGTH = endTag.length;
		this.lengthIncludeLengthFieldLength = lengthIncludeLengthFieldLength;
		this.lengthIncludeEndTagLength = lengthIncludeEndTagLength;
	}

	public WlockFrameDecoder(byte[] endTag, int maxFrameLength) {
		this(endTag, maxFrameLength, false, false);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {

		if (discardingTooLongFrame) {
			skipToFirstEndTag(buffer);
		}

		if (buffer.readableBytes() < TOTAL_LENGTH_SIZE) {
			return null;
		}
		int actualLengthFieldOffset = buffer.readerIndex();
		long frameLength = buffer.getUnsignedInt(actualLengthFieldOffset);

		if (frameLength < 0) {
			skipToFirstEndTag(buffer);
			throw new CorruptedFrameException(
					"negative pre-adjustment length field: " + frameLength);
		}

		if (lengthIncludeLengthFieldLength) {
			frameLength -= TOTAL_LENGTH_SIZE;
		}
		if (lengthIncludeEndTagLength) {
			frameLength -= ENDTAG_LENGTH;
		}
		if (frameLength < 0) {
			skipToFirstEndTag(buffer);
			throw new CorruptedFrameException(
					"Adjusted frame length (" + frameLength + ") is less " +
							"than 0");
		}

		if (frameLength > maxFrameLength) {
			long discard = frameLength - buffer.readableBytes();
			skipToFirstEndTag(buffer);
			return null;
		}

		// never overflows because it's less than maxFrameLength
		int frameLengthInt = (int) frameLength;
		if (buffer.readableBytes() < frameLengthInt + TOTAL_LENGTH_SIZE + ENDTAG_LENGTH) {
			return null;
		}

		int readerIndex = buffer.readerIndex();
		ChannelBuffer endTag = extractFrame(buffer, readerIndex + frameLengthInt + TOTAL_LENGTH_SIZE, ENDTAG_LENGTH);
		if (!endTag.toByteBuffer().equals(END_TAG.toByteBuffer())) {
			skipToFirstEndTag(buffer);
			throw new CorruptedFrameException(
					"Adjusted frame not endwith endTag ,skip to first end tag. ");
		}
		buffer.skipBytes(TOTAL_LENGTH_SIZE);
		ChannelBuffer frame = extractFrame(buffer, buffer.readerIndex(), frameLengthInt);
		buffer.readerIndex(buffer.readerIndex() + frameLengthInt);
		buffer.skipBytes(ENDTAG_LENGTH);
		return frame;

	}

	private void skipToFirstEndTag(ChannelBuffer buffer) {
		int index = indexOf(buffer, END_TAG);
		if (index < 0) {
			buffer.skipBytes(buffer.readableBytes() - ENDTAG_LENGTH);
			discardingTooLongFrame = true;
		} else {
			buffer.skipBytes(index + ENDTAG_LENGTH);
			discardingTooLongFrame = false;
		}
	}

	private static int indexOf(ChannelBuffer haystack, ChannelBuffer needle) {
		for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
			int haystackIndex = i;
			int needleIndex;
			for (needleIndex = 0; needleIndex < needle.capacity(); needleIndex++) {
				if (haystack.getByte(haystackIndex) != needle.getByte(needleIndex)) {
					break;
				} else {
					haystackIndex++;
					if (haystackIndex == haystack.writerIndex() &&
							needleIndex != needle.capacity() - 1) {
						return -1;
					}
				}
			}

			if (needleIndex == needle.capacity()) {
				// Found the needle from the haystack!
				return i - haystack.readerIndex();
			}
		}
		return -1;
	}

}
