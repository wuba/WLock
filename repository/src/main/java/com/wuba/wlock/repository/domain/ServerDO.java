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
package com.wuba.wlock.repository.domain;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;
import java.util.Objects;

@Data
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_server")
public class ServerDO extends BaseDO<ServerDO> {

	public static final String FIELD_SEQUENCE_ID = "sequence_id";
	public static final String FIELD_SERVER_ADDR = "server";
	public static final String FIELD_TELNET_PORT = "telnet_port";
	public static final String FIELD_PAXOS_PORT = "paxos_port";
	public static final String FIELD_UDP_PORT = "udp_port";
	public static final String FIELD_CLUSTER_ID = "cluster_id";
	public static final String FIELD_STATE = "state";
	public static final String FIELD_LAST_UPDATE_TIME = "last_update_time";
	public static final String FIELD_ID = "id";

	/**
	 * 主键
	 */
	@TableId
	private Long id;
	@TableField("sequence_id")
	private Integer sequenceId;

	@TableField("server")
	private String serverAddr;

	@TableField("telnet_port")
	private Integer telnetPort;

	@TableField("paxos_port")
	private Integer paxosPort;

	@TableField("udp_port")
	private Integer udpPort;

	@TableField("cluster_id")
	private String clusterId;

	@TableField("state")
	private Integer state;

	@TableField("last_update_time")
	private Date lastUpdateTime;

	@TableField(exist = false)
	private String serverIp;

	@TableField(exist = false)
	private Integer tcpPort;

	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
		String[] addrItems = this.serverAddr.split(":");
		this.setServerIp(addrItems[0]);
		this.setTcpPort(Integer.parseInt(addrItems[1]));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ServerDO serverDO = (ServerDO) o;
		return getId().equals(serverDO.getId()) && sequenceId.equals(serverDO.sequenceId) && telnetPort.equals(serverDO.telnetPort) && paxosPort.equals(serverDO.paxosPort) && udpPort.equals(serverDO.udpPort) && state.equals(serverDO.state) && tcpPort.equals(serverDO.tcpPort) && Objects.equals(serverAddr, serverDO.serverAddr) && Objects.equals(clusterId, serverDO.clusterId) && Objects.equals(serverIp, serverDO.serverIp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), sequenceId, serverAddr, telnetPort, paxosPort, udpPort, clusterId, state, serverIp, tcpPort);
	}

}
