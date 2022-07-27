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
package com.wuba.wlock.server.lock.protocol;

public enum LockCodeEnum {
    Reentrant_Lock,
    Read_Lock,
    Write_lock
    ;

    public static LockCodeEnum getBy(byte lockType, byte opcode) {
        if (lockType == LockTypeEnum.reentrantLock.getValue()) {
            return Reentrant_Lock;
        }

        if (lockType == LockTypeEnum.readWriteReentrantLock.getValue()) {
            if (opcode == OpcodeEnum.ReadWriteOpcode.READ.getValue()) {
                return Read_Lock;
            }

            if (opcode == OpcodeEnum.ReadWriteOpcode.WRITE.getValue()) {
                return Write_lock;
            }
        }

        return null;
    }
}
