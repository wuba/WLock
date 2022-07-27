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
import com.wuba.wlock.common.collector.protocol.ServerQps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerLog extends CollectorLog {

    private static final Logger log = LoggerFactory.getLogger("ServerLog");

    public static boolean enable = true;

    public static void write(String nowMinue, ServerQps serverQps) {
        if (mainEnable && enable) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(nowMinue, serverQps.getQps());
            log.error(jsonObject.toJSONString());
        }
    }
}
