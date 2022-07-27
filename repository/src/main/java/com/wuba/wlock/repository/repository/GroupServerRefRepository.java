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
import com.wuba.wlock.repository.domain.GroupServerRefDO;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.mappers.GroupServerRefMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class GroupServerRefRepository extends BaseRepository<GroupServerRefMapper, GroupServerRefDO> {

	@Resource
	private GroupServerRefMapper groupServerRefMapper;

	public boolean saveGroupServerRef(String choseEnv, GroupServerRefDO groupServerRef) throws Exception {
		return save(groupServerRef);
	}

	public List<GroupServerRefDO> getAll(String choseEnv) throws Exception {
		return list();
	}

	public List<GroupServerRefDO> getGroupServerRefByClusterId(String choseEnv, String clusterName) throws Exception {
		return list(Wrappers.<GroupServerRefDO>query().eq(Objects.nonNull(clusterName), GroupServerRefDO.FIELD_CLUSTER_ID, clusterName));
	}


	public List<GroupServerRefDO> getGroupServerRefByServerAddr(String choseEnv, String serverAddr) throws Exception {
		return list(Wrappers.<GroupServerRefDO>query().eq(Objects.nonNull(serverAddr), GroupServerRefDO.FIELD_SERVER, serverAddr));
	}

	public List<GroupServerRefDO> getGroupServerRefByClusterAndServer(String choseEnv, String clusterName, String server) throws Exception {
		return list(Wrappers.<GroupServerRefDO>query()
				.eq(Objects.nonNull(server), GroupServerRefDO.FIELD_SERVER, server)
				.eq(Objects.nonNull(clusterName), GroupServerRefDO.FIELD_CLUSTER_ID, clusterName));
	}

	public boolean deleteGroupServerRefById(String choseEnv, long id) throws Exception {
		return removeById(id);
	}

	public boolean batchDeleteGroupServer(String choseEnv, List<Long> ids) throws Exception {
		return removeByIds(ids);
	}

	public boolean updateGroupServerRef(String choseEnv, GroupServerRefDO groupServerRefDO) throws Exception {
		UpdateWrapper<GroupServerRefDO> wrapper = new UpdateWrapper<>();
		wrapper.set(GroupServerRefDO.FIELD_GROUP_ID, groupServerRefDO.getGroupId());
		wrapper.set(GroupServerRefDO.FIELD_CLUSTER_ID, groupServerRefDO.getClusterId());
		wrapper.set(GroupServerRefDO.FIELD_SERVER, groupServerRefDO.getServerAddr());
		wrapper.set(GroupServerRefDO.FIELD_UPDATE_TIME, groupServerRefDO.getUpdateTime());
		wrapper.eq(GroupServerRefDO.FIELD_ID, groupServerRefDO.getId());
		wrapper.lt(GroupServerRefDO.FIELD_VERSION, groupServerRefDO.getVersion());
		return update(wrapper);

	}

	public boolean updateGroupServerVersionById(String choseEnv, GroupServerRefDO groupServerRefDO) throws Exception {
		UpdateWrapper<GroupServerRefDO> wrapper = new UpdateWrapper<>();
		wrapper.eq(GroupServerRefDO.FIELD_ID, groupServerRefDO.getId());
		wrapper.set(GroupServerRefDO.FIELD_VERSION, groupServerRefDO.getVersion());
		return update(wrapper);
	}

	public int getCountByCondition(String choseEnv, String condition) throws Exception {
		return groupServerRefMapper.getCountByCondition(condition);
	}

	public Page<GroupServerRefDO> getGroupByCondition(String choseEnv, String condition, int pageNumber, int pageSize) throws Exception {
		int count = getCountByCondition(choseEnv, condition);
		List<GroupServerRefDO> result = new ArrayList<>();
		if (count > 0) {
			int startIndex = (pageNumber - 1) * pageSize;
			result = groupServerRefMapper.getGroupByCondition(condition, pageSize, startIndex);
		}
		return new Page<>(pageNumber, result.size(), count, pageSize, result);
	}


}
