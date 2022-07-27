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
package com.wuba.wlock.registry.server.entity;

import com.wuba.wlock.registry.server.context.WLockRegistryContext;

public class ChannelMessage {
	
	private String keyName;
	private WLockRegistryContext context;
	private ChannelMessageType channelType;
	
	public ChannelMessage(String key, ChannelMessageType type, WLockRegistryContext context) {
		this.keyName = key;
		this.channelType = type;
		this.context = context;
	}
	
	public String getKeyName() {
		return keyName;
	}
	
	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}
	
	public WLockRegistryContext getContext() {
		return context;
	}
	
	public void setContext(WLockRegistryContext context) {
		this.context = context;
	}
	
	public ChannelMessageType getChannelType() {
		return channelType;
	}
	
	public void setChannelType(ChannelMessageType channelType) {
		this.channelType = channelType;
	}

}
