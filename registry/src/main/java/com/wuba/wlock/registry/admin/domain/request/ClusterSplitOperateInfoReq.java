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

import com.wuba.wlock.registry.admin.constant.ValidationConstant;
import com.wuba.wlock.registry.admin.validators.ValidationCheck;

public class ClusterSplitOperateInfoReq extends BaseMigrateReq {

	@ValidationCheck(allowEmpty = false, maxLength = 30, filedDescription = "源集群名")
	private String sourceCluster;
	@ValidationCheck(allowEmpty = false, maxLength = 30, filedDescription = "目标集群名")
	private String targetCluster;
	@ValidationCheck(allowEmpty = false, filedDescription = "需要处理的节点")
	private String serverIds;
	@ValidationCheck(allowEmpty = true, regexExpression = ValidationConstant.REGEX_NAME, filedDescription = "迁移秘钥信息")
	private String keyName;

	public String getSourceCluster() {
		return sourceCluster;
	}

	public void setSourceCluster(String sourceCluster) {
		this.sourceCluster = sourceCluster;
	}

	public String getTargetCluster() {
		return targetCluster;
	}

	public void setTargetCluster(String targetCluster) {
		this.targetCluster = targetCluster;
	}

	public String getServerIds() {
		return serverIds;
	}

	public void setServerIds(String serverIds) {
		this.serverIds = serverIds;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}
}
