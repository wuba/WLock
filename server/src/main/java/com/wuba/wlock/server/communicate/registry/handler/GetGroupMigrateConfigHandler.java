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
package com.wuba.wlock.server.communicate.registry.handler;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.GetGroupMigrateConfig;
import com.wuba.wlock.common.registry.protocol.response.GetGroupMigrateConfigRes;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.migrate.service.MigrateService;
import com.wuba.wlock.server.exception.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetGroupMigrateConfigHandler extends AbstractPaxosHandler implements IPaxosHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetGroupMigrateConfigHandler.class);

    @Override
    protected boolean checkProtocol(byte opaque) {
        if (opaque != OptionCode.RES_GROUP_MIGRATE_CONFIG) {
            logger.error("GetGroupMigrateConfigHandler response opaque error {}.", opaque);
            return false;
        }
        return true;
    }

    @Override
    public boolean doSuccess(RegistryProtocol registryProtocol) throws Exception {
        GetGroupMigrateConfigRes getGroupMigrateConfigRes = JSONObject.parseObject(registryProtocol.getBody(), GetGroupMigrateConfigRes.class);
        logger.info("GetGroupMigrateConfigHandler getGroupMigrateConfigRes {}", JSONObject.toJSONString(getGroupMigrateConfigRes));

        MigrateService.getInstance().execute(getGroupMigrateConfigRes);

        return true;
    }

    @Override
    public boolean doError(RegistryProtocol registryProtocol) {
        logger.error("GetGroupMigrateConfigHandler doError.");
        return false;
    }

    @Override
    public boolean doElse(RegistryProtocol registryProtocol) {
        logger.error("GetGroupMigrateConfigHandler doElse.");
        return false;
    }

    @Override
    public RegistryProtocol buildMessage() throws ConfigException {
        return new GetGroupMigrateConfig(ServerConfig.getInstance().getCluster(), ServerConfig.getInstance().getServerListenIP(),
                ServerConfig.getInstance().getServerListenPort(), MigrateService.getInstance().getVersion());
    }

    @Override
    public boolean handleResponse(RegistryProtocol registryProtocol) throws Exception {
        return super.doHandler(registryProtocol);
    }
}
