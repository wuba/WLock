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
package com.wuba.wlock.registry.server.service;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.common.entity.PushMessage;
import com.wuba.wlock.common.enums.MigrateEndState;
import com.wuba.wlock.common.registry.protocol.ServerNode;
import com.wuba.wlock.common.registry.protocol.request.GetPaxosConfig;
import com.wuba.wlock.common.registry.protocol.request.UploadGroupMaster;
import com.wuba.wlock.common.registry.protocol.response.GetPaxosConfRes;
import com.wuba.wlock.common.registry.protocol.response.GetRegistryKeyQpsRes;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.server.entity.ServerResult;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.registry.util.RedisUtil;
import com.wuba.wlock.repository.domain.*;
import com.wuba.wlock.repository.enums.MasterLoadBalance;
import com.wuba.wlock.repository.enums.ServerState;
import com.wuba.wlock.repository.enums.UseMasterState;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.wuba.wlock.repository.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class ServerService {
    protected final String SEP = ":";
    protected final Integer EXPIRE_DEFAULT = 3;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    KeyRepository keyRepository;
    @Autowired
    ServerRepository serverRepository;
    @Autowired
    ClusterRepository clusterRepository;
    @Autowired
    GroupNodeRepository groupNodeRepository;
    @Autowired
    GroupServerRefRepository groupServerRefRepository;
    @Autowired
    MigrateRepository migrateRepository;

    public ServerResult<GetRegistryKeyQpsRes> getKeyQps(String cluster, long version) {
        ServerResult<GetRegistryKeyQpsRes> result = new ServerResult<>();
        try {
            long redisVersion = -1;
            if(redisUtil.isUseRedis()) {
                String qpsVersion = redisUtil.getValue(RedisKeyConstant.getClusterQpsVersion(cluster));
                if (StringUtils.isNotEmpty(qpsVersion)) {
                    redisVersion = Long.parseLong(qpsVersion);
                }
            }

            GetRegistryKeyQpsRes getRegistryKeyQpsRes = new GetRegistryKeyQpsRes();
            getRegistryKeyQpsRes.setVersion(redisVersion);
            if (version != -1 && version >= redisVersion) {
                result.setStatus(ServerResult.NO_CHANGE);
                result.setResult(getRegistryKeyQpsRes);
                return result;
            }

            result.setStatus(ServerResult.HAS_CHANGE);
            result.setResult(getRegistryKeyQpsRes);

            List<KeyDO> keyDos = keyRepository.getKeyByClusterId(Environment.env(), cluster);
            if (CollectionUtils.isNotEmpty(keyDos)) {
                Map<String, Integer> qps = new HashMap<>();
                for (KeyDO keyDO : keyDos) {
                    qps.put(keyDO.getHashKey(), keyDO.getQps());
                }
                getRegistryKeyQpsRes.setKeyQpsMap(qps);
            }
        } catch (Exception e) {
            log.error("ServerService.getKeyQps", e);
        }

        return result;
    }


    public ServerResult<GetPaxosConfRes> getPaxosConfig(GetPaxosConfig.ServerConfig getPaxos) throws Exception {
        ServerResult<GetPaxosConfRes> result = new ServerResult<>();
        ServerDO serverDO = serverRepository.getByServer(Environment.env(), getPaxos.getIp() + ":" + getPaxos.getPort());
        if (serverDO == null) {
            throw new Exception(getPaxos.getIp() + ":" + getPaxos.getPort() + " server not exist.");
        }
        
        GetPaxosConfRes res = new GetPaxosConfRes();
        String clusterId = serverDO.getClusterId();
        res.setClusterName(clusterId);
        res.setPaxosServer(serverDO.getServerIp() + GetPaxosConfRes.SEP + serverDO.getPaxosPort());
        if (redisUtil.isUseRedis()) {
            String versionString = redisUtil.getValue(RedisKeyConstant.getPaxosConfigVersion(clusterId));
            if (StringUtils.isNotEmpty(versionString)) {
                int redisVersion = Integer.parseInt(versionString);
                if (redisVersion <= getPaxos.getVersion()) {
                    res.setVersion(redisVersion);
                    result.setResult(res);
                    result.setStatus(ServerResult.NO_CHANGE);
                    return result;
                }
                res.setVersion(redisVersion);
            }
        }
        ClusterDO clusterDO = clusterRepository.getClusterByClusterName(Environment.env(), clusterId);
        res.setGroupCount(clusterDO.getGroupCount());
        List<ServerDO> serverDos = serverRepository.getServerByClusterId(Environment.env(), clusterId);
        Set<Integer> nodeIds = new HashSet<>();
        Map<Integer, ServerNode> nodeMap = new HashMap<>();
        Map<String, Integer> serverToNodeId = new HashMap<>();
        if (CollectionUtils.isNotEmpty(serverDos)) {
            Map<Integer, String> oldNodeMap;
            if (serverDO.getState() != ServerState.online.getValue()) {
                serverDos = new ArrayList<>();
                serverDos.add(serverDO);
            }
            serverDos.forEach(server -> {
                ServerNode serverNode = new ServerNode(server.getSequenceId(), server.getServerIp(), server.getTcpPort(), server.getPaxosPort(), server.getUdpPort());
                nodeMap.put(server.getSequenceId(), serverNode);
                serverToNodeId.put(server.getServerAddr(), server.getSequenceId());
                nodeIds.add(server.getSequenceId());
            });
            oldNodeMap = serverDos.stream().filter(server -> server.getState() == ServerState.online.getValue()).collect(Collectors.toMap(ServerDO::getSequenceId, server -> server.getServerIp() + GetPaxosConfRes.SEP + server.getTcpPort() + GetPaxosConfRes.SEP + server.getPaxosPort() + GetPaxosConfRes.SEP + server.getUdpPort(), (a, b) -> b));
            res.setServerMap(oldNodeMap);
        }
        result.setResult(res);
        result.setStatus(ServerResult.HAS_CHANGE);
        res.setUdpPort(serverDO.getUdpPort());
        List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), GroupNodeDO.GROUP_NORMAL, getPaxos.getIp() + ":" + getPaxos.getPort(), clusterDO.getClusterId());
        Set<Integer> noUseMasterGroups = new HashSet<>();
        Set<Integer> noUseMasterLoadBalanceGroups = new HashSet<>();
        Map<Integer, Set<Integer>> groupNodeMap = new HashMap<>();

        if (groupNodeDos.isEmpty()) {
			/*
					秘钥迁移过程中对于冗余分组不开启 master 负载均衡以及 master 选举
			*/
            IntStream.range(0, clusterDO.getGroupCount() * 2).forEach(i -> {
                if (i >= clusterDO.getGroupCount()) {
                    noUseMasterGroups.add(i);
                    noUseMasterLoadBalanceGroups.add(i);
                }
                groupNodeMap.put(i, nodeIds);
            });
        } else {
            // 拉取到的是每个 group 的节点情况
            assert groupNodeDos.size() == clusterDO.getGroupCount() * 2 : "分组节点数据错误,请确认";
            for (GroupNodeDO groupNodeDO : groupNodeDos) {
                // 不开启 master 选举的分组
                if (groupNodeDO.getUseMaster() == UseMasterState.noUse.getValue()) {
                    noUseMasterGroups.add(groupNodeDO.getGroupId());
                }
            }
            for (GroupNodeDO groupNodeDO : groupNodeDos) {
                // 不开启 master 负载均衡的分组
                if (groupNodeDO.getLoadBalance() == MasterLoadBalance.noUse.getValue()) {
                    noUseMasterLoadBalanceGroups.add(groupNodeDO.getGroupId());
                }
            }
            for (GroupNodeDO groupNodeDO : groupNodeDos) {
                HashSet<Integer> nodes = Sets.newHashSet();
                for (String node : groupNodeDO.getNodes().split(CommonConstant.COMMA)) {
                    if (!serverToNodeId.containsKey(node)) {
                        ServerDO server = serverRepository.getByServer(Environment.env(), node);
                        ServerNode serverNode = new ServerNode(server.getSequenceId(), server.getServerIp(), server.getTcpPort(), server.getPaxosPort(), server.getUdpPort());
                        nodeMap.put(server.getSequenceId(), serverNode);
                        serverToNodeId.put(node, server.getSequenceId());
                    }
                    nodes.add(serverToNodeId.get(node));
                }
                groupNodeMap.put(groupNodeDO.getGroupId(), nodes);
            }
        }
        res.setAllServerMap(nodeMap);
        res.setNoLoadBalanceGroups(noUseMasterLoadBalanceGroups);
        res.setNoUseMasterGroups(noUseMasterGroups);
        assert groupNodeMap.size() == res.getGroupCount() * 2 : "容量变更中的数据要包含所有分组";
        res.setGroupNodeMap(groupNodeMap);
        return result;
    }

    public void handleGroupMaster(UploadGroupMaster.GroupMaster groupMaster) {
        try {
            ServerDO serverDO = serverRepository.getByServer(Environment.env(), groupMaster.getIp() + ":" + groupMaster.getPort());
            if (serverDO == null || serverDO.getState() == ServerState.offline.getValue()) {
                log.warn("current cluster: {} server: {}:{} server not exist or offline", groupMaster.getClusterName(), groupMaster.getIp(), groupMaster.getPort());
                return;
            }
            bindGroupToServer(groupMaster);
        } catch (Exception e) {
            log.error("ServerService.handleGroupMaster error", e);
        }
    }

    private void bindGroupToServer(UploadGroupMaster.GroupMaster groupMaster) throws Exception {
        String clusterId = groupMaster.getClusterName();
        boolean needPushClient = false;
        long clusterVersion = 0L;
        List<GroupServerRefDO> updateList = new ArrayList<GroupServerRefDO>();
        List<GroupServerRefDO> insertList = new ArrayList<GroupServerRefDO>();
        List<GroupServerRefDO> deleteList = new ArrayList<GroupServerRefDO>();
        List<GroupServerRefDO> groupServerRefDOList = null;
        try {
            groupServerRefDOList = groupServerRefRepository.getGroupServerRefByClusterId(Environment.env(), clusterId);

            if (null == groupServerRefDOList) {
                groupServerRefDOList = new ArrayList<GroupServerRefDO>();
            }
            Map<Integer, GroupServerRefDO> groupDoMap = new HashMap<Integer, GroupServerRefDO>();
            for (GroupServerRefDO groupServerRefDO : groupServerRefDOList) {
                groupDoMap.put(groupServerRefDO.getGroupId(), groupServerRefDO);
            }
            List<UploadGroupMaster.GroupMasterVersion> groupMasterVersionList = groupMaster.getGroupMasterVersions();
            if (null == groupMasterVersionList) {
                groupMasterVersionList = new ArrayList<UploadGroupMaster.GroupMasterVersion>();
            }
            List<ServerDO> serverDOList = serverRepository.getServerByClusterIdAndState(Environment.env(), clusterId, ServerState.online.getValue());
            if (null == serverDOList) {
                serverDOList = new ArrayList<ServerDO>();
            }
            Map<String, ServerDO> serverMap = new HashMap<String, ServerDO>();
            for (ServerDO serverDO : serverDOList) {
                if (serverDO.getState() == ServerState.online.getValue()) {
                    serverMap.put(serverDO.getServerIp() + SEP + serverDO.getPaxosPort(), serverDO);
                }
            }
            Map<Integer, Long> groupVersion = new HashMap<>();
            for (UploadGroupMaster.GroupMasterVersion groupMasterVersion : groupMasterVersionList) {
                if (groupMasterVersion.getVersion() <= getRedisVersion(clusterId, groupMasterVersion.getGroup())) {
                    continue;
                }
                groupVersion.put(groupMasterVersion.getGroup(), groupMasterVersion.getVersion());
                GroupServerRefDO groupServerRefDO = groupDoMap.get(groupMasterVersion.getGroup());
                if (!Strings.isNullOrEmpty(groupMasterVersion.getMaster())
                        && !"0.0.0.0:0".equalsIgnoreCase(groupMasterVersion.getMaster())) {
                    ServerDO serverDO = serverMap.get(groupMasterVersion.getMaster());
                    if (null == serverDO) {
                        continue;
                    }
                    if (null == groupServerRefDO) {
                        groupServerRefDO = buildGroupServerRefDO(clusterId, groupMasterVersion.getGroup(),
                                serverDO.getServerAddr(), groupMasterVersion.getVersion());
                        log.info("cluster {} group {} server {} not exist in version {} insert it", clusterId,
                                groupMasterVersion.getGroup(), serverDO.getServerAddr(),
                                groupMasterVersion.getVersion());
                        insertList.add(groupServerRefDO);
                    } else if (!groupServerRefDO.getServerAddr().equals(serverDO.getServerAddr())
                            && groupServerRefDO.getVersion() < groupMasterVersion.getVersion()) {
                        log.info("cluster {} group {} changed master from {} to {} in version {}, update it!",
                                clusterId, groupMasterVersion.getGroup(), groupServerRefDO.getServerAddr(),
                                serverDO.getServerAddr(), groupMasterVersion.getVersion());
                        groupServerRefDO.setServerAddr(serverDO.getServerAddr());
                        groupServerRefDO.setVersion(groupMasterVersion.getVersion());
                        groupServerRefDO.setUpdateTime(new Date());
                        updateList.add(groupServerRefDO);
                    }
                } else {
                    if (null != groupServerRefDO) {
                        if (groupServerRefDO.getVersion() < groupMasterVersion.getVersion()) {
                            log.info("cluster {} group {} has no master in version {} delete it", clusterId,
                                    groupMasterVersion.getGroup(), groupMasterVersion.getVersion());
                            deleteList.add(groupServerRefDO);
                        }
                    }
                }
            }

            if (!updateList.isEmpty()) {
                for (GroupServerRefDO groupServerRefDO : updateList) {
                    groupServerRefRepository.updateGroupServerRef(Environment.env(), groupServerRefDO);
                }
                needPushClient = true;
            }

            if (!insertList.isEmpty()) {
                for (GroupServerRefDO insert : insertList) {
                    groupServerRefRepository.saveGroupServerRef(Environment.env(), insert);
                }
                needPushClient = true;
            }

            if (!deleteList.isEmpty()) {
                List<Long> ids = new ArrayList<Long>();
                for (GroupServerRefDO groupServerRefDO: deleteList) {
                    ids.add(groupServerRefDO.getId());
                }
                groupServerRefRepository.batchDeleteGroupServer(Environment.env(), ids);
                needPushClient = true;
            }
            if (!groupVersion.isEmpty() && redisUtil.isUseRedis()) {
                for (Map.Entry<Integer, Long> entry : groupVersion.entrySet()) {
                    redisUtil.setValueAndExpire(RedisKeyConstant.getGroupVersionKey(clusterId, entry.getKey()), entry.getValue().toString() , EXPIRE_DEFAULT);
                }
            }
            if (needPushClient) {
                clusterVersion = System.currentTimeMillis();
                clusterRepository.updateClusterVersionByClusterName(Environment.env(), clusterVersion, clusterId);
            }
        } catch (Exception e) {
            log.info("cluster {} bind group to master error:", clusterId, e);
        }
        if (needPushClient && redisUtil.isUseRedis()) {
            // 先清理下redis中的配置
            delClusterMasterGroupDistributeFromRedis(clusterId);
            log.info("cluster {} group master changed, push to client!", clusterId);
            PushMessage pushMessage = new PushMessage();
            pushMessage.setCluster(clusterId);
            pushMessage.setVersion(clusterVersion);
            redisUtil.publish(RedisKeyConstant.REDIS_SUBSCRIBE_CHANNEL, JSON.toJSONString(pushMessage));
        }
    }

    private long getRedisVersion(String cluster, int group) {
        if (redisUtil.isUseRedis()) {
            String value = redisUtil.getValue(RedisKeyConstant.getGroupVersionKey(cluster, group));
            if (Strings.isNullOrEmpty(value)) {
                return -1;
            }
            return Long.parseLong(value);
        }
        return -1;
    }

    private GroupServerRefDO buildGroupServerRefDO(String clusterId, int groupId, String serverAddr, long version) {
        GroupServerRefDO groupServerRefDO = new GroupServerRefDO();
        groupServerRefDO.setClusterId(clusterId);
        groupServerRefDO.setGroupId(groupId);
        groupServerRefDO.setServerAddr(serverAddr);
        groupServerRefDO.setVersion(version);
        groupServerRefDO.setCreateTime(new Date());
        groupServerRefDO.setUpdateTime(new Date());
        return groupServerRefDO;
    }

    public void delClusterMasterGroupDistributeFromRedis(String clusterName) {
        if (redisUtil.isUseRedis()) {
            String key = RedisKeyConstant.getClusterMasterGroupDistributeKey(clusterName);
            redisUtil.delKey(key);
        }
    }

    public List<MigrateDO> getMigrateConfigByClusterAndIp(String cluster, String server) {
        try {
            return migrateRepository.searchMigrateByCondition(Environment.env(), cluster, server, -1, MigrateEndState.NoEnd.getValue(), -1L);
        } catch (Exception e) {
            log.error("ServerService.getMigrateConfigByClusterAndIp error", e);
        }
        return Collections.emptyList();
    }

    public MigrateDO getMigrateConfigByClusterAndIpAndGroup(String cluster, String server, Integer group) {
        try {
            List<MigrateDO> result = migrateRepository.searchMigrateByCondition(Environment.env(), cluster, server, group, MigrateEndState.NoEnd.getValue(), -1L);
            if (CollectionUtils.isNotEmpty(result)) {
                return result.get(0);
            }
        } catch (Exception e) {
            log.error("ServerService.getMigrateConfigByClusterAndIpAndGroup", e);
        }
        return null;
    }


    public boolean updateMigrateState(MigrateDO migrateDO) {
        try {
            migrateRepository.updateMigrateStateById(Environment.env(), migrateDO);
            return true;
        } catch (Exception e) {
            log.error("update migrate error", e);
            return false;
        }
    }

    public List<MigrateDO> getMigrateConfigByCluster(String cluster) {
        try {
            return migrateRepository.searchMigrateByCondition(Environment.env(), cluster, "", -1, MigrateEndState.NoEnd.getValue(), -1L);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    public boolean updateGroupNode(List<GroupNodeDO> groupNodes) {
        try {
            groupNodeRepository.batchUpdateById(Environment.env(), groupNodes);
            return true;
        } catch (Exception e) {
            log.error("updateGroupNode error : ", e);
            return false;
        }
    }

    public GroupNodeDO getGroupDOByClusterGroupServer(String cluster, int group, String server) {
        try {
            return groupNodeRepository.searchByCondition(Environment.env(), group, server, cluster).get(0);
        } catch (Exception e) {
            log.error("getGroupDOByClusterGroupServer error : ", e);
            return null;
        }
    }
}
