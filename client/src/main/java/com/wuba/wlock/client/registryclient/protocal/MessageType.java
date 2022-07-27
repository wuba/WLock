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
package com.wuba.wlock.client.registryclient.protocal;

public class MessageType {
	
	public static final byte RESPONSE_NO_CHANGE = 0x00;
	public static final byte RESPONSE_ALL_DATAS_SUCCESS = 0x01;
	public static final byte RESPONSE_HAS_CHANGE = 0x02;
	public static final byte RESPONSE_ERROR = 0x03;
	public static final byte RESPONSE_ALL_DATAS_ERROR = 0x04;
	public static final byte RESPONSE_ACK = 0x05;
	
	public static final byte REQUEST_ALL_DATAS = 0x06;
	public static final byte REQUEST_VALIDATION = 0x07;
	
	public static final byte PUSH_ALL_DATAS = 0x01;
	public static final byte PUSH_CHANGED_SERVER = 0x02;
	
	public static final byte CLIENT_HEARTBEAT = 0x01;
	
	public static final byte CLIENT_VERSION = 0x01;

}
