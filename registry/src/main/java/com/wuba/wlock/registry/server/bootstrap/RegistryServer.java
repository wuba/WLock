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
package com.wuba.wlock.registry.server.bootstrap;

import com.wuba.wlock.registry.server.communication.IServer;
import com.wuba.wlock.registry.server.communication.tcp.TcpServer;
import com.wuba.wlock.registry.server.communication.tcp.TcpUpstreamHandler;
import com.wuba.wlock.registry.server.config.Configuration;
import com.wuba.wlock.registry.server.config.TCPServerConfig;
import com.wuba.wlock.registry.util.ThreadRenameFactory;
import com.wuba.wlock.registry.server.worker.RedisClusterConfSubscribeWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RegistryServer {
	ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1,0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(), new ThreadRenameFactory("redisSubscribe-Thread"));

	private final List<IServer> serverList = new ArrayList<IServer>();

	@Value("${tcp.workerCount}")
	int workerCount;
	@Value("${tcp.keepAlive}")
	boolean keepAlive;
	@Value("${tcp.nagle}")
	boolean nagle;
	@Value("${tcp.maxPackageSize}")
	int maxPackageSize;
	@Value("${tcp.recvBufferSize}")
	int recvBufferSize;
	@Value("${tcp.sendBufferSize}")
	int sendBufferSize;
	@Value("${tcp.pollWaitTime}")
	int pollWaitTime;
	@Value("${tcp.local}")
	String local;
	@Value("${tcp.port}")
	int port;

	@Autowired
	RedisClusterConfSubscribeWorker redisClusterConfSubscribeWorker;
	@Autowired
    TcpUpstreamHandler tcpUpstreamHandler;

	@PostConstruct
	public void init() throws Exception{
		startTcpServer();

		startRedisClusterConfScriber();

		registerExcetEven();
	}


	private void startRedisClusterConfScriber() {
		threadPool.submit(redisClusterConfSubscribeWorker);
	}

	private void startTcpServer() throws Exception {
		Configuration tcpConfig = new TCPServerConfig();
		tcpConfig.setOption("workerCount", workerCount);
		tcpConfig.setOption("keepAlive", keepAlive);
		tcpConfig.setOption("nagle", nagle);
		tcpConfig.setOption("maxPakageSize", maxPackageSize);
		tcpConfig.setOption("recvBufferSize", recvBufferSize);
		tcpConfig.setOption("sendBufferSize", sendBufferSize);
		tcpConfig.setOption("pollWaitTime", pollWaitTime);
		tcpConfig.setOption("local", local);
		tcpConfig.setOption("port", port);
		IServer server = new TcpServer(tcpConfig, tcpUpstreamHandler);
		server.start();
		serverList.add(server);
	}


	private void registerExcetEven() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					// 关闭服务
					for (IServer server : serverList) {
						server.stop();
					}
					super.finalize();
				} catch (Throwable e) {
					log.error("super.finalize() error when stop server", e);
				}
			}
		});
	}
}
