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
import com.wuba.wlock.server.migrate.domain.MigrateChangePoint;
import com.wuba.wlock.server.wpaxos.rocksdb.RocksDBHolder;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MigrateChangePointRepository {
    private static final Logger log = LoggerFactory.getLogger(MigrateChangePointRepository.class);

    private static final MigrateChangePointRepository INSTANCE = new MigrateChangePointRepository();

    private static final String MIGRATE_POINT_ = "migrate_point_";

    private static Map<Integer, MigrateChangePoint> migratePointMap = new ConcurrentHashMap<Integer, MigrateChangePoint>();

    public static MigrateChangePointRepository getInstance() {
        return INSTANCE;
    }

    private String getKey(int groupId) {
        return MIGRATE_POINT_ + groupId;
    }

    public void add(MigrateChangePoint migrateChangePoint, int groupId) throws RocksDBException, ProtocolException {
        log.error("MigrateChangePointRepository.add! migrateChangePoint: {}", JSON.toJSONString(migrateChangePoint));
        RocksDBHolder.put(getKey(groupId).getBytes(), migrateChangePoint.toBytes(), groupId);
        migratePointMap.put(groupId, migrateChangePoint);
    }

    public MigrateChangePoint get(int groupIdx) {
        MigrateChangePoint migrateChangePoint = migratePointMap.get(groupIdx);
        return migrateChangePoint;
    }

    public void remove(int groupIdx) throws RocksDBException {
        log.error("MigrateChangePointRepository.remove! groupId: {}", groupIdx);
        RocksDBHolder.delete(getKey(groupIdx).getBytes(), groupIdx);
        migratePointMap.remove(groupIdx);
    }

    public void init() throws RocksDBException, ProtocolException {
        int groupCount = PaxosConfig.getInstance().getGroupCount();
        for (int groupId = 0; groupId < groupCount; groupId++) {
            byte[] bytes = RocksDBHolder.get(getKey(groupId).getBytes(), groupId);
            if (bytes != null && bytes.length > 0) {
                MigrateChangePoint migrateChangePoint = MigrateChangePoint.fromBytes(bytes);
                log.error("MigrateChangePointRepository.init! groupId: {}, migrateChangePoint: {}", groupId, JSON.toJSONString(migrateChangePoint));
                migratePointMap.put(groupId, migrateChangePoint);
            } else {
                log.error("MigrateChangePointRepository.init! groupId: {}, migrateChangePoint is null", groupId);
            }
        }
    }
}
