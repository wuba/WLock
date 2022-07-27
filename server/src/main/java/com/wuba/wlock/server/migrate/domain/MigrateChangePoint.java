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

public class MigrateChangePoint {
    private int targetGroupId;

    private long sourceGroupMaxInstanceId;

    public MigrateChangePoint() {
    }

    public MigrateChangePoint(int targetGroupId, long sourceGroupMaxInstanceId) {
        this.targetGroupId = targetGroupId;
        this.sourceGroupMaxInstanceId = sourceGroupMaxInstanceId;
    }


    public long getSourceGroupMaxInstanceId() {
        return sourceGroupMaxInstanceId;
    }

    public void setSourceGroupMaxInstanceId(long sourceGroupMaxInstanceId) {
        this.sourceGroupMaxInstanceId = sourceGroupMaxInstanceId;
    }

    public int getTargetGroupId() {
        return targetGroupId;
    }

    public void setTargetGroupId(int targetGroupId) {
        this.targetGroupId = targetGroupId;
    }

    public byte[] toBytes() throws ProtocolException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            stream.write(ByteConverter.intToBytesLittleEndian(this.getTargetGroupId()));
            stream.write(ByteConverter.longToBytesLittleEndian(this.getSourceGroupMaxInstanceId()));
            return stream.toByteArray();
        } catch(IOException e) {
            throw new ProtocolException(e);
        }
    }

    public static MigrateChangePoint fromBytes(byte[] bytes) {
        MigrateChangePoint migrateChangePoint = new MigrateChangePoint();
        int index = 0;
        int targetGroupId = ByteConverter.bytesToIntLittleEndian(bytes, index);
        migrateChangePoint.setTargetGroupId(targetGroupId);
        index += 4;

        long sourceGroupMaxInstanceId = ByteConverter.bytesToLongLittleEndian(bytes, index);
        migrateChangePoint.setSourceGroupMaxInstanceId(sourceGroupMaxInstanceId);
        return migrateChangePoint;
    }
}
