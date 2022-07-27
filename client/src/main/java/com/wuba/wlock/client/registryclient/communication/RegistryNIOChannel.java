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

import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.exception.ConnectTimeoutException;
import com.wuba.wlock.client.exception.OperationCanceledException;
import com.wuba.wlock.client.exception.RegistryClientRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class RegistryNIOChannel {
	
	private static final Log logger = LogFactory.getLog(RegistryNIOChannel.class);
	private ByteBuffer receiveBuffer, sendBuffer, receiveData;

	private SocketChannel sockChannel;
	private RegistryServer server;
	private final Object sLock = new Object();
	private boolean isOpen = false;
	private Map<Long, WindowData> waitWindows = new ConcurrentHashMap<Long, WindowData>(1024);
	private RegistryNIOHandler registryNIOHandler;
	private IFrameDecoder frameDecoder;
	public static int REGISTRY_SEND_RETRY_COUNT = 10;

	protected RegistryNIOChannel(ServerConfig servConf, RegistryServer server) throws IOException, ConnectTimeoutException {
		InetSocketAddress addr = new InetSocketAddress(servConf.getIp(), servConf.getPort());
		sockChannel = SocketChannel.open();
		sockChannel.configureBlocking(false);
		sockChannel.socket().setReceiveBufferSize(servConf.getRecvBufferSize());
		sockChannel.socket().setSendBufferSize(servConf.getSendBufferSize());
		sockChannel.socket().setTcpNoDelay(!servConf.isNagle());
		sockChannel.socket().setKeepAlive(servConf.isKeepAlive());
		sockChannel.connect(addr);

		long begin = System.currentTimeMillis();
		while (true) {
			if ((System.currentTimeMillis() - begin) > servConf.getConnectTimeOut()) {
				throw new ConnectTimeoutException(Version.INFO + ", connect to " + addr + " timeout：" + servConf.getConnectTimeOut());
			}
			sockChannel.finishConnect();
			if (sockChannel.isConnected()) {
				break;
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
		}

		this.receiveBuffer = ByteBuffer.allocateDirect(servConf.getMaxPakageSize());
		this.receiveData = ByteBuffer.allocate(servConf.getMaxPakageSize());
		this.sendBuffer = ByteBuffer.allocateDirect(servConf.getSendBufferSize());
		this.isOpen = true;
		this.server = server;
		this.registryNIOHandler = RegistryNIOHandler.getInstance();
		this.registryNIOHandler.regChannel(this);
		this.frameDecoder = new RegistryDecoder();
		logger.info(Version.INFO + ", wlock registry client create a new connection:" + this.toString());
	}

	public void setIsOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public boolean isOpen() {
		return this.isOpen && this.sockChannel.isOpen();
	}

	public int syncSend(byte[] data) throws CommunicationException, IOException {
		int count = 0;
		synchronized (sLock) {
			sendBuffer.clear();
			sendBuffer.put(data);
			sendBuffer.flip();

			int retryCount = 0;
			while (sendBuffer.hasRemaining()) {
				count += sockChannel.write(sendBuffer);

				if (retryCount++ > REGISTRY_SEND_RETRY_COUNT) {
					throw new CommunicationException(Version.INFO + ", registry client retry write count(" + retryCount + ") above SEND_RETRY_COUNT");
				}
			}
		}
		return count;
	}

	public void asyncSend(WindowData cd) {
		registryNIOHandler.offerWriteData(cd);
	}

	public byte[][] receive(long sessionId, int timeout) throws TimeoutException, CommunicationException, InterruptedException, OperationCanceledException, RegistryClientRuntimeException {
		WindowData cd = waitWindows.get(sessionId);
		if (cd == null) {
			throw new CommunicationException(Version.INFO + ", sessionID:" + sessionId + " need invoke 'registerRec' method before invoke 'receive' method!");
		}
		if (!cd.getEvent().waitOne(timeout)) {
			throw new TimeoutException(Version.INFO + ", receive data from wlock registry server timeout:" + timeout + "ms sessionID:" + sessionId + "[" + server.getServerConfig().getIp() + ":" + server.getServerConfig().getPort() + "]");
		}
		if (cd.isCanceled()) {
			throw new OperationCanceledException(Version.INFO + ", canceled waitting data due to server dead:" + this.server.getServerConfig().getIp() + ":" + this.server.getServerConfig().getPort());
		}

		return cd.getData();
	}

	public void notify(long sid, byte[] response) {
		WindowData cd = waitWindows.get(sid);
		if (cd != null) {
			cd.appendData(sid, response);
			cd.countDown();
		} else {
			logger.warn(Version.INFO + ", config file has been received, but the request was timeout");
		}
	}

	public void cancelWd(long sessionId) {
		WindowData wd = waitWindows.get(sessionId);
		if (wd != null && !wd.isCanceled()) {
			for (int i = 0; i < wd.getEvent().getWaitCount(); i++) {
				wd.countDown();
			}
			wd.setCanceled(true);
		}
	}

	public void close() throws IOException {
		isOpen = false;
		if (sockChannel != null) {
			logger.warn(Version.INFO + ", close a wlock registry client socket: local addr:" + sockChannel.socket().getLocalAddress() + " remote addr:" + sockChannel.socket().getReuseAddress());
			sockChannel.close();
			server.getChannelPool().destroy(this);
			sockChannel = null;
		}
	}

	public SocketChannel getSockChannel() {
		return sockChannel;
	}

	public RegistryServer getServer() {
		return server;
	}

	public void registerRec(long sessionID, WindowData cd) {
		waitWindows.put(sessionID, cd);
	}

	public void unregisterRec(long sessionID) {
		waitWindows.remove(sessionID);
	}

	public ByteBuffer getReceiveBuffer() {
		return receiveBuffer;
	}

	public void setReceiveBuffer(ByteBuffer receiveBuffer) {
		this.receiveBuffer = receiveBuffer;
	}

	public ByteBuffer getSendBuffer() {
		return sendBuffer;
	}

	public void setSendBuffer(ByteBuffer sendBuffer) {
		this.sendBuffer = sendBuffer;
	}

	public RegistryNIOHandler getRegistryNIOHandler() {
		return registryNIOHandler;
	}

	public void setRegistryNIOHandler(RegistryNIOHandler registryNIOHandler) {
		this.registryNIOHandler = registryNIOHandler;
	}

	public IFrameDecoder getFrameDecoder() {
		return frameDecoder;
	}

	public void setFrameDecoder(IFrameDecoder frameDecoder) {
		this.frameDecoder = frameDecoder;
	}

	public ByteBuffer getReceiveData() {
		return receiveData;
	}

	public void setReceiveData(ByteBuffer receiveData) {
		this.receiveData = receiveData;
	}

	@Override
	public String toString() {
		try {
			return (sockChannel == null) ? "" : sockChannel.socket().toString();
		} catch (Throwable ex) {
			return "socket[error:" + ex.getMessage() + "]";
		}
	}
}
