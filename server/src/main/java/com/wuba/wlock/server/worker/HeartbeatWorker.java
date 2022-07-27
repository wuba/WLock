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

import com.wuba.wlock.server.communicate.IProtocolFactory;
import com.wuba.wlock.server.communicate.ResponseStatus;
import com.wuba.wlock.server.communicate.TcpServer;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.HeartbeatRequest;
import com.wuba.wlock.server.communicate.protocol.HeartbeatResponse;
import com.wuba.wlock.server.communicate.protocol.ProtocolFactoryImpl;
import com.wuba.wlock.server.util.ConnManager;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.*;

public class HeartbeatWorker {
	private static final Logger logger = LoggerFactory.getLogger(HeartbeatWorker.class);
	private static final int COUNT = 4;
	private static final int MAX_QUEUE_SIZE = 4096;
	private static final ConnManager CONN_MANAGER = ConnManager.getInstance();
	private static HeartbeatWorker instance = new HeartbeatWorker();
	private static final ExecutorService executorService = new ThreadPoolExecutor(COUNT, COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(MAX_QUEUE_SIZE), new ThreadFactoryImpl("heart_beat_worker"), new ThreadPoolExecutor.DiscardPolicy());
	private static final ScheduledExecutorService scheduledExecutorService = Executors
			.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("heart_beat_check_worker"));
	private static IProtocolFactory protocolFactory = ProtocolFactoryImpl.getInstance();

	public void offer(LockContext context) {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (context != null) {
						HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
						heartbeatRequest.fromBytes(context.getBuf());
						HeartbeatResponse heartbeatResponse = protocolFactory.createHeartBeatRes(heartbeatRequest, ResponseStatus.SUCCESS);
						ChannelFuture future = context.getChannel().write(ChannelBuffers.copiedBuffer(heartbeatResponse.toBytes()));
						future.addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								if (!future.isSuccess()) {
									logger.error("send heartbeat detect response failed.");
								}
							}
						});
						// 对接收心跳的处理逻辑
						CONN_MANAGER.updateHeartBeat(context.getChannel(), TimeUtil.getCurrentTimestamp());
					}
				} catch(Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
	}

	public static HeartbeatWorker getInstance() {
		return instance;
	}

	private HeartbeatWorker() {
	}

	public void shutdown() {
		scheduledExecutorService.shutdown();
		try {
			if (!scheduledExecutorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				scheduledExecutorService.shutdownNow();
			}
		} catch(InterruptedException e) {
		} finally {
			scheduledExecutorService.shutdownNow();
		}
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				executorService.shutdownNow();
			}
		} catch(InterruptedException e) {
		} finally {
			executorService.shutdownNow();
		}
		logger.info("heartbeat worker shutdown");
	}

	public void start() {
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					heartbeatCheck();
				} catch(Throwable th) {
					logger.error(th.getMessage(), th);
				}
			}

		}, 10, 10, TimeUnit.SECONDS);
	}

	public void heartbeatCheck() {
		try {
			Iterator<Channel> iter = TcpServer.ALL_CHANNELS.iterator();
			while (iter.hasNext()) {
				Channel channel = iter.next();
				if (!CONN_MANAGER.isAlive(channel)) {
					channel.close();
				}
			}
		} catch(Exception e) {
			logger.error("heartbeat check error.", e);
		}
	}

}
