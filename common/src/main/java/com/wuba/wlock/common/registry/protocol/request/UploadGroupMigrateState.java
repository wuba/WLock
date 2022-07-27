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
package com.wuba.wlock.common.registry.protocol.request;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;


public class UploadGroupMigrateState extends RegistryProtocol {
    public static final String SEP = ":";

    public UploadGroupMigrateState(String cluster, String ip, int port, Long version, Integer groupId, Integer sta) {
        this.setOpaque(OptionCode.UPLOAD_MIGRATE_STATE);
        State state = new State();
        state.setCluster(cluster);
        state.setIp(ip);
        state.setPort(port);
        if (version != null) {
            state.setVersion(version);
        }
        state.setGroupId(groupId);
        state.setState(sta);
        this.setBody(JSONObject.toJSONString(state).getBytes());
    }

    public static class State {

        private String cluster;
        private String ip;
        private int port;

        private long version = -1;

        private Integer groupId;

        /**
         * 迁移类型
         */
        private Integer state;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getCluster() {
            return cluster;
        }

        public void setCluster(String cluster) {
            this.cluster = cluster;
        }

        public long getVersion() {
            return version;
        }

        public void setVersion(long version) {
            this.version = version;
        }

        public Integer getGroupId() {
            return groupId;
        }

        public void setGroupId(Integer groupId) {
            this.groupId = groupId;
        }

        public Integer getState() {
            return state;
        }

        public void setState(Integer state) {
            this.state = state;
        }
    }
}
