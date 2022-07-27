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
package com.wuba.wlock.registry.server.context;

import com.wuba.wlock.registry.server.communication.IServerHandler;

public class WLockRegistryContext {
	
	private WLockRegistryChannel channel;
	
	private byte[] request;
	
	private byte[] response;
	
	private IServerHandler serverHandler;
	
	private boolean isAck = false;
	
	private int clientVersion = 1;
	
	public WLockRegistryContext(byte[] requestBuffer, WLockRegistryChannel channel, IServerHandler serverHandler) {
		this.request = requestBuffer;
		this.channel = channel;
		this.serverHandler = serverHandler;
	}

	public WLockRegistryChannel getChannel() {
		return channel;
	}

	public void setChannel(WLockRegistryChannel channel) {
		this.channel = channel;
	}

	public byte[] getRequest() {
		return request;
	}

	public void setRequest(byte[] request) {
		this.request = request;
	}

	public byte[] getResponse() {
		return response;
	}

	public void setResponse(byte[] response) {
		this.response = response;
	}

	public IServerHandler getServerHandler() {
		return serverHandler;
	}

	public void setServerHandler(IServerHandler serverHandler) {
		this.serverHandler = serverHandler;
	}

	public boolean isAck() {
		return isAck;
	}

	public void setAck(boolean isAck) {
		this.isAck = isAck;
	}

	public int getClientVersion() {
		return clientVersion;
	}

	public void setClientVersion(int clientVersion) {
		this.clientVersion = clientVersion;
	}


	
}
