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

import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DataReceiver {
	private static final Log logger = LogFactory.getLog(DataReceiver.class);
	private final static Object locker = new Object();
	private int workerCount = 1;
	private NIOWorker workers[];
	private final static AtomicInteger workerIndex = new AtomicInteger();
	private static DataReceiver instance = new DataReceiver();
    
	private DataReceiver() {
		try {
			workers = new NIOWorker[workerCount];
			for(int i = 0; i < workerCount; i++) {
				workers[i] = new NIOWorker();
				Thread thread = new Thread(workers[i]);
				thread.setName("wlock client DataReceiver "+i);
				thread.setDaemon(true);
				thread.start();
			}	
		} catch(Exception e) {
			logger.error(Version.INFO + " init DataReceiver failed.", e);
		}
	}
	
	public static DataReceiver getInstance() {
		return instance;
	}
	
	public void regChannel(NIOChannel nioChannel) throws ClosedChannelException, IOException {
		synchronized (locker) {
			workers[Math.abs(workerIndex.getAndIncrement() % workers.length)].register(nioChannel);
		}
	}
}

class NIOWorker implements Runnable {
	private static final Log logger = LogFactory.getLog(NIOWorker.class);
	private Object locker = new Object();
	private Selector selector;
	private ByteBuffer receiveBuffer = ByteBuffer.wrap(new byte[ServerConfig.BASE_BUFFER_SIZE]);
	
	private List<NIOChannel> regList = new ArrayList<NIOChannel>();
	
	public NIOWorker() throws IOException {
		selector = Selector.open();
	}
	
	public void register(NIOChannel nioChannel) throws IOException {
		if(nioChannel.getSockChannel().isConnected()) {
			synchronized (locker) {
				regList.add(nioChannel);
			}
			selector.wakeup();
		} else {
			throw new IOException(Version.INFO + " channel is not open when register selector");
		}
	}

	@Override
	public void run() {
		while(true) {
			NIOChannel nioChannel = null;
			try {
				selector.select();
				if(regList.size() > 0) {
					synchronized (locker) {
						for(NIOChannel channel : regList) {
							try {
								channel.getSockChannel().register(selector, SelectionKey.OP_READ, channel);
							} catch(Exception e) {
								logger.error(Version.INFO + ",register channel to selector failed.", e);
							}
						}
						regList.clear();
					}
				}
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();
					if(key.isValid()) {
						if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
							nioChannel = (NIOChannel) key.attachment();
							nioChannel.frameHandleLen(receiveBuffer);
						}
					}
				}
			} catch (IOException e) {
				logger.error(Version.INFO + " receive data error", e);
				if(nioChannel != null) {
					try {
						nioChannel.replaceChannel();
					} catch (IOException ioE) {
						logger.error(Version.INFO + " nio channel destroy error", ioE);
					}
				}

			} catch(NotYetConnectedException e) {
				if(nioChannel != null) {
					try {
						nioChannel.replaceChannel();
					} catch (IOException ioE) {
						logger.error(Version.INFO + " nio channel destroy error", ioE);
					}

				}
				logger.error(Version.INFO + " receive data error", e);
			} catch (InterruptedException e) {
				logger.error(Version.INFO + " receive data error", e);
			} catch (Throwable t) {
				logger.error(Version.INFO + " receive data error", t);
			}
		}
	}
}
