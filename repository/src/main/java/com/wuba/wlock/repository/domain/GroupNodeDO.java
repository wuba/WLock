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
@TableName("t_group_node")
public class GroupNodeDO extends BaseDO<GroupNodeDO> {

	public static final int GROUP_NORMAL = -1;

	public static final String FIELD_CLUSTER_ID = "cluster_id";
	public static final String FIELD_GROUP_ID = "group_id";
	public static final String FIELD_SERVER = "server";
	public static final String FIELD_NODE_LIST = "node_list";
	public static final String FIELD_USE_MASTER = "use_master";
	public static final String FIELD_LOAD_BALANCE = "load_balance";
	public static final String FIELD_CREATE_TIME = "create_time";
	public static final String FIELD_UPDATE_TIME = "update_time";
	public static final String FIELD_ID = "id";

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	@TableField("cluster_id")
	private String clusterId;
	@TableField("group_id")
	private Integer groupId;
	@TableField("server")
	private String server;
	@TableField("node_list")
	private String nodes;
	@TableField("use_master")
	private Integer useMaster;
	@TableField("load_balance")
	private Integer loadBalance;
	@TableField("create_time")
	private Date createTime;
	@TableField("update_time")
	private Date updateTime;

}
