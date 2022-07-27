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
package com.wuba.wlock.server.worker;

import com.wuba.wlock.server.communicate.constant.AckContext;
import com.wuba.wlock.server.util.SystemUtils;
import com.wuba.wlock.server.util.ThreadRenameFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AckWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(AckWorker.class);

	private AckWorker() {
	}

	private static AckWorker ackWorker = new AckWorker();

	public static AckWorker getInstance() {
		return ackWorker;
	}

	private static final int P_THD_COUNT = SystemUtils.getHalfCpuProcessorCount();
	private static final ThreadPoolExecutor EXECUTORS = new ThreadPoolExecutor(P_THD_COUNT, P_THD_COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadRenameFactory("ack-Thread-pool"));


	public void offer(AckContext ackContext) {
		final Channel channel = ackContext.getChannel();
		final byte[] buf = ackContext.getBuf();
		EXECUTORS.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (!channel.isOpen() || !channel.isWritable()) {
						LOGGER.warn("channel isOpen {}, isWritable {}, can not be used, so ignore ack {}.", channel.isOpen(), channel.isWritable(), channel.getLocalAddress().toString());
						return;
					}
					ChannelFuture future = channel.write(ChannelBuffers.copiedBuffer(buf));
					future.addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (!future.isSuccess()) {
								LOGGER.error("channel {} ack failed", channel.getLocalAddress().toString());
							} else {
								LOGGER.debug("channel {} ack success", channel.getLocalAddress().toString());
							}
						}
					});
				} catch(Throwable e) {
					LOGGER.error("execute ack task throws exception", e);
				}
			}
		});
	}

	public void shutdown() {
		EXECUTORS.shutdown();
		try {
			if (EXECUTORS.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				EXECUTORS.shutdownNow();
			}
		} catch(InterruptedException e) {
		} finally {
			EXECUTORS.shutdownNow();
		}
		LOGGER.info("ack worker shutdown");
	}

}

