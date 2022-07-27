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
package com.wuba.wlock.server.migrate.protocol;

import com.wuba.wlock.common.util.ByteConverter;
import com.wuba.wlock.server.exception.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MigrateChangePointDO {
    private int targetGroupId;

    private long sourceGroupMaxInstanceId;

    private long groupVersion;

    public int getTargetGroupId() {
        return targetGroupId;
    }

    public void setTargetGroupId(int targetGroupId) {
        this.targetGroupId = targetGroupId;
    }

    public long getSourceGroupMaxInstanceId() {
        return sourceGroupMaxInstanceId;
    }

    public void setSourceGroupMaxInstanceId(long sourceGroupMaxInstanceId) {
        this.sourceGroupMaxInstanceId = sourceGroupMaxInstanceId;
    }

    public long getGroupVersion() {
        return groupVersion;
    }

    public void setGroupVersion(long groupVersion) {
        this.groupVersion = groupVersion;
    }

    public static MigrateChangePointDO from(int targetGroupIdx, long maxInstanceId, long groupVersion) {
        MigrateChangePointDO migrateChangePointDO = new MigrateChangePointDO();
        migrateChangePointDO.targetGroupId = targetGroupIdx;
        migrateChangePointDO.sourceGroupMaxInstanceId = maxInstanceId;
        migrateChangePointDO.groupVersion = groupVersion;
        return migrateChangePointDO;
    }


    public static MigrateChangePointDO fromBytes(byte[] buf) {
        MigrateChangePointDO migrateChangePointDO = new MigrateChangePointDO();
        int index = 0;
        int targetGroupId = ByteConverter.bytesToIntLittleEndian(buf, index);
        migrateChangePointDO.setTargetGroupId(targetGroupId);
        index += 4;

        long sourceGroupMaxInstanceId = ByteConverter.bytesToLongLittleEndian(buf, index);
        migrateChangePointDO.setSourceGroupMaxInstanceId(sourceGroupMaxInstanceId);
        index += 8;

        long groupVersion = ByteConverter.bytesToLongLittleEndian(buf, index);
        migrateChangePointDO.setGroupVersion(groupVersion);

        return migrateChangePointDO;
    }

    public byte[] toBytes() throws ProtocolException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            stream.write(ByteConverter.intToBytesLittleEndian(this.getTargetGroupId()));
            stream.write(ByteConverter.longToBytesLittleEndian(this.getSourceGroupMaxInstanceId()));
            stream.write(ByteConverter.longToBytesLittleEndian(this.getGroupVersion()));
            return stream.toByteArray();
        } catch(IOException e) {
            throw new ProtocolException(e);
        }
    }


}
