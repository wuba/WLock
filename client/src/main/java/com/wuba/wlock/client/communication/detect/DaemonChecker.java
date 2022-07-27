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
package com.wuba.wlock.client.communication.detect;

import com.wuba.wlock.client.communication.Server;
import com.wuba.wlock.client.communication.ServerState;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.registryclient.entity.NodeAddr;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class DaemonChecker implements Runnable {
	private static final Log logger = LogFactory.getLog(DaemonChecker.class);
	private final static ConcurrentHashMap<NodeAddr, DaemonChecker> checkmap = new ConcurrentHashMap<NodeAddr, DaemonChecker>();
	private static final Object locker = new Object();
	private NodeAddr nodeAddr ;
	private ConcurrentHashMap<Server, Boolean> servers = new ConcurrentHashMap<Server, Boolean>();
	
	public DaemonChecker(NodeAddr nodeAddr){
		this.nodeAddr = nodeAddr;
	}
	
	public static boolean check(Server serv) {
		if (serv.isDelete()) {
			return false;
		}
		
		if (serv.getState().equals(ServerState.Closing)) {
			return false;
	    }

		NodeAddr nodeAddr = serv.getNodeAddr();
		DaemonChecker daemonChecker = checkmap.get(nodeAddr);
		if (daemonChecker == null) {
			synchronized (locker) {
				daemonChecker = checkmap.get(nodeAddr);
				if (daemonChecker == null) {
					daemonChecker = new DaemonChecker(nodeAddr);
					Thread thread = new Thread(daemonChecker);
					thread.setName("WLockClient server state checker" + serv.toString());
					thread.setDaemon(true);
					thread.start();

					checkmap.put(nodeAddr, daemonChecker);
					logger.info(Version.INFO + "channel checkmap : " + checkmap.keySet());
				}
			}
		}

		serv.setState(ServerState.Testing);
		daemonChecker.servers.put(serv, true);
		return true;
	}
	
	private static boolean ping(NodeAddr nodeAddr) {
		String serverIp = nodeAddr.getIp();
		int port = nodeAddr.getPort();
		
		String strAddr = serverIp
						+ ":"
						+ port; 
		logger.warn(Version.INFO + "test server:" + strAddr);
		
		Socket sock = null;
		
		try {
			InetSocketAddress addr = new InetSocketAddress(serverIp, port);
			sock = new Socket();
			sock.connect(addr, 1000 * 3);
			boolean res = sock.isConnected();
			if (!res) {
				logger.info(Version.INFO + "WLockServer(" + strAddr + ") ping failed");
			}
			return res;
		} catch (Exception e) {
			logger.info(Version.INFO + "WLockServer(" + strAddr + ") is dead");
		}finally{
			if(sock != null){
				try {
					sock.close();
				} catch (IOException e) {
					logger.error(Version.INFO + "", e);
				}
			}
		}
		return false;
	}
	
	public static void removeCheckServer(Server serv) {
		DaemonChecker daemonChecker = checkmap.get(serv.getNodeAddr());
		if (daemonChecker != null) {
			daemonChecker.servers.remove(serv);
		}
	}

	private void notifyServerState(ServerState state) {
		if (!servers.isEmpty()) {
			for (Server server: servers.keySet()) {
				this.statusChanged(server, state);
			}
		}
	}

	public void statusChanged(Server server, ServerState state){
		if (state == null) {
			return;
		}

		if (server.isDelete()) {
			DaemonChecker.removeCheckServer(server);
			return;
		}

		if (state == ServerState.Normal) {
			try {
				server.connect();
				DaemonChecker.removeCheckServer(server);
			} catch (Exception e) {
				logger.error("statusChanged connect error", e);
			}
		}

		server.setState(state);
	}

	@Override
	public void run() {
		try {
			while(true) {
				try {
					Thread.sleep(1000 * 10);

					if(!servers.isEmpty()) {
						if(ping(nodeAddr)) {
							logger.info(Version.INFO + "WLockserver(" + nodeAddr + ") ping success");
							notifyServerState(ServerState.Normal);
						} else {
							notifyServerState(ServerState.Dead);
						}
					}
				} catch(Exception ex) {
					logger.error(Version.INFO + "checker thread error", ex);
				}
			}
		} catch (Exception e) {
			logger.warn(Version.INFO + " DetectTimeJob catch IOException, cause : " + e.getMessage());
		} finally {
			checkmap.remove(nodeAddr);
		}
	}
}
