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
import java.util.concurrent.ConcurrentHashMap;

public final class GroupState {
	private static boolean stopPush;
	private static Map<Integer, Boolean> masterChangeState = new HashMap<>();


	private static Map<Integer, Boolean> groupStates = new ConcurrentHashMap<>();
	private static Map<Integer, Boolean> keepMasterState = new ConcurrentHashMap<>();
	private static Map<Integer, Long> targetNode = new ConcurrentHashMap<>();

	private static Map<Integer, Boolean> dropKeepMaster = new ConcurrentHashMap<>();

	private GroupState() {
	}

	public static void setGroupChangeState(int groupIdx, boolean masterChanged) {
		masterChangeState.put(groupIdx, masterChanged);
	}

	public static boolean getGroupChangeState(int groupIdx) {
		return masterChangeState.getOrDefault(groupIdx, false);
	}

	public static boolean isStopPush() {
		return stopPush;
	}

	public static void setStopPush(boolean stopPush) {
		GroupState.stopPush = stopPush;
	}

	public static boolean isGroupKeepMaster(int group) {
		return keepMasterState.containsKey(group) && keepMasterState.get(group);
	}

	public static void setGroupTarget(int group, long targerId) {
		targetNode.put(group, targerId);
	}

	public static long getGroupTarget(int group) {
		return targetNode.containsKey(group) ? targetNode.get(group) : -1;
	}

	public static void setGroupKeepMaster(int groupIdx) {
		keepMasterState.put(groupIdx, true);
	}

	public static void clearGroupKeepMaster(int groupIdx) {
		keepMasterState.put(groupIdx, false);
		clearDropKeepMaster(groupIdx);
	}

	public static void setDropKeepMaster(int groupIdx) {
		dropKeepMaster.put(groupIdx, true);
	}

	private static void clearDropKeepMaster(int groupIdx) {
		dropKeepMaster.put(groupIdx, false);
	}

	public static boolean needDropKeepMaster(int groupIdx) {
		return dropKeepMaster.getOrDefault(groupIdx, false);
	}
}
