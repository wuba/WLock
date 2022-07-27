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
package com.wuba.wlock.common.registry.protocol;

public class OptionCode {

	// 客户端操作码
	public static final byte OPCODE_LOCK_CLIENT_CONFIG_GET = (byte) 0x00;
	public static final byte OPCODE_LOCK_CLIENT_CONFIG_PUSH = (byte) 0x01;
	public static final byte OPCODE_LOCK_CLIENT_HEARTBEAT = (byte) 0x02;
	public static final byte OPCODE_LOCK_CLIENT_VERSION = (byte) 0x03;

	// 服务端操作
	public static final byte GET_PAXOS_CONFIG = (byte) 0x10;
	public static final byte RES_PAXOS_CONFIG = (byte) 0x11;
	public static final byte UPLOAD_MASTER_CONFIG = (byte) 0x12;
	public static final byte RES_UPLOAD_MASTER_CONFIG = (byte) 0x13;
	public static final byte GET_REGISTRY_KEY_QPS= (byte) 0x14;
	public static final byte RES_REGISTRY_KEY_QPS= (byte) 0x15;

	/**
	 * 分组迁移配置
	 */
	public static final byte GET_GROUP_MIGRATE_CONFIG= (byte) 0x16;
	public static final byte RES_GROUP_MIGRATE_CONFIG= (byte) 0x17;

	public static final byte UPLOAD_MIGRATE_STATE = (byte) 0x18;
	public static final byte RES_UPLOAD_MIGRATE_STATE = (byte) 0x19;

	/**
	 * 限流相关操作
	 */
	public static final byte OPCODE_LOCK_KEY_OVERSPEED= (byte) 0x20;

}
