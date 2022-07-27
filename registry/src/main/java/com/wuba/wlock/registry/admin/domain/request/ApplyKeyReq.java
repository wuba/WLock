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


public class ApplyKeyReq {

	@ValidationCheck(allowEmpty = false,filedDescription = "描述")
	private String des;
	@ValidationCheck(allowEmpty = false, allowChineseLanguage = false, filedDescription = "密钥名称")
	private String key;
	@ValidationCheck(allowEmpty = false,filedDescription = "所属组织")
	private String orgId;
	@ValidationCheck(allowEmpty = false,filedDescription = "负责人")
	private String owners;
	@ValidationCheck(allowEmpty = false,filedDescription = "qps")
	private int qps;
	@ValidationCheck(allowEmpty = false,filedDescription = "自动续约")
	private int autoRenew;
	@ValidationCheck(allowEmpty = false,filedDescription = "集群名称")
	private String clusterName;
	
	public String getDes() {
		return des;
	}

	public void setDes(String des) {
		this.des = des;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public String getOwners() {
		return owners;
	}

	public void setOwners(String owners) {
		this.owners = owners;
	}

	public int getQps() {
		return qps;
	}

	public void setQps(int qps) {
		this.qps = qps;
	}

	public int getAutoRenew() {
		return autoRenew;
	}

	public void setAutoRenew(int autoRenew) {
		this.autoRenew = autoRenew;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
}
