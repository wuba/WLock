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
import com.wuba.wlock.repository.domain.MigrateDO;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.mappers.MigrateMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class MigrateRepository extends BaseRepository<MigrateMapper, MigrateDO> {

	public long searchCount(String choseEnv, String cluster, String server, Integer groupId, Integer isEnd, Long version) throws Exception {
		return count(Wrappers.<MigrateDO>query()
				.eq(Objects.nonNull(cluster), MigrateDO.FIELD_CLUSTER, cluster)
				.eq(Objects.nonNull(server), MigrateDO.FIELD_SERVER, server)
				.eq(groupId != -1, MigrateDO.FIELD_GROUP_ID, groupId)
				.eq(isEnd != -1, MigrateDO.FIELD_IS_END, isEnd)
				.eq(version != -1, MigrateDO.FIELD_VERSION, version));
	}

	public Page<MigrateDO> searchMigrateByConditionPage(String choseEnv, String cluster, String server, Integer groupId, Integer isEnd, Long version, int pageNumber, int pageSize) throws Exception {
		int count = (int) searchCount(choseEnv, cluster, server, groupId, isEnd, version);
		List<MigrateDO> list = list(Wrappers.<MigrateDO>query()
				.eq(Objects.nonNull(cluster), MigrateDO.FIELD_CLUSTER, cluster)
				.eq(Objects.nonNull(server), MigrateDO.FIELD_SERVER, server)
				.eq(groupId != -1, MigrateDO.FIELD_GROUP_ID, groupId)
				.eq(isEnd != -1, MigrateDO.FIELD_IS_END, isEnd)
				.eq(version != -1, MigrateDO.FIELD_VERSION, version));
		return new Page<>(pageNumber, list.size(), count, pageSize, list);
	}

	public List<MigrateDO> searchMigrateByCondition(String choseEnv, String cluster, String server, Integer groupId, Integer isEnd, Long version) throws Exception {
		return list(Wrappers.<MigrateDO>query()
				.eq(!Strings.isNullOrEmpty(cluster), MigrateDO.FIELD_CLUSTER, cluster)
				.eq(!Strings.isNullOrEmpty(server), MigrateDO.FIELD_SERVER, server)
				.eq(groupId != -1, MigrateDO.FIELD_GROUP_ID, groupId)
				.eq(isEnd != -1, MigrateDO.FIELD_IS_END, isEnd)
				.eq(version != -1, MigrateDO.FIELD_VERSION, version));
	}

	public boolean saveMigrate(String choseEnv, final MigrateDO migrateDO) throws Exception {
		return save(migrateDO);
	}

	public void batchSaveMigrate(String choseEnv, final List<MigrateDO> migrateDos) throws Exception {
		saveBatch(migrateDos);
	}

	public void batchUpdateById(String choseEnv, final List<MigrateDO> migrateDos) throws Exception {
		updateBatchById(migrateDos);
	}

	public boolean updateMigrateStateById(String choseEnv, final MigrateDO migrateDO) throws Exception {
		UpdateWrapper<MigrateDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.set(MigrateDO.FIELD_MIGRATE_STATE, migrateDO.getMigrateState());
		updateWrapper.set(MigrateDO.FIELD_EXECUTE_RESULT, migrateDO.getExecuteResult());
		updateWrapper.set(MigrateDO.FIELD_IS_END, migrateDO.getEnd());
		updateWrapper.eq(MigrateDO.FIELD_ID, migrateDO.getId());
		return update(updateWrapper);
	}

	public boolean deleteById(String choseEnv, final long id) throws Exception {
		return removeById(id);
	}

	public boolean deleteByIds(String choseEnv, final List<MigrateDO> migrateDos) throws Exception {
		return removeByIds(migrateDos);
	}


}
