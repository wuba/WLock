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
import com.wuba.wlock.registry.admin.domain.request.ClusterInfoReq;
import com.wuba.wlock.registry.admin.domain.CommonResponse;
import com.wuba.wlock.registry.admin.domain.request.ListInfoReq;
import com.wuba.wlock.registry.admin.domain.response.ClusterResp;
import com.wuba.wlock.registry.admin.service.ClusterService;
import com.wuba.wlock.registry.admin.utils.CommonResultUtil;
import com.wuba.wlock.registry.admin.validators.ParamValidateUtil;
import com.wuba.wlock.registry.admin.validators.ValidateResult;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.repository.helper.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "集群管理接口")
@RestController
@RequestMapping("/wlock/cluster")
public class ClusterRest {
    @Autowired
    ClusterService clusterService;

    @ApiOperation("")
    @PostMapping("add")
    public ActionResult addCluster(@RequestBody ClusterInfoReq clusterInfoReq) {
        try {
            ValidateResult valid = ParamValidateUtil.valid(clusterInfoReq);
            if (!valid.isPass()) {
                return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
            }
            clusterService.addCluster(Environment.env(), clusterInfoReq);
        } catch (Exception e) {
            return CommonResultUtil.buildResult(e);
        }
        return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.ADD_SUCCESS));
    }

    @ApiOperation("")
    @PostMapping("delete")
    public ActionResult deleteCluster(long id) {
        try {
            if (id <= 0) {
                return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION));
            }
            clusterService.deleteCluster(Environment.env(), id);
        } catch (Exception e) {
            return CommonResultUtil.buildResult(e);
        }
        return CommonResultUtil.buildResult(CommonResponse.buildSuccess(ExceptionConstant.DELETE_SUCCESS));
    }

    @ApiOperation("")
    @PostMapping("list")
    public ActionResult getClusterList(ListInfoReq clusterListInfoReq) {
        try {
            ValidateResult valid = ParamValidateUtil.valid(clusterListInfoReq);
            if (!valid.isPass()) {
                return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
            }
            Page<ClusterResp> page = clusterService.getClusterList(Environment.env(), clusterListInfoReq);
            return CommonResultUtil.buildResult(CommonResponse.buildSuccess(page));
        } catch (Exception e) {
            return CommonResultUtil.buildResult(e);
        }
    }
}
