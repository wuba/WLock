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


import com.wuba.wlock.registry.admin.constant.ExceptionConstant;
import com.wuba.wlock.registry.admin.domain.request.ClusterInfoReq;
import com.wuba.wlock.registry.admin.domain.request.ListInfoReq;
import com.wuba.wlock.registry.admin.domain.response.ClusterResp;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.repository.domain.ClusterDO;
import com.wuba.wlock.repository.domain.ServerDO;
import com.wuba.wlock.repository.enums.ClusterState;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.repository.ClusterRepository;
import com.wuba.wlock.repository.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ClusterService {
	@Autowired
	ClusterRepository clusterRepository;
	@Autowired
	ServerRepository serverRepository;


	public Page<ClusterResp> getClusterList(String env, ListInfoReq clusterListInfoReq) throws ServiceException {
		Page<ClusterDO> clusterDoPage = null;
		List<ClusterResp> clusterRespList = new ArrayList<ClusterResp>();
		String condition = this.getSearchCondition(env, clusterListInfoReq);
		if (clusterListInfoReq.getIp() != null && !clusterListInfoReq.getIp().isEmpty() && condition.isEmpty()) {
			throw new ServiceException(ExceptionConstant.CLUSTER_NOT_EXIST);
		}
		try {
			clusterDoPage = clusterRepository.getClusterByCondition(env, condition.toString(), clusterListInfoReq.getPageNumber(), clusterListInfoReq.getPageSize());
		} catch (Exception e) {
			log.info("getClusterList error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		List<ClusterDO> clusterDOList = clusterDoPage.getPageList();
		if (null == clusterDOList || clusterDOList.size() < 1) {
			return new Page<ClusterResp>(clusterDoPage.getPageInfo(), clusterRespList);
		}
		ClusterResp clusterResp = null;
		for (ClusterDO clusterDO : clusterDOList) {
			clusterResp = new ClusterResp();
			clusterResp.setId(clusterDO.getId());
			clusterResp.setClusterName(clusterDO.getClusterId());
			String stateStr = (clusterDO.getStatus() == ClusterState.online.getValue()) ? "正常" : "下线";
			clusterResp.setState(stateStr);
			clusterResp.setQps(clusterDO.getQps());
			clusterResp.setGroupCount(clusterDO.getGroupCount());
			clusterRespList.add(clusterResp);
		}
		return new Page<ClusterResp>(clusterDoPage.getPageInfo(), clusterRespList);
	}

	public boolean checkClusterExist(String env,String clusterName) throws Exception {
		return clusterRepository.isExistCluster(env, clusterName);
	}

	public void addCluster(String env, ClusterInfoReq clusterInfoReq) throws ServiceException {
		boolean flag = false;
		try {
			flag = clusterRepository.isExistCluster(env, clusterInfoReq.getClusterName());
		} catch (Exception e) {
			log.info("addCluster error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (flag) {
			throw new ServiceException(ExceptionConstant.CLUSTER_NAME_EXISTED);
		}
		try {
			ClusterDO clusterDO = new ClusterDO();
			clusterDO.setHashCode(clusterInfoReq.getClusterName().hashCode());
			clusterDO.setClusterId(clusterInfoReq.getClusterName());
			clusterDO.setGroupCount(clusterInfoReq.getGroupCount());
			clusterDO.setUpdateTime(System.currentTimeMillis());
			clusterDO.setStatus(ClusterState.online.getValue());
			clusterDO.setQps(0);
			clusterRepository.insertCluster(env, clusterDO);
		} catch (Exception e) {
			log.info("addCluster error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}

	public void deleteCluster(String env, long id) throws ServiceException {
		boolean flag = false;
		ClusterDO clusterDO = null;
		try {
			clusterDO = clusterRepository.getClusterById(env, id);
		} catch (Exception e) {
			log.error("delete cluster error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (clusterDO == null) {
			throw new ServiceException(ExceptionConstant.CLUSTER_NOT_EXIST);
		}
		try {
			flag = this.isClusterHasServers(env, clusterDO.getClusterId());
		} catch (Exception e) {
			log.error("delete cluster error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (flag) {
			throw new ServiceException(ExceptionConstant.CLUSTER_EXIST_RESOURCES);
		}
		try {
			clusterRepository.deleteClusterById(env, id);
		} catch (Exception e) {
			log.error("delete cluster error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}

	private boolean isClusterHasServers(String env, String clusterName) throws Exception {
		List<ServerDO> serverList = serverRepository.getServersByClusterName(env, clusterName);
		return serverList != null && serverList.size() >= 1;
	}


	private String getSearchCondition(String env, ListInfoReq clusterListInfoReq) throws ServiceException {
		String ip = clusterListInfoReq.getIp();
		String clusterName = clusterListInfoReq.getClusterName();
		StringBuilder condition = new StringBuilder("");
		if (null != ip && !ip.isEmpty()) {
			// 按IP查找再对比集群名
			List<String> clusterNames = new ArrayList<String>();
			String serverCondition = " where server like " + "'%" + ip + "%'";
			List<ServerDO> serverList = null;
			try {
				serverList = serverRepository.getServersByCondition(env, serverCondition);
			} catch (Exception e) {
				log.info("getClusterList error", e);
				throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
			}
			if (null != serverList && serverList.size() > 0) {
				for (int i = 0; i < serverList.size(); i++) {
					clusterNames.add(serverList.get(i).getClusterId());
				}
				if (null == clusterName) {
					condition.append(" where cluster_id in ( ");
					for (int i = 0; i < clusterNames.size(); i++) {
						if (i < (clusterNames.size() - 1)) {
							condition.append("'").append(clusterNames.get(i)).append("'").append(", ");
						} else {
							condition.append("'").append(clusterNames.get(i)).append("'").append(" )");
						}
					}
				} else {
					if (clusterNames.contains(clusterName)) {
						condition.append(" where cluster_id like " + "'%" + clusterName + "%'");
					} else {
						return " where 1 != 1";
					}
				}
			}
		} else {
			if (null != clusterName && !clusterName.isEmpty()) {
				condition.append(" where cluster_id like ").append("'%" + clusterName + "%'");
			}
		}
		return condition.toString();
	}
}
