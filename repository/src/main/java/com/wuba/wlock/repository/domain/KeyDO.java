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
@TableName("t_key")
public class KeyDO extends BaseDO<KeyDO> {
	public static final String FIELD_NAME = "name";
	public static final String FIELD_HASH_KEY = "hash_key";
	public static final String FIELD_CLUSTER_ID = "cluster_id";
	public static final String FIELD_QPS= "qps";
	public static final String FIELD_DESCRIPTION= "description";
	public static final String FIELD_CREATOR= "creator";
	public static final String FIELD_CREATE_TIME = "create_time";
	public static final String FIELD_GROUP_ID = "group_id";
	public static final String FIELD_GROUP_IDS = "group_ids";
	public static final String FIELD_AUTO_RENEW = "auto_renew";
	public static final String FIELD_MULTI_GROUP = "multi_group";
	public static final String FIELD_ID = "id";

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	@TableField("name")
	private String name;

	@TableField("hash_key")
	private String hashKey;

	@TableField("cluster_id")
	private String clusterId;

	@TableField("qps")
	private Integer qps;

	@TableField("description")
	private String description;

	@TableField("creator")
	private String creator;

	@TableField("create_time")
	private Date createTime;

	/**
	 * 老版本单分组使用 , 新版本统一使用 groupIds
	 */
	@TableField("group_id")
	private Integer groupId;

	@TableField("group_ids")
	private String groupIds;

	@TableField("auto_renew")
	private Integer autoRenew;

	/**
	 * 是否使用多分组 : 新申请的秘钥默认都使用多分组
	 * 0 不使用,1 使用
	 */
	@TableField("multi_group")
	private Integer multiGroup;
}
