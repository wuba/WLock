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

import java.util.List;
import java.util.Map;

public interface GroupMasterStrategy {
	/**
	 * 获取负载均衡后的mastergroup分布
	 *
	 * @param groupCounts group数量
	 * @param nodeInfos   成员列表
	 * @return group->nodeid
	 */
	Map<Integer, Long> getBalancedGroupMaster(int groupCounts, Map<Integer, NodeInfo> nodeInfos, Map<Integer, List<Integer>> groupNodeMap);
}
