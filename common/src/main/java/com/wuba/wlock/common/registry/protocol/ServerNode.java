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
package com.wuba.wlock.common.registry.protocol;

public class ServerNode {
	/**
	 * 节点序列 id
	 */
	private int sequenceId;
	/**
	 * 节点 ip
	 */
	private String ip;
	/**
	 * 节点 tcp 端口
	 */
	private int tcpPort;
	/**
	 * 节点 paxos 端口
	 */
	private int paxosPort;
	/**
	 * 节点 keepMaster 端口
	 */
	private int keepMasterPort;

	public int getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(int sequenceId) {
		this.sequenceId = sequenceId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getTcpPort() {
		return tcpPort;
	}

	public void setTcpPort(int tcpPort) {
		this.tcpPort = tcpPort;
	}

	public int getPaxosPort() {
		return paxosPort;
	}

	public void setPaxosPort(int paxosPort) {
		this.paxosPort = paxosPort;
	}

	public int getKeepMasterPort() {
		return keepMasterPort;
	}

	public void setKeepMasterPort(int keepMasterPort) {
		this.keepMasterPort = keepMasterPort;
	}

	public ServerNode() {
	}

	public ServerNode(int sequenceId, String ip, int tcpPort, int paxosPort, int keepMasterPort) {
		this.sequenceId = sequenceId;
		this.ip = ip;
		this.tcpPort = tcpPort;
		this.paxosPort = paxosPort;
		this.keepMasterPort = keepMasterPort;
	}

	@Override
	public String toString() {
		return "NodeInfo{" +
				"sequenceId=" + sequenceId +
				", ip='" + ip + '\'' +
				", tcpPort=" + tcpPort +
				", paxosPort=" + paxosPort +
				", keepMasterPort=" + keepMasterPort +
				'}';
	}
}
