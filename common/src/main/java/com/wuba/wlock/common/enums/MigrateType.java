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
package com.wuba.wlock.common.enums;


public enum MigrateType {
	/**
	 * 迁移初始化状态
	 */
	Init(0,"迁移初始化状态"),
	/**
	 * 迁移准备 - 开始
	 */
	MigratePrepare(1,"迁移准备状态"),
	/**
	 * 迁移开始 - 开始
	 */
	MigrateGroupStartMoving(2,"迁移开始状态"),
	/**
	 * 迁移安全状态 - 开始
	 */
	MigrateGroupMovingSafePoint(3,"迁移安全状态"),
	/**
	 * 迁移秘钥结束
	 */
	MigrateGroupEndMoving(4,"迁移秘钥结束状态"),
	/**
	 * 迁移结束
	 */
	MigrateEnd(5,"迁移结束状态"),
	/**
	 * 迁移准备 - 回滚
	 */
	MigratePrepareRollBack(6,"迁移准备回滚状态"),
	/**
	 * 迁移开始 - 回滚
	 */
	MigrateGroupStartMovingRollBack(7,"迁移开始回滚状态"),
	/**
	 * 迁移安全状态 - 回滚
	 */
	MigrateGroupMovingSafePointRollBack(8,"迁移安全回滚状态");



	private final int value;

	private final String name;

	MigrateType(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return this.value;
	}

	public String getName() {
		return name;
	}

	public static MigrateType parse(int value) {
		for (MigrateType tmp : values()) {
			if (value == tmp.value) {
				return tmp;
			}
		}
		throw new IllegalArgumentException("Illegal Argument [" + value + "] for MigrateType");
	}
}
