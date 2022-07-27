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
import com.wuba.wlock.repository.domain.MigrateProcessDO;
import com.wuba.wlock.repository.mappers.MigrateProcessMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MigrateProcessRepository extends BaseRepository<MigrateProcessMapper, MigrateProcessDO> {

	public boolean saveMigrateProcess(String choseEnv, final MigrateProcessDO migrateProcess) throws Exception {
		return save(migrateProcess);
	}

	public MigrateProcessDO searchById(String choseEnv, final Long id) throws Exception {
		return getById(id);
	}

	public List<MigrateProcessDO> searchByCondition(String choseEnv, final int state, final String keyHash, final Long migrateKeyVersion, final int isEnd) throws Exception {
		return list(Wrappers.<MigrateProcessDO>query()
				.eq(state != -1, MigrateProcessDO.FIELD_STATE, state)
				.eq(!Strings.isNullOrEmpty(keyHash), MigrateProcessDO.FIELD_KEY_HASH, keyHash)
				.eq(migrateKeyVersion != -1, MigrateProcessDO.FIELD_MIGRATE_KEY_VERSION, migrateKeyVersion)
				.eq(isEnd != -1, MigrateProcessDO.FIELD_IS_END, isEnd));
	}

	public boolean updateMigrateStateByKeyHash(String choseEnv, final MigrateProcessDO migrateDO) throws Exception {
		UpdateWrapper<MigrateProcessDO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.set(MigrateProcessDO.FIELD_STATE, migrateDO.getState());
		updateWrapper.set(MigrateProcessDO.FIELD_MIGRATE_KEY_VERSION, migrateDO.getMigrateKeyVersion());
		updateWrapper.set(MigrateProcessDO.FIELD_IS_END, migrateDO.getIsEnd());
		updateWrapper.eq(MigrateProcessDO.FIELD_KEY_HASH, migrateDO.getKayHash());
		updateWrapper.eq(MigrateProcessDO.FIELD_ID, migrateDO.getId());
		return update(updateWrapper);
	}

	public boolean deleteById(String choseEnv, final long id) throws Exception {
		return removeById(id);
	}
}
