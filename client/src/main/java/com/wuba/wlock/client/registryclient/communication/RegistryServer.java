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
package com.wuba.wlock.client.registryclient.communication;

import com.wuba.wlock.client.communication.ServerState;
import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.exception.OperationCanceledException;
import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import com.wuba.wlock.client.registryclient.protocal.RegistryProtocol;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RegistryServer {
	
	private ServerState state = ServerState.Normal;
	
	private RegistryChannelPool channelPool;
	
	private ServerConfig servConfig;
	
	private RegistryServerPool serverPool;

	private String hashKey;
	
	private int currentIdIndex; // 当前链接位置
	
	private int formalIdIndex; // 应该链接位置 (验证时,位置加IP验证)
	
	public RegistryServer(ServerConfig servConfig, RegistryServerPool serPool, int currentIdIndex, String hashKey) throws IOException, RegistryClientRuntimeException {
		this.currentIdIndex = currentIdIndex;
		this.hashKey = hashKey;
		this.serverPool = serPool;
		this.servConfig = servConfig;
		this.formalIdIndex = serverPool.getFormalIdIndex(hashKey);
		this.channelPool = new RegistryChannelPool(servConfig, this);
	}
	
	/**
	 * 当前hashKey是否链接了正确的注册中心
	 * @return
	 * @throws RegistryClientRuntimeException 
	 */
	public boolean isConnectCorrectRegistry() throws RegistryClientRuntimeException {
		synchronized (RegistryServer.class) {
			this.formalIdIndex = serverPool.getFormalIdIndex(hashKey);
			if (this.currentIdIndex != this.formalIdIndex) {
				return false;
			}
			ServerConfig formalServerConfig = serverPool.getFormalServerConfig(hashKey);
			if (formalServerConfig.getIp().equals(servConfig.getIp()) && formalServerConfig.getPort() == servConfig.getPort()) {
				this.currentIdIndex = this.formalIdIndex;
				return true;
			}
			return false;
		}
	}

	public void asyncInvoke(RegistryProtocol request) throws IOException, ProtocolException, RegistryClientRuntimeException {
		RegistryNIOChannel channel = null;
		String hashKey = null;
		boolean exceptionFlag = false;
		try {
			channel = this.channelPool.getChannel();
			hashKey = channel.getServer().getHashKey();
			if (!channel.isOpen()) {
				throw new IOException(Version.INFO + ", channel is close:" + channel.toString());
			}

			WindowData cd = new WindowData(request, channel);
			channel.asyncSend(cd);
		} catch (IOException ex) {
			exceptionFlag = true;
			if (channel != null) {
				RegistryKeyFactory.getInsatnce().getSerPool().addDaemonCheckTask(hashKey, channel.getServer().getServerConfig());
				destroyChannel(channel);
			}
			throw ex;
		} finally {
			if (exceptionFlag && null != hashKey) {
				RegistryKeyFactory.getInsatnce().getSerPool().replaceRegistryServer(hashKey);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T syncInvoke(RegistryProtocol request, int waitCount, int timeout)
			throws IOException, CommunicationException, InterruptedException, ProtocolException, TimeoutException, OperationCanceledException, RegistryClientRuntimeException {
		RegistryNIOChannel channel = null;
		long sid = request.getSessionId();
		request.setSessionId(sid);
		boolean exceptionFlag = false;
		String hashKey = null;
		try {
			channel = this.channelPool.getChannel();
			hashKey = channel.getServer().getHashKey();
			if (!channel.isOpen()) {
				throw new IOException(Version.INFO + ", channel is close:" + channel.toString());
			}

			WindowData cd = new WindowData(sid, waitCount, request, channel);
			channel.registerRec(sid, cd);
			channel.asyncSend(cd);
			byte[][] data = channel.receive(sid, timeout);

			if (waitCount > 1) {
				List<RegistryProtocol> list = new ArrayList<RegistryProtocol>();
				for (int i = 0; i < waitCount; i++) {
					list.add(RegistryProtocol.fromBytes(data[i]));
				}
				return (T) list;
			} else {
				return (T) RegistryProtocol.fromBytes(data[0]);
			}
		} catch (IOException ex) {
			exceptionFlag = true;
			if (channel != null) {
				RegistryKeyFactory.getInsatnce().getSerPool().addDaemonCheckTask(hashKey, channel.getServer().getServerConfig());
				destroyChannel(channel);
			}
			throw ex;
		} finally {
			if (channel != null) {
				channel.unregisterRec(sid);
			}
			if (exceptionFlag && null != hashKey) {
				RegistryKeyFactory.getInsatnce().getSerPool().replaceRegistryServer(hashKey);
			}
		}
	}

	private void destroyChannel(RegistryNIOChannel channel) {
		channelPool.destroy(channel);
	}

	public ServerConfig getServerConfig() {
		return servConfig;
	}

	public RegistryChannelPool getChannelPool() {
		return channelPool;
	}

	public ServerState getState() {
		return state;
	}

	public void setState(ServerState state) {
		this.state = state;
	}

	public RegistryServerPool getServerPool() {
		return serverPool;
	}

	public void destroy() {
		this.channelPool.destroyAll();
	}

	public int getCurrentIdIndex() {
		return currentIdIndex;
	}

	public void setCurrentIdIndex(int currentIdIndex) {
		this.currentIdIndex = currentIdIndex;
	}

	public String getHashKey() {
		return hashKey;
	}

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}

	public int getFormalIdIndex() {
		return formalIdIndex;
	}

	public void setFormalIdIndex(int formalIdIndex) {
		this.formalIdIndex = formalIdIndex;
	}

	@Override
	public String toString() {
		return servConfig.getIp() + ":" + servConfig.getPort();
	}
	
}
