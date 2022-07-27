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
package com.wuba.wlock.server.client;

import com.wuba.wlock.server.communicate.protocol.AcquireLockRequest;
import com.wuba.wlock.server.communicate.protocol.RenewLockRequest;
import com.wuba.wlock.server.communicate.protocol.WatchLockRequest;
import com.wuba.wlock.server.config.PaxosConfig;
import com.wuba.wlock.server.domain.LockOwner;
import com.wuba.wlock.server.util.ThreadPoolUtil;
import com.wuba.wlock.server.util.ThreadRenameFactory;
import com.wuba.wlock.server.watch.IWatchService;
import com.wuba.wlock.server.watch.WatchIndex;
import com.wuba.wlock.server.watch.impl.WatchServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientManager.class);
	private ConcurrentHashMap<Integer, ClientGroupManager> keyClientMap = new ConcurrentHashMap<Integer, ClientGroupManager>();
	private static ClientManager instance = new ClientManager();
	private ScheduledExecutorService scheduledExcutorService;

	private ClientManager() {
		for (int i = 0; i < PaxosConfig.getInstance().getGroupCount(); i++) {
			keyClientMap.put(i, new ClientGroupManager(i));
		}
		
		scheduledExcutorService = ThreadPoolUtil.newSingleThreadScheduledExecutor(new ThreadRenameFactory("ClientManagerScheduleThread-"));
	}
	
	public static ClientManager getInstance() {
		return instance;
	}
	
	public void print() {
		scheduledExcutorService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < PaxosConfig.getInstance().getGroupCount(); i++) {
					keyClientMap.get(i).printLockClient();
				}
			}
			
		}, 20, 20, TimeUnit.SECONDS);
	}
	
	/**
	 * 连接关闭回调处理（注意是否会产生死锁）
	 * @param channel
	 */
	public void channelClosed(Channel channel) {
		if (channel == null) {
			return;
		}
		
		Iterator<Entry<Integer, ClientGroupManager>> iter = keyClientMap.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Integer, ClientGroupManager> entry = iter.next();
			ClientGroupManager clientGroupManager = entry.getValue();
			clientGroupManager.channelClosed(channel);
		}
	}

	/**
	 * 迁移结束，根据密钥删除lockClient
	 */
	public void removeLockClient(String registryKey, int groupId) {
		if (StringUtils.isNotEmpty(registryKey)) {
			LOGGER.error("removeLockClient registryKey is null");
			return;
		}

		ClientGroupManager clientGroupManager = keyClientMap.get(groupId);
		if (clientGroupManager != null) {
			clientGroupManager.removeLockClient(registryKey);
		}
	}
	
	/**
	 * 添加client
	 * @param lockkey
	 * @param lockClient
	 * acquire, watch情况
	 */
	public void addLockClient(String lockkey, LockClient lockClient, int groupId,Channel channel) {
		if (lockkey == null || lockClient == null) {
			LOGGER.error("add lock client null.");
			return;
		}
		if (groupId < 0 || groupId >= PaxosConfig.getInstance().getGroupCount()) {
			LOGGER.error("group {} not illegal",groupId);
			return;
		}
		ClientGroupManager clientGroupManager = keyClientMap.get(groupId);
		if (clientGroupManager == null) {
			clientGroupManager = new ClientGroupManager(groupId);
			keyClientMap.put(groupId, clientGroupManager);
		}
		clientGroupManager.addLockClient(lockkey, lockClient, channel);
	}
	
	/**
	 * 删除client
	 * @param lockkey
	 * @param lockClient
	 * release, expired情况
	 */
	public void removeLockClient(String lockkey, LockClient lockClient, int groupId) {
		if (lockkey == null || lockClient == null) {
			LOGGER.error("add lock client null.");
			return;
		}
		
		ClientGroupManager clientGroupManager = keyClientMap.get(groupId);
		if (clientGroupManager != null) {
			clientGroupManager.removeLockClient(lockkey, lockClient);
		}
	}
	
	/**
	 * 获取owner client信息
	 * @param lockkey
	 * @param owner
	 * @return
	 */
	public LockClient getLockOwnerClient(String lockkey, LockOwner owner, int groupId) {
		if (lockkey == null || owner == null) {
			LOGGER.error("get lock owner client null.");
			return null;
		}
		
		LOGGER.debug("getLockOwnerClient......, lockkey : " + lockkey);
		
		ClientGroupManager clientGroupManager = keyClientMap.get(groupId);
		if (clientGroupManager != null) {
			return clientGroupManager.getLockOwnerClient(lockkey, owner);
		}
		
		return null;
	}
	
	public Channel getChannelByID(int channelId, int groupId) {
		ClientGroupManager clientGroupManager = keyClientMap.get(groupId);
		if (clientGroupManager != null) {
			return clientGroupManager.getChannelByID(channelId);
		}
		
		return null;
	}
	
	public LockClient createLockClient(String lockkey, int groupId, Channel channel, AcquireLockRequest acquireLockReq) {
		LockClient lockClient = new LockClient(channel.getId(), lockkey, acquireLockReq.getVersion());
		lockClient.setcHost(acquireLockReq.getHost());
		lockClient.setcThreadID(acquireLockReq.getThreadID());
		lockClient.setcPid(acquireLockReq.getPid());
		lockClient.setGroupId(groupId);
		lockClient.setWatchID(acquireLockReq.getWatchID());
		return lockClient;
	}

	public LockClient createLockClient(String lockkey, int groupId, Channel channel, WatchLockRequest watchLockReq) {
		LockClient lockClient = new LockClient(channel.getId(), lockkey, watchLockReq.getVersion());
		lockClient.setcHost(watchLockReq.getHost());
		lockClient.setcThreadID(watchLockReq.getThreadID());
		lockClient.setcPid(watchLockReq.getPid());
		lockClient.setGroupId(groupId);
		lockClient.setWatchID(watchLockReq.getWatchID());

		return lockClient;
	}
	
	public LockClient createLockClient(String lockkey, int groupId, Channel channel, RenewLockRequest renewLockReq) {
		LockClient lockClient = new LockClient(channel.getId(), lockkey, renewLockReq.getVersion());
		lockClient.setcHost(renewLockReq.getHost());
		lockClient.setcThreadID(renewLockReq.getThreadID());
		lockClient.setcPid(renewLockReq.getPid());
		lockClient.setGroupId(groupId);
		lockClient.setWatchID(-1);
		return lockClient;
	}

	public void removeLockClient(HashMap<String, Set<WatchIndex>> realDeletes, int groupId) {
		ClientGroupManager clientGroupManager = keyClientMap.get(groupId);
		if (clientGroupManager == null) {
			return;
		}
		clientGroupManager.removeLockClient(realDeletes);
	}

	class ClientGroupManager {
		private ConcurrentHashMap<String/*key*/, HashSet<LockClient>> keyClientMap = new ConcurrentHashMap<String, HashSet<LockClient>>();
		private ConcurrentHashMap<Integer/*channelId*/, HashSet<LockClient>/*key list*/> channelClientMap = new ConcurrentHashMap<Integer, HashSet<LockClient>>();
		private ConcurrentHashMap<Integer/*channelId*/, Channel> channelMap = new ConcurrentHashMap<Integer, Channel>();
		private IWatchService watchService;
		private ReentrantLock clientLock = new ReentrantLock();
		private int groupId;
		
		public ClientGroupManager(int groupId) {
			this.watchService = WatchServiceImpl.getInstance(); 
			this.groupId = groupId;
		}
		
		/**
		 * 连接关闭回调处理（注意是否会产生死锁）
		 * @param channel
		 */
		public void channelClosed(Channel channel) {
			if (channel == null) {
				return;
			}
			
			if (!this.channelMap.containsKey(channel.getId())) {
				return;
			}
			
			clientLock.lock();
			try {
				Map<String, List<LockClient>> removeClients = removeChannel(channel);
				if (removeClients != null && !removeClients.isEmpty()) {
					Iterator<Entry<String, List<LockClient>>> iter = removeClients.entrySet().iterator();
					while (iter.hasNext()) {
						Entry<String, List<LockClient>> entry = iter.next();
						String lockkey = entry.getKey();
						List<LockClient> clientList = entry.getValue();
						
						HashSet<LockClient> keyClients = this.keyClientMap.get(lockkey);
						if (keyClients == null || keyClients.isEmpty()) {
							LOGGER.error("remove key client null, lockkey {}, channel {}.", lockkey, channel);
						} else {
							keyClients.removeAll(clientList);
							if (keyClients.isEmpty()) {
								this.keyClientMap.remove(lockkey);
							}
						}
						
						this.watchService.channelClosedTrigger(lockkey, clientList);
					}
				}
				
				this.channelMap.remove(channel.getId());
			} catch(Exception e) {
				LOGGER.info("channelClosed error.", e);
			} finally {
				clientLock.unlock();
			}
		}

		/**
		 * 删除channel
		 * @param channel
		 * @return
		 */
		public Map<String, List<LockClient>> removeChannel(Channel channel) {
			if (channel == null || channel.getRemoteAddress() == null) {
				LOGGER.error("remove channel is null.");
				return null;
			}
			
			Map<String, List<LockClient>> removedClients = new HashMap<String, List<LockClient>>();
			clientLock.lock();
			try {
				int channelId = channel.getId();
				HashSet<LockClient> clientSet = channelClientMap.get(channelId);
				if (clientSet == null || clientSet.isEmpty()) {
					return null;
				}
				Iterator<LockClient> keyIter = clientSet.iterator();
				while(keyIter.hasNext()) {
					LockClient lockClient = keyIter.next();
					String lockkey = lockClient.getLockkey();
					List<LockClient> removeList = removedClients.get(lockkey);
					if (removeList == null) {
						removeList = new LinkedList<LockClient>();
					}
					removeList.add(lockClient);
					removedClients.put(lockkey, removeList);
				}
				channelClientMap.remove(channelId);
				return removedClients;
			} catch(Exception e) {
				LOGGER.error("remove client channel error.", e);
			} finally {
				clientLock.unlock();
			}
			
			return removedClients;
		}
		
		/**
		 * 添加client
		 * @param lockkey
		 * @param lockClient
		 * acquire, watch情况
		 */
		public void addLockClient(String lockkey, LockClient lockClient,Channel channel) {
			if (lockkey == null || lockClient == null) {
				LOGGER.error("add lock client null.");
				return;
			}
			
			clientLock.lock();
			try {
				if (channel != null && channel.getRemoteAddress() != null) {
					channelMap.put(channel.getId(),channel);
					HashSet<LockClient> clientSet1 = this.channelClientMap.get(channel.getId());
					if (clientSet1 == null) {
						clientSet1 = new HashSet<LockClient>();
					}
					clientSet1.add(lockClient);
					this.channelClientMap.put(channel.getId(), clientSet1);
					
					HashSet<LockClient> clientSet2 = this.keyClientMap.get(lockkey);
					if (clientSet2 == null) {
						clientSet2 = new HashSet<LockClient>();
					}
					clientSet2.add(lockClient);
					this.keyClientMap.put(lockkey, clientSet2);
				}
				
				LOGGER.debug("add lockclient channelMap {}, channelClientMap {}, keyClientMap {}.", channelMap, this.channelClientMap, this.keyClientMap);
			} catch(Exception e) {
				LOGGER.error("add client channel error.", e);
			} finally {
				clientLock.unlock();
			}
		}

		/**
		 * 删除client
		 * @param lockkey
		 * @param lockClient
		 * release, expired情况
		 */
		public void removeLockClient(String lockkey, LockClient lockClient) {
			if (lockkey == null || lockClient == null) {
				LOGGER.error("remove lock client null.");
				return;
			}
			
			clientLock.lock();
			try {
				Channel channel = this.channelMap.get(lockClient.getChannelId());
				if (channel != null && channel.getRemoteAddress() != null) {
					HashSet<LockClient> clientSet1 = this.channelClientMap.get(channel.getId());
					if (clientSet1 == null) {
						LOGGER.error("remove lock client error, channel client set null : {}.", channel.getRemoteAddress());
					} else {
						clientSet1.remove(lockClient);
					}
					
					if (clientSet1 == null || clientSet1.isEmpty()) {
						this.channelClientMap.remove(channel.getId());
					}
					
					HashSet<LockClient> clientSet2 = this.keyClientMap.get(lockkey);
					if (clientSet2 == null) {
						LOGGER.error("remove lock client error, client list null : {}.", lockkey);
					} else {
						clientSet2.remove(lockClient);
					}
					
					if (clientSet2 == null || clientSet2.isEmpty()) {
						this.keyClientMap.remove(lockkey);
					}
				}
				
				LOGGER.debug("remove lockclient , key {} , client {}, channelMap {}, channelClientMap {}, keyClientMap {}.", lockkey, lockClient, channelMap, this.channelClientMap, this.keyClientMap);
			} catch(Exception e) {
				LOGGER.error("remove lock client error.", e);
			} finally {
				clientLock.unlock();
			}
		}
		
		/**
		 * 获取owner client信息
		 * @param lockkey
		 * @param owner
		 * @return
		 */
		public LockClient getLockOwnerClient(String lockkey, LockOwner owner) {
			if (lockkey == null || owner == null) {
				LOGGER.error("get lock owner client null.");
				return null;
			}
			
			clientLock.lock();
			try {
				HashSet<LockClient> clientList = this.keyClientMap.get(lockkey);
				if (clientList == null) {
					LOGGER.error("get lock owner client error, client list null : {}.", lockkey);
					return null;
				}
				
				for (LockClient lockClient : clientList) {
					if (lockClient.getcHost() == owner.getIp() && lockClient.getcThreadID() == owner.getThreadId() && lockClient.getcPid() == owner.getPid()) {
						return lockClient;
					}
				}
			} catch(Exception e) {
				LOGGER.error("get lock owner client error.", e);
			} finally {
				clientLock.unlock();
			}
			
			return null;
		}
		
		public Channel getChannelByID(int channelId) {
			return this.channelMap.get(channelId);
		}

		public void removeLockClient(HashMap<String, Set<WatchIndex>> realDeletes) {
			clientLock.lock();
			try {
				for (Entry<String, Set<WatchIndex>> entry : realDeletes.entrySet()) {
					String key = entry.getKey();
					HashSet<LockClient> lockClients = keyClientMap.get(key);
					if (lockClients == null || lockClients.isEmpty()) {
						continue;
					}
					Set<WatchIndex> watchIndices = entry.getValue();
					List<LockClient> toBeDelete = lockClients.stream().filter(lockClient -> watchIndices.stream().
							collect(Collectors.groupingBy((Function<WatchIndex, Object>) watchIndex -> buildDeleteKey(watchIndex.getWatchID(),watchIndex.getHost(),watchIndex.getPid()))
					).containsKey(buildDeleteKey(lockClient.getWatchID(), lockClient.getcHost(), lockClient.getcPid())))
							.collect(Collectors.toList());
					if (toBeDelete == null || toBeDelete.isEmpty()) {
						continue;
					}
					lockClients.removeAll(toBeDelete);
					Map<Integer, List<LockClient>> deleteChanelClient = toBeDelete.stream().collect(Collectors.groupingBy(LockClient::getChannelId));
					for (Entry<Integer, List<LockClient>> deleteEntry : deleteChanelClient.entrySet()) {
						int channelId = deleteEntry.getKey();
						if (channelClientMap.containsKey(channelId)) {
							channelClientMap.get(channelId).removeAll(deleteEntry.getValue());
						} else {
							LOGGER.error("removeLockClient by watchevent channel null.");
						}
					}
				}
			} catch(Exception e) {
				LOGGER.error("removeLockClient by watchevent.", e);
			} finally {
				clientLock.unlock();
			}
		}

		public void removeLockClient(String registryKey) {
			clientLock.lock();
			try {
				if (!keyClientMap.isEmpty()) {
					Iterator<Entry<String, HashSet<LockClient>>> iterator = keyClientMap.entrySet().iterator();
					while (iterator.hasNext()) {
						Entry<String, HashSet<LockClient>> entry = iterator.next();
						String lockKey = entry.getKey();
						HashSet<LockClient> lockClients = entry.getValue();
						if (lockKey.startsWith(registryKey)) {
							if (lockClients != null && !lockClients.isEmpty()) {
								List<LockClient> removedClients = new ArrayList<>(lockClients);
								for (LockClient lockClient: lockClients) {
									removeLockClient(lockKey, lockClient);
								}

								this.watchService.migrateEndTrigger(lockKey, removedClients);
							}
						}
					}
				}

			} catch (Exception e) {
				LOGGER.error("removeLockClient error", e);
			} finally {
				clientLock.unlock();
			}
		}
		
		public void printLockClient() {
			clientLock.lock();
			try {
				StringBuilder strBuilder = new StringBuilder();
				strBuilder.append("printLockClient group : " + this.groupId).append("\n");
				strBuilder.append("channelMap " + channelMap).append("\n");
				strBuilder.append("channelClientMap " + channelClientMap).append("\n");
				strBuilder.append("keyClientMap " + keyClientMap).append("\n");
				LOGGER.info(strBuilder.toString());
			} catch(Exception e) {
				LOGGER.error("printLockClient.", e);
			} finally {
				clientLock.unlock();
			}
		}

		private String buildDeleteKey(long watchID, int host, int pid) {
			return watchID + "_" + host + "_" + pid;
		}
	}
}