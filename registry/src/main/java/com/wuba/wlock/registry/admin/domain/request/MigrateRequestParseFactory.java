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
package com.wuba.wlock.registry.admin.domain.request;

import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class MigrateRequestParseFactory {

	private static final MigrateRequestParseFactory INSTANCE = new MigrateRequestParseFactory();

	private MigrateRequestParseFactory() {
	}

	public static MigrateRequestParseFactory getInstance() {
		return INSTANCE;
	}

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public <T extends BaseMigrateReq> T parse(String info, Class<T> valueType) throws ServiceException {
		try {
			return OBJECT_MAPPER.readValue(info, valueType);
		} catch (IOException e) {
			throw new ServiceException("协议解析错误");
		}
	}

	public String parseToString(Object info) throws ServiceException {
		try {
			return OBJECT_MAPPER.writeValueAsString(info);
		} catch (IOException e) {
			throw new ServiceException("协议转换字符串错误");
		}
	}
}
