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

import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.request.UploadGroupMigrateState;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.exception.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadGroupMigrateStateHandler extends AbstractPaxosHandler implements IPaxosHandler  {
    private static final Logger logger = LoggerFactory.getLogger(UploadGroupMigrateStateHandler.class);

    private Long version;
    private Integer groupId;
    private Integer state;

    public UploadGroupMigrateStateHandler(Long version, Integer groupId, Integer state) {
        this.version = version;
        this.groupId = groupId;
        this.state = state;
    }

    @Override
    protected boolean checkProtocol(byte opaque) {
        if (opaque != OptionCode.RES_UPLOAD_MIGRATE_STATE) {
            logger.error("UploadGroupMigrateStateHandler response opaque error {}.", opaque);
            return false;
        }
        return true;
    }

    @Override
    public boolean doSuccess(RegistryProtocol registryProtocol) throws Exception {
        logger.error("UploadGroupMigrateStateHandler doSuccess");
        return true;
    }

    @Override
    public boolean doError(RegistryProtocol registryProtocol) {
        logger.error("UploadGroupMigrateStateHandler doError");
        return false;
    }

    @Override
    public boolean doElse(RegistryProtocol registryProtocol) {
        return false;
    }

    @Override
    public RegistryProtocol buildMessage() throws ConfigException {
        return new UploadGroupMigrateState(ServerConfig.getInstance().getCluster(), ServerConfig.getInstance().getServerListenIP(),
                ServerConfig.getInstance().getServerListenPort(), version, groupId, state);
    }

    @Override
    public boolean handleResponse(RegistryProtocol registryProtocol) throws Exception {
        return super.doHandler(registryProtocol);
    }
}
