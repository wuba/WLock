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
package com.wuba.wlock.server.migrate.domain;

import com.wuba.wlock.common.util.ByteConverter;
import com.wuba.wlock.server.exception.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GroupMigrateState {
    private int sourceGroupId;
    private int state;
    private long version;
    private String registryKey;
    private byte registerKeyLen = 0;

    public GroupMigrateState() {
    }

    public GroupMigrateState(int sourceGroupId, int state, long version, String registryKey) {
        this.sourceGroupId = sourceGroupId;
        this.state = state;
        this.version = version;
        this.setRegistryKey(registryKey);
    }


    public int getState() {
        return state;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
        if (registryKey != null) {
            this.registerKeyLen = (byte) registryKey.length();
        }
    }

    public byte getRegisterKeyLen() {
        return registerKeyLen;
    }

    public void setRegisterKeyLen(byte registerKeyLen) {
        this.registerKeyLen = registerKeyLen;
    }

    public int getSourceGroupId() {
        return sourceGroupId;
    }

    public void setSourceGroupId(int sourceGroupId) {
        this.sourceGroupId = sourceGroupId;
    }

    public void setState(int state) {
        this.state = state;
    }

    public byte[] toBytes() throws ProtocolException {

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            stream.write(ByteConverter.intToBytesLittleEndian(this.getState()));
            stream.write(ByteConverter.intToBytesLittleEndian(this.getSourceGroupId()));
            stream.write(ByteConverter.longToBytesLittleEndian(this.getVersion()));

            stream.write(this.getRegisterKeyLen());
            if (this.registryKey != null) {
                stream.write(this.registryKey.getBytes());
            }

            return stream.toByteArray();
        } catch(IOException e) {
            throw new ProtocolException(e);
        }
    }

    public static GroupMigrateState fromBytes(byte[] buf) {
        GroupMigrateState groupMigrateState = new GroupMigrateState();
        int index = 0;

        int migrateType = ByteConverter.bytesToIntLittleEndian(buf, index);
        groupMigrateState.setState(migrateType);
        index += 4;

        int sourceGroupId = ByteConverter.bytesToIntLittleEndian(buf, index);
        groupMigrateState.setSourceGroupId(sourceGroupId);
        index += 4;

        long version = ByteConverter.bytesToLongLittleEndian(buf, index);
        groupMigrateState.setVersion(version);
        index += 8;

        byte registerKeyLen = buf[index];
        groupMigrateState.setRegisterKeyLen(registerKeyLen);
        index += 1;

        if (registerKeyLen > 0) {
            byte[] keyBuf = new byte[registerKeyLen];
            System.arraycopy(buf, index, keyBuf, 0, registerKeyLen);
            String registerKey = new String(keyBuf);
            groupMigrateState.setRegistryKey(registerKey);
        }

        return groupMigrateState;
    }

    public boolean isDel() {
        return state == -1;
    }
}
