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

import com.wuba.wlock.client.helper.AutoResetEvent;
import com.wuba.wlock.client.registryclient.protocal.RegistryProtocol;

public class WindowData {
	private long sessionId;
	private RegistryProtocol request;
	private AutoResetEvent event;
	private RegistryNIOChannel channel;
	Boolean result;
	byte[][] data;
	private boolean isCanceled;
	private int idx = 0;

	public WindowData(RegistryProtocol request, RegistryNIOChannel channel) {
		 this(0, 1, request, channel);
	}
	
	public WindowData(long sessionID, int waitCount, RegistryProtocol request, RegistryNIOChannel channel) {
		this.sessionId = sessionID;
		this.event = new AutoResetEvent(waitCount);
		this.request = request;
		this.channel = channel;
		this.data = new byte[64][];
	}
	
	public WindowData(AutoResetEvent event) {
		this.event = event;
	}

	public AutoResetEvent getEvent() {
		return event;
	}

	public void setEvent(AutoResetEvent event) {
		this.event = event;
	}

	public byte[][] getData() {
		return data;
	}

	public void setData(byte[][] data) {
		this.data = data;
	}

	public Boolean getResult() {
		return result;
	}
	
	public void appendData(long sid, byte[] buf) {
		data[idx++] = buf;
	}
	
	public void countDown() {
		event.countDown();
	}

	public void setResult(Boolean result) {
		this.result = result;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}
	
	public boolean isCanceled() {
		return isCanceled;
	}

	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	public RegistryNIOChannel getChannel() {
		return channel;
	}

	public void setChannel(RegistryNIOChannel channel) {
		this.channel = channel;
	}

	public RegistryProtocol getRequest() {
		return request;
	}

	public void setRequest(RegistryProtocol request) {
		this.request = request;
	}
	
}
