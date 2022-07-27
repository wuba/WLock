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
package com.wuba.wlock.server.communicate.retrans;

import com.wuba.wlock.server.util.TimeUtil;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.Thread.State;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class RetransDaemonChecker {
	private static final Logger logger = LoggerFactory.getLogger(RetransDaemonChecker.class);
	private static List<RetransServer> checklist = new LinkedList<RetransServer>();
	private static final long CHECKER_RETRANS_ALIVE_TIME = 1000 * 86400;
	private static final Object RETRANS_LOCKER = new Object();
	private static Thread retransChecker = null;

	private static long dispatchCheckerStartTime;

	public static void check(RetransServer server) {
		server.setState(RetransServerState.Testing);

		if (checklist.contains(server) || server.isDelete()) {
			return;
		}
		synchronized (RETRANS_LOCKER) {
			if (!checklist.contains(server) && !server.isDelete()) {
				checklist.add(server);
			}

			if (retransChecker == null || retransChecker.getState() == State.TERMINATED) {
				retransChecker = createRetransChecker();
				dispatchCheckerStartTime = TimeUtil.getCurrentTimestamp();
				retransChecker.start();
			}
		}
	}

	private static boolean ping(RetransServer server) {
		String strAddr = server.getRetransServeConfig().getAddr();
		logger.info("test retrans server:" + strAddr);

		Socket sock = null;
		try {
			InetSocketAddress addr = new InetSocketAddress(server.getRetransServeConfig().getIp(), server.getRetransServeConfig().getPort());
			sock = new Socket();
			sock.connect(addr, 1000 * 3);
			return sock.isConnected();
		} catch (IOException e) {
			logger.warn("WLock Retrans Server(" + strAddr + ") is dead");
		} finally {
			if (sock != null) {
				try {
					sock.close();
				} catch (IOException e) {
					logger.error("ping clos");
				}
			}
		}

		return false;
	}

	private static Thread createRetransChecker() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean flag = false;
				while (true) {
					synchronized (RETRANS_LOCKER) {
						if (checklist.size() == 0 && (TimeUtil.getCurrentTimestamp() - dispatchCheckerStartTime) > CHECKER_RETRANS_ALIVE_TIME) {
							logger.info("all retrans server is ok the checker thread exit");
							return;
						}
					}

					try {
						if (checklist.size() > 0) {							
							for (int i = 0; i < checklist.size(); i++) {
								RetransServer server = null;
								synchronized (RETRANS_LOCKER) {
									if (i < checklist.size()) {
										server = checklist.get(i);
										if (server.isDelete()) {
											checklist.remove(i);
											continue;
										}
									}
								}
								flag = false;
								if (server != null && !server.getState().equals(RetransServerState.Normal)) {
									if (ping(server)) {
										try {
											RetransChannel retransChannel = server.createChannel();
											Channel channel = retransChannel.getChannel();
											if (channel != null && channel.isOpen()) {
												server.setState(RetransServerState.Normal);
												flag = true;
											} 
										} catch (Exception e) {
											flag = false;
										}
									}
									if (flag) {
										synchronized (RETRANS_LOCKER) {
											checklist.remove(i);
										}
									}
								}
							}
						} else {
							logger.info("no server to be checked, so checker exit.");
							return;
						}
					} catch (Throwable th) {
						logger.warn("checker retrans thread error", th);
					}
					
					try {
						Thread.sleep(1000 * 10);
					} catch (InterruptedException e) {
						logger.error("createRetransChecker sleep error", e);
					}
				}

			}
		});

		t.setName("WLock retrans server state checker");
		t.setDaemon(true);
		return t;
	}

}
