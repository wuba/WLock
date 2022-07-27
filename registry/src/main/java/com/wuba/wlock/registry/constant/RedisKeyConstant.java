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
package com.wuba.wlock.registry.constant;

public final class RedisKeyConstant {

	public static final String REDIS_SUBSCRIBE_CHANNEL = "wlock_message_push";

	private static final String PAXOS_CONFIG_VERSION = "paxos:config:version:clustername:%s";

	private static final String CLIENT_CLUSTER_CONFIG = "client:cluster:config:%s";

	private static final String GROUP_MASTER_LOCK = "cluster:group:master:lock:%s";

	private static final String CLUSTER_MASTER_GROUP_DISTRIBUTE = "cluster:master:group:distribute:%s";

	private static final String HASHKEY_CLINTCONFONFO_MAPPING = "hashkey:client:conf:info:mapping:%s";

	private static final String CLUSTER_QPS_VERSION = "cluster_qps_cluster_%s";

	private static final String GROUP_MASTER_VERSION = "cluster:%s:group:%s";

	public static String getGroupVersionKey(String cluster, int group) {
		return String.format(GROUP_MASTER_VERSION, cluster, group);
	}

	public static String getClusterQpsVersion(String clusterName) {
		return String.format(CLUSTER_QPS_VERSION, clusterName);
	}

	public static String getClusterGroupMasterLockKey(String clusterId) {
		return String.format(GROUP_MASTER_LOCK, clusterId);
	}

	public static String getPaxosConfigVersion(String clusterName) {
		return String.format(PAXOS_CONFIG_VERSION, clusterName);
	}

	public static String getClientClusterConfigKey(String hashKey) {
		return String.format(CLIENT_CLUSTER_CONFIG, hashKey);
	}

	public static String getClusterMasterGroupDistributeKey(String clusterName) {
		return String.format(CLUSTER_MASTER_GROUP_DISTRIBUTE, clusterName);
	}

	public static String getClientConfInfoMappingKey(String hashKey) {
		return String.format(HASHKEY_CLINTCONFONFO_MAPPING, hashKey);
	}

}
