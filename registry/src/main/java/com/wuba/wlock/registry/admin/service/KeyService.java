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
package com.wuba.wlock.registry.admin.service;

import com.google.common.base.Strings;
import com.wuba.wlock.registry.admin.constant.ExceptionConstant;
import com.wuba.wlock.registry.admin.constant.KeyConfig;
import com.wuba.wlock.registry.admin.domain.request.ApplyKeyReq;
import com.wuba.wlock.registry.admin.domain.request.KeyInfoReq;
import com.wuba.wlock.registry.admin.domain.request.KeyUpdateReq;
import com.wuba.wlock.registry.admin.domain.response.KeyResp;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;
import com.wuba.wlock.registry.util.MD5;
import com.wuba.wlock.registry.admin.utils.GroupUtil;
import com.wuba.wlock.repository.domain.ClusterDO;
import com.wuba.wlock.repository.domain.KeyDO;
import com.wuba.wlock.repository.enums.MultiGroup;
import com.wuba.wlock.repository.helper.Page;
import com.wuba.wlock.repository.repository.ClusterRepository;
import com.wuba.wlock.repository.repository.KeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.APPLY_KEY_ERROR;
import static com.wuba.wlock.registry.admin.constant.ExceptionConstant.KEY_EXIST;

@Service
@Slf4j
public class KeyService {
	@Autowired
    KeyRepository keyRepository;
	@Autowired
    ClusterRepository clusterRepository;

	public void getKeyByName(String env, String keyName, CompletableFuture<KeyDO> keyInfo) throws ServiceException {
		CompletableFuture.runAsync(() -> {
			if (Strings.isNullOrEmpty(keyName)) {
				keyInfo.completeExceptionally(new RuntimeException("key name is null."));
			}
			try {
				KeyDO keyByName = keyRepository.getKeyByName(env, keyName);
				keyInfo.complete(keyByName);
			} catch (Exception e) {
				log.error("search key info error.", e);
				keyInfo.completeExceptionally(e);
			}
		});
	}

	public Page<KeyResp> getKeyList(String env, KeyInfoReq keyInfoReq) throws ServiceException {
		if (keyInfoReq == null) {
			return null;
		}

		StringBuilder condition = new StringBuilder();
		if (keyInfoReq != null && StringUtils.isNotBlank(keyInfoReq.getKeyName())) {
			condition.append(" where ").append(" name like ").append("'%").append(keyInfoReq.getKeyName().trim()).append("%'");
		}

		Page<KeyDO> keyPage = null;
		List<KeyResp> adminKeyResp = new ArrayList<KeyResp>();
		try {
			keyPage = keyRepository.getKeyByCondition(env, condition.toString(), keyInfoReq.getPageNumber(), keyInfoReq.getPageSize());
		} catch (Exception e) {
			log.info("getKeyList error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		List<KeyDO> keyDOList = keyPage.getPageList();
		if (null == keyDOList || keyDOList.size() < 1) {
			return new Page<KeyResp>(keyPage.getPageInfo(), adminKeyResp);
		}
		KeyResp keyResp = null;
		for (KeyDO keyDO : keyDOList) {
			keyResp = new KeyResp();
			keyResp.setId(String.valueOf(keyDO.getId()));
			keyResp.setKeyName(keyDO.getName());
			keyResp.setHashCode(keyDO.getHashKey());
			keyResp.setQps(keyDO.getQps());
			keyResp.setAutoRenew(keyDO.getAutoRenew());
			if (keyDO.getAutoRenew() == 0) {
				keyResp.setAutoRenewStr(KeyConfig.NO_AUTO_RENEW);
			} else {
				keyResp.setAutoRenewStr(KeyConfig.AUTO_RENEW);
			}
			keyResp.setDescription(keyDO.getDescription());
			adminKeyResp.add(keyResp);
		}
		return new Page<KeyResp>(keyPage.getPageInfo(), adminKeyResp);
	}

	public void deleteKey(String env, long id) throws ServiceException {
		KeyDO keyDO = null;
		boolean isOwner = false;
		try {
			keyDO = keyRepository.getKeyById(env, id);
		} catch (Exception e) {
			log.info("deleteKey error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (null == keyDO) {
			throw new ServiceException(ExceptionConstant.KEY_NOT_EXIST);
		} else {
			String keyName = keyDO.getName();
			KeyDO onlineKeyDO = null;
			try {
				onlineKeyDO = keyRepository.getKeyByName(env, keyName);
			} catch (Exception e) {
				log.info("deleteKey error", e);
				throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
			}
			if (null != onlineKeyDO) {
				throw new ServiceException(ExceptionConstant.ONLINE_KEY_EXIST);
			}
		}

		String clusterName = keyDO.getClusterId();
		int qps = keyDO.getQps();
		ClusterDO clusterDO = null;
		try {
			clusterDO = clusterRepository.getClusterByClusterName(env, clusterName);
		} catch (Exception e) {
			log.info("deleteKey error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		int clusterQps = clusterDO.getQps();
		clusterQps = (clusterQps - qps) > 0 ? (clusterQps - qps) : 0;
		try {
			clusterRepository.updateClusterQpsByClusterName(env, clusterQps, clusterName);
		} catch (Exception e) {
			log.info("deleteKey error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		try {
			keyRepository.deleteKeyById(env, id);
		} catch (Exception e) {
			log.info("deleteKey error", e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}


	public boolean updateKey(String env, KeyUpdateReq keyUpdateReq) throws ServiceException {
		boolean isOwner = false;
		long keyId = Long.parseLong(keyUpdateReq.getId());
		KeyDO keyDO = null;
		try {
			keyDO = keyRepository.getKeyById(env, keyId);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
		if (null == keyDO) {
			throw new ServiceException(ExceptionConstant.KEY_NOT_EXIST);
		}

		try {
			if (keyUpdateReq.getDescription() != null) {
				keyDO.setDescription(keyUpdateReq.getDescription());
			}

			if (keyUpdateReq.getAutoRenew() != null) {
				keyDO.setAutoRenew(keyUpdateReq.getAutoRenew());
			}

			if (keyUpdateReq.getQps() != null) {
				keyDO.setQps(keyUpdateReq.getQps());
			}
			return keyRepository.updateKeyDO(env, keyDO);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ServiceException(ExceptionConstant.SERVER_EXCEPTION);
		}
	}

	public void applyKey(String env, ApplyKeyReq applyKeyReq) throws ServiceException {
		String keyName = applyKeyReq.getKey();
		KeyDO keyDO;
		try {
			keyDO = keyRepository.getKeyByName(env, keyName);
		} catch (Exception e) {
			log.error("查询密钥失败:{}", keyName, e);
			throw new ServiceException(APPLY_KEY_ERROR);
		}
		if (keyDO != null) {
			throw new ServiceException(KEY_EXIST);
		}

		keyDO = new KeyDO();
		keyDO.setName(keyName);
		keyDO.setHashKey(MD5.encodeByMd5(keyName));
		String clusterName = applyKeyReq.getClusterName();

		ClusterDO clusterDO;
		try {
			clusterDO = clusterRepository.getClusterByClusterName(env, clusterName);
		} catch (Exception e) {
			log.error("查询集群失败", e);
			throw new ServiceException(APPLY_KEY_ERROR);
		}
		if (clusterDO == null) {
			log.error("集群不存在,{}", clusterName);
			throw new ServiceException(APPLY_KEY_ERROR);
		}
		try {
			clusterRepository.updateQps(env, clusterDO.getQps() + applyKeyReq.getQps(), clusterDO.getId());
		} catch (Exception e) {
			log.error("更新集群qps失败:{}", clusterName, e);
			throw new ServiceException(APPLY_KEY_ERROR);
		}

		keyDO.setGroupId(GroupUtil.getGroupByKey(keyName, clusterDO.getGroupCount()));
		keyDO.setGroupIds(makeGroupIds(clusterDO.getGroupCount()));
		keyDO.setClusterId(clusterName);
		keyDO.setDescription(applyKeyReq.getDes());
		keyDO.setQps(applyKeyReq.getQps());
		keyDO.setAutoRenew(applyKeyReq.getAutoRenew());
		keyDO.setCreateTime(new Date());
		keyDO.setMultiGroup(MultiGroup.Use.getValue());
		try {
			keyRepository.saveKey(env, keyDO);
		} catch (Exception e) {
			log.error("保存测试环境密钥失败:{}", keyName, e);
			throw new ServiceException(APPLY_KEY_ERROR);
		}
	}

	protected String makeGroupIds(int groupCount) {
		return IntStream.range(0, groupCount)
				.mapToObj(String::valueOf)
				.collect(Collectors.joining(","));
	}
}
