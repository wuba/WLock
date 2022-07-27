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

import java.util.Objects;

public class ChangeGroupNodeOperateInfoReq extends BaseMigrateReq {
	/**
	 * 新的上线节点id列表
	 */
	@ValidationCheck(allowEmpty = false, filedDescription = "上线节点列表")
	private String nodeIds;

	public String getNodes() {
		return nodeIds;
	}

	public void setNodes(String nodeIds) {
		this.nodeIds = nodeIds;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ChangeGroupNodeOperateInfoReq that = (ChangeGroupNodeOperateInfoReq) o;
		return Objects.equals(nodeIds, that.nodeIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeIds);
	}
}
