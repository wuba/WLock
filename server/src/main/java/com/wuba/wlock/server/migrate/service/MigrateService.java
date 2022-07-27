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

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.enums.MigrateType;
import com.wuba.wlock.common.registry.protocol.response.GetGroupMigrateConfigRes;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.exception.GroupMetaException;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.migrate.domain.GroupMigrateState;
import com.wuba.wlock.server.migrate.domain.MigrateChangePoint;
import com.wuba.wlock.server.migrate.protocol.MigrateChangePointDO;
import com.wuba.wlock.server.migrate.protocol.MigrateCommandDO;
import com.wuba.wlock.server.migrate.protocol.MigrateSmCtx;
import com.wuba.wlock.server.wpaxos.SMID;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.ProposeResult;
import com.wuba.wpaxos.storemachine.SMCtx;
import com.wuba.wpaxos.utils.JavaOriTypeWrapper;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MigrateService {
    private static final Logger log = LoggerFactory.getLogger(MigrateService.class);

    private static final MigrateService INSTANCE = new MigrateService();

    private MigrateStateService migrateStateService = MigrateStateService.getInstance();

    private WpaxosService wpaxosService =  WpaxosService.getInstance();

    private MigrateChangePointService migrateChangePointService = MigrateChangePointService.getInstance();

    private volatile int[] mappingGroup;
    private volatile boolean[] firstPropose;

    private int[] convertGroup;

    private MigrateService() {
    }

    public static MigrateService getInstance() {
        return INSTANCE;
    }

    public void init() throws RocksDBException, ProtocolException {
        log.info("MigrateService init start.");
        migrateStateService.init();

        int groupCount = PaxosConfig.getInstance().getGroupCount();
        mappingGroup = new int[groupCount];
        for (int groupId = 0; groupId < groupCount; groupId++) {
            mappingGroup[groupId] = groupId;
        }

        firstPropose = new boolean[groupCount];

        migrateChangePointService.init();

        convertGroup = new int[groupCount];
        int halfGroupCount = groupCount/2;
        for (int groupId = 0; groupId < groupCount; groupId++) {
            convertGroup[groupId] = (groupId + halfGroupCount) % groupCount;
        }

        log.info("MigrateService init success.");
    }

    /**
     * 执行迁移指令
     */
    public void execute(GetGroupMigrateConfigRes getGroupMigrateConfigRes) {
        try {
            if(getGroupMigrateConfigRes.getMigrateType() == null) {
                log.error("MigrateService.execute migrateType is null");
                return;
            }

            if (getGroupMigrateConfigRes.getSourceGroups() == null || getGroupMigrateConfigRes.getSourceGroups().isEmpty()) {
                log.error("MigrateService.execute sourceGroups is null");
                return;
            }

            MigrateType migrateType = MigrateType.parse(getGroupMigrateConfigRes.getMigrateType());
	        log.info("migrateType is  : {}", migrateType.getValue());
            if (!isNeedExecute(migrateType)) {
                log.error("MigrateService.execute not need execute migrateType: {}", migrateType);
                return;
            }

            List<Integer> sourceGroups = getGroupMigrateConfigRes.getSourceGroups();
            for (Integer sourceGroupId: sourceGroups) {
                sendCommandPropose(sourceGroupId, getGroupMigrateConfigRes);
            }
        } catch (Exception e) {
            log.error("MigrateManager error", e);
        }
    }

    private void sendCommandPropose(int sourceGroupId, GetGroupMigrateConfigRes getGroupMigrateConfigRes) throws ProtocolException {
        if (wpaxosService.isIMMaster(sourceGroupId)) {
            MigrateCommandDO migrateCommandDO = MigrateCommandDO.fromGetGroupMigrateConfigRes(getGroupMigrateConfigRes, sourceGroupId);
            log.error("MigrateService.execute group[{}] send propose! MigrateCommandDO: {}", sourceGroupId, JSON.toJSONString(migrateCommandDO));
            SMCtx smCtx = new SMCtx(SMID.MIGRATE_COMMAND.getValue(), new MigrateSmCtx());
            ProposeResult result = wpaxosService.propose(migrateCommandDO.toBytes(), sourceGroupId, smCtx);
            MigrateSmCtx migrateSmCtx = (MigrateSmCtx) smCtx.getpCtx();
            log.error("MigrateService.execute group[{}] propose result is: {}, migrateResult: {}", sourceGroupId, result.getResult(), migrateSmCtx.getMigrateResult());
        } else {
            log.error("MigrateService.execute group not master! groupId: {}", sourceGroupId);
        }
    }

    private boolean isNeedExecute(MigrateType migrateType) {
        if (MigrateType.MigratePrepare == migrateType
            || MigrateType.MigratePrepareRollBack == migrateType

            || MigrateType.MigrateGroupStartMoving == migrateType
            || MigrateType.MigrateGroupStartMovingRollBack == migrateType

            || MigrateType.MigrateGroupMovingSafePoint == migrateType
            || MigrateType.MigrateGroupMovingSafePointRollBack == migrateType

            || MigrateType.MigrateEnd == migrateType
        ) {
            return true;
        }

        return false;
    }


    public int mappingGroup(int sourceGroupId, String registryKey, JavaOriTypeWrapper<Integer> firstProposeResult) {
        int groupId = mappingGroup[sourceGroupId];
        if (sourceGroupId != groupId) {
            // source分组迁移到，分组迁移到target分组情况，针对迁移密钥，做流量转发,同步sourceMaxInstanceId到targetGroup
            GroupMigrateState groupMigrateState = migrateStateService.getGroupMigrateState(sourceGroupId);
            if (groupMigrateState != null && registryKey != null && registryKey.equals(groupMigrateState.getRegistryKey())) {
                ProposeResult result = wpaxosService.syncMigrateChangePoint(groupId);
                if (result != null) {
                    firstProposeResult.setValue(result.getResult());
                }
                return groupId;
            }
        } else {
            // 分组变更回滚情况，source分组，需要同步target分组的maxInstanceId
            if (MigrateService.getInstance().isFirstPropose(groupId)) {
                GroupMigrateState groupMigrateState = migrateStateService.getGroupMigrateState(sourceGroupId);
                if (groupMigrateState != null && registryKey != null && registryKey.equals(groupMigrateState.getRegistryKey())) {
                    ProposeResult result = wpaxosService.syncMigrateChangePoint(groupId);
                    if (result != null) {
                        firstProposeResult.setValue(result.getResult());
                    }
                }
            }
        }

        return sourceGroupId;
    }

    /**
     *  迁移时执行完分组变更指令，锁操作分组会从source 转到 target，如果从节点的sourceGroup上数据未同步到最新，即锁状态非最新，
     *  这时targetGroup执行最新的锁操作，会有问题，所以将sourceGroup的maxInstanceId通知给targetGroup，targetGroup进行锁操作时，
     *  会去校验，sourceGroup的数据是否同步到这个maxInstanceId了，来保证锁状态一致
     */
    public synchronized ProposeResult exeFirstPropose(int targetGroupIdx, long maxInstanceId, long groupVersion) throws ProtocolException {
        if (!isFirstPropose(targetGroupIdx)) {
            return null;
        }

        MigrateChangePointDO migrateChangePointDO = MigrateChangePointDO.from(targetGroupIdx, maxInstanceId, groupVersion+1);
        if (migrateChangePointDO != null) {
            byte[] message = migrateChangePointDO.toBytes();

            // maxInstanceId
            SMCtx smCtx = new SMCtx(SMID.MIGRATE_POINT.getValue(), null);
            ProposeResult result = wpaxosService.propose(message, targetGroupIdx, smCtx, 800);
            log.info("MigrateService.exeFirstPropose propose migrateChangePointDO: {}, result: {}", JSON.toJSONString(migrateChangePointDO), result.getResult());
            return result;
        }
        return null;
    }

    public Long getVersion() {
        return migrateStateService.getVersion();
    }

    public GroupMigrateState getGroupMigrateState(int sourceGroupId) {
        return migrateStateService.getGroupMigrateState(sourceGroupId);
    }

    public void saveGroupMirateState(int sourceGroupId, int state, long version, String registerKey) throws RocksDBException, ProtocolException {
        migrateStateService.save(sourceGroupId, state, version, registerKey);
    }

    /**
     *  存在逻辑删除的原因时，分组变更回滚操作时，需要使用到迁移状态里的数据
     */
    public void deleteGroupMirateState(int sourceGroupId, boolean isLogic) throws RocksDBException, ProtocolException {
        if (isLogic) {
            log.info("MigrateService.deleteGroupMirateState logic del! groupId: {}", sourceGroupId);
            GroupMigrateState groupMigrateState = migrateStateService.getGroupMigrateState(sourceGroupId);
            if (groupMigrateState != null) {
                groupMigrateState.setState(-1);
                migrateStateService.save(groupMigrateState);
            }
        } else {
            log.info("MigrateService.deleteGroupMirateState physical del! groupId: {}", sourceGroupId);
            migrateStateService.delete(sourceGroupId);
        }
    }


    public void changeGroup(int sourceGroupId) {
        int targetGroupId = convertGroupId(sourceGroupId);
        mappingGroup[sourceGroupId] = targetGroupId;
        firstPropose[targetGroupId] = true;
        log.error("MigrateService.changeGroup mappingGroup sourceGroupId: {} to targetGroupId: {}, targetGroupId set firstPropose=true", sourceGroupId, targetGroupId);
    }

    public void changeGroupRollback(int sourceGroupId) throws RocksDBException {
        int targetGroupId = convertGroupId(sourceGroupId);

        migrateChangePointService.delete(targetGroupId);
        mappingGroup[sourceGroupId] = sourceGroupId;
        firstPropose[sourceGroupId] = true;
        firstPropose[targetGroupId] = false;

        log.error("MigrateService.changeGroupRollback groupId: {}", sourceGroupId);
    }

    public boolean isFirstPropose(int groupIdx) {
        boolean isFirst = firstPropose[groupIdx];
        return isFirst;
    }

    public int convertGroupId(int groupIdx) {
        return convertGroup[groupIdx];
    }

    public void setMigratePoint(MigrateChangePointDO migrateChangePointDO) throws RocksDBException, GroupMetaException, ProtocolException {
        migrateChangePointService.setMigratePoint(migrateChangePointDO.getTargetGroupId(), migrateChangePointDO.getGroupVersion(), migrateChangePointDO.getSourceGroupMaxInstanceId());
        firstPropose[migrateChangePointDO.getTargetGroupId()] = false;
        log.error("MigrateService.setMigratePoint targetGroupId: {} set firstPropose=false", migrateChangePointDO.getTargetGroupId());
    }

    /**
     *  校验是否同步到maxInstanceId
     */
    public boolean isSyncToMigratePoint(int groupIdx) {
        // 需要等wpaxos初始化完成后才能操作
        if (!WpaxosService.getInstance().isInit()) {
            return true;
        }

        MigrateChangePoint migrateChangePoint = migrateChangePointService.get(groupIdx);
        if (migrateChangePoint == null) {
            return true;
        }

        int sourceGroupId = convertGroupId(groupIdx);
        long sourceGroupMaxInstanceId = migrateChangePoint.getSourceGroupMaxInstanceId();
        long instanceId = WpaxosService.getInstance().getNowInstanceId(sourceGroupId);
        if (instanceId >= sourceGroupMaxInstanceId) {
            try {
                // 数据同步完成后，把迁移点删除
                migrateChangePointService.delete(groupIdx);
            } catch (RocksDBException e) {
                log.error("MigrateService.isSyncToMigratePoint migrate point delete error", e);
            }
            return true;
        }
        return false;
    }

    /**
     * 变更到安全点
     * 开启master选举
     */
    public void changeSafetypoint(int sourceGroupId) {
        int targetGroupId = convertGroupId(sourceGroupId);
        // 需要等wpaxos初始化完成后才能操作
        if (wpaxosService.isInit()) {
            if (wpaxosService.isIMMaster(sourceGroupId)) {
                wpaxosService.setMasterElectionPriority(targetGroupId, 0);
            } else {
                wpaxosService.setMasterElectionPriority(targetGroupId, 30000);
            }
        } else {
            log.warn("MigrateService.changeSafetypoint wpaxosService not init");
        }
    }

    public void changeSafetypointRollback(int sourceGroupId) throws RocksDBException {
        int targetGroupId = convertGroupId(sourceGroupId);
        // 需要等wpaxos初始化完成后才能操作
        if (wpaxosService.isInit()) {
            wpaxosService.disableMasterElection(targetGroupId);
            wpaxosService.setMasterElectionPriority(targetGroupId, 0);
        } else {
            log.warn("MigrateService.changeSafetypointRollback wpaxosService not init");
        }

        changeGroupRollback(sourceGroupId);
    }

    /**
     * 清空state
     */
    public void migrateEnd(int sourceGroupId) throws RocksDBException, ProtocolException {
        // 清除 变更点数据
        migrateChangePointService.delete(sourceGroupId);

        // 清除 state
        deleteGroupMirateState(sourceGroupId, false);

        int targetGroupId = convertGroupId(sourceGroupId);
        mappingGroup[sourceGroupId] = sourceGroupId;
        firstPropose[sourceGroupId] = false;
        firstPropose[targetGroupId] = false;

        // 需要等wpaxos初始化完成后才能操作
        if (wpaxosService.isInit()) {
            wpaxosService.setMasterElectionPriority(targetGroupId, 0);
        }
    }
}
