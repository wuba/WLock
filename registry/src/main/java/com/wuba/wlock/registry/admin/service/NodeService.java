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
package com.wuba.wlock.registry.admin.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.entity.PushMessage;
import com.wuba.wlock.common.enums.MigrateProcessEndState;
import com.wuba.wlock.registry.admin.constant.ExceptionConstant;
import com.wuba.wlock.registry.admin.domain.request.ListInfoReq;
import com.wuba.wlock.registry.admin.domain.request.ServerInfoReq;
import com.wuba.wlock.registry.admin.domain.response.ServerOnlineOfflineResp;
import com.wuba.wlock.registry.admin.domain.response.ServerResp;
import com.wuba.wlock.registry.admin.enums.ServerQueryType;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.registry.util.RedisUtil;
import com.wuba.wlock.repository.domain.MigrateProcessDO;
import com.wuba.wlock.repository.domain.ServerDO;
import com.wuba.wlock.repository.enums.ServerState;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.repository.ClusterRepository;
import com.wuba.wlock.repository.repository.MigrateProcessRepository;
import com.wuba.wlock.repository.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@Slf4j
public class NodeService {

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	ServerRepository serverRepository;
	@Autowired
	ClusterRepository clusterRepository;
	@Autowired
	MigrateProcessRepository migrateProcessRepository;

	private static final String IP_LIST = "ipList";

	private static final String SQL_LIKE_SEPARATOR = "%";

	private static final String COLON_SEPARATOR = ":";



	public Page<ServerResp> getServerList(String env, ListInfoReq serverListInfoReq, ServerQueryType type) throws ServiceException {
		String condition = this.getSearchCondition(serverListInfoReq, type);
		Page<ServerDO> serverPage = null;
		List<ServerResp> serverRespList = new ArrayList<ServerResp>();
		try {
			serverPage = serverRepository.getServerByCondition(env, condition, serverListInfoReq.getPageNumber(), serverListInfoReq.getPageSize());
		} catch (Exception e) {
			log.info("getServerList error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		List<ServerDO> serverDOList = serverPage.getPageList();
		if (null == serverDOList || serverDOList.size() < 1) {
			return new Page<ServerResp>(serverPage.getPageInfo(), serverRespList);
		}
		ServerResp serverResp = null;
		for (ServerDO serverDO : serverDOList) {
			serverResp = new ServerResp();
			serverResp.setId(String.valueOf(serverDO.getId()));
			serverResp.setServer(serverDO.getServerAddr());
			serverResp.setTelnetPort(serverDO.getTelnetPort());
			serverResp.setPaxosPort(serverDO.getPaxosPort());
			serverResp.setUdpPort(serverDO.getUdpPort());
			serverResp.setClusterName(serverDO.getClusterId());
			serverResp.setSequenceId(serverDO.getSequenceId());
			String stateStr = (serverDO.getState() == ServerState.online.getValue()) ? "上线" : "下线";
			serverResp.setState(stateStr);
			serverRespList.add(serverResp);
		}
		return new Page<ServerResp>(serverPage.getPageInfo(), serverRespList);
	}

	private String getSearchCondition(ListInfoReq serverListInfoReq, ServerQueryType type) {
		String clusterName = serverListInfoReq.getClusterName();
		String ip = serverListInfoReq.getIp();
		Map<String, String> map = new HashMap<String, String>();
		StringBuilder condition = new StringBuilder("");
		if (null != clusterName && !clusterName.isEmpty()) {
			map.put("cluster_id", clusterName);
		}
		if (null != ip && !ip.isEmpty()) {
			map.put("server", ip);
		}
		if (map.size() < 1) {
			return condition.toString();
		} else {
			condition.append(" where ");
			int beginLength = condition.length();
			for (Entry<String, String> entry : map.entrySet()) {
				if (condition.length() != beginLength) {
					condition.append(" and ");
				}
				if ("cluster_id".equals(entry.getKey())) {
					if (type.getValue().equals(ServerQueryType.QUERY.getValue())) {
						condition.append("cluster_id like ").append("\'").append(SQL_LIKE_SEPARATOR).append(entry.getValue().toString()).append(SQL_LIKE_SEPARATOR).append("\'");
					} else {
						condition.append("cluster_id = ").append("\'").append(entry.getValue().toString()).append("\'");
					}
				} else if ("server".equals(entry.getKey())) {
					condition.append("server like ").append("\'").append(SQL_LIKE_SEPARATOR).append(entry.getValue().toString()).append(SQL_LIKE_SEPARATOR).append("\'");
				} else {
					condition.append(entry.getKey()).append("=").append("\'").append(entry.getValue().toString()).append("\'");
				}
			}
		}
		return condition.toString();
	}

	public void deleteServer(String env, long id) throws ServiceException {
		ServerDO serverDO = null;
		try {
			serverDO = serverRepository.getServerById(env, id);
		} catch (Exception e) {
			log.info("deleteServer error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (null == serverDO) {
			return;
		} else {
			if (serverDO.getState() == ServerState.online.getValue()) {
				throw new ServiceException(ExceptionConstant.ONLINE_DELETE_ERROR);
			} else if (serverDO.getState() == ServerState.offline.getValue()) {
				try {
					serverRepository.deleteServerById(env, id);
				} catch (Exception e) {
					log.info("deleteServer error", e);
					throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
				}
			} else {
				log.info("deleteServer server {} error, state is {}", serverDO.getServerAddr(), serverDO.getState());
			}
		}
	}

	public boolean checkServerExist(String env, String ip,int port) throws Exception {
		String server = ip + COLON_SEPARATOR + port;
		ServerDO serverDO = serverRepository.getByServer(env, server);
		return serverDO != null;
	}

	public void addServer(String env, ServerInfoReq serverInfoReq) throws ServiceException {
		String server = serverInfoReq.getIp() + COLON_SEPARATOR + serverInfoReq.getTcpPort();
		ServerDO serverDO = null;
		boolean hasSameSequenceIdFlag = false;
		try {
			serverDO = serverRepository.getByServer(env, server);
		} catch (Exception e) {
			log.info("addServer error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (serverDO != null) {
			throw new ServiceException(ExceptionConstant.SERVER_EXISTED);
		}
		try {
			hasSameSequenceIdFlag = serverRepository.isHasSameSequenceId(env, serverInfoReq.getClusterName(), serverInfoReq.getSequenceId());
		} catch (Exception e) {
			log.info("addServer error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (hasSameSequenceIdFlag) {
			throw new ServiceException(ExceptionConstant.SERVER_SEQID_EXIST);
		}
		serverDO = new ServerDO();
		serverDO.setSequenceId(serverInfoReq.getSequenceId());
		serverDO.setServerAddr(server);
		serverDO.setTelnetPort(serverInfoReq.getTelnetPort());
		serverDO.setPaxosPort(serverInfoReq.getPaxosPort());
		serverDO.setClusterId(serverInfoReq.getClusterName());
		serverDO.setUdpPort(serverInfoReq.getUdpPort());
		serverDO.setState(ServerState.offline.getValue());
		try {
			serverRepository.saveServer(env, serverDO);
		} catch (Exception e) {
			log.info("addServer error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}


	public CompletableFuture<List<ServerDO>> getServerList(String env, String clusterName, ServerState serverState) throws ServiceException {
		CompletableFuture<List<ServerDO>> completableFuture = new CompletableFuture<>();
		CompletableFuture.runAsync(() -> {
			try {
				completableFuture.complete(serverRepository.getServerByClusterIdAndState(env, clusterName, serverState.getValue()));
			} catch (Exception e) {
				log.info("getServerList error", e);
				completableFuture.completeExceptionally(e);
			}
		});
		return completableFuture;
	}




	public JSONObject getServerOnlineOfflineList(String env, String clusterName, ServerState serverState) throws ServiceException {
		List<ServerDO> serverList = null;
		List<ServerOnlineOfflineResp> onlineList = new ArrayList<ServerOnlineOfflineResp>();
		try {
			serverList = serverRepository.getServerByClusterIdAndState(env, clusterName, serverState.getValue());
		} catch (Exception e) {
			log.info("getServerOnlineOfflineList error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (null != serverList && serverList.size() > 0) {
			for (int i = 0; i < serverList.size(); i++) {
				ServerDO tempServer = serverList.get(i);
				ServerOnlineOfflineResp serverOnline = new ServerOnlineOfflineResp();
				serverOnline.setServerId(tempServer.getId());
				serverOnline.setIp(tempServer.getServerIp());
				onlineList.add(serverOnline);
			}
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(IP_LIST, onlineList);
		return jsonObject;
	}

	public void onlineServers(String env, String clusterName, String idString) throws ServiceException {
		List<Long> idList = this.getIdList(idString);
		List<ServerDO> serverList = null;
		try {
			serverList = serverRepository.getServerByClusterIdAndState(env, clusterName, ServerState.online.getValue());
		} catch (Exception e) {
			log.info("onlineServers error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}

		migrateStateCheck(env);
		try {
			serverRepository.onlineServerByIds(env, clusterName, idList);
			clusterRepository.updateClusterVersionByClusterName(env, System.currentTimeMillis(), clusterName);
			this.notifyRegistryClusterChange(clusterName, env);
		} catch (Exception e) {
			log.info("onlineServers error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}

	/**
	 * 迁移过程不允许节点上下线
	 * @param env
	 * @throws ServiceException
	 */
	private void migrateStateCheck(String env) throws ServiceException {
		try {
			List<MigrateProcessDO> migrateProcessDos = migrateProcessRepository.searchByCondition(env, -1, "", -1L, MigrateProcessEndState.NoEnd.getValue());
			if (!migrateProcessDos.isEmpty()) {
				throw new ServiceException(ExceptionConstant.MIGRATE_STATE_LIMIT_ONLINE_OFFLINE);
			}
		} catch (Exception e) {
			log.error("migrateStateCheck", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}

	private void notifyRegistryClusterChange(String clusterName, String env) {
		if (redisUtil.isUseRedis()) {
			PushMessage pushMessage = new PushMessage();
			pushMessage.setCluster(clusterName);
			pushMessage.setVersion(System.currentTimeMillis());
			redisUtil.publish(RedisKeyConstant.REDIS_SUBSCRIBE_CHANNEL, JSON.toJSONString(pushMessage));
		}
	}

	public void offlineServers(String env, String clusterName, String idString) throws ServiceException {
		List<Long> idList = this.getIdList(idString);
		if (idList.size() < 1) {
			throw new ServiceException(ExceptionConstant.PARAMS_EXCEPTION);
		}
		List<ServerDO> serverList = null;
		try {
			serverList = serverRepository.getServerByClusterIdAndState(env, clusterName, ServerState.online.getValue());
		} catch (Exception e) {
			log.info("onlineServers error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		migrateStateCheck(env);

		try {
			serverRepository.offlineServerByIds(env, clusterName, idList);
			clusterRepository.updateClusterVersionByClusterName(env, System.currentTimeMillis(), clusterName);
			this.notifyRegistryClusterChange(clusterName, env);
		} catch (Exception e) {
			log.info("onlineServers error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}

	private List<Long> getIdList(String idString) {
		List<Long> idList = new ArrayList<Long>();
		idString = idString.trim();
		int beginIndex = 0;
		int endIndex = idString.length();
		if (idString.contains(CommonConstant.LEFT_BRACKETS)) {
			beginIndex = idString.indexOf(CommonConstant.LEFT_BRACKETS) + 1;
		}
		if (idString.contains(CommonConstant.RIGHT_BRACKETS)) {
			endIndex = idString.indexOf(CommonConstant.RIGHT_BRACKETS);
		}
		String[] idArray = idString.substring(beginIndex, endIndex).trim().split(",");
		if (idArray.length > 0) {
			for (String id : idArray) {
				idList.add(Long.parseLong(id.trim()));
			}
		}
		return idList;
	}
}
