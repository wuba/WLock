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
package com.wuba.wlock.server.repository;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.domain.GroupMeta;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.wpaxos.rocksdb.RocksDBHolder;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupMetaRepository {
    private static final Logger log = LoggerFactory.getLogger(GroupMetaRepository.class);

    private static final GroupMetaRepository INSTANCE = new GroupMetaRepository();

    private static final String GROUP_META_KEY = "group_meta_";

    private static Map<Integer, GroupMeta> groupMetaMap = new ConcurrentHashMap<Integer, GroupMeta>();

    public static GroupMetaRepository getInstance() {
        return INSTANCE;
    }

    private String getKey(int groupId) {
        return GROUP_META_KEY + groupId;
    }

    public void add(GroupMeta groupMeta, int groupId) throws RocksDBException, ProtocolException {
        log.error("GroupMetaRepository.add! groupMeta: {}", JSON.toJSONString(groupMeta));
        RocksDBHolder.put(getKey(groupId).getBytes(), groupMeta.toBytes(), groupId);
        groupMetaMap.put(groupId, groupMeta);
    }

    public GroupMeta get(int groupId) {
        GroupMeta groupMeta = groupMetaMap.get(groupId);
        return groupMeta;
    }

    public void init() throws RocksDBException {
        int groupCount = PaxosConfig.getInstance().getGroupCount();
        for (int groupId = 0; groupId < groupCount; groupId++) {
            byte[] bytes = RocksDBHolder.get(getKey(groupId).getBytes(), groupId);
            if (bytes != null && bytes.length > 0) {
                GroupMeta groupMeta = GroupMeta.fromBytes(bytes);
                log.error("GroupMetaRepository.init! groupId: {}, groupMeta: {}", groupId, JSON.toJSONString(groupMeta));
                groupMetaMap.put(groupId, groupMeta);
            } else {
                log.error("GroupMetaRepository.init! groupId: {}, groupMeta is null", groupId);
            }

        }
    }
}
