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
package com.wuba.wlock.server.service;

import com.wuba.wlock.server.domain.GroupMeta;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.repository.GroupMetaRepository;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupMetaService {
    private static final Logger log = LoggerFactory.getLogger(GroupMetaService.class);

    private static GroupMetaService INSTANCE = new GroupMetaService();

    private GroupMetaRepository groupMetaRepository = GroupMetaRepository.getInstance();

    public static GroupMetaService getInstance() {
        return INSTANCE;
    }


    public void save(long groupVersion, int groupId) throws RocksDBException, ProtocolException {
        GroupMeta groupMeta = groupMetaRepository.get(groupId);
        if (groupMeta == null) {
            groupMetaRepository.add(new GroupMeta(groupVersion, groupId), groupId);
            return;
        }

        if (groupMeta != null && groupMeta.getGroupVersion() >= groupVersion) {
            log.error("GroupMetaService.save nowGroupVersion: {} >= groupVersion: {}", groupMeta.getGroupVersion(), groupVersion);
            return;
        }

        groupMeta.setGroupVersion(groupVersion);
        groupMetaRepository.add(groupMeta, groupId);
    }

    public long getGroupVersion(int groupId) {
        GroupMeta groupMeta = groupMetaRepository.get(groupId);
        if (groupMeta == null) {
            return 0;
        }

        return groupMeta.getGroupVersion();
    }

    public void init() throws RocksDBException {
        log.error("GroupMetaService init start.");
        groupMetaRepository.init();
        log.error("GroupMetaService init success.");
    }

    public GroupMeta get(int groupIdx) {
        GroupMeta groupMeta = groupMetaRepository.get(groupIdx);
        if (groupMeta == null) {
            return new GroupMeta(0, groupIdx);
        }
        return groupMeta;
    }

    public void save(GroupMeta groupMeta) throws RocksDBException, ProtocolException {
        save(groupMeta.getGroupVersion(), groupMeta.getGroupId());
    }
}
