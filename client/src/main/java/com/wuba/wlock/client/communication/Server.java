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
package com.wuba.wlock.client.communication;

import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.communication.detect.DaemonChecker;
import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.exception.ConnectTimeoutException;
import com.wuba.wlock.client.exception.SerializeException;
import com.wuba.wlock.client.helper.OpaqueGenerator;
import com.wuba.wlock.client.protocol.WLockProtocol;
import com.wuba.wlock.client.protocol.WLockRequest;
import com.wuba.wlock.client.registryclient.entity.NodeAddr;
import com.wuba.wlock.client.util.InetAddressUtil;
import com.wuba.wlock.client.util.UniqueCodeGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Server {
	private static final Log logger = LogFactory.getLog(Server.class);
	
	private WLockClient wlockClient;
	private ChannelPool channelPool;
	private ServerConfig servConfig;
	private ServerState state = ServerState.Dead;
	private WaitWindow waitWindow = WaitWindow.getWindow();
	private long uniqueCode;
	private boolean isDelete = false;

	private NodeAddr nodeAddr;

	public Server(ServerConfig servConfig, WLockClient wlockClient, NodeAddr nodeAddr) {
		this.wlockClient = wlockClient;
		this.servConfig = servConfig;
		this.nodeAddr = nodeAddr;
		this.channelPool = ChannelPool.getInstance();
		this.uniqueCode = UniqueCodeGenerator.getUniqueCode();
		try {
			this.connect();
		} catch (IOException e) {
			logger.error(Version.INFO + " create sock error in SockPool", e);
		} catch (ConnectTimeoutException e) {
			logger.error(Version.INFO + " create sock timeout in SockPool", e);
		}

		if (channelPool.count(this) == 0 && servConfig.getInitConn() > 0) {
			this.check();
			logger.error(Version.INFO + " create sockpool error:" + servConfig.getIp() + ":" + servConfig.getPort());
		} else {
			this.state = ServerState.Normal;
		}
	}

	public Server(WLockClient wlockClient, NodeAddr nodeAddr) {
		this(new ServerConfig(nodeAddr.getIp(), nodeAddr.getPort(), nodeAddr.getIsMaster()),wlockClient, nodeAddr);
	}
	
	public void connect() throws IOException, ConnectTimeoutException {
		this.channelPool.add(this, this.servConfig);
	}

	public void destroy() {
		setDelete(true);
		getChannelPool().destroy(this);
	}

	@Deprecated
	public void asyncInvoke(WLockRequest wlockReq) throws IOException, ConnectTimeoutException {
		NIOChannel channel = null;
		long sessionID = OpaqueGenerator.getOpaque();
		try {
			wlockReq.setHost(InetAddressUtil.getIpInt());
			wlockReq.setThreadID(Thread.currentThread().getId());
			wlockReq.setTimestamp(System.currentTimeMillis());
			wlockReq.setSessionID(sessionID);
			
			channel = this.channelPool.getChannel(this);
			if (!channel.isOpen()) {
				throw new IOException("channel is close:" + channel.toString());
			}
			
			WindowData wd = new WindowData(sessionID, wlockReq, channel);
			channel.asyncSend(wd);
			
			logger.debug(Version.INFO + ", method:sendRequest :" + wlockReq.getLockKey() + ", protocol type : " + wlockReq.getProtocolType() + " NIOChannel:" + channel.toString());			
		} catch (IOException | ConnectTimeoutException e) {
			if (channel != null) {
				channel.replaceChannel();
			}
			logger.error(Version.INFO + ", syncInvoke error.", e);
			throw e;
		} catch (Exception exc) {
			logger.error(Version.INFO + ", syncInvoke error.", exc);
		} finally {
			if(channel != null) {
				channel.unregisterRec(sessionID);
			}
		}
	}
	
	public byte[] syncInvoke(WLockProtocol wlockProc, int timeout) throws IOException, ConnectTimeoutException, SerializeException, CommunicationException, TimeoutException {
		NIOChannel channel = null;
		long sessionID = OpaqueGenerator.getOpaque();
		try {
			wlockProc.setTimestamp(System.currentTimeMillis());
			wlockProc.setSessionID(sessionID);
			
			channel = this.channelPool.getChannel(this);
			if (!channel.isOpen()) {
				throw new IOException("channel is close:" + channel.toString());
			}
			
			WindowData wd = new WindowData(sessionID, wlockProc, channel);
			channel.registerRec(sessionID, wd);
			channel.asyncSend(wd);
			
			byte[] data = channel.receive(sessionID, timeout);
			
			logger.debug(Version.INFO + ", method:sendRequest :" + wlockProc.getLockKey() + ", protocol type : " + wlockProc.getProtocolType() + " NIOChannel:" + channel.toString());			
			return data;
		} catch (IOException | ConnectTimeoutException e) {
			if (channel != null) {
				channel.replaceChannel();
			}
			logger.error(Version.INFO + ", syncInvoke error.", e);
			throw e;
		} catch (TimeoutException e) {
			logger.error(Version.INFO + ", syncInvoke error.", e);
			throw e;
		} catch (Exception exc) {
			logger.error(Version.INFO + ", syncInvoke error.", exc);
			return null;
		} finally {
			if(channel != null) {
				channel.unregisterRec(sessionID);
			}
		}
	}
	
	public void check() {
		DaemonChecker.check(this);
	}
	
	public ChannelPool getChannelPool() {
		return channelPool;
	}
	
	public void setChannelPool(ChannelPool channelPool) {
		this.channelPool = channelPool;
	}
	
	public ServerConfig getServConfig() {
		return servConfig;
	}
	
	public void setServConfig(ServerConfig servConfig) {
		this.servConfig = servConfig;
	}
	
	public ServerState getState() {
		return state;
	}
	
	public void setState(ServerState state) {
		this.state = state;
	}
	
	public WaitWindow getWaitWindow() {
		return waitWindow;
	}
	
	public void setWaitWindow(WaitWindow waitWindow) {
		this.waitWindow = waitWindow;
	}
	
	public long getUniqueCode() {
		return uniqueCode;
	}
	
	public void setUniqueCode(long uniqueCode) {
		this.uniqueCode = uniqueCode;
	}

	public WLockClient getWlockClient() {
		return wlockClient;
	}

	public void setWlockClient(WLockClient wlockClient) {
		this.wlockClient = wlockClient;
	}

	public boolean isDelete() {
		return isDelete;
	}

	public void setDelete(boolean isDelete) {
		this.isDelete = isDelete;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((servConfig == null) ? 0 : servConfig.hashCode());
		result = prime * result + (int) (uniqueCode ^ (uniqueCode >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Server other = (Server) obj;
		if (servConfig == null) {
			if (other.servConfig != null) {
				return false;
			}
		} else if (!servConfig.equals(other.servConfig)) {
			return false;
		}
		if (uniqueCode != other.uniqueCode) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Server [servConfig=" + servConfig + "]";
	}

	public NodeAddr getNodeAddr() {
		return nodeAddr;
	}

	public void setNodeAddr(NodeAddr nodeAddr) {
		this.nodeAddr = nodeAddr;
	}

	public String node() {
		return nodeAddr.getIp() + ":" + nodeAddr.getPort();
	}

}