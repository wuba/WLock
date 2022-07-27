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
package com.wuba.wlock.common.registry.protocol.response;

import java.util.Map;

public class GetRegistryKeyQpsRes {
	private Map<String, Integer> keyQpsMap;
	private long version;

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Map<String, Integer> getKeyQpsMap() {
		return keyQpsMap;
	}

	public void setKeyQpsMap(Map<String, Integer> keyQpsMap) {
		this.keyQpsMap = keyQpsMap;
	}
}
