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
import lombok.Data;

@Data
public class ApplyKeyReq {

	public static final String DEFAULT_KEY_NAME = "default_key";

	@ValidationCheck(allowEmpty = false, filedDescription = "描述")
	private String des;
	@ValidationCheck(allowEmpty = false, allowChineseLanguage = false, filedDescription = "密钥名称")
	private String key;
	@ValidationCheck(allowEmpty = false, filedDescription = "qps")
	private int qps;
	@ValidationCheck(allowEmpty = false, filedDescription = "自动续约")
	private int autoRenew;
	@ValidationCheck(allowEmpty = false, filedDescription = "集群名称")
	private String clusterName;

	public static ApplyKeyReq getDefaultApplyKeyReq() {
		ApplyKeyReq applyKeyReq = new ApplyKeyReq();
		applyKeyReq.setDes("default");
		applyKeyReq.setKey(DEFAULT_KEY_NAME);
		applyKeyReq.setQps(100);
		applyKeyReq.setAutoRenew(0);
		applyKeyReq.setClusterName(ClusterInfoReq.DEFAULT_CLUSTER);
		return applyKeyReq;
	}
}
