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
package com.wuba.wlock.repository.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_migrate_process")
public class MigrateProcessDO extends BaseDO<MigrateProcessDO> {

	public static final String FIELD_STATE = "state";
	public static final String FIELD_KEY_HASH = "key_hash";
	public static final String FIELD_MIGRATE_KEY_VERSION = "migrate_key_version";
	public static final String FIELD_IS_END = "is_end";
	public static final String FIELD_GROUPS = "groups";
	public static final String FIELD_CREATE_TIME = "create_time";
	public static final String FIELD_UPDATE_TIME = "update_time";
	public static final String FIELD_NODE_LIST = "node_list";
	public static final String FIELD_ID = "id";

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	@TableField("state")
	private Integer state;
	@TableField("key_hash")
	private String kayHash;
	@TableField("migrate_key_version")
	private Long migrateKeyVersion;
	@TableField("is_end")
	private Integer isEnd;
	@TableField("groups")
	private String groups;
	@TableField("create_time")
	private Date createTime;
	@TableField("update_time")
	private Date updateTime;
	@TableField("node_list")
	private String nodes;

	public Set<Integer> groupIds() {
		if (groups == null) {
			return new HashSet<Integer>();
		}

		Set<Integer> groupIds = new HashSet<Integer>();
		String[] split = groups.split(",");
		for (String groupIdString: split) {
			groupIds.add(Integer.parseInt(groupIdString));
		}
		return groupIds;
	}

	public Set<String> nodes() {
		if (nodes == null) {
			return new HashSet<String>();
		}
		return Sets.newHashSet(nodes.split(","));
	}
}
