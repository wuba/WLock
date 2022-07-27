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
@TableName("t_group_server_ref")
public class GroupServerRefDO extends BaseDO<GroupServerRefDO> {

	public static final String FIELD_CLUSTER_ID = "cluster_id";
	public static final String FIELD_GROUP_ID = "group_id";
	public static final String FIELD_SERVER = "server";
	public static final String FIELD_CREATE_TIME = "create_time";
	public static final String FIELD_UPDATE_TIME = "update_time";
	public static final String FIELD_VERSION = "version";
	public static final String FIELD_ID = "id";

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	@TableField("group_id")
	private Integer groupId;

	@TableField("cluster_id")
	private String clusterId;

	@TableField("server")
	private String serverAddr;

	@TableField("create_time")
	private Date createTime;

	@TableField("update_time")
	private Date updateTime;

	@TableField("version")
	private Long version;
}
