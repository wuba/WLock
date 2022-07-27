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
package com.wuba.wlock.client.registryclient.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupNode {

    private int groupId;

    private List<Integer> nodeList;

    private int masterNode;

    private long masterVersion;

    public GroupNode() {
    }

    public GroupNode(int groupId, List<Integer> nodeList, int masterNode, long masterVersion) {
        this.groupId = groupId;
        this.nodeList = nodeList;
        this.masterNode = masterNode;
        this.masterVersion = masterVersion;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public List<Integer> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<Integer> nodeList) {
        this.nodeList = nodeList;
    }

    public int getMasterNode() {
        return masterNode;
    }

    public void setMasterNode(int masterNode) {
        this.masterNode = masterNode;
    }

    public long getMasterVersion() {
        return masterVersion;
    }

    public void setMasterVersion(long masterVersion) {
        this.masterVersion = masterVersion;
    }

    public List<NodeAddr> getNodeAddrList(Map<Integer, Node> allNodeMap) {
        List<NodeAddr> nodeAddrList = new ArrayList<NodeAddr>();
        for (Integer nodeId: nodeList) {
            Node node = allNodeMap.get(nodeId);

            nodeAddrList.add(new NodeAddr(node.getIp(), node.getPort(), nodeId == masterNode, masterVersion));
        }

        return nodeAddrList;
    }
}
