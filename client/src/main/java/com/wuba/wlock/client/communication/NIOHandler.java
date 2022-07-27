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
import com.wuba.wlock.client.config.WLockConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NIOHandler {
	private static final Log logger = LogFactory.getLog(NIOHandler.class);

	public final BlockingQueue<WindowData> writeQueue = new LinkedBlockingQueue<WindowData>();
	private static WriteWorker writeWorker;
	private static NIOHandler nioHandler = new NIOHandler();
	
	private NIOHandler() {
		writeWorker = new WriteWorker();
		Thread thd = new Thread(writeWorker);
		thd.setName("wlock client writer");
		thd.setDaemon(true);
		thd.start();
	}
	
	public static NIOHandler getInstance() {
		return nioHandler;
	}
	
	public void offerWriteData(WindowData cd) {
		if (writeQueue.size() > WLockConfig.MAX_WRITE_QUEUE_LEN) {
			logger.error(Version.INFO + "writeQueue overflow len:" + WLockConfig.MAX_WRITE_QUEUE_LEN);
			return;
		}

		if (!writeQueue.offer(cd)) {
			logger.error(Version.INFO + "the element add to writeQueue error");
		}
	}
	
	class WriteWorker implements Runnable {
		private ByteBuffer sendBuffer = ByteBuffer.wrap(new byte[ServerConfig.BASE_BUFFER_SIZE]);

		@Override
		public void run() {
			while (true) {
				try {
					NIOChannel nioChannel = null;
					WindowData wd = null;
					try {
						wd = NIOHandler.getInstance().writeQueue.poll(1000, TimeUnit.MILLISECONDS);
						if (wd != null) {
							nioChannel = wd.getChannel();
							if (nioChannel != null) {
								SocketChannel sockChannel = nioChannel.getSockChannel();

								if (sockChannel != null && sockChannel.isOpen()) {
									nioChannel.send(wd.getRequest().toBytes(), sendBuffer);
								} else {
									long sessionID = wd.getSessionId();
									nioChannel.cancelWd(sessionID);
								}
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
						logger.error(Version.INFO + "write to channel error", ex);
					} catch (Throwable ex) {
						ex.printStackTrace();
						logger.error(Version.INFO + "write to channel error", ex);
					}
				} catch (Throwable ex) {
					logger.error(Version.INFO + "write to channel error", ex);
				}
			}
		}
	}
}
