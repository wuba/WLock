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
package com.wuba.wlock.server.lock;

public enum LockResult {
	SUCCESS(0,"成功"),
	PROTOCOL_SERIALIZATION_ERROR(1,"协议序列化错误"),
	PROTOCOL_TYPE_ERROR(2,"协议类型错误"),
	EXCEPTION(3,"异常情况"),
	KEY_NOT_EXIST(4,"key不存在"),
	OWNER_ERROR(5,"owner错误"),
	TOKEN_ERROR(6,"版本号错误");

	private int value;
	private String des;

	LockResult(int value, String des) {
		this.value = value;
		this.des = des;
	}

	public int getValue() {
		return value;
	}

	public String getDes() {
		return des;
	}
}
