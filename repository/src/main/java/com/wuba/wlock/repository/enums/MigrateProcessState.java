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
package com.wuba.wlock.repository.enums;


public enum MigrateProcessState {
	/**
	 * 秘钥正向迁移
	 */
	ForwardMigrate(0, "秘钥正向迁移"),
	/**
	 * 变更节点
	 */
	ChangeNode(1, "变更节点"),
	/**
	 * group 节点变更
	 */
	ChangeGroupNode(2, "group 节点变更"),
	/**
	 * 集群拆分
	 */
	ClusterSplit(3, "集群拆分"),
	/**
	 * 秘钥逆向迁移
	 */
	BackWardTransfer(4, "秘钥逆向迁移"),
	/**
	 * 恢复为迁移之前的状态
	 */
	RestoreMigrationState(5, "恢复迁移状态");

	private int value;
	private String des;

	MigrateProcessState(int value, String des) {
		this.value = value;
		this.des = des;
	}

	public static MigrateProcessState parse(int type) {
		for (MigrateProcessState tmp : values()) {
			if (type == tmp.value) {
				return tmp;
			}
		}
		throw new IllegalArgumentException("Illegal Argument [" + type + "] for MigrateProcessState");
	}

	public int getValue() {
		return value;
	}


	public String getDes() {
		return des;
	}

}
