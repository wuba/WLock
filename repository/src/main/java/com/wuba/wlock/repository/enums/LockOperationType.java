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

public enum LockOperationType {
	/**
	 * 加锁
	 */
	ACQUIRE_LOCK(0x00, "加锁"),
	/**
	 * 释放锁
	 */
	RELEASE_LOCK(0x02, "释放锁"),
	/**
	 * 刷新所
	 */
	RENEW_LOCK(0X04, "刷新锁"),
	/**
	 * 删除锁
	 */
	DELETE_LOCK(0x07, "删除锁");

	private int type;
	private String des;

	LockOperationType(int type, String des) {
		this.type = type;
		this.des = des;
	}

	public int getType() {
		return type;
	}

	public String getDes() {
		return des;
	}
}
