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

public class ClusterInfoReq {

	@ValidationCheck(allowEmpty = false, regexExpression = ValidationConstant.REGEX_NAME, maxLength = 30, filedDescription = "集群名")
	private String clusterName;
	
	@ValidationCheck(allowEmpty = false, minValue = "1", filedDescription = "分组数")
	private int groupCount;

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public int getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}
	
}
