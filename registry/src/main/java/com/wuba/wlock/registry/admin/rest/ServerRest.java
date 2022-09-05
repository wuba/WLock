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
import com.wuba.wlock.registry.admin.domain.request.ListInfoReq;
import com.wuba.wlock.registry.admin.domain.request.ServerInfoReq;
import com.wuba.wlock.registry.admin.domain.response.ServerResp;
import com.wuba.wlock.registry.admin.enums.ServerQueryType;
import com.wuba.wlock.registry.admin.service.NodeService;
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

@Api(value = "服务管理接口")
@RestController
@RequestMapping("/wlock/server")
public class ServerRest {

	@Autowired
	NodeService nodeService;

	@ApiOperation("")
	@PostMapping("/list")
	public ActionResult getServerList(ListInfoReq serverListInfoReq) {
		try {
			ValidateResult valid = ParamValidateUtil.valid(serverListInfoReq);
			if (!valid.isPass()) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
			}
			Page<ServerResp> page = nodeService.getServerList(Environment.env(), serverListInfoReq, ServerQueryType.QUERY);
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(page));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	@ApiOperation("")
	@PostMapping("/delete")
	public ActionResult deleteServer(long id) {
		try {
			if (id <= 0) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION));
			}
			nodeService.deleteServer(Environment.env(), id);
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
		return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.DELETE_SUCCESS));
	}

	@ApiOperation("")
	@PostMapping("/add")
	public ActionResult addServer(ServerInfoReq serverInfoReq) {
		try {
			ValidateResult valid = ParamValidateUtil.valid(serverInfoReq);
			if (!valid.isPass()) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
			}
			nodeService.addServer(Environment.env(), serverInfoReq);
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
		return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.ADD_SUCCESS));
	}

	@ApiOperation("")
	@PostMapping("online")
	public ActionResult onlineServers(String clusterName, String serverIdList) {
		try {
			if (null == clusterName || clusterName.isEmpty() || null == serverIdList || serverIdList.isEmpty()) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION));
			}
			nodeService.onlineServers(Environment.env(), clusterName, serverIdList);
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
		return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.ONLINE_SUCCESS));
	}

	@ApiOperation("")
	@PostMapping("offline")
	public ActionResult offlineServers(String clusterName, String serverIdList) {
		try {
			if (null == clusterName || clusterName.isEmpty() || null == serverIdList || serverIdList.isEmpty()) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION));
			}
			nodeService.offlineServers(Environment.env(), clusterName, serverIdList);
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
		return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.OFFLINE_SUCCESS));
	}


}
