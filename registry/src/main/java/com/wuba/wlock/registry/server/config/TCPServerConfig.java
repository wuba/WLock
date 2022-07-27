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
package com.wuba.wlock.registry.server.config;

import com.wuba.wlock.registry.server.util.ConversionUtil;
import com.wuba.wlock.registry.util.Validator;

public class TCPServerConfig extends ServerConfig {
	private static final String WORK_COUNT_KEY = "workerCount";
	private static final String PORT_KEY = "port";

	private int workerCount = Runtime.getRuntime().availableProcessors();
	private int port = 22020;
	
	@Override
	public boolean setOption(String key, Object value) {
		if (WORK_COUNT_KEY.equals(key)) {
			setWorkerCount((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : this.workerCount);
		} else if (PORT_KEY.equals(key)) {
			super.setOption(key, ((Validator.notNullAndEmpty(value))? ConversionUtil.toInt(value) : this.port));
		} else {
			super.setOption(key, value);
		}
		return true;
	}
	
	public int getWorkerCount() {
		return workerCount;
	}

	public void setWorkerCount(int workerCount) {
		this.workerCount = workerCount;
	}
	
}
