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
package com.wuba.wlock.client.communication;

import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.exception.ConnectTimeoutException;
import com.wuba.wlock.client.exception.ProtocolException;
import com.wuba.wlock.client.exception.SerializeException;
import com.wuba.wlock.client.helper.ByteConverter;
import com.wuba.wlock.client.protocol.ProtocolConst;
import com.wuba.wlock.client.protocol.ResponseStatus;
import com.wuba.wlock.client.protocol.WLockRequest;
import com.wuba.wlock.client.protocol.WLockResponse;
import com.wuba.wlock.client.protocol.extend.CommonWlockResponse;
import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;
import com.wuba.wlock.client.registryclient.entity.NodeAddr;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKey;
import com.wuba.wlock.client.service.HeartbeatService;
import com.wuba.wlock.client.util.TimeUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ServerPoolHandler {
	private static final Log logger = LogFactory.getLog(ServerPoolHandler.class);
	
	private final WLockClient wlockClient;
	
	private final RegistryKey registryKey;
	
	private static Object lock = new Object();

	private Map<Integer, ServerPool> groupServerPoolMap = new ConcurrentHashMap<Integer, ServerPool>();
	
	private static ConcurrentHashMap<Long, ServerPoolHandler> map = new ConcurrentHashMap<Long, ServerPoolHandler>();
	
	private ServerPoolHandler(WLockClient wlockClient) throws IOException, ExecutionException, InterruptedException {
		this.wlockClient = wlockClient;
		this.registryKey = wlockClient.getRegistryKey();
		initSeverPool();
		HeartbeatService.getHbService().start();
	}
	
	public void initSeverPool() throws IOException, ExecutionException, InterruptedException {
		ClientKeyEntity clusterConf = this.registryKey.getClusterConf();
		updateClusters(clusterConf.groupNodeAddrList());
	}
	
	public static ServerPoolHandler getInstance(WLockClient wlockClient) throws IOException, ExecutionException, InterruptedException {
		ServerPoolHandler handler = map.get(wlockClient.getUniqueCode());
		if (handler == null) {
			synchronized (lock) {
				handler = map.get(wlockClient.getUniqueCode());
				if (handler == null) {
					handler = new ServerPoolHandler(wlockClient);
					map.put(wlockClient.getUniqueCode(), handler);
				}
			}
		}

		return handler;
	}
	
	public SendReqResult syncSendRequest(WLockRequest wlockReq, int timeout, String type) {
		long startTimestamp = System.currentTimeMillis();
		SendReqResult sendReqResult = null;
		
		int retries = 0;
		while (retries < this.wlockClient.getDefaultRetries()) {
			try {
				if (retries > 0) {
					logger.info(Version.INFO + ", send request retry " + retries + ", type : " + type);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e1) {
						logger.error("", e1);
					}
					
					if (retries > 1 && TimeUtil.getCurrentMills() - startTimestamp >= timeout) {
						logger.error(Version.INFO + ", syncSendRequest timeout, timeout : " + timeout);
						break;
					}
				}
				
				sendReqResult = syncSend(wlockReq, timeout);
				if (!sendReqResult.getResult()) {
					logger.error(Version.INFO + ", syncSendRequest failed.");
					retries++;
					continue;
				}
				
				return sendReqResult;
			} catch (Exception e) {
				logger.error(Version.INFO + ", syncSendRequest error.", e);
				retries++;
			}
		}
		
		return sendReqResult;
	}
	
	public SendReqResult syncSend(WLockRequest wlockReq, int timeout) throws IOException, ConnectTimeoutException, SerializeException, CommunicationException, TimeoutException, ProtocolException {
		SendReqResult sendReqResult = null;
		ServerPool serverpool = groupServerPoolMap.get(wlockReq.getGroupId());

		long startTimestamp = System.currentTimeMillis();
		Server server = serverpool.getTargetServer();
		if (server == null || !server.getState().equals(ServerState.Normal)) {
			logger.error(Version.INFO + " no avaliable server, registry key : " + this.wlockClient.getRegistryKey().getRegistryKey());
			return new SendReqResult(null, null, false);
		}

		byte[] data = server.syncInvoke(wlockReq, timeout);
		if (data == null) {
			return new SendReqResult(null, null, false);
		}
		
		short status = ByteConverter.bytesToShortLittleEndian(data, WLockResponse.STATUS_OFFSET);
				
		while(status == ResponseStatus.MASTER_REDIRECT) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				logger.error("", e);
			}
			
			WLockResponse wlockResp = new CommonWlockResponse();
			wlockResp.fromBytes(data);
			
			String masterAddr = wlockResp.getRedirectMaster();
			logger.info("redirect master is  : " + masterAddr);
			Server redirectMaster = parseRedirectMaster(masterAddr, serverpool);
			serverpool.setMaster(redirectMaster);
		
			wlockReq.setRedirectTimes((short) (wlockResp.getRedirectTimes() + 1));
			
			if (redirectMaster != null && redirectMaster.getState().equals(ServerState.Normal)) {
				logger.info(Version.INFO + ", redirect master : " + redirectMaster + ", lockkey: " + wlockReq.getLockKey());
				data = redirectMaster.syncInvoke(wlockReq, timeout);
			} else {
				server = serverpool.getCandidateServer();
				data = server.syncInvoke(wlockReq, timeout);
			}
			
			status = ByteConverter.bytesToShortLittleEndian(data, WLockResponse.STATUS_OFFSET);
			
			long currentStamp = System.currentTimeMillis();
			if (currentStamp - startTimestamp > timeout) {
				throw new TimeoutException(Version.INFO + ", syncSendRequest timeout, server : " + server + ", timeout : " + timeout + ", cost : " + (currentStamp - startTimestamp));
			}
		}
		
		if (data != null) {
			sendReqResult= new SendReqResult(server, data, true);
		}
		
		return sendReqResult;
	}
	
	public Server parseRedirectMaster(String masterAddr, ServerPool serverpool) {
		Server master = null;
		try {
			if (masterAddr != null) {
				String addrstr[] = masterAddr.split(ProtocolConst.IP_PORT_SEPARATOR);
				if (addrstr != null) {
					String ip = addrstr[0];
					for (Server server : serverpool.getServList()) {
						ServerConfig servConfig = server.getServConfig();
						if (servConfig.getIp().equals(ip)) {
							master = server;
							break;
						}
					}
				}
			}
		} catch(Exception e) {
			logger.error("parse redirect master failed.", e);
		}
		return master;
	}
	
//	public void addServer(NodeAddr node) throws IOException {
//		this.serverpool.addServer(node);
//	}
//
//	public void deleteServer(NodeAddr node) {
//		this.serverpool.deleteServer(node);
//	}
//
//	public void masterChangeCallback(NodeAddr oldMaster, NodeAddr newMaster) {
//		this.serverpool.masterChangeCallback(oldMaster, newMaster);
//	}
//
//	public ServerPool getServerpool() {
//		return serverpool;
//	}
//
//	public void setServerpool(ServerPool serverpool) {
//		this.serverpool = serverpool;
//	}
//
	public List<Server> getAllServer() {
		List<Server> allServer = new ArrayList<Server>();
		for (ServerPool serverPool: this.groupServerPoolMap.values()) {
			allServer.addAll(serverPool.getServList());
		}
		return allServer;
	}

	public synchronized void updateClusters(Map<Integer, List<NodeAddr>> groupNodeListMap) throws IOException, ExecutionException, InterruptedException {
		Map<Integer, ServerPool> groupServerPoolMap = this.groupServerPoolMap;

		for (Map.Entry<Integer, List<NodeAddr>> entry: groupNodeListMap.entrySet()) {
			Integer groupId = entry.getKey();
			List<NodeAddr> nodeAddrList = entry.getValue();

			ServerPool serverPool = groupServerPoolMap.get(groupId);
			if (serverPool == null) {
				serverPool = new ServerPool(wlockClient, nodeAddrList, groupId);
				groupServerPoolMap.put(groupId, serverPool);
			} else {
				serverPool.setServer(nodeAddrList);
			}
		}

		registryKey.notifyGroupChange(new ArrayList<Integer>(groupNodeListMap.keySet()));

		for (Map.Entry<Integer, ServerPool> entry: groupServerPoolMap.entrySet()) {
			Integer groupId = entry.getKey();
			ServerPool serverPool = entry.getValue();

			if (!groupNodeListMap.containsKey(groupId)) {
				serverPool.destroy();
				groupServerPoolMap.remove(groupId);
			}
		}
	}
}