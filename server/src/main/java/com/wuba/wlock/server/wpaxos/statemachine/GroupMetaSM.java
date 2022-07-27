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
package com.wuba.wlock.server.wpaxos.statemachine;

import com.wuba.wlock.server.domain.GroupMeta;
import com.wuba.wlock.server.service.GroupMetaService;
import com.wuba.wpaxos.storemachine.SMCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupMetaSM extends AbstractStateMachine{
    private static final Logger log = LoggerFactory.getLogger(GroupMetaSM.class);

    public GroupMetaSM(int groupIdx, int smID, boolean needCheckpoint) {
        super(groupIdx, smID, needCheckpoint);
    }

    @Override
    public boolean execute(int groupIdx, long instanceID, byte[] paxosValue, SMCtx smCtx) {
        try {
            GroupMeta groupMeta = GroupMeta.fromBytes(paxosValue);
            GroupMetaService.getInstance().save(groupMeta.getGroupVersion(), groupMeta.getGroupId());
            return true;
        } catch (Exception e) {
            log.error("GroupMetaSM error", e);
        }

        return false;
    }

    @Override
    public byte[] beforePropose(int groupIdx, byte[] sValue) {
        return new byte[0];
    }

    @Override
    public boolean needCallBeforePropose() {
        return false;
    }
}
