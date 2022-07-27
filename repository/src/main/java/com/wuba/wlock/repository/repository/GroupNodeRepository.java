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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuba.wlock.repository.domain.GroupNodeDO;
import com.wuba.wlock.repository.mappers.GroupNodeMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GroupNodeRepository extends BaseRepository<GroupNodeMapper, GroupNodeDO> {


	public List<GroupNodeDO> searchByCondition(String choseEnv ,final int groupId, String server, String cluster) throws Exception {
		return list(Wrappers.<GroupNodeDO>query()
				.eq(groupId != -1, GroupNodeDO.FIELD_GROUP_ID, groupId)
				.eq(!Strings.isNullOrEmpty(server), GroupNodeDO.FIELD_SERVER, server)
				.eq(!Strings.isNullOrEmpty(cluster), GroupNodeDO.FIELD_CLUSTER_ID, cluster));
	}

	public boolean batchSave(String choseEnv ,final List<GroupNodeDO> groupNodeDos) throws Exception {
		return saveBatch(groupNodeDos);
	}

	public boolean batchUpdateById(String choseEnv ,final List<GroupNodeDO> groupNodeDos) throws Exception {
		return updateBatchById(groupNodeDos);
	}

	public boolean deleteByIds(String choseEnv ,final List<Long> ids) throws Exception {
		return removeByIds(ids);
	}

}
