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
package com.wuba.wlock.registry.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IDHelper {
	// 取当前时间低16位作为serverId，则serverId范围在0-65535之间，降低多server获取相同serverId的概率，好处是不需要明确指定serverId的数值；如果要严格保证唯一性，则不同server的serverId要给出不同的值
	// 理论上，该算法可以做到1秒内最大生成3.2万个ID；如果觉得还不够，count值还可以最大设置为64，使用count计数也是期望最大程度上避免时间回调导致出现重复ID
	private static Logger logger = LoggerFactory.getLogger(IDHelper.class);
	private final static long SERVER_ID = System.currentTimeMillis() & 65535;
	// 2016-01-01 00:00:00
	private static final long ID_BEGIN_TIME = 1451577600000L;
	private static long count = 0;

	private static final int MASK = 31;

	public static synchronized long getUniqueId() {
		count++;

		if (count > MASK) {
			count = 0;
			try {
				Thread.sleep(1);
			} catch(InterruptedException e) {
				logger.error(e.getMessage(),e);
				Thread.currentThread().interrupt();
			}
		}

		long destId = System.currentTimeMillis() - ID_BEGIN_TIME;
		destId = (destId << 21) | SERVER_ID | (count << 16);
		return destId;
	}

}
