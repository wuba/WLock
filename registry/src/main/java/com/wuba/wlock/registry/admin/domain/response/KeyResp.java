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
package com.wuba.wlock.registry.admin.domain.response;


import com.wuba.wlock.registry.admin.validators.ValidationCheck;

public class KeyResp {

	@ValidationCheck(allowEmpty = true, filedDescription = "秘钥Id")
	private String id;
	private String keyName;
	private String hashCode;
	private int qps;
	private int autoRenew;
	private String autoRenewStr;
	@ValidationCheck(allowEmpty = true, filedDescription = "描述")
	private String description;

	public int getQps() {
		return qps;
	}

	public void setQps(int qps) {
		this.qps = qps;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public String getHashCode() {
		return hashCode;
	}

	public void setHashCode(String hashCode) {
		this.hashCode = hashCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getAutoRenew() {
		return autoRenew;
	}

	public void setAutoRenew(int autoRenew) {
		this.autoRenew = autoRenew;
	}

	public String getAutoRenewStr() {
		return autoRenewStr;
	}

	public void setAutoRenewStr(String autoRenewStr) {
		this.autoRenewStr = autoRenewStr;
	}
}
