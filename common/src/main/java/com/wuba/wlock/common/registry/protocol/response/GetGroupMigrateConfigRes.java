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
package com.wuba.wlock.common.registry.protocol.response;

import java.util.List;

public class GetGroupMigrateConfigRes {
    /**
     * MigrateType  迁移指令
     */
    private Integer migrateType;

    private String registerKey;

    private List<Integer> sourceGroups;

    private Long version;

    public Integer getMigrateType() {
        return migrateType;
    }

    public void setMigrateType(Integer migrateType) {
        this.migrateType = migrateType;
    }

    public String getRegisterKey() {
        return registerKey;
    }

    public void setRegisterKey(String registerKey) {
        this.registerKey = registerKey;
    }

    public List<Integer> getSourceGroups() {
        return sourceGroups;
    }

    public void setSourceGroups(List<Integer> sourceGroups) {
        this.sourceGroups = sourceGroups;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "GetGroupMigrateConfigRes{" +
                "migrateType=" + migrateType +
                ", registerKey='" + registerKey + '\'' +
                ", sourceGroups=" + sourceGroups +
                ", version=" + version +
                '}';
    }
}
