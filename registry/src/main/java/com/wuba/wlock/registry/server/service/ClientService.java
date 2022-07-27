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
import com.wuba.wlock.common.entity.ClientKeyEntity;
import com.wuba.wlock.common.entity.GroupNode;
import com.wuba.wlock.common.entity.Node;
import com.wuba.wlock.common.entity.NodeAddr;
import com.wuba.wlock.registry.config.Environment;
import com.wuba.wlock.registry.constant.CommonConstant;
import com.wuba.wlock.registry.server.entity.ClientConfInfo;
import com.wuba.wlock.registry.server.entity.ClusterMasterGroupDistribute;
import com.wuba.wlock.registry.server.entity.ServerResult;
import com.wuba.wlock.registry.constant.RedisKeyConstant;
import com.wuba.wlock.registry.util.RedisUtil;
import com.wuba.wlock.registry.util.Validator;
import com.wuba.wlock.repository.domain.*;
import com.wuba.wlock.repository.enums.ServerState;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.wuba.wlock.repository.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class ClientService {
    protected final int REDIS_EXPIRE_THREE = 3;

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

    public ClientKeyEntity getClientKeyEntity(String hashKey) throws Exception{
        Validator.keyValidate(hashKey);

        ClientConfInfo clientConfInfo = this.getClientConfInfoByMapping(hashKey, false);
        ClusterMasterGroupDistribute clusterConf = this.getClusterMasterGroupDistribute(clientConfInfo.getClusterName(), false);
        ClientKeyEntity resClientKey = this.getClientKeyEntityFromClusterConf(hashKey, clusterConf, clientConfInfo);
        return resClientKey;
    }


    public ClientConfInfo getClientConfInfoByMapping(String hashKey, boolean useRedis) throws Exception {
        if (useRedis) {
            ClientConfInfo clientConfInfo = getClientConfInfoFromRedis(hashKey);
            if (clientConfInfo != null) {
                return clientConfInfo;
            }
        }

        KeyDO keyDO = keyRepository.getKeyByHashKey(Environment.env(), hashKey);
        if (null == keyDO) {
            throw new Exception("keyDO is null! key: " + hashKey);
        }

        ClientConfInfo clientConfInfo = new ClientConfInfo();
        clientConfInfo.setClusterName(keyDO.getClusterId());
        clientConfInfo.setGroupId(keyDO.getGroupId());
        clientConfInfo.setGroupIds(keyDO.getGroupIds());
        clientConfInfo.setAutoRenew(keyDO.getAutoRenew() != 0);
        clientConfInfo.setMultiGroup(keyDO.getMultiGroup() != 0);

        setClientConfInfoToRedis(hashKey, clientConfInfo);
        return clientConfInfo;
    }

    public void setClientConfInfoToRedis(String hashKey, ClientConfInfo clientConfInfo) {
        try {
            String key = RedisKeyConstant.getClientConfInfoMappingKey(hashKey);
            redisUtil.setValueAndExpire(key, JSON.toJSONString(clientConfInfo), REDIS_EXPIRE_THREE);
        } catch (Exception e) {
            log.error("ClientService.setClientConfInfoToRedis error", e);
        }
    }

    public ClientConfInfo getClientConfInfoFromRedis(String hashKey) {
        try {
            String key = RedisKeyConstant.getClientConfInfoMappingKey(hashKey);
            String value = redisUtil.getValue(key);
            if (StringUtils.isNotEmpty(value)) {
                return JSON.parseObject(value, ClientConfInfo.class);
            }
        } catch (Exception e) {
            log.error("getClientConfInfoByMapping error", e);
        }

        return null;
    }

    public ClientKeyEntity getClientKeyEntityFromClusterConf(String hashKey, ClusterMasterGroupDistribute clusterConf, ClientConfInfo clientConfInfo) throws Exception {
        if (clusterConf == null) {
            log.error("get client config error : cluster config is null");
            return new ClientKeyEntity();
        }

        List<ServerDO> serverDOList = clusterConf.getServerList();
        if (CollectionUtils.isEmpty(serverDOList)) {
            return new ClientKeyEntity();
        }

        int groupId = clientConfInfo.getGroupId();
        if (!clientConfInfo.isMultiGroup() && groupId < 0 || groupId >= clusterConf.getGroupCount() * CommonConstant.TWO) {
            log.error("hashKey: {} groupId distribute Exception, groupId: {} cluster: {}", hashKey, groupId, clientConfInfo.getClusterName());
            return new ClientKeyEntity();
        }

        List<NodeAddr> nodeList = makeNodeList(clusterConf, clientConfInfo, groupId);

        ClientKeyEntity clientKeyEntity = new ClientKeyEntity();
        clientKeyEntity.setKey(hashKey);
        clientKeyEntity.setHashCode(clusterConf.getHashCode());
        clientKeyEntity.setVersion(clusterConf.getVersion());
        clientKeyEntity.setGroupId(clientConfInfo.getGroupId());
        clientKeyEntity.setAutoRenew(clientConfInfo.getAutoRenew());
        clientKeyEntity.setNodeList(nodeList);
        clientKeyEntity.setMultiGroup(clientConfInfo.isMultiGroup());
        if (clientConfInfo.isMultiGroup()) {
            List<GroupNode> groupNodeList = makeGroupNodeList(clusterConf, clientConfInfo);
            Map<Integer, Node> allNodeMap = makeAllNodeMap(clusterConf);
            clientKeyEntity.setAllNodeMap(allNodeMap);
            clientKeyEntity.setGroupNodeList(groupNodeList);
        }
        return clientKeyEntity;
    }

    protected static String makeGroupIds(int groupCount) {
        return IntStream.range(0, groupCount)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private static Map<Integer, Node> makeAllNodeMap(ClusterMasterGroupDistribute clusterConf) {
        return clusterConf.getServerList().stream()
                .map((server) -> {
                    Node node = new Node();
                    node.setIp(server.getServerIp());
                    node.setPort(server.getTcpPort());
                    node.setSequence(server.getSequenceId());
                    return node;
                }).collect(toMap(Node::getSequence, node -> node));
    }

    private List<GroupNode> makeGroupNodeList(ClusterMasterGroupDistribute clusterConf, ClientConfInfo clientConfInfo) {
        // 如果 groupIds 字段是空 那么默认使用前一半分组
        String groupIds = Strings.isNullOrEmpty(clientConfInfo.getGroupIds()) ? makeGroupIds(clusterConf.getGroupCount()) : clientConfInfo.getGroupIds();
        Set<Integer> useGroupSet = Arrays.stream(groupIds.split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
        Map<String, Integer> serviceAddressToServiceSequence = clusterConf.getServerList().stream().collect(Collectors.toMap(ServerDO::getServerAddr, ServerDO::getSequenceId));
        List<Integer> nodeList = clusterConf.getServerList().stream().map(ServerDO::getSequenceId).collect(Collectors.toList());
        HashMap<Integer, String> groupServerMap = clusterConf.getGroupServerMap();
        HashMap<Integer, Long> groupVersionMap = clusterConf.getGroupVersionMap();
        List<GroupNode> groupNodes = new ArrayList<>();
        groupServerMap.forEach((group, server) -> {
            if (useGroupSet.contains(group) && serviceAddressToServiceSequence.containsKey(server)) {
                GroupNode groupNode = new GroupNode();
                groupNode.setGroupId(group);
                groupNode.setNodeList(nodeList);
                groupNode.setMasterNode(serviceAddressToServiceSequence.get(server));
                groupNode.setMasterVersion(groupVersionMap.get(group));
                groupNodes.add(groupNode);
            } else {
                log.info("group server no use , group is {} ,server is {}", group, server);
            }
        });
        return groupNodes;
    }

    private List<NodeAddr> makeNodeList(ClusterMasterGroupDistribute clusterConf, ClientConfInfo clientConfInfo, int groupId) throws Exception {
        List<ServerDO> serverDOList = clusterConf.getServerList();
        String masterAddr = clusterConf.getGroupServerMap().get(groupId);
        List<GroupNodeDO> groupNodeDos = groupNodeRepository.searchByCondition(Environment.env(), groupId, masterAddr, clientConfInfo.getClusterName());
        HashSet<String> nodes = new HashSet<>();
        if (!groupNodeDos.isEmpty()) {
            nodes = Sets.newHashSet(groupNodeDos.get(0).getNodes().split(","));
        }

        List<NodeAddr> nodeList = new ArrayList<>();
        for (ServerDO serverDO : serverDOList) {
            boolean isMaster = false;
            long version = -1;
            if (serverDO.getServerAddr().equals(masterAddr)) {
                isMaster = true;
                version = clusterConf.getGroupVersionMap().get(groupId);
            }
            if (!nodes.isEmpty() && !nodes.contains(serverDO.getServerIp() + ":" + serverDO.getTcpPort())) {
                continue;
            }
            NodeAddr nodeAddr = new NodeAddr();
            nodeAddr.setIp(serverDO.getServerIp());
            nodeAddr.setPort(serverDO.getTcpPort());
            nodeAddr.setIsMaster(isMaster);
            nodeAddr.setVersion(version);
            nodeList.add(nodeAddr);
        }
        return nodeList;
    }

    public ServerResult<ClientKeyEntity> getChangedClusterConf(ClientKeyEntity reqClientKey) throws Exception {
        ServerResult<ClientKeyEntity> serverResult = new ServerResult<ClientKeyEntity>();
        ClientConfInfo clientConfInfo = this.getClientConfInfoByMapping(reqClientKey.getKey(), true);
        ClusterMasterGroupDistribute clusterConf = this.getClusterMasterGroupDistribute(clientConfInfo.getClusterName(), true);
        ClientKeyEntity clientKeyEntity = this.getClientKeyEntityFromClusterConf(reqClientKey.getKey(), clusterConf, clientConfInfo);
        if (reqClientKey.getVersion() < clientKeyEntity.getVersion()) {
            serverResult.setStatus(ServerResult.HAS_CHANGE);
        } else {
            serverResult.setStatus(ServerResult.NO_CHANGE);
        }
        serverResult.setResult(clientKeyEntity);
        return serverResult;
    }

    public KeyDO getKeyByHashKey(String env, String hashKey) throws Exception {
        return keyRepository.getKeyByHashKey(env, hashKey);
    }

    public ClusterMasterGroupDistribute getClusterMasterGroupDistribute(String clusterName, boolean useRedis) throws Exception {
        if (useRedis) {
            ClusterMasterGroupDistribute clusterMasterGroupDistribute = getClusterMasterGroupDistributeFromRedis(clusterName);
            if (clusterMasterGroupDistribute != null) {
                return clusterMasterGroupDistribute;
            }
        }

        ClusterDO clusterDO = clusterRepository.getClusterByClusterName(Environment.env(), clusterName);
        if (clusterDO == null) {
            log.warn("current cluster {} unregistry in this registry server.", clusterName);
            return null;
        }

        List<ServerDO> serverDOList = serverRepository.getServerByClusterIdAndState(Environment.env(), clusterName, ServerState.online.getValue());
        if (CollectionUtils.isEmpty(serverDOList)) {
            log.warn("current cluster {} has no online server nodes.", clusterName);
            return null;
        }

        List<GroupServerRefDO> groupServerRefList = groupServerRefRepository.getGroupServerRefByClusterId(Environment.env(), clusterName);
        if (CollectionUtils.isEmpty(groupServerRefList)) {
            log.error("cluster no have master .cluster name is {}", clusterName);
            return null;
        }

        HashMap<Integer/*groupId*/, String/*IP + port*/> groupServerMap = new HashMap<Integer, String>();
        HashMap<Integer/*groupId*/, Long/*group version*/> groupVersionMap = new HashMap<Integer, Long>();
        for (GroupServerRefDO groupServerRefDO: groupServerRefList) {
            groupServerMap.put(groupServerRefDO.getGroupId(), groupServerRefDO.getServerAddr());
            groupVersionMap.put(groupServerRefDO.getGroupId(), groupServerRefDO.getVersion());
        }

        ClusterMasterGroupDistribute conf = new ClusterMasterGroupDistribute();
        conf.setServerList(serverDOList);
        conf.setClusterName(clusterName);
        conf.setGroupCount(clusterDO.getGroupCount());
        conf.setHashCode(clusterDO.getHashCode());
        conf.setVersion(clusterDO.getUpdateTime());
        conf.setGroupServerMap(groupServerMap);
        conf.setGroupVersionMap(groupVersionMap);

        setClusterMasterGroupDistributeToRedis(clusterName, conf);
        return conf;
    }

    private ClusterMasterGroupDistribute getClusterMasterGroupDistributeFromRedis(String clusterName) {
        try {
            String redisKey = RedisKeyConstant.getClusterMasterGroupDistributeKey(clusterName);
            String result = redisUtil.getValue(redisKey);
            if (StringUtils.isNotEmpty(result)) {
                return JSON.parseObject(result, ClusterMasterGroupDistribute.class);
            }
        } catch (Exception e) {
            log.error("ClientService.getClusterMasterGroupDistributeFromRedis error", e);
        }

        return null;
    }

    private void setClusterMasterGroupDistributeToRedis(String clusterName, ClusterMasterGroupDistribute entity) {
        try {
            String key = RedisKeyConstant.getClusterMasterGroupDistributeKey(clusterName);
            redisUtil.setValueAndExpire(key, JSON.toJSONString(entity), REDIS_EXPIRE_THREE);
        } catch (Exception e) {
            log.error("ClientService.setClusterMasterGroupDistributeToRedis error", e);
        }
    }
}
