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
package com.wuba.wlock.server.collector.log;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.collector.protocol.GroupQps;
import com.wuba.wlock.common.collector.protocol.QpsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class GroupLog extends CollectorLog {

    private static final Logger log = LoggerFactory.getLogger("GroupLog");

    public static boolean enable = true;

    public static void write(String nowMinue, GroupQps groupQps) {
        if (mainEnable && enable) {
            Map<Integer, QpsEntity> qps = groupQps.getQps();
            if (qps != null && !qps.isEmpty()) {
                List<Integer> groupIds = new ArrayList<Integer>(qps.keySet());
                Collections.sort(groupIds);
                for (Integer groupId: groupIds) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(nowMinue+"-" + groupId, qps.get(groupId));
                    log.error(jsonObject.toJSONString());
                }
            }
        }
    }
}
