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
import com.wuba.wlock.registry.admin.domain.request.MigrateControlInfoReq;
import com.wuba.wlock.registry.admin.domain.request.MigrateKeyInfoReq;
import com.wuba.wlock.registry.admin.service.MigrateService;
import com.wuba.wlock.registry.admin.utils.CommonResultUtil;
import com.wuba.wlock.registry.admin.validators.ParamValidateUtil;
import com.wuba.wlock.registry.admin.validators.ValidateResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "密钥迁移管理接口")
@RestController
@RequestMapping("/wlock/migrate")
public class MigrateRest {

	@Autowired
    MigrateService migrateService;


	/**
	 * 开始迁移接口
	 * @param migrateKeyInfoReq
	 * @return
	 */
	@ApiOperation("")
	@PostMapping("/migrateProcessStart")
	public ActionResult migrateProcessStart(MigrateKeyInfoReq migrateKeyInfoReq) {
		ValidateResult valid = ParamValidateUtil.valid(migrateKeyInfoReq);
		if (!valid.isPass()) {
			return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
		}
		try {
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(migrateService.migrateProcessStart(migrateKeyInfoReq)));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}


	/**
	 * 秘钥迁移列表查询接口 :
	 * @param version
	 * @return
	 */
	@ApiOperation("")
	@GetMapping("/migrateList")
	public ActionResult migrateList(long version) {
		if (version <= 0) {
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.PARAMS_EXCEPTION));
		}
		try {
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(migrateService.migrateList(version)));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	@ApiOperation("")
	@PostMapping("/operate")
	public ActionResult migrateOperate(MigrateControlInfoReq migrateControlInfoReq) {
		ValidateResult valid = ParamValidateUtil.valid(migrateControlInfoReq);
		if (!valid.isPass()) {
			return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
		}
		try {
			migrateService.migrateOperate(migrateControlInfoReq);
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.OPERATE_SUCCESS));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}


	/**
	 * 回滚接口
	 * @param migrateControlInfoReq
	 * @return
	 */
	@ApiOperation("")
	@PostMapping("/rollback")
	public ActionResult migrateRollback(MigrateControlInfoReq migrateControlInfoReq) {
		ValidateResult valid = ParamValidateUtil.valid(migrateControlInfoReq);
		if (!valid.isPass()) {
			return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
		}
		try {
			migrateService.rollback(migrateControlInfoReq);
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.ROLLBACK_SUCCESS));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	/**
	 * 接口 拉取 group node 信息 , 用于进行 group 变更
	 * @param version
	 * @return
	 */
	@ApiOperation("")
	@PostMapping("/groupNodeInfo")
	public ActionResult groupNodeInfo(Long version) {
		try {
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(migrateService.groupNodeInfo(version)));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	@ApiOperation("")
	@PostMapping("/mandatoryEnd")
	public ActionResult mandatoryEnd(Long version) {
		try {
			migrateService.mandatoryEnd(version);
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.OPERATE_SUCCESS));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	@ApiOperation("")
	@PostMapping("/clusterGroupIdsSearch")
	public ActionResult clusterGroupIdsSearch(String clusterName) {
		try {
			if (Strings.isEmpty(clusterName)) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + "集群名不能为空"));
			}
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(migrateService.clusterGroupIdsSearch(clusterName)));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	@ApiOperation("")
	@GetMapping("/currentMigrateVersion")
	public ActionResult searchMigrateVersion() {
		try {
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(migrateService.searchMigrateVersion()));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	@ApiOperation("")
	@PostMapping("/migrateOfflineNodeList")
	public ActionResult migrateOfflineNodeList(Long version) {
		try {
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(migrateService.migrateOfflineNodeList(version)));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

}
