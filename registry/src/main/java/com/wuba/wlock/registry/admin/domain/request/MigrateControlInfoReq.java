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


import com.wuba.wlock.registry.admin.validators.ValidationCheck;


public class MigrateControlInfoReq {
	@ValidationCheck(allowEmpty = false, filedDescription = "迁移过程版本")
	private Long version;
	@ValidationCheck(allowEmpty = false, filedDescription = "迁移过程期望状态")
	private int processState;
	@ValidationCheck(allowEmpty = false, filedDescription = "秘钥迁移期望状态")
	private int migrateState;
	@ValidationCheck(allowEmpty = true, filedDescription = "操作信息")
	private String operateInfo;

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public int getProcessState() {
		return processState;
	}

	public void setProcessState(int processState) {
		this.processState = processState;
	}

	public int getMigrateState() {
		return migrateState;
	}

	public void setMigrateState(int migrateState) {
		this.migrateState = migrateState;
	}

	public String getOperateInfo() {
		return operateInfo;
	}

	public void setOperateInfo(String operateInfo) {
		this.operateInfo = operateInfo;
	}
}
