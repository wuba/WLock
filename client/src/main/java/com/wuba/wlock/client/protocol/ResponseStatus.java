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
package com.wuba.wlock.client.protocol;

public class ResponseStatus {
	public static short SUCCESS = 0x0000; // request success
	public static short TIMEOUT = 0x0001; // request failed, 请求超时
	public static short ERROR = 0X0002;
	public static short LOCK_OCCUPIED = 0X0003; // request failed, 锁已被占有
	public static short LOCK_DELETED = 0X0004;
	public static short LOCK_CHANGED_OWNER = 0X0005;

	public static short MASTER_REDIRECT = 0X0006;

//	public static short PROPOSE_FAIL = 0X0007;
	public static short TOKEN_ERROR = 0X0008;
	public static short LOCK_WAIT = 0X0009;
	
	public static String toStr(short status) {
		switch (status) {
		case 0x0000:
			return "SUCCESS";
		case 0x0001:
			return "TIMEOUT";
		case 0x0002:
			return "ERROR";	
		case 0x0003:
			return "LOCK_OCCUPIED";
		case 0X0004:
			return "LOCK_DELETED";
		case 0x0005:
			return "LOCK_CHANGED_OWNER";
		case 0x0006:
			return "MASTER_REDIRECT";
		case 0x0007:
			return "PROPOSE_FAIL";
		case 0x0008:
			return "TOKEN_ERROR";
		case 0x0009:
			return "LOCK_WAIT";
		default:
			return "UNKOWN_STATUS";
		}
	}
}