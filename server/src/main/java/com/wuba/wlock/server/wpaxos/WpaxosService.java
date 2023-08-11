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
package com.wuba.wlock.server.wpaxos;

import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.constant.ConfigPath;
import com.wuba.wlock.server.migrate.service.MigrateService;
import com.wuba.wlock.server.service.GroupMetaService;
import com.wuba.wlock.server.service.impl.MasterChangeService;
import com.wuba.wlock.server.wpaxos.checkpoint.CheckpointManager;
import com.wuba.wlock.server.wpaxos.checkpoint.meta.MetaCheckpointManager;
import com.wuba.wlock.server.wpaxos.rocksdb.RocksDBHolder;
import com.wuba.wpaxos.ProposeResult;
import com.wuba.wpaxos.comm.GroupSMInfo;
import com.wuba.wpaxos.comm.NodeInfo;
import com.wuba.wpaxos.comm.Options;
import com.wuba.wpaxos.config.PaxosTryCommitRet;
import com.wuba.wpaxos.config.WriteOptions;
import com.wuba.wpaxos.master.MasterStateMachine;
import com.wuba.wpaxos.node.Node;
import com.wuba.wpaxos.storemachine.SMCtx;
import com.wuba.wpaxos.storemachine.StateMachine;
import com.wuba.wpaxos.utils.JavaOriTypeWrapper;
import com.wuba.wlock.server.wpaxos.statemachine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WpaxosService {
	private static final Logger log = LoggerFactory.getLogger(WpaxosService.class);

	private Node paxosNode;

	private Options options;

	private WpaxosService() {
	}

	private static WpaxosService wpaxosService = new WpaxosService();

	public static WpaxosService getInstance() {
		return wpaxosService;
	}

	public void start() throws Exception {
		this.runPaxos();
	}

	public void runPaxos() throws Exception {
		int groupCount = PaxosConfig.getInstance().getGroupCount();
		RocksDBHolder.init(groupCount);
		GroupMetaService.getInstance().init();
		MigrateService.getInstance().init();

		this.options = new Options();
		//this groupcount means run wpaxos group count.
		//every wpaxos group is independent, there are no any communicate between any 2 wpaxos group.
		options.setGroupCount(groupCount);
		options.setMyNode(PaxosConfig.getInstance().getMyNode());

		options.setNodeInfoList(PaxosConfig.getInstance().getNodeList());
		// 需要设置节点和分组的关系
		Map<Integer, NodeInfo> nodeMap = PaxosConfig.getInstance().getNodeMap();
		Map<Integer, Set<Integer>> groupNodeMap = PaxosConfig.getInstance().getGroupNodeMap();
		Map<Integer, ArrayList<NodeInfo>> nodeInfoMap = makeGroupNodeMap(nodeMap, groupNodeMap);
		options.setNodeInfoMap(nodeInfoMap);

		options.setUseMembership(PaxosConfig.getInstance().isOptionsUseMembership());
		options.setUseBatchPropose(PaxosConfig.getInstance().isOptionsUseBatchPropose());
		options.setUDPMaxSize(PaxosConfig.getInstance().getOptionsUDPMaxSize());
		options.setPrepareTimeout(PaxosConfig.getInstance().getOptionsPrepareTimeout());
		options.setAcceptTimeout(PaxosConfig.getInstance().getOptionsAcceptTimeout());
		options.setAskForLearnTimeout(PaxosConfig.getInstance().getOptionsAskForLearnTimeout());
		options.setIoThreadCount(PaxosConfig.getInstance().getOptionsIoThreadCount());
		options.setMasterChangeCallback(MasterChangeService.getInstance());
		options.setLogStorageConfPath(ConfigPath.STORE_CONFIG);
		options.setOpenChangeValueBeforePropose(true);
		boolean useMaster = PaxosConfig.getInstance().isOptionsUseMaster();
		options.setNoUseMasterGroups(PaxosConfig.getInstance().getNoUseMasterGroups());
		options.setLogStoragePath(PaxosConfig.getInstance().getStorePathIndexLog());
		options.setLearnerSendSpeed(PaxosConfig.getInstance().getOptionslearnerSendSpeed());
		for (int i = 0; i < groupCount; i++) {
			GroupSMInfo smInfo = new GroupSMInfo();
			smInfo.setUseMaster(useMaster);
			smInfo.setGroupIdx(i);
			smInfo.getSmList().add(new LockStateMachine(i, SMID.LOCK_SMID.getValue(), true));
			smInfo.getSmList().add(new KeepMasterSM(i, SMID.KEEP_MASTER.getValue(), false));
			smInfo.getSmList().add(new MigrateCommandSM(i, SMID.MIGRATE_COMMAND.getValue(), false));
			smInfo.getSmList().add(new MigrateChangePointSM(i, SMID.MIGRATE_POINT.getValue(), true));
			smInfo.getSmList().add(new GroupMetaSM(i, SMID.GROUP_META.getValue(), false));
			smInfo.getSmList().add(new NullStateMachine(i, SMID.NULL.getValue(), false));
			options.getGroupSMInfoList().add(smInfo);
		}

		CheckpointManager.getInstance().start();
		MetaCheckpointManager.getInstance().start();
		options.setCommitTimeout(PaxosConfig.getInstance().getOptionsCommitTimeout());
		this.paxosNode = Node.runNode(options);
		if (this.paxosNode == null) {
			throw new Exception("run wpaxos node error,should shutdown system.");
		}
		// wlock 需要保证 io 线程数量 == group 数量
		if (options.getGroupCount() != options.getIoThreadCount()) {
			throw new Exception("wlocll's paxos group count must equal ioThread count.");
		}
		this.paxosNode.setTimeoutMs(PaxosConfig.getInstance().getOptionsCommitTimeout());
	}

	protected Map<Integer, ArrayList<NodeInfo>> makeGroupNodeMap(Map<Integer, NodeInfo> nodeMap, Map<Integer, Set<Integer>> groupNodeMap) {
		Map<Integer, ArrayList<NodeInfo>> nodeInfoMap = new HashMap<>();
		for (Map.Entry<Integer, Set<Integer>> groupNodeInfos : groupNodeMap.entrySet()) {
			ArrayList<NodeInfo> nodeInfos = new ArrayList<>();
			for (Integer nodeId : groupNodeInfos.getValue()) {
				nodeInfos.add(nodeMap.get(nodeId));
			}
			nodeInfoMap.put(groupNodeInfos.getKey(),nodeInfos);
		}
		return nodeInfoMap;
	}

	public void stopPaxos() {
		this.paxosNode.stopPaxos();
		RocksDBHolder.stop(PaxosConfig.getInstance().getGroupCount());
		CheckpointManager.getInstance().shutdown();
	}

	public ProposeResult propose(byte[] message, int groupIdx, SMCtx ctx) {
		ProposeResult pr = this.paxosNode.propose(groupIdx, message, ctx);
		return pr;
	}

	public ProposeResult propose(byte[] message, int groupIdx, SMCtx ctx, int timeout) {
		JavaOriTypeWrapper<Long> instanceIdWrap = new JavaOriTypeWrapper<Long>();
		instanceIdWrap.setValue(0L);

		ProposeResult pr = this.paxosNode.propose(groupIdx, message, instanceIdWrap, ctx, timeout);
		return pr;
	}

	public ProposeResult syncMigrateChangePoint(int groupIdx) {
		try {
			if (MigrateService.getInstance().isFirstPropose(groupIdx)) {
				int sourceGroupId = MigrateService.getInstance().convertGroupId(groupIdx);
				long sourceGroupMaxInstanceID = this.paxosNode.getNowInstanceId(sourceGroupId);
				long groupVersion = GroupMetaService.getInstance().getGroupVersion(sourceGroupId);
				return MigrateService.getInstance().exeFirstPropose(groupIdx, sourceGroupMaxInstanceID, groupVersion);
			}
		} catch (Exception e) {
			log.error("WpaxosService.syncMigrateChangePoint error", e);
			return new ProposeResult(PaxosTryCommitRet.PaxosTryCommitRet_ExecuteFail.getRet());
		}

		return null;
	}

	public ProposeResult batchPropose(byte[] message, int groupIdx, SMCtx ctx, String registryKey) {
		JavaOriTypeWrapper<Integer> firstProposeResult = new JavaOriTypeWrapper<Integer>(PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet());
		groupIdx = MigrateService.getInstance().mappingGroup(groupIdx, registryKey, firstProposeResult);
		if (firstProposeResult.getValue() != PaxosTryCommitRet.PaxosTryCommitRet_OK.getRet()) {
			return new ProposeResult(firstProposeResult.getValue());
		}

		JavaOriTypeWrapper<Integer> indexIDWrap = new JavaOriTypeWrapper<Integer>();
		indexIDWrap.setValue(0);

		ProposeResult pr = this.paxosNode.batchPropose(groupIdx, message, indexIDWrap, ctx);
		return pr;
	}

	/**
	 * 添加成员
	 *
	 * @param groupId
	 * @param ip
	 * @param port
	 * @throws Exception
	 */
	public int addMember(int groupId, String ip, int port) throws Exception {
		NodeInfo node = new NodeInfo(ip, port);
		return this.paxosNode.addMember(groupId, node);
	}

	/**
	 * 删除成员
	 *
	 * @param groupId
	 * @param ip
	 * @param port
	 * @throws Exception
	 */
	public int deleteMember(int groupId, String ip, int port) throws Exception {
		NodeInfo node = new NodeInfo(ip, port);
		return this.paxosNode.removeMember(groupId, node);
	}

	/**
	 * 成员变更
	 *
	 * @param groupId
	 * @param fromNode
	 * @param toNode
	 * @throws Exception
	 */
	public int changeMember(int groupId, NodeInfo fromNode, NodeInfo toNode) throws Exception {
		return this.paxosNode.changeMember(groupId, fromNode, toNode);
	}

	/**
	 * 删除master
	 *
	 * @param groupId
	 */
	public int dropMaster(int groupId) {
		return this.paxosNode.dropMaster(groupId);
	}
	
	/**
	 * 删除所有master
	 */
	public void dropAllMaster() {
		for (int i = 0; i < this.options.getGroupCount(); i++) {
			dropMaster(i);
		}
	}

	/**
	 * 判断当前节点是否为master
	 *
	 * @param groupId
	 */
	public boolean isIMMaster(int groupId) {
		return this.paxosNode.isIMMaster(groupId);
	}

	/**
	 * 竞争master
	 *
	 * @param groupId
	 */
	public int toBeMaster(int groupId) {
		return this.paxosNode.toBeMaster(groupId);
	}

	/**
	 * 判断当前group是否有master
	 *
	 * @param groupId
	 */
	public boolean isNoMaster(int groupId) {
		return this.paxosNode.isNoMaster(groupId);
	}

	/**
	 * 获取所有成员
	 *
	 * @param groupId
	 * @return
	 */
	public List<NodeInfo> getAllMembers(int groupId) {
		List<NodeInfo> nodeInfoList = new ArrayList<NodeInfo>();
		this.paxosNode.showMembership(groupId, nodeInfoList);
		return nodeInfoList;
	}

	/**
	 * get master version of groupIdx's
	 *
	 * @param groupIdx
	 * @return master version
	 */
	public long getMasterVersion(int groupIdx) {
		return paxosNode.getMasterVersion(groupIdx);
	}

	public NodeInfo getMaster(int groupIdx) {
		return paxosNode != null ? paxosNode.getMaster(groupIdx) : null;
	}

	public MasterStateMachine getMasterSM(int groupIdx) {
		return this.paxosNode.getMasterMgr(groupIdx).getMasterSM();
	}

	public List<StateMachine> getSMList(int groupIdx) {
		GroupSMInfo groupSmInfo = this.options.getGroupSMInfoList().get(groupIdx);
		List<StateMachine> smList = groupSmInfo.getSmList();
		return smList;
	}
	
	public long getMyNodeID() {
		return this.paxosNode.getMyNodeID();
	}

	public Node getPaxosNode() {
		return paxosNode;
	}

	public void setPaxosNode(Node paxosNode) {
		this.paxosNode = paxosNode;
	}

	public Options getOptions() {
		return options;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public void setMinChosenInstanceId(int groupId, long instanceId) {
		this.paxosNode.getLogStorage().setMinChosenInstanceID(new WriteOptions(), groupId, instanceId);
	}

	public boolean isLearn(int groupId){
		return this.paxosNode.isLearning(groupId);
	}

	public long getNowInstanceId(int sourceGroupId) {
		return this.paxosNode.getNowInstanceId(sourceGroupId);
	}

	public void enableMasterElection(int groupId) {
		this.options.enableMasterElection(groupId);
	}

	public void disableMasterElection(int groupId) {
		this.options.disableMasterElection(groupId);
	}


	public void setMasterLease(int groupIdx, int leaseTimeMs) {
		this.paxosNode.setMasterLease(groupIdx, leaseTimeMs);
	}

	public void setMasterElectionPriority(int groupIdx, int electionPriority) {
		this.paxosNode.setMasterElectionPriority(groupIdx, electionPriority);
	}

	public boolean isInit() {
		return this.paxosNode != null;
	}
}
