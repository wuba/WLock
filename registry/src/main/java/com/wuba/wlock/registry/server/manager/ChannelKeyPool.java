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
package com.wuba.wlock.registry.server.manager;

import com.wuba.wlock.registry.server.context.WLockRegistryChannel;
import com.wuba.wlock.registry.server.entity.Key;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class ChannelKeyPool {

	private static ChannelKeyPool instance = new ChannelKeyPool();

	private static ConcurrentHashMap<String, Key> keyMap = new ConcurrentHashMap<String, Key>();
	
	private static ConcurrentHashMap<Channel, String/*HashKey*/> channelMap = new ConcurrentHashMap<Channel, String/*HashKey*/>();
	
	private ChannelKeyPool() {
	}

	public static ChannelKeyPool getInstance() {
		return instance;
	}

	public ConcurrentHashMap<String, Key> getKeyMap() {
		return keyMap;
	}

	public ArrayList<WLockRegistryChannel> getChannelList(String key) {
		if (keyMap.containsKey(key)) {
			return keyMap.get(key).getChannelList();
		}
		return null;
	}

	public void setChannelList(String key, ArrayList<WLockRegistryChannel> cList) {
		if (keyMap.containsKey(key)) {
			keyMap.get(key).setChannelList(cList);
		} else {
			Key keyObj = new Key(key, cList);
			keyMap.put(key, keyObj);
		}
	}

	public String getHashKeyByChannel(Channel channel) {
		if (null == channel) {
			return null;
		}
		if (channelMap.containsKey(channel)) {
			return channelMap.get(channel);
		}
		return null;
	}

	public void addChannelHashKeyMapping(Channel channel, String hashKey) {
		if (null == channel) {
			return;
		}
		if (!channelMap.containsKey(channel)) {
			channelMap.put(channel, hashKey);
		} 
	}
	
	public void removeChannelHashKeyMapping(Channel channel) {
		if (null == channel) {
			return;
		}
		channelMap.remove(channel);
	}
	
}
