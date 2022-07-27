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

import com.wuba.wlock.client.config.RegistryClientConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.registryclient.registrykey.RegistryKeyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class RegistryNIOHandler {

	private static final Log logger = LogFactory.getLog(RegistryNIOHandler.class);
	public final BlockingQueue<WindowData> writeQueue = new LinkedBlockingQueue<WindowData>();
	private static ReadWorker readWorker;
	private static WriteWorker writeWorker;
	private final static Object registryLocker = new Object();
	private static RegistryNIOHandler registryNIOHandler = null;

	private RegistryNIOHandler() throws IOException {
		readWorker = new ReadWorker();

		Thread thd1 = new Thread(readWorker);
		thd1.setName("registry client reader");
		thd1.setDaemon(true);
		thd1.start();

		writeWorker = new WriteWorker();
		Thread thd2 = new Thread(writeWorker);
		thd2.setName("registry client writer");
		thd2.setDaemon(true);
		thd2.start();
	}

	public static RegistryNIOHandler getInstance() throws ClosedChannelException, IOException {
		if (registryNIOHandler == null) {
			synchronized (registryLocker) {
				if (registryNIOHandler == null) {
					registryNIOHandler = new RegistryNIOHandler();
				}
			}
		}
		return registryNIOHandler;
	}

	public void regChannel(RegistryNIOChannel nioChannel) throws ClosedChannelException, IOException {

		synchronized (registryLocker) {
			readWorker.register(nioChannel);
		}
	}

	public void offerWriteData(WindowData cd) {
		if (writeQueue.size() > RegistryClientConfig.MAX_WRITE_QUEUE_LEN) {
			logger.error(Version.INFO + ", registry writeQueue overflow len:" + RegistryClientConfig.MAX_WRITE_QUEUE_LEN);
			return;
		}

		if (!writeQueue.offer(cd)) {
			logger.error(Version.INFO + ", the element add to writeQueue error.");
		}
	}
	
}

class WriteWorker implements Runnable {

	private static final Log logger = LogFactory.getLog(WriteWorker.class);

	@Override
	public void run() {
		while (true) {
			try {
				RegistryNIOChannel nioChannel = null;
				WindowData wd = null;
				try {
					wd = RegistryNIOHandler.getInstance().writeQueue.poll(1000, TimeUnit.MILLISECONDS);
					if (wd != null) {
						nioChannel = wd.getChannel();
						SocketChannel sockChannel = nioChannel.getSockChannel();
						if (sockChannel != null && sockChannel.isOpen()) {
							nioChannel.syncSend(wd.getRequest().toBytes());
						} else {
							long sessionID = wd.getSessionId();
							nioChannel.cancelWd(sessionID);
						}
					}
				} catch (IOException ex) {
					if (nioChannel != null) {
						nioChannel.setIsOpen(false);
						if (wd != null) {
							long sessionID = wd.getSessionId();
							nioChannel.cancelWd(sessionID);
						}
					}
					logger.error(Version.INFO + ", write to registry channel error", ex);
				} catch (Throwable ex) {
					logger.error(Version.INFO + ", write to registry channel error", ex);
				}
			} catch (Throwable ex) {
				logger.error(Version.INFO + ", write to registry channel error", ex);
			}
		}
	}
}

class ReadWorker implements Runnable {

	private static final Log logger = LogFactory.getLog(ReadWorker.class);
	private List<RegistryNIOChannel> regList = new ArrayList<RegistryNIOChannel>();
	private Object locker = new Object();
	private Selector selector;

	public ReadWorker() throws IOException {
		selector = Selector.open();
	}

	public void register(RegistryNIOChannel nioChannel) throws IOException {
		if (nioChannel.getSockChannel().isConnected()) {
			synchronized (locker) {
				regList.add(nioChannel);
			}
			selector.wakeup();
		} else {
			throw new IOException(Version.INFO + ", registry channel is not open when register selector");
		}
	}

	public void unregister(RegistryNIOChannel nioChannel) throws IOException {
		synchronized (locker) {
			regList.remove(nioChannel);
		}
		selector.wakeup();
	}

	@Override
	public void run() {
		while (true) {
			RegistryNIOChannel nioChannel = null;
			String hashKey = null;
			boolean exceptionFlag = false;
			try {
				selector.select();
				if (regList.size() > 0) {
					synchronized (locker) {
						for (RegistryNIOChannel channel : regList) {
							logger.info(Version.INFO + ", register a channel " + channel.toString());
							channel.getSockChannel().register(selector, SelectionKey.OP_READ, channel);
						}
						regList.clear();
					}
				}

				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					if (key.isValid()) {
						if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
							SocketChannel sockChannel = (SocketChannel) key.channel();
							nioChannel = (RegistryNIOChannel) key.attachment();
							nioChannel.getFrameDecoder().decode(sockChannel, nioChannel);
						}
					}
				}
				selectedKeys.clear();
			} catch (IOException ex) {
				exceptionFlag = true;
				if (nioChannel != null) {
					try {
						hashKey = nioChannel.getServer().getHashKey();
						RegistryKeyFactory.getInsatnce().getSerPool().addDaemonCheckTask(hashKey, nioChannel.getServer().getServerConfig());
						nioChannel.close();
					} catch (IOException e1) {
						logger.error(Version.INFO + ", close socket error", e1);
					}
				}
			} catch (NotYetConnectedException ex) {
				exceptionFlag = true;
				if (nioChannel != null) {
					try {
						hashKey = nioChannel.getServer().getHashKey();
						RegistryKeyFactory.getInsatnce().getSerPool().addDaemonCheckTask(hashKey, nioChannel.getServer().getServerConfig());
						nioChannel.close();
					} catch (IOException e1) {
						logger.error(Version.INFO + ", close socket error", e1);
					}
				}
			} catch (CommunicationException ex) {
				exceptionFlag = true;
				if (nioChannel != null) {
					try {
						hashKey = nioChannel.getServer().getHashKey();
						RegistryKeyFactory.getInsatnce().getSerPool().addDaemonCheckTask(hashKey, nioChannel.getServer().getServerConfig());
						nioChannel.close();
					} catch (IOException e1) {
						logger.error(Version.INFO + ", close socket error", e1);
					}
				}
			} catch (Throwable ex) {
				logger.warn(Version.INFO + ", selector Exception:" + ex.getMessage());
			} finally {
				if (exceptionFlag && hashKey != null) {
					try {
						RegistryKeyFactory.getInsatnce().getSerPool().replaceRegistryServer(hashKey);
					} catch (Exception e) {
						logger.error(Version.INFO + ", threre is no registry server can be connected.");
					}
				}
			}
		}
	}
}
