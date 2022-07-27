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
package com.wuba.wlock.client.communication;

import java.util.concurrent.ConcurrentHashMap;

public class WaitWindow {
	private static WaitWindow instance = new WaitWindow();
	// 缓存所有对外请求
	protected final ConcurrentHashMap<Long /* opaque */, WindowData> waitTable = new ConcurrentHashMap<Long, WindowData>();
	
	public static WaitWindow getWindow() {
		return instance;
	}

	public void addWd(long opaque, WindowData wd) {
		this.waitTable.put(opaque, wd);
	}

	public WindowData remove(long opaque) {
		return this.waitTable.remove(opaque);
	}

	public WindowData get(long opaque) {
		return this.waitTable.get(opaque);
	}
}
