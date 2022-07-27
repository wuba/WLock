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
import com.wuba.wlock.repository.domain.ClusterDO;
import com.wuba.wlock.repository.enums.ClusterState;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.mappers.ClusterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ClusterRepository extends BaseRepository<ClusterMapper, ClusterDO> {

	@Resource
	private ClusterMapper clusterMapper;

	public ClusterDO getClusterByClusterName(String choseEnv, String clusterName) throws Exception {
		return getOne(Wrappers.<ClusterDO>query()
				.eq(Objects.nonNull(clusterName), ClusterDO.FIELD_CLUSTER_ID, clusterName));
	}

	public ClusterDO getClusterById(String choseEnv, long id) throws Exception {
		return getById(id);
	}

	public List<ClusterDO> getClusterListByClusterNameLike(String choseEnv, String like) throws Exception {
		return list(Wrappers.<ClusterDO>query().like(Objects.nonNull(like), ClusterDO.FIELD_CLUSTER_ID, like));
	}

	public List<ClusterDO> getAllCluster(String choseEnv) throws Exception {
		return list();
	}

	public List<ClusterDO> getClustersByState(String choseEnv, ClusterState clusterState) throws Exception {
		return list(Wrappers.<ClusterDO>query().eq(Objects.nonNull(clusterState), ClusterDO.FIELD_STATUS, clusterState.getValue()));
	}

	public boolean isExistCluster(String choseEnv, String clusterName) throws Exception {
		return getClusterByClusterName(choseEnv, clusterName) != null;
	}

	public boolean updateQps(String choseEnv, int qps, long id) throws Exception {
		UpdateWrapper<ClusterDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(ClusterDO.FIELD_ID, id)
				.set(ClusterDO.FIELD_QPS, qps);
		return update(updateWrapper);
	}

	public boolean updateClusterVersionByClusterName(String choseEnv, long version, String clusterName) throws Exception {
		UpdateWrapper<ClusterDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(Objects.nonNull(clusterName), ClusterDO.FIELD_CLUSTER_ID, clusterName)
				.set(ClusterDO.FIELD_VERSION, version);
		return update(updateWrapper);
	}

	public boolean updateClusterStatusByClusterName(String choseEnv, int status, String clusterName) throws Exception {
		UpdateWrapper<ClusterDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(Objects.nonNull(clusterName), ClusterDO.FIELD_CLUSTER_ID, clusterName)
				.set(ClusterDO.FIELD_STATUS, status);
		return update(updateWrapper);
	}

	public boolean updateClusterQpsByClusterName(String choseEnv, int qps, String clusterName) throws Exception {
		UpdateWrapper<ClusterDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(Objects.nonNull(clusterName), ClusterDO.FIELD_CLUSTER_ID, clusterName)
				.set(ClusterDO.FIELD_QPS, qps);
		return update(updateWrapper);
	}

	public boolean insertCluster(String choseEnv, final ClusterDO clusterDO) throws Exception {
		return save(clusterDO);
	}


	public boolean deleteClusterById(String choseEnv, long id) throws Exception {
		return removeById(id);
	}

	public int getCountByCondition(String choseEnv, String condition) throws Exception {
		return clusterMapper.getCountByCondition(condition);
	}

	public Page<ClusterDO> getClusterByCondition(String choseEnv, String condition, int pageNumber, int pageSize) throws Exception {
		int count = getCountByCondition(choseEnv, condition);
		List<ClusterDO> result = new ArrayList<>();
		if (count > 0) {
			int startIndex = (pageNumber - 1) * pageSize;
			result = clusterMapper.getClusterByCondition(condition, pageSize, startIndex);
		}
		return new Page<>(pageNumber, result.size(), count, pageSize, result);
	}
}
