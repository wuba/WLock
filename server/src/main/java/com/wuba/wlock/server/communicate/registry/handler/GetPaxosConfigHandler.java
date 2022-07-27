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
package com.wuba.wlock.server.communicate.registry.handler;

import com.alibaba.fastjson.JSONObject;
import com.wuba.wlock.common.registry.protocol.MessageType;
import com.wuba.wlock.common.registry.protocol.OptionCode;
import com.wuba.wlock.common.registry.protocol.RegistryProtocol;
import com.wuba.wlock.common.registry.protocol.ServerNode;
import com.wuba.wlock.common.registry.protocol.request.GetPaxosConfig;
import com.wuba.wlock.common.registry.protocol.response.GetPaxosConfRes;
import com.wuba.wlock.server.communicate.retrans.RetransServerManager;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import com.wuba.wpaxos.comm.NodeInfo;
import com.wuba.wpaxos.comm.Options;
import com.wuba.wpaxos.config.PaxosNodeFunctionRet;
import com.wuba.wpaxos.config.PaxosTryCommitRet;
import com.wuba.wlock.server.exception.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GetPaxosConfigHandler extends AbstractPaxosHandler implements IPaxosHandler {
	private static final Logger logger = LoggerFactory.getLogger(GetPaxosConfigHandler.class);
	private static final int RETRY_TIMES = 2;
	private static final long[] RETRY_DELAY = {1000L, 2000L, 3000L};
	private static boolean isInit = true;

	@Override
	protected boolean checkProtocol(byte opaque) {
		if (opaque != OptionCode.RES_PAXOS_CONFIG) {
			logger.error("get paxos config response opaque error {}.", opaque);
			return false;
		}
		return true;
	}

	@Override
	public boolean doSuccess(RegistryProtocol registryProtocol) throws Exception {
		GetPaxosConfRes getPaxosConfRes = JSONObject.parseObject(registryProtocol.getBody(), GetPaxosConfRes.class);
		logger.info("receive paxos server config {}", JSONObject.toJSONString(getPaxosConfRes));
		String[] paxosServer = getPaxosConfRes.getPaxosServer().split(GetPaxosConfRes.SEP);
		Map<Integer, ServerNode> serverMap = getPaxosConfRes.getAllServerMap();
		Map<Integer, NodeInfo> nodeInfoMap = new HashMap<>();
		Map<Long, Integer> udpPorts = new HashMap<>();
		Map<Long, String> tcpIpPorts = new HashMap<>();
		genericNodeInfo(serverMap, nodeInfoMap, udpPorts, tcpIpPorts);
		// 集群可配置 , 根据拉取的信息进行更新
		if (!getPaxosConfRes.getClusterName().equals(ServerConfig.getInstance().getCluster())) {
			ServerConfig.getInstance().setCluster(getPaxosConfRes.getClusterName());
			// 拆集群的时候 要重新更新 节点 map , 否则可能一致进行节点变更
			PaxosConfig.getInstance().setNodeMap(nodeInfoMap);
		}

		PaxosConfig.getInstance().setNoUseMasterGroups(getPaxosConfRes.getNoUseMasterGroups());
		PaxosConfig.getInstance().setNoLoadBalanceGroups(getPaxosConfRes.getNoLoadBalanceGroups());
		Options options = WpaxosService.getInstance().getOptions();
		if (options != null) {
			options.setNoUseMasterGroups(getPaxosConfRes.getNoUseMasterGroups());
		}

		if (isInit) {
			PaxosConfig.getInstance().setMyNode(new NodeInfo(paxosServer[0], Integer.parseInt(paxosServer[1])));
			PaxosConfig.getInstance().setGroupCount(getPaxosConfRes.getGroupCount() * 2);
			// io thread count 必须和 groupCount 相同,否则 group 扩缩容时候会有问题
			PaxosConfig.getInstance().setOptionsIoThreadCount(getPaxosConfRes.getGroupCount() * 2);
			PaxosConfig.getInstance().setNodeMap(nodeInfoMap);
			PaxosConfig.setVersion(getPaxosConfRes.getVersion());
			ServerConfig.getInstance().setCluster(getPaxosConfRes.getClusterName());
			ServerConfig.getInstance().setMyUdpPort(getPaxosConfRes.getUdpPort());
			ServerConfig.getInstance().setUdpPort(udpPorts);
			ServerConfig.getInstance().setTcpIpPorts(tcpIpPorts);
			PaxosConfig.getInstance().setGroupNodeMap(getPaxosConfRes.getGroupNodeMap());
			isInit = false;
			return true;
		}
//		if (getPaxosConfRes.getVersion() == PaxosConfig.getVersion()) {
//			return true;
//		}
		/**
		 * 2019/6/12 集群发生变化，增加或减少成员
		 */
		return reCheckMember(nodeInfoMap, udpPorts, tcpIpPorts, getPaxosConfRes.getVersion(), getPaxosConfRes.getGroupNodeMap());
	}

	protected void genericNodeInfo(Map<Integer, ServerNode> serverMap, Map<Integer, NodeInfo> nodeInfoMap, Map<Long, Integer> udpPorts, Map<Long, String> tcpIpPorts) throws Exception {
		NodeInfo nodeInfo;
		for (Map.Entry<Integer, ServerNode> server : serverMap.entrySet()) {
			ServerNode serverNode = server.getValue();
			nodeInfo = new NodeInfo(serverNode.getIp(), serverNode.getPaxosPort());
			udpPorts.put(nodeInfo.getNodeID(), serverNode.getKeepMasterPort());
			tcpIpPorts.put(nodeInfo.getNodeID(), nodeInfo.getIp() + ":" + serverNode.getTcpPort());
			nodeInfoMap.put(server.getKey(), nodeInfo);
		}
	}

	private boolean reCheckMember(Map<Integer, NodeInfo> registryNodes, Map<Long, Integer> udpPorts, Map<Long, String> tcpIpPorts, long version, Map<Integer, Set<Integer>> groupCapacityChange) {
		// 1. 遍历 groupCapacityChange 生成每个 group 需要变更的成员列表
		Map<Integer, Set<Integer>> allGroupNodeMap = PaxosConfig.getInstance().getGroupNodeMap();
		Map<Integer, Set<NodeInfo>> groupRemoveNode = new HashMap<>();
		Map<Integer, Set<NodeInfo>> groupAddNode = new HashMap<>();
		Map<Integer, NodeInfo> allNodeMap = PaxosConfig.getInstance().getNodeMap();
		checkMemberInner(allNodeMap, registryNodes, groupCapacityChange, allGroupNodeMap, groupRemoveNode, groupAddNode);
		logger.debug("need remove member is {}", groupRemoveNode);
		logger.debug("need add member is {}", groupAddNode);
		// 2. 进行成员变更操作
		boolean addResult = true, deleteResult = true;
		boolean isChanged = false;
		for (int groupIdx = 0; groupIdx < PaxosConfig.getInstance().getGroupCount(); groupIdx++) {
			Set<NodeInfo> removeNode = groupRemoveNode.get(groupIdx);
			Set<NodeInfo> newNode = groupAddNode.get(groupIdx);
			// 如果是迁移分组,并且其 他分组节点列表
			if (groupIdx >= PaxosConfig.getInstance().getGroupCount() / 2 && allGroupNodeMap.get(0).size() == removeNode.size()) {
				isChanged = true;
				logger.info("need reset group node");
				WpaxosService.getInstance().getPaxosNode().resetPaxosNode(groupIdx, new ArrayList<>(newNode));
				logger.info(" node change end.");
				continue;
			}

			if (removeNode != null && !removeNode.isEmpty()) {
				isChanged = true;
				deleteResult &= deleteMember(groupIdx, removeNode);
				logger.info("server changed.remove old member {} groupId {} result {} nodeInfo {}", removeNode.size(), groupIdx, deleteResult, removeNode);
			}
			if (newNode != null && !newNode.isEmpty()) {
				isChanged = true;
				// 对于某一个 group 添加成员
				addResult &= addMember(groupIdx, newNode);
				logger.info("server changed.add new member {} groupId {} result {} nodeInfo {}", newNode.size(), groupIdx, addResult, newNode);
			}
		}
		// 如果初始化时候为 null , 那么设置一下
		if (PaxosConfig.getInstance().getGroupNodeMap() == null) {
			PaxosConfig.getInstance().setGroupNodeMap(groupCapacityChange);
		}
		if (isChanged && addResult && deleteResult) {
			logger.info("update node success");
			PaxosConfig.getInstance().setNodeMap(registryNodes);
			PaxosConfig.getInstance().setGroupNodeMap(groupCapacityChange);
			PaxosConfig.setVersion(version);
			ServerConfig.getInstance().setTcpIpPorts(tcpIpPorts);
			ServerConfig.getInstance().setUdpPort(udpPorts);
			RetransServerManager.getInstance().serverChanged(tcpIpPorts);
		}
		return addResult && deleteResult;
	}

	protected void checkMemberInner(Map<Integer, NodeInfo> allNodeMap, Map<Integer, NodeInfo> registryNodes, Map<Integer, Set<Integer>> groupCapacityChange, Map<Integer, Set<Integer>> allGroupNodeMap, Map<Integer, Set<NodeInfo>> groupRemoveNode, Map<Integer, Set<NodeInfo>> groupAddNode) {
		for (Map.Entry<Integer/* group */, Set<Integer>/* nodeId */> groupNodeInfo : groupCapacityChange.entrySet()) {
			Set<NodeInfo> removeSet = new HashSet<>();
			Set<NodeInfo> addSet = new HashSet<>();
			groupRemoveNode.put(groupNodeInfo.getKey(), removeSet);
			groupAddNode.put(groupNodeInfo.getKey(), addSet);
			Set<Integer> newGroupNodes = groupNodeInfo.getValue();
			Set<Integer> oldGroupNodes = allGroupNodeMap.get(groupNodeInfo.getKey());

			if (oldGroupNodes != null) {
				for (Integer oldGroupNode : oldGroupNodes) {
					if (!newGroupNodes.contains(oldGroupNode)) {
						// 需要添加的节点
						if (registryNodes.containsKey(oldGroupNode)) {
							removeSet.add(registryNodes.get(oldGroupNode));
						} else if (allNodeMap.containsKey(oldGroupNode)) {
							removeSet.add(allNodeMap.get(oldGroupNode));
						} else {
							logger.error("checkMemberInner error , node info error , node id is : {}", oldGroupNode);
						}
					}
				}
			}
			for (Integer newGroupNode : newGroupNodes) {
				if (oldGroupNodes != null && !oldGroupNodes.contains(newGroupNode)) {
					if (registryNodes.containsKey(newGroupNode)) {
						addSet.add(registryNodes.get(newGroupNode));
					} else {
						addSet.add(allNodeMap.get(newGroupNode));
					}
				}
			}
		}
	}

	private boolean addMember(int groupIdx, final Set<NodeInfo> addNodes) {
		if (!WpaxosService.getInstance().isIMMaster(groupIdx) && !WpaxosService.getInstance().isNoMaster(groupIdx)) {
			return true;
		}
		boolean result = true;
		for (NodeInfo node : addNodes) {
			int i = 0;
			int addMember = -1;
			while (i++ <= RETRY_TIMES) {
				try {
					addMember = WpaxosService.getInstance().addMember(groupIdx, node.getIp(), node.getPort());
					if (addMember != PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && addMember != PaxosNodeFunctionRet.Paxos_MembershipOp_Add_NodeExist.getRet()) {
						try {
							Thread.sleep(RETRY_DELAY[Math.min(i, RETRY_TIMES)]);
						} catch (InterruptedException e) {
						}
					} else {
						break;
					}
				} catch (Exception e) {
					result = false;
					logger.error("add member error", e);
				}
				if (addMember != PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && addMember != PaxosNodeFunctionRet.Paxos_MembershipOp_Add_NodeExist.getRet()) {
					logger.debug("TEST => add member false : result is " + addMember);
					result = false;
				}
			}
		}
		return result;
	}

	private boolean deleteMember(int groupIdx, final Set<NodeInfo> deleteNodes) {
		if (!WpaxosService.getInstance().isIMMaster(groupIdx) && !WpaxosService.getInstance().isNoMaster(groupIdx)) {
			return true;
		}

		boolean result = true;
		for (NodeInfo node : deleteNodes) {
			int i = 0;
			int deleteMember = -1;
			while (i++ <= RETRY_TIMES) {
				try {
					deleteMember = WpaxosService.getInstance().deleteMember(groupIdx, node.getIp(), node.getPort());
					if (deleteMember != PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && deleteMember != PaxosNodeFunctionRet.Paxos_MembershipOp_Remove_NodeNotExist.getRet()) {
						try {
							Thread.sleep(RETRY_DELAY[Math.min(i, RETRY_TIMES)]);
						} catch (InterruptedException e) {
						}
					} else {
						break;
					}
				} catch (Exception e) {
					result = false;
					logger.error("delete member error", e);
				}
				if (deleteMember != PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet() && deleteMember != PaxosNodeFunctionRet.Paxos_MembershipOp_Remove_NodeNotExist.getRet()) {
					result = false;
				}
			}
		}
		return result;
	}

	@Override
	public boolean doError(RegistryProtocol registryProtocol) {
		logger.error("init get paxos config error.");
		return false;
	}

	@Override
	public boolean doElse(RegistryProtocol registryProtocol) {
		if (registryProtocol.getMsgType() == MessageType.NO_CHANGE) {
			GetPaxosConfRes getPaxosConfRes = JSONObject.parseObject(registryProtocol.getBody(), GetPaxosConfRes.class);
			PaxosConfig.setVersion(getPaxosConfRes.getVersion());
			logger.info("init get paxos config no change.");
		}
		return false;
	}

	@Override
	public RegistryProtocol buildMessage() throws ConfigException {
		return new GetPaxosConfig(ServerConfig.getInstance().getServerListenIP(), ServerConfig.getInstance().getServerListenPort(), PaxosConfig.getVersion());
	}

	@Override
	public boolean handleResponse(RegistryProtocol registryProtocol) throws Exception {
		return super.doHandler(registryProtocol);
	}
}
