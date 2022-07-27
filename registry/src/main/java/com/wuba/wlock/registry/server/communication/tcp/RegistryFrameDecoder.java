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
import com.wuba.wlock.common.util.ProtocolHelper;
import com.wuba.wlock.registry.constant.CommonConstant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class RegistryFrameDecoder extends ByteToMessageDecoder {

	private static final Logger logger = LogManager.getLogger(RegistryFrameDecoder.class);
	private int maxFrameLength;
	private volatile int index = 0;
	private static int EXTRA_TOTAL_LEN_SIZE = 4;
	private boolean lengthIncludeLengthFieldLength;
	private boolean lengthIncludeEndTagLength;
	
	public RegistryFrameDecoder(int maxFrameLength) {
		this(maxFrameLength, false, false);
	}
	
	public RegistryFrameDecoder(int maxFrameLength, boolean lengthIncludeLengthFieldLength, boolean lengthIncludeEndTagLength) {
		super();
		this.maxFrameLength = maxFrameLength;
		this.lengthIncludeLengthFieldLength = lengthIncludeLengthFieldLength;
		this.lengthIncludeEndTagLength = lengthIncludeEndTagLength;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		while (in.readableBytes() > CommonConstant.FOUR) {
			if (index > 0 && index <= ProtocolConstant.P_END_TAG.length - 1) { //有错误时,需要找到一个完整的尾跳过
				skipErrorMsg(in);
			}
			try {
				in.markReaderIndex();
				int totalLen = in.readInt();
				int msgLen = totalLen;
				
				if (lengthIncludeLengthFieldLength) {
					msgLen -= EXTRA_TOTAL_LEN_SIZE;
				}
				
				int remainingDataMinLen = msgLen;
				
				if (!lengthIncludeEndTagLength) {
					remainingDataMinLen += ProtocolConstant.P_END_TAG.length;
				} else {
					msgLen -= ProtocolConstant.P_END_TAG.length;
				}
				
				if (in.readableBytes() >= remainingDataMinLen) {	
					ByteBuf data = Unpooled.buffer(msgLen);
					in.readBytes(data, msgLen);
					ByteBuf endTag = Unpooled.buffer(ProtocolConstant.P_END_TAG.length);
					in.readBytes(endTag, ProtocolConstant.P_END_TAG.length);
					if (ProtocolHelper.getInstance().checkEndDelimiter(endTag.array())) {
						out.add(data);
						continue;
					} else {
						//跳过错误的数据
						skipErrorMsg(in);
						logger.warn("RegistryFrameDecoder: decode check endDelimter failed, skip this data!");
					}
				} else {
					in.resetReaderIndex();
					break;
				}
			} catch (Exception e) {
				logger.info("RegistryFrameDecoder error:", e);
			}
		}

	}

	private void skipErrorMsg(ByteBuf inBuf) {
		if (index == 0) {
			inBuf.resetReaderIndex();
		}
		while (inBuf.readableBytes() > 0) {
			byte b = inBuf.readByte();
			if (b == ProtocolConstant.P_END_TAG[index]) {
				index++;
				if (index == ProtocolConstant.P_END_TAG.length) {
					index = 0;
					break;
				}
			} else if (index != 0) {
				if (b == ProtocolConstant.P_END_TAG[0]) {
					index = 1;
				} else {
					index = 0;
				}
			}
		}
	}
	
	public boolean isOverMaxFrameLength(int msgLength) {
		if (msgLength > maxFrameLength) {
			return true;
		}
		return false;
	}
	
}
