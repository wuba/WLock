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
import lombok.Data;


@Data
public class QuickInitReq {

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

	public ServerInfoReq toServerInfoReq(){
		ServerInfoReq serverInfoReq = new ServerInfoReq();
		serverInfoReq.setClusterName(ClusterInfoReq.DEFAULT_CLUSTER);
		serverInfoReq.setSequenceId(sequenceId);
		serverInfoReq.setIp(ip);
		serverInfoReq.setTcpPort(tcpPort);
		serverInfoReq.setTelnetPort(telnetPort);
		serverInfoReq.setPaxosPort(paxosPort);
		serverInfoReq.setUdpPort(udpPort);
		return serverInfoReq;
	}

}
