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
package com.wuba.wlock.registry.admin.domain.request;

public class MigrateReqKeyOperateInfoReq extends BaseMigrateReq {

	/**
	 * 需要下发的迁移指令
	 */
	private int migrateState;
	/**
	 * 集群
	 */
	private String cluster;
	/**
	 * 秘钥
	 */
	private String key;

	public int getMigrateState() {
		return migrateState;
	}

	public void setMigrateState(int migrateState) {
		this.migrateState = migrateState;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
