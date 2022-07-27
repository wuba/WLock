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


public class GetGroupMigrateConfig extends RegistryProtocol {
    public static final String SEP = ":";

    public GetGroupMigrateConfig(String cluster, String ip, int port, Long version) {
        this.setOpaque(OptionCode.GET_GROUP_MIGRATE_CONFIG);
        Config config = new Config();
        config.setCluster(cluster);
        config.setIp(ip);
        config.setPort(port);
        if (version != null) {
            config.setVersion(version);
        }
        this.setBody(JSONObject.toJSONString(config).getBytes());
    }

    public static class Config {

        private String cluster;
        private String ip;
        private int port;

        private long version = -1;


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
    }
}
