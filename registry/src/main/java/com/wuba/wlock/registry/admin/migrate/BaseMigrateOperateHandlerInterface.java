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
package com.wuba.wlock.registry.admin.migrate;
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.domain.request.MigrateRequestParseFactory;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.util.RedisUtil;
import com.wuba.wlock.repository.repository.*;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseMigrateOperateHandlerInterface {
	@Autowired
	RedisUtil redisUtil;

	@Autowired
    MigrateRepository migrateRepository;
	@Autowired
    ClusterRepository clusterRepository;
	@Autowired
    KeyRepository keyRepository;
	@Autowired
	GroupNodeRepository groupNodeRepository;
	@Autowired
    MigrateProcessRepository migrateProcessRepository;

	@Autowired
    ServerRepository serverRepository;
	@Autowired
	GroupServerRefRepository groupServerRefRepository;

	MigrateRequestParseFactory migrateRequestParseFactory = MigrateRequestParseFactory.getInstance();

	public abstract void operate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException;

	public abstract void rollback(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException;

	public abstract void checkOperate(MigrateControlInfoReq migrateControlInfoReq) throws ServiceException;

}
