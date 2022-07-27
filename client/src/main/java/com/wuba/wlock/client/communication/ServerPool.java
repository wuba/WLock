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
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.registryclient.entity.NodeAddr;
import com.wuba.wlock.client.util.ThreadRenameFactory;
import com.wuba.wlock.client.watch.EventCachedHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class ServerPool {
	private static final Log logger = LogFactory.getLog(ServerPoolHandler.class);

	private WLockClient wlockClient;

	private final List<Server> servList = new ArrayList<Server>();

	private Server master;

	private Integer groupId;

	private static Object lock = new Object();
	public static final ScheduledExecutorService scheduleExecutorService = Executors.newScheduledThreadPool(1, new ThreadRenameFactory("ServerPool_"));

	public ServerPool(WLockClient wlockClient, List<NodeAddr> nodeAddrList, Integer groupId) throws IOException {
		this.wlockClient = wlockClient;
		this.groupId = groupId;

		if (nodeAddrList != null) {
			for (NodeAddr nodeAddr : nodeAddrList) {
				Server serv = new Server(this.wlockClient, nodeAddr);

				servList.add(serv);
				if (nodeAddr.getIsMaster()) {
					this.setMaster(serv);
					logger.info(Version.INFO + " master node is : " + this.master);
				}
				Collections.sort(servList, new Comparator<Server>() {
					@Override
					public int compare(Server o1, Server o2) {
						return o1.getServConfig().getIp().hashCode()-o2.getServConfig().getIp().hashCode();
					}
				});
			}
		}

		if (servList.size() == 0) {
			throw new IOException(Version.INFO + " can't connect to server serverPool size:0");
		}
	}

	public Server getTargetServer() {
		Server targetServer = null;
		synchronized (lock) {
			if (master == null || !master.getState().equals(ServerState.Normal)) {
				logger.warn(Version.INFO + " master node is null, registry key : " + this.wlockClient.getRegistryKey().getRegistryKey());
				if (servList.size() == 0) {
					logger.error(Version.INFO + " server list is empty, no avaliable server, registry key : " + this.wlockClient.getRegistryKey().getRegistryKey());
				} else {
					int index = 0;
					for (int i = 0; i < servList.size(); i++) {
						Server server = this.servList.get(i);
						if (server.getServConfig().isMaster()) {
							index = i;
							break;
						}
					}
					for (int i = 0, len = servList.size(); i < len; i++) {
						Server server = this.servList.get(index);
						if (server.getState().equals(ServerState.Normal)) {
							targetServer = server;
							logger.info("server reboot.get new server " + targetServer.toString());
							break;
						}
						index = (++index) % (servList.size());
					}
				}
			} else {
				targetServer = master;
			}
		}

		return targetServer;
	}
	
	public Server getCandidateServer() {
		Server targetServer = null;
		synchronized (lock) {
			Random random = new Random();
			int idx = random.nextInt(1000)%(servList.size());
			
			if (master == null) {
				targetServer = this.servList.get(idx);
			} else {
				for (int i = 0; i < servList.size(); i++) {
					Server server = this.servList.get(idx);
					if (!server.equals(master) && server.getState().equals(ServerState.Normal)) {
						targetServer = server;
						break;
					}
					idx = (++idx)%(servList.size());
				}
			}
		}
		
		return targetServer;
	}
	
	public Future<Boolean> addServer(final NodeAddr nodeAddr) throws IOException {
		return scheduleExecutorService.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() {
				synchronized (lock) {
					Server serv = new Server(wlockClient, nodeAddr);
					servList.add(serv);
					Collections.sort(servList, new Comparator<Server>() {
						@Override
						public int compare(Server o1, Server o2) {
							return o1.getServConfig().getIp().hashCode() - o2.getServConfig().getIp().hashCode();
						}
					});
				}

				return true;
			}

		});
	}
	
	public void deleteServer(NodeAddr node) {
		synchronized (lock) {
			for (Server server : servList) {
				// 因为此时存在连个一样的节点 , Collections.sort 是稳定性排序,所以此时取到的第一个节点就是需要删除的节点
				if (server.getServConfig().getIp().equals(node.getIp()) && server.getServConfig().getPort() == node.getPort()) {
					servList.remove(server);
					server.destroy();
					if (this.master == server) {
						this.master = null;
					}
					Collections.sort(servList, new Comparator<Server>() {
						@Override
						public int compare(Server o1, Server o2) {
							return o1.getServConfig().getIp().hashCode()-o2.getServConfig().getIp().hashCode();
						}
					});
					break;
				}
			}
		}
	}

	public void masterChangeCallback(NodeAddr oldMaster, NodeAddr newMaster) {
		synchronized (lock) {
			for (Server server : servList) {
				if (server.getServConfig().getIp().equals(newMaster.getIp()) && server.getServConfig().getPort() == newMaster.getPort()) {
					setMaster(server);
					break;
				}
			}
			logger.info(Version.INFO + ", masterchanged oldmaster " + oldMaster + ", newmaster " + newMaster);
		}
	}
	
	public Server getMaster() {
		return master;
	}

	public void setMaster(Server master) {
		this.master = master;
		EventCachedHandler.getInstance(wlockClient).eventMakeUp(groupId);
	}

	public List<Server> getServList() {
		return servList;
	}

	public void setServer(List<NodeAddr> nodeAddrList) throws IOException, ExecutionException, InterruptedException {
		Server master = this.master;
		List<Server> servList = this.servList;

		NodeAddr masterNodeAddr = null;

		List<NodeAddr> add = new ArrayList<NodeAddr>();
		for (NodeAddr nodeAddr: nodeAddrList) {
			boolean isExit = false;
			for (Server server: servList) {
				if (nodeAddr.equals(server.getNodeAddr())) {
					isExit = true;
					break;
				}
			}

			if (!isExit) {
				add.add(nodeAddr);
			}

			if (nodeAddr.getIsMaster()) {
				masterNodeAddr = nodeAddr;
			}
		}

		List<NodeAddr> delete = new ArrayList<NodeAddr>();
		for (Server server: servList) {
			boolean isExit = false;
			for (NodeAddr nodeAddr: nodeAddrList) {
				if (nodeAddr.equals(server.getNodeAddr())) {
					isExit = true;
					break;
				}
			}

			if (!isExit) {
				delete.add(server.getNodeAddr());
			}
		}

		Future<Boolean> masterFuture = null;
		if (!add.isEmpty()) {
			for (NodeAddr nodeAddr: add) {
				Future<Boolean> future = addServer(nodeAddr);
				if (nodeAddr.getIsMaster()) {
					masterFuture = future;
				}
			}
		}

		if (master == null || (masterNodeAddr != null && !masterNodeAddr.equals(master.getNodeAddr()))) {
			if (masterFuture != null) {
				masterFuture.get();
			}

			masterChangeCallback(master == null ? null : master.getNodeAddr(), masterNodeAddr);
		}

		if (!delete.isEmpty()) {
			for (NodeAddr nodeAddr: delete) {
				deleteServer(nodeAddr);
			}
		}
	}

	public void destroy() {
		Iterator<Server> iterator = getServList().iterator();
		while (iterator.hasNext()) {
			Server server = iterator.next();
			server.destroy();
			iterator.remove();
		}
	}
}
