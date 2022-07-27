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
package com.wuba.wlock.server.constant;

import java.util.HashMap;
import java.util.Map;

public class PaxosState {
	private static Map<Integer, Boolean> groupStart = new HashMap<>();

	private PaxosState() {
	}

	public static boolean isStarted(int group) {
		return groupStart.containsKey(group) && groupStart.get(group);
	}

	public static boolean isStarted() {
		return !groupStart.values().stream().anyMatch(isStart -> !isStart);
	}

	public static void setGroupStart(int group) {
		groupStart.put(group, true);
	}
}
