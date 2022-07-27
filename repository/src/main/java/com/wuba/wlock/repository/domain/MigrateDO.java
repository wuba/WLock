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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_migrate")
public class MigrateDO extends BaseDO<MigrateDO> {
	public static final String FIELD_VERSION = "version";
	public static final String FIELD_GROUP_ID = "group_id";
	public static final String FIELD_CLUSTER = "cluster";
	public static final String FIELD_MIGRATE_STATE = "migrate_state";
	public static final String FIELD_SERVER = "server";
	public static final String FIELD_EXECUTE_RESULT = "execute_result";
	public static final String FIELD_KEY_HASH = "key_hash";
	public static final String FIELD_IS_END = "is_end";
	public static final String FIELD_CREATE_TIME = "create_time";
	public static final String FIELD_UPDATE_TIME = "update_time";
	public static final String FIELD_ID = "id";

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	@TableField("version")
	private Long version;

	@TableField("group_id")
	private Integer groupId;

	@TableField("cluster")
	private String cluster;

	@TableField("migrate_state")
	private Integer migrateState;

	@TableField("server")
	private String server;

	@TableField("execute_result")
	private Integer executeResult;

	@TableField("key_hash")
	private String keyHash;

	/**
	 * 迁移是否结束标志
	 */
	@TableField("is_end")
	private Integer end;

	@TableField("create_time")
	private Date createTime;

	@TableField("update_time")
	private Date updateTime;

}
