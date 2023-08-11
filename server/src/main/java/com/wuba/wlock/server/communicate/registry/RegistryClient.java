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

import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.server.bootstrap.base.IServer;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.config.RegistryConfig;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.constant.PaxosState;
import com.wuba.wlock.server.constant.ServerState;
import com.wuba.wlock.server.exception.RegistryClientRuntimeException;
import com.wuba.wlock.server.util.HostUtil;
import com.wuba.wlock.server.exception.ConfigException;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import com.wuba.wlock.server.communicate.registry.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


public final class RegistryClient implements IServer {
	private static final Logger logger = LoggerFactory.getLogger(RegistryClient.class);
	private static RegisterChannel registerChannel;
	private static ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("registry-schedule-worker"));
	private static ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("mastercallback_upload_paxos_config"));
	private static ScheduledExecutorService heartBeatExecute = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("registry_heartbeat"));
	private static ExecutorService uploadMigrateStateExecutorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("uploadMigrateState"));

	private RegistryClient() {
	}

	private static RegistryClient registryClient = new RegistryClient();

	public static RegistryClient getInstance() {
		return registryClient;
	}

	private void init() throws ConfigException, RegistryClientRuntimeException {
		/**
		 * 随机选择一个ip
		 */
		createChannel();
		scheduledExecutorService.scheduleWithFixedDelay(this::getPaxosConfig, 60000, 5000, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			if (PaxosState.isStarted()) {
				uploadConfig();
			}
		}, 60000, 5000, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(this::getKeyQps, 10, 60, TimeUnit.SECONDS);
		heartBeatExecute.scheduleAtFixedRate(this::heartBeatTimer, 10, 3, TimeUnit.SECONDS);

		scheduledExecutorService.scheduleAtFixedRate(() -> {
			if (PaxosState.isStarted()) {
				getGroupMigrateConfig();
			}
		}, 60000, 30000, TimeUnit.MILLISECONDS);
	}

	private void createChannel() throws ConfigException, RegistryClientRuntimeException {
		List<String> ips = HostUtil.getInstance().getAllServerIP(RegistryConfig.getInstance().getHost());
		int index = new Random().nextInt(ips.size());
		int times = 0;
		while (times++ < ips.size()) {
			final String ip = ips.get(index);
			try {
				registerChannel = new RegisterChannel(ip, RegistryConfig.getInstance().getPort());
				if (registerChannel.getChannel() == null) {
					logger.error("init registry error ip :{}", ip);
					index++;
				} else {
					return;
				}
			} catch (Exception e) {
				logger.error("init registry error ip :{}", ip);
				index++;
			}
		}
		throw new RegistryClientRuntimeException("no registry alive.");
	}

	private void getKeyQps() {
		try {
			if (!PaxosState.isStarted()) {
				return;
			}
			if (registerChannel == null || registerChannel.getChannel() == null || !registerChannel.getChannel().isOpen()) {
				throw new RegistryClientRuntimeException("registry channel not alive");
			}
			IPaxosHandler paxosHandler = new GetKeyQpsHandler();
			RegistryProtocol registryProtocol = registerChannel.syncSend(paxosHandler.buildMessage(), PaxosConfig.REGISTRY_TIMEOUT);
			paxosHandler.handleResponse(registryProtocol);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * 上传master分布
	 * 失败之后缩短间隔时间
	 */
	private void uploadConfig() {
		try {
			if (!PaxosState.isStarted()) {
				return;
			}
			if (registerChannel == null || registerChannel.getChannel() == null || !registerChannel.getChannel().isOpen()) {
				throw new RegistryClientRuntimeException("registry channel not alive");
			}
			IPaxosHandler paxosHandler = new UploadConfigHandler();
			RegistryProtocol registryProtocol = registerChannel.syncSend(paxosHandler.buildMessage(), PaxosConfig.REGISTRY_TIMEOUT);
			paxosHandler.handleResponse(registryProtocol);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public void uploadMigrateState(long version, int groupId, int state) {
		try {
			if (registerChannel == null || registerChannel.getChannel() == null || !registerChannel.getChannel().isOpen()) {
				throw new RegistryClientRuntimeException("registry channel not alive");
			}

			IPaxosHandler paxosHandler = new UploadGroupMigrateStateHandler(version, groupId, state);
			uploadMigrateStateExecutorService.execute(new Runnable() {
				@Override
				public void run() {
					int retry = 3;
					while (retry-- > 0) {
						try {
							logger.error("RegistryClient.uploadMigrateState version: {}, groupId: {}, state: {}", version, groupId, state);
							RegistryProtocol registryProtocol = registerChannel.syncSend(paxosHandler.buildMessage(), PaxosConfig.REGISTRY_TIMEOUT);
							paxosHandler.handleResponse(registryProtocol);
							return;
						} catch (Exception e) {
							logger.error("RegistryClient.uploadMigrateState error.", e);
						}
					}

					logger.error("RegistryClient.uploadMigrateState retry fail.");
				}
			});

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 异步发送一次group master分布
	 */
	public void asyncUploadConfig() {
		if (ServerState.isRebooting()) {
			return;
		}
		executorService.execute(this::uploadConfig);
	}

	public boolean initDynamicConfig() {
		return getPaxosConfig();
	}

	private boolean getPaxosConfig() {
		try {
			if (registerChannel == null || registerChannel.getChannel() == null || !registerChannel.getChannel().isOpen()) {
				throw new RegistryClientRuntimeException("registry channel not alive");
			}
			IPaxosHandler paxosHandler = new GetPaxosConfigHandler();
			RegistryProtocol registryProtocol = paxosHandler.buildMessage();
			RegistryProtocol res = registerChannel.syncSend(registryProtocol, PaxosConfig.REGISTRY_TIMEOUT);
			return paxosHandler.handleResponse(res);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	private boolean getGroupMigrateConfig() {
		try {
			if (registerChannel == null || registerChannel.getChannel() == null || !registerChannel.getChannel().isOpen()) {
				throw new RegistryClientRuntimeException("registry channel not alive");
			}
			IPaxosHandler paxosHandler = new GetGroupMigrateConfigHandler();
			RegistryProtocol registryProtocol = paxosHandler.buildMessage();
			RegistryProtocol res = registerChannel.syncSend(registryProtocol, PaxosConfig.REGISTRY_TIMEOUT);
			return paxosHandler.handleResponse(res);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public void start() {
		try {
			init();
		} catch (Exception e) {
			logger.error("init registry client error", e);
			System.exit(0);
		}
	}


	@Override
	public void stop() {
		logger.info("close registry channel");
		scheduledExecutorService.shutdown();
		try {
			if (!scheduledExecutorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				scheduledExecutorService.shutdownNow();
			}
		} catch (InterruptedException e) {
		} finally {
			scheduledExecutorService.shutdownNow();
		}
		heartBeatExecute.shutdown();
		try {
			if (!heartBeatExecute.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				heartBeatExecute.shutdownNow();
			}
		} catch (InterruptedException e) {
		} finally {
			heartBeatExecute.shutdownNow();
		}
		registerChannel.close();
	}

	public void notify(long sid, byte[] reciveByte) {
		registerChannel.notify(sid, reciveByte);
	}

	private void heartBeatTimer() {
		try {
			List<String> allServerIP = HostUtil.getInstance().getAllServerIP(RegistryConfig.getInstance().getHost());
			int index = Math.abs(ServerConfig.getInstance().getCluster().hashCode() % allServerIP.size());
			String ip = allServerIP.get(index);
			logger.info("registry heartbeat check target ip {}", ip);
			int times = 0;
			if (registerChannel == null || registerChannel.getChannel() == null
					|| registerChannel.getErrorTimes() > 0 || !registerChannel.getChannel().isOpen()) {
				while (times++ < allServerIP.size()) {
					String targetIp = allServerIP.get((index + times) % allServerIP.size());
					RegisterChannel newChannel = null;
					try {
						newChannel = new RegisterChannel(targetIp, RegistryConfig.getInstance().getPort());
						logger.info("registry channel changed,{}", newChannel.toString());
						IPaxosHandler paxosHandler = new GetPaxosConfigHandler();
						RegistryProtocol registryProtocol = paxosHandler.buildMessage();
						RegistryProtocol res = newChannel.syncSend(registryProtocol, PaxosConfig.REGISTRY_TIMEOUT);
						if (res != null && MessageType.ERROR != res.getMsgType()) {
							if (registerChannel != null) {
								logger.info("create new channel with registry {}", registerChannel.toString());
								registerChannel.close();
							}
							registerChannel = newChannel;
							break;
						} else {
							logger.info("close registry channel {}", newChannel);
							newChannel.close();
						}
					} catch (Exception e) {
						logger.error("registry heartbeat check error ,check next", e);
						if (newChannel != null) {
							newChannel.close();
						}
					}

				}
			}
			if (registerChannel == null) {
				logger.error("registry error is null");
				return;
			}
			RegisterChannel newChannel = null;
			try {
				if (!ip.equalsIgnoreCase(registerChannel.getIp())) {
					newChannel = new RegisterChannel(ip, RegistryConfig.getInstance().getPort());
					logger.info("check target registry {}", ip);
					IPaxosHandler paxosHandler = new GetPaxosConfigHandler();
					RegistryProtocol registryProtocol = paxosHandler.buildMessage();
					RegistryProtocol res = newChannel.syncSend(registryProtocol, PaxosConfig.REGISTRY_TIMEOUT);
					if (res != null && MessageType.ERROR != res.getMsgType()) {
						logger.info("create new channel with registry {}", newChannel.toString());
						registerChannel.close();
						registerChannel = newChannel;
					} else {
						logger.info("close registry channel {}", newChannel);
						newChannel.close();
					}
				}
			} catch (Exception e) {
				if (newChannel != null) {
					newChannel.close();
				}
				logger.error("target registry not OK.return", e);
			}
		} catch (Exception e) {
			logger.error("registry heartbeat check error", e);
		}
	}
}
