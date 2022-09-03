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
import com.wuba.wlock.registry.admin.domain.request.ClusterInfoReq;
import com.wuba.wlock.registry.admin.domain.request.QuickInitReq;
import com.wuba.wlock.registry.admin.domain.response.QuickInitResp;
import com.wuba.wlock.registry.admin.service.ClusterService;
import com.wuba.wlock.registry.admin.service.KeyService;
import com.wuba.wlock.registry.admin.service.NodeService;
import com.wuba.wlock.registry.admin.utils.CommonResultUtil;
import com.wuba.wlock.registry.admin.validators.ParamValidateUtil;
import com.wuba.wlock.registry.admin.validators.ValidateResult;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.repository.domain.KeyDO;
import com.wuba.wlock.repository.domain.ServerDO;
import com.wuba.wlock.repository.enums.ServerState;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Api(value = "快速启动接口")
@Slf4j
@RestController
@RequestMapping("/wlock/quick")
public class QuickRest {

	@Autowired
	ClusterService clusterService;
	@Autowired
	NodeService nodeService;
	@Autowired
	KeyService keyService;

	/**
	 * 快速启动接口 ,用于快速创建集群
	 *
	 * @return
	 */
	@ApiOperation("")
	@PostMapping("/init")
	public ActionResult quickInit(@RequestBody QuickInitReq quickInitReq) {
		try {
			ValidateResult valid = ParamValidateUtil.valid(quickInitReq);
			if (!valid.isPass()) {
				return CommonResultUtil.buildResult(CommonResponse.buildFail(ExceptionConstant.PARAMS_EXCEPTION + valid.getErrMsg()));
			}
			if (!clusterService.checkClusterExist(Environment.env(), ClusterInfoReq.DEFAULT_CLUSTER)) {
				clusterService.addCluster(Environment.env(), ClusterInfoReq.getDefaultClusterInfoReq());
			}
			initNode(quickInitReq);
			keyService.applyKey(Environment.env(), ApplyKeyReq.getDefaultApplyKeyReq());
			CompletableFuture<KeyDO> keyInfo = new CompletableFuture<>();
			keyService.getKeyByName(Environment.env(), ApplyKeyReq.DEFAULT_KEY_NAME, keyInfo);
			QuickInitResp quickInitResp = new QuickInitResp();
			quickInitResp.setClusterName(ClusterInfoReq.DEFAULT_CLUSTER);
			quickInitResp.setKeyName(ApplyKeyReq.DEFAULT_KEY_NAME);
			quickInitResp.setKeyHash(keyInfo.get().getHashKey());
			return CommonResultUtil.buildResult(CommonResponse.buildSuccess(quickInitResp));
		} catch (Exception e) {
			return CommonResultUtil.buildResult(e);
		}
	}

	private void initNode(QuickInitReq quickInitReq) throws Exception {
		if (nodeService.checkServerExist(Environment.env(), quickInitReq.getIp(), quickInitReq.getTcpPort())) {
			log.info("service node already exist jump create.");
		} else {
			nodeService.addServer(Environment.env(), quickInitReq.toServerInfoReq());
		}
		CompletableFuture<List<ServerDO>> serverList = nodeService.getServerList(Environment.env(), ClusterInfoReq.DEFAULT_CLUSTER, ServerState.offline);
		CompletableFuture<String> serviceId = new CompletableFuture<>();
		serverList.whenComplete((infos, e) -> {
					if (e == null) {
						Optional<ServerDO> any = infos.stream().findAny();
						if (any.isPresent()) {
							serviceId.complete(String.valueOf(any.get().getId()));
						} else {
							serviceId.completeExceptionally(new RuntimeException("service list is null"));
						}
					}
				}
		);
		nodeService.onlineServers(Environment.env(), ClusterInfoReq.DEFAULT_CLUSTER, serviceId.get());
	}
}
