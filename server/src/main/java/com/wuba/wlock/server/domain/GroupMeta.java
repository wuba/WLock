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
package com.wuba.wlock.server.domain;

import com.wuba.wlock.common.util.ByteConverter;
import com.wuba.wlock.server.exception.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class GroupMeta {
    private int groupId;

    private long groupVersion;

    public GroupMeta() {
    }

    public GroupMeta(long groupVersion, int groupId) {
        this.groupVersion = groupVersion;
        this.groupId = groupId;
    }


    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public long getGroupVersion() {
        return groupVersion;
    }

    public void setGroupVersion(long groupVersion) {
        this.groupVersion = groupVersion;
    }

    public byte[] toBytes() throws ProtocolException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            stream.write(ByteConverter.intToBytesLittleEndian(this.getGroupId()));
            stream.write(ByteConverter.longToBytesLittleEndian(this.getGroupVersion()));
            return stream.toByteArray();
        } catch(IOException e) {
            throw new ProtocolException(e);
        }
    }

    public static GroupMeta fromBytes(byte[] buf) {
        int index = 0;
        int groupId = ByteConverter.bytesToIntLittleEndian(buf, index);
        index+=4;

        long groupVersion = ByteConverter.bytesToLongLittleEndian(buf, index);
        return new GroupMeta(groupVersion, groupId);
    }


    public static GroupMeta fromByteBuffer(ByteBuffer byteBuffer) {
        byte[] buf = new byte[12];
        byteBuffer.get(buf);

        return fromBytes(buf);
    }
}
