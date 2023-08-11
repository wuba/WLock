package com.wuba.wlock.server.wpaxos.statemachine;

import com.wuba.wpaxos.storemachine.SMCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 用于做一些初始化操作，比如 生成 gid
 * @author huguocai
 */
public class NullStateMachine extends AbstractStateMachine {
    private static final Logger log = LoggerFactory.getLogger(NullStateMachine.class);

    public NullStateMachine(int groupIdx, int smID, boolean needCheckpoint) {
        super(groupIdx, smID, needCheckpoint);
    }

    @Override
    public boolean execute(int groupIdx, long instanceID, byte[] paxosValue, SMCtx smCtx) {
        log.info("execute NullStateMachine groupId: {}, instanceID: {}", groupIdx, instanceID);
        return true;
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
