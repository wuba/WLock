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
package com.wuba.wlock.registry.admin.rest;


import com.wuba.wlock.registry.admin.constant.ExceptionConstant;
import com.wuba.wlock.registry.admin.domain.ActionResult;
import com.wuba.wlock.registry.admin.domain.CommonResponse;
import com.wuba.wlock.registry.admin.domain.request.ApplyKeyReq;
import com.wuba.wlock.registry.admin.domain.request.KeyInfoReq;
import com.wuba.wlock.registry.admin.domain.response.KeyResp;
import com.wuba.wlock.registry.admin.service.KeyService;
import com.wuba.wlock.registry.admin.utils.CommonResultUtil;
import com.wuba.wlock.registry.admin.validators.ParamValidateUtil;
import com.wuba.wlock.registry.admin.validators.ValidateResult;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.repository.helper.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "密钥管理接口")
@RestController
@RequestMapping("/wlock/key")
public class KeyRest {
	@Autowired
	KeyService keyService;

	@ApiOperation("")
	@PostMapping("/list")
	public ActionResult getKeyList(KeyInfoReq keyInfoReq) {
		try {
			ValidateResult valid = ParamValidateUtil.valid(keyInfoReq);
			if (!valid.isPass()) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
			}
			Page<KeyResp> page = keyService.getKeyList(Environment.env(), keyInfoReq);
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(page));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	@ApiOperation("")
	@PostMapping("/delete")
	public ActionResult deleteKey(long keyId) {
		try {
			if (keyId <= 0) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION));
			}
			keyService.deleteKey(Environment.env(), keyId);
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
		return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.DELETE_SUCCESS));
	}

	@ApiOperation("")
	@PostMapping("/update")
	public ActionResult updateKey(KeyResp keyResp) {
		try {
			keyService.updateKey(Environment.env(), keyResp);
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
		return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.UPDATE_SUCCESS));
	}

	@ApiOperation("")
	@PostMapping("/add")
	public ActionResult addKey(ApplyKeyReq applyKeyReq) {
		try {
			keyService.applyKey(Environment.env(), applyKeyReq);
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
		return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.ADD_SUCCESS));
	}


}
