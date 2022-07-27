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
package com.wuba.wlock.server.keepmaster;

import com.wuba.wpaxos.comm.NodeInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashStrategy implements GroupMasterStrategy {
	@Override
	public Map<Integer, Long> getBalancedGroupMaster(int groupCounts, Map<Integer, NodeInfo> nodeInfos, Map<Integer, List<Integer>> groupNodeMap) {
		Map<Integer, Long> result = new HashMap<>(groupCounts, 1);
		if (nodeInfos == null || nodeInfos.isEmpty()) {
			return new HashMap<>(0);
		}
		for (int i = 0; i < groupCounts; i++) {
			int pos = i % groupNodeMap.get(i).size();
			Integer integer = groupNodeMap.get(i).get(pos);
			result.put(i, nodeInfos.get(integer).getNodeID());
		}
		return result;
	}
}
