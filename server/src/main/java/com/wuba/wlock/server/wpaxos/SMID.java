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
package com.wuba.wlock.server.wpaxos;

public enum SMID {
	/**
	 * 锁操作相关
	 */
	LOCK_SMID(1),
	KEEP_MASTER(2),
	WHEEL_TICK(3),
	MIGRATE_COMMAND(4),
	MIGRATE_POINT(5),
	GROUP_META(6),
	NULL(7);
	;

	private int value;

	SMID(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
