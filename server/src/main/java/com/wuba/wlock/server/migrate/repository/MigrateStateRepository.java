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
package com.wuba.wlock.server.migrate.repository;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.migrate.domain.GroupMigrateState;
import com.wuba.wlock.server.wpaxos.rocksdb.RocksDBHolder;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MigrateStateRepository {
    private static final Logger log = LoggerFactory.getLogger(MigrateStateRepository.class);

    private static final MigrateStateRepository INSTANCE = new MigrateStateRepository();

    private Map<Integer, GroupMigrateState> groupMigrateStateMap = new ConcurrentHashMap<Integer, GroupMigrateState>();

    private static final String MIGRATE_STATE_KEY = "migrate_state_";

    public static MigrateStateRepository getInstance() {
        return INSTANCE;
    }

    private String getKey(int groupId) {
        return MIGRATE_STATE_KEY + groupId;
    }

    public void add(GroupMigrateState groupMigrateState, int groupId) throws RocksDBException, ProtocolException {
        log.error("MigrateStateRepository.add! groupMigrateState: {}", JSON.toJSONString(groupMigrateState));
        String key = getKey(groupId);
        byte[] value = groupMigrateState.toBytes();
        RocksDBHolder.put(key.getBytes(), value, groupId);

        groupMigrateStateMap.put(groupId, groupMigrateState);
    }

    public void remove(int groupId) throws RocksDBException {
        log.error("MigrateStateRepository.remove! groupId: {}", groupId);
        RocksDBHolder.delete(getKey(groupId).getBytes(), groupId);
        groupMigrateStateMap.remove(groupId);
    }

    public GroupMigrateState get(int sourceGroupId) {
        GroupMigrateState groupMigrateState = groupMigrateStateMap.get(sourceGroupId);
        return groupMigrateState;
    }

    public Map<Integer, GroupMigrateState> getGroupMigrateStateMap() {
        return groupMigrateStateMap;
    }

    public long init() throws RocksDBException, ProtocolException {
        long groupVersion = -1;
        int groupCount = PaxosConfig.getInstance().getGroupCount();
        for (int groupId = 0; groupId < groupCount; groupId++) {
            byte[] bytes = RocksDBHolder.get(getKey(groupId).getBytes(), groupId);
            if (bytes != null && bytes.length > 0) {
                GroupMigrateState groupMigrateState = GroupMigrateState.fromBytes(bytes);
                log.error("MigrateStateRepository.init! groupId: {}, groupMigrateState: {}", groupId, JSON.toJSONString(groupMigrateState));
                groupMigrateStateMap.put(groupId, groupMigrateState);
                groupVersion = groupMigrateState.getVersion();
            } else {
                log.error("MigrateStateRepository.init! groupId: {}, groupMigrateState is null", groupId);
            }
        }

        return groupVersion;
    }
}
