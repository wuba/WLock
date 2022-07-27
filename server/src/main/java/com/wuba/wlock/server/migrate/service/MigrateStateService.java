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
package com.wuba.wlock.server.migrate.service;

import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.migrate.domain.GroupMigrateState;
import com.wuba.wlock.server.migrate.repository.MigrateStateRepository;
import org.rocksdb.RocksDBException;

import java.util.Map;

public class MigrateStateService {
    private static MigrateStateService INSTANCE = new MigrateStateService();

    private volatile long version;


    MigrateStateRepository migrateStateRepository = MigrateStateRepository.getInstance();

    public static MigrateStateService getInstance() {
        return INSTANCE;
    }


    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public GroupMigrateState getGroupMigrateState(int sourceGroupId) {
        return migrateStateRepository.get(sourceGroupId);
    }

    public void save(int sourceGroupId, int state, long version, String registerKey) throws RocksDBException, ProtocolException {
        GroupMigrateState groupMigrateState = new GroupMigrateState(sourceGroupId, state, version, registerKey);
        save(groupMigrateState);
    }

    public void save(GroupMigrateState groupMigrateState) throws RocksDBException, ProtocolException {
        migrateStateRepository.add(groupMigrateState, groupMigrateState.getSourceGroupId());
        this.version = groupMigrateState.getVersion();
    }

    public void delete(int sourceGroupId) throws RocksDBException {
        migrateStateRepository.remove(sourceGroupId);
        this.version = -1;
    }

    public void init() throws RocksDBException, ProtocolException {
        version = migrateStateRepository.init();
    }

    public Map<Integer, GroupMigrateState> allGroupMigrateState() {
        return migrateStateRepository.getGroupMigrateStateMap();
    }
}
