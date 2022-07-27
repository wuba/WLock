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

@Data
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_cluster")
public class ClusterDO extends BaseDO<ClusterDO> {

	public static final String FIELD_CLUSTER_ID = "cluster_id";
	public static final String FIELD_HASH_CODE = "hash_code";
	public static final String FIELD_VERSION = "version";
	public static final String FIELD_STATUS = "status";
	public static final String FIELD_GROUP_COUNT = "group_count";
	public static final String FIELD_QPS = "qps";
	public static final String FIELD_ID = "id";

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	@TableField("cluster_id")
	private String clusterId;

	@TableField("hash_code")
	private Integer hashCode;

	@TableField("version")
	private Long updateTime;

	@TableField("status")
	private Integer status;

	@TableField("group_count")
	private Integer groupCount;

	@TableField("qps")
	private Integer qps;
}
