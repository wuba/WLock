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
import com.wuba.wlock.server.migrate.domain.MigrateChangePoint;
import com.wuba.wlock.server.migrate.repository.MigrateChangePointRepository;
import com.wuba.wlock.server.service.GroupMetaService;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateChangePointService {
    private static final Logger log = LoggerFactory.getLogger(MigrateChangePointService.class);

    private static MigrateChangePointService INSTANCE = new MigrateChangePointService();

    private MigrateChangePointRepository migrateChangePointRepository = MigrateChangePointRepository.getInstance();
    private GroupMetaService groupMetaService;

    public static MigrateChangePointService getInstance() {
        return INSTANCE;
    }

    public void init() throws RocksDBException, ProtocolException {
        migrateChangePointRepository.init();
        groupMetaService = GroupMetaService.getInstance();
    }

    public void setMigratePoint(int targetGroupId, long groupVersion, long sourceGroupMaxInstanceId) throws RocksDBException, ProtocolException {
        groupMetaService.save(groupVersion, targetGroupId);

        int sourceGroupId = MigrateService.getInstance().convertGroupId(targetGroupId);
        if (MigrateService.getInstance().getGroupMigrateState(sourceGroupId) != null) {
            MigrateChangePoint migrateChangePoint = new MigrateChangePoint(targetGroupId, sourceGroupMaxInstanceId);
            migrateChangePointRepository.add(migrateChangePoint, targetGroupId);
        } else {
            log.info("MigrateChangePointService.setMigratePoint group[{}] not MigrateState", sourceGroupId);
        }
    }

    public MigrateChangePoint get(int groupIdx) {
        return migrateChangePointRepository.get(groupIdx);
    }

    public void delete(int groupIdx) throws RocksDBException {
        migrateChangePointRepository.remove(groupIdx);
    }


}
