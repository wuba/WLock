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
package com.wuba.wlock.registry.admin.domain.request;

import com.wuba.wlock.registry.admin.constant.ValidationConstant;
import com.wuba.wlock.registry.admin.validators.ValidationCheck;

public class ServerInfoReq {

	@ValidationCheck(allowEmpty = false, regexExpression = ValidationConstant.REGEX_NAME, filedDescription = "集群名")
	private String clusterName;
	
	@ValidationCheck(allowEmpty = false, minValue = "1", filedDescription = "序列号")
	private int sequenceId;

	@ValidationCheck(allowEmpty = false, regexExpression = ValidationConstant.IP_REGEXP, filedDescription = "IP")
	private String ip;
	
	@ValidationCheck(allowEmpty = false, minValue = "1", filedDescription = "tcp端口")
	private int tcpPort;
	
	@ValidationCheck(allowEmpty = false, minValue = "1", filedDescription = "telnet端口")
	private int telnetPort;
	
	@ValidationCheck(allowEmpty = false, minValue = "1", filedDescription = "paxos端口")
	private int paxosPort;
	
	@ValidationCheck(allowEmpty = false, minValue = "1", filedDescription = "udp端口")
	private int udpPort;

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

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

	public int getTelnetPort() {
		return telnetPort;
	}

	public void setTelnetPort(int telnetPort) {
		this.telnetPort = telnetPort;
	}

	public int getPaxosPort() {
		return paxosPort;
	}

	public void setPaxosPort(int paxosPort) {
		this.paxosPort = paxosPort;
	}

	public int getUdpPort() {
		return udpPort;
	}

	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}

}
