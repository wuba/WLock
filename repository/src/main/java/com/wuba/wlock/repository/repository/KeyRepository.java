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
import com.wuba.wlock.repository.domain.KeyDO;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.mappers.KeyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeyRepository extends BaseRepository<KeyMapper, KeyDO> {

	@Resource
	private KeyMapper keyMapper;

	public List<KeyDO> getAllKey(String choseEnv) throws Exception {
		return list();
	}

	public List<KeyDO> getKeyByClusterId(String choseEnv, String clusterName) throws Exception {
		return list(Wrappers.<KeyDO>query().eq(Objects.nonNull(clusterName), KeyDO.FIELD_CLUSTER_ID, clusterName));
	}

	public KeyDO getKeyByName(String choseEnv, String keyName) throws Exception {
		return getOne(Wrappers.<KeyDO>query().eq(Objects.nonNull(keyName), KeyDO.FIELD_NAME, keyName));
	}


	public KeyDO getKeyByHashKey(String choseEnv, String hashKey) throws Exception {
		return getOne(Wrappers.<KeyDO>query().eq(Objects.nonNull(hashKey), KeyDO.FIELD_HASH_KEY, hashKey));
	}

	public boolean deleteKeyByName(String choseEnv, String keyName) throws Exception {
		return remove(Wrappers.<KeyDO>query().eq(Objects.nonNull(keyName), KeyDO.FIELD_NAME, keyName));
	}

	public boolean deleteKeyById(String choseEnv, long id) throws Exception {
		return removeById(id);
	}

	public boolean saveKey(String choseEnv, final KeyDO keyDO) throws Exception {
		return save(keyDO);
	}

	public long getAllCount(String choseEnv) throws Exception {
		return count();
	}

	public KeyDO getKeyById(String choseEnv, long id) throws Exception {
		return getById(id);
	}

	public boolean updateKeyDO(String choseEnv, KeyDO keyDO) throws Exception {
		UpdateWrapper<KeyDO> wrapper = new UpdateWrapper<>();
		wrapper.set(KeyDO.FIELD_CLUSTER_ID, keyDO.getClusterId());
		wrapper.set(KeyDO.FIELD_DESCRIPTION, keyDO.getDescription());
		wrapper.set(KeyDO.FIELD_GROUP_ID, keyDO.getGroupId());
		wrapper.set(KeyDO.FIELD_QPS, keyDO.getQps());
		wrapper.set(KeyDO.FIELD_AUTO_RENEW, keyDO.getAutoRenew());
		wrapper.set(KeyDO.FIELD_GROUP_IDS, keyDO.getGroupIds());
		wrapper.eq(KeyDO.FIELD_ID, keyDO.getId());
		return update(wrapper);
	}

	public int getCountByCondition(String choseEnv, String condition) throws Exception {
		return keyMapper.getCountByCondition(condition);
	}

	public Page<KeyDO> getKeyByCondition(String choseEnv, String condition, int pageNumber, int pageSize) throws Exception {
		int count = getCountByCondition(choseEnv, condition);
		List<KeyDO> result = new ArrayList<>();
		if (count > 0) {
			int startIndex = (pageNumber - 1) * pageSize;
			result = keyMapper.getKeyByCondition(condition, pageSize, startIndex);
		}
		return new Page<>(pageNumber, result.size(), count, pageSize, result);
	}

}
