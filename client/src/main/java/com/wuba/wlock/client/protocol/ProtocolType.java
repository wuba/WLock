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

public class ProtocolType {	
	public static byte ACQUIRE_LOCK = 0x00;
	public static byte WATCH_LOCK = 0x01;
	public static byte RELEASE_LOCK = 0x02;
	public static byte GET_LOCK = 0X03;
	public static byte RENEW_LOCK = 0X04;
	public static byte EVENT_NOTIFY = 0X05;
	public static byte HEARTBEAT = 0X06;
	public final static byte REBOOT = 0x08;
}
