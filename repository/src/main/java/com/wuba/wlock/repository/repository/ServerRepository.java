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
package com.wuba.wlock.repository.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuba.wlock.repository.domain.ServerDO;
import com.wuba.wlock.repository.enums.ServerState;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.mappers.ServerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ServerRepository extends BaseRepository<ServerMapper, ServerDO> {

	@Resource
	private ServerMapper serverMapper;

	public boolean saveServer(String choseEnv, final ServerDO serverDO) throws Exception {
		return save(serverDO);
	}

	public List<ServerDO> getAllServer(String choseEnv) throws Exception {
		return list();
	}

	public List<ServerDO> getServerByClusterId(String choseEnv, String clusterId) throws Exception {
		return list(Wrappers.<ServerDO>query().eq(Objects.nonNull(clusterId), ServerDO.FIELD_CLUSTER_ID, clusterId));
	}

	public List<ServerDO> getServerByClusterIdAndState(String choseEnv, String clusterId, int state) throws Exception {
		return list(Wrappers.<ServerDO>query()
				.eq(Objects.nonNull(clusterId), ServerDO.FIELD_CLUSTER_ID, clusterId)
				.eq(ServerDO.FIELD_STATE, state));
	}

	public ServerDO getByServer(String choseEnv, String server) throws Exception {
		return getOne(Wrappers.<ServerDO>query()
				.eq(Objects.nonNull(server), ServerDO.FIELD_SERVER_ADDR, server));
	}

	public boolean deleteServerById(String choseEnv, long id) throws Exception {
		return removeById(id);
	}

	public List<ServerDO> getServersByClusterName(String choseEnv, String clusterName) throws Exception {
		return getServerByClusterId(choseEnv, clusterName);
	}


	public ServerDO getServerById(String choseEnv, long id) throws Exception {
		return getById(id);
	}

	public boolean updateClusterById(String choseEnv, String clusterName, long id) throws Exception {
		UpdateWrapper<ServerDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.set(ServerDO.FIELD_CLUSTER_ID, clusterName);
		updateWrapper.eq(ServerDO.FIELD_ID, id);
		return update(updateWrapper);
	}

	public boolean onlineServerByIds(String choseEnv, String clusterName, List<Long> idList) throws Exception {
		if (idList == null || idList.isEmpty()) {
			return true;
		}
		UpdateWrapper<ServerDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(ServerDO.FIELD_CLUSTER_ID, clusterName);
		updateWrapper.in(ServerDO.FIELD_ID, idList);
		updateWrapper.set(ServerDO.FIELD_STATE, ServerState.online.getValue());
		return update(updateWrapper);
	}

	public boolean offlineServerByIds(String choseEnv, String clusterName, List<Long> idList) throws Exception {
		if (idList == null || idList.isEmpty()) {
			return true;
		}
		UpdateWrapper<ServerDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(ServerDO.FIELD_CLUSTER_ID, clusterName);
		updateWrapper.in(ServerDO.FIELD_ID, idList);
		updateWrapper.set(ServerDO.FIELD_STATE, ServerState.offline.getValue());
		return update(updateWrapper);
	}

	public boolean isHasSameSequenceId(String choseEnv, String clusterName, int sequenceId) throws Exception {
		return count(Wrappers.<ServerDO>query()
				.eq(ServerDO.FIELD_CLUSTER_ID, clusterName)
				.eq(ServerDO.FIELD_SEQUENCE_ID, sequenceId)) > 0;
	}

	public boolean updateLastTimeById(String choseEnv, long id) throws Exception {
		UpdateWrapper<ServerDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(ServerDO.FIELD_ID, id);
		updateWrapper.set(ServerDO.FIELD_LAST_UPDATE_TIME, new Date());
		return update(updateWrapper);
	}

	public boolean resetLastTimeById(String choseEnv, long id) throws Exception {
		UpdateWrapper<ServerDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(ServerDO.FIELD_ID, id);
		updateWrapper.set(ServerDO.FIELD_LAST_UPDATE_TIME, null);
		return update(updateWrapper);
	}

	public int getCountByCondition(String choseEnv, String condition) throws Exception {
		return serverMapper.getCountByCondition(condition);
	}

	public List<ServerDO> getServersByCondition(String choseEnv, String condition) throws Exception {
		return serverMapper.getServerByCondition(condition);
	}

	public Page<ServerDO> getServerByCondition(String choseEnv, String condition, int pageNumber, int pageSize) throws Exception {
		int count = getCountByCondition(choseEnv, condition);
		List<ServerDO> result = new ArrayList<>();
		if (count > 0) {
			int startIndex = (pageNumber - 1) * pageSize;
			result = serverMapper.getServerPageByCondition(condition, pageSize, startIndex);
		}
		return new Page<>(pageNumber, result.size(), count, pageSize, result);
	}

}
