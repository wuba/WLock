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

import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.registryclient.entity.DaemonCheckTask;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.Thread.State;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RegistryDaemonChecker {
	private static final Log logger = LogFactory.getLog(RegistryDaemonChecker.class);
	private static final long CHECKER_REGISTRY_ALIVE_TIME = 1000 * 86400; // 检测线程存活时间
	private static final LinkedBlockingQueue<DaemonCheckTask> taskQueue = new LinkedBlockingQueue<DaemonCheckTask>();
	private static final Object regLocker = new Object();
	private static Thread regChecker = null;

	private static long regCheckerStartTime;

	public static void check(DaemonCheckTask task) {
		synchronized (regLocker) {
			boolean flag = false;
			Iterator<DaemonCheckTask> iterator = taskQueue.iterator();
			while (iterator.hasNext()) {
				DaemonCheckTask tempTask = iterator.next();
				if (task.isEqualObject(tempTask)) {
					flag = true;
					break;
				}
			}
			if (!flag) {
				taskQueue.offer(task);
			}
			if (regChecker == null || regChecker.getState() == State.TERMINATED) {
				regChecker = createRegistryChecker();
				regCheckerStartTime = System.currentTimeMillis();
				regChecker.start();
			}
		}
	}
	
	private static boolean ping(DaemonCheckTask task) {
		String strAddr = task.getRegistryIP() + ":" + task.getRegistryPort();
		logger.debug(Version.INFO + ", test registry server:" + strAddr);
		Socket sock = null;
		try {
			InetSocketAddress addr = new InetSocketAddress(task.getRegistryIP(), task.getRegistryPort());
			sock = new Socket();
			sock.connect(addr, 1000 * 3);
			return sock.isConnected();
		} catch (IOException e) {
			logger.debug(Version.INFO + ", WLock Registry Server(" + strAddr + ") is dead");
		} finally {
			if (sock != null) {
				try {
					sock.close();
				} catch (IOException e) {
					logger.error("ping sock close error", e);
				}
			}
		}
		return false;
	}

	private static Thread createRegistryChecker() {
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				while (true) {
					
					synchronized (regLocker) {
						if (taskQueue.size() == 0 || (System.currentTimeMillis() - regCheckerStartTime) > CHECKER_REGISTRY_ALIVE_TIME) {
							logger.info(Version.INFO + ", all registry server is ok the checker thread exit");
							return;
						}
					}
					
					DaemonCheckTask task = null;
					try {
						
						synchronized (regLocker) { 
							task = taskQueue.poll(1000, TimeUnit.MILLISECONDS);
						}
						
						if (null == task) {
							continue;
						}
						
						// 注册中心配置链表中不存在该配置
						if (!RegistryKeyFactory.getInsatnce().getSerPool().isExistRegistryServerConfig(task.getRegistryIP(), task.getRegistryPort())) {
							continue;
						}
						
						if (!ping(task)) {
							check(task);
							try {
								Thread.sleep(5 * 1000);
							} catch (Exception e) {
							}
							continue;
						}
						
						RegistryServerPool serverPool = RegistryKeyFactory.getInsatnce().getSerPool();
						RegistryServer registryServer = serverPool.getServer(task.getHashKey());
						
						if (null != registryServer && registryServer.isConnectCorrectRegistry()) {
							continue;
						}
						
						// 尝试链接应该链接的注册中心
						serverPool.oldRegistryServerResumeCallBack(task.getHashKey());
					} catch (Exception e) {
						check(task);
						logger.warn(Version.INFO + ", checker registry thread error", e);
					}
					
				}
			}
			
		});

		t.setName("Wlock Registry Client server state checker");
		t.setDaemon(true);
		
		return t;
	}

}
