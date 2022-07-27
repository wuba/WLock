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
package com.wuba.wlock.common.collector.protocol;

import java.util.Map;


public class KeyQps {

	/**
	 * 单分组 : value 的 map 只有一个值
	 * 多分组 : value 的 map 是所有分组的流量数据
	 */
	private Map<String, Map<Integer, QpsEntity>> keyGroupQps;

	public Map<String, Map<Integer, QpsEntity>> getKeyGroupQps() {
		return keyGroupQps;
	}

	public void setKeyGroupQps(Map<String, Map<Integer, QpsEntity>> keyGroupQps) {
		this.keyGroupQps = keyGroupQps;
	}
}
