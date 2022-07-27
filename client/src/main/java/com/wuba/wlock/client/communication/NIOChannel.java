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
import com.wuba.wlock.client.config.Delimiter;
import com.wuba.wlock.client.config.ServerConfig;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.config.WLockConfig;
import com.wuba.wlock.client.exception.CommunicationException;
import com.wuba.wlock.client.exception.ConnectTimeoutException;
import com.wuba.wlock.client.exception.OperationCanceledException;
import com.wuba.wlock.client.exception.SerializeException;
import com.wuba.wlock.client.helper.ByteConverter;
import com.wuba.wlock.client.protocol.ProtocolType;
import com.wuba.wlock.client.protocol.WLockResponse;
import com.wuba.wlock.client.service.HeartbeatService;
import com.wuba.wlock.client.service.LockService;
import com.wuba.wlock.client.watch.WatchManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class NIOChannel {
	private static final Log logger = LogFactory.getLog(NIOChannel.class);

	Set<Server> servers = new HashSet<Server>();

	private boolean isOpen = false;
	private SocketChannel sockChannel;
	private final ReentrantLock lock = new ReentrantLock();
	private ByteBuffer receiveMsg;
	private NIOHandler nioHandler;
	private WaitWindow waitWindows = WaitWindow.getWindow();
	private volatile long lastReadTime = 0L;

	private ServerConfig serverConfig;
	
	protected NIOChannel(ServerConfig servConf) throws IOException, ConnectTimeoutException {
		this.serverConfig = servConf;

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
				sockChannel.close();
				throw new ConnectTimeoutException(Version.INFO + " connect to " + addr + " timeout：" + servConf.getConnectTimeOut());
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

		receiveMsg = ByteBuffer.allocate(servConf.getMaxPakageSize());
		this.isOpen = true;
		this.nioHandler = NIOHandler.getInstance();
		DataReceiver.getInstance().regChannel(this);
		logger.info(Version.INFO + " create a new connection:" + this.toString());		
	}

	public ServerConfig getServerConfig() {
		return serverConfig;
	}

	public void setIsOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public boolean isOpen() {
		return this.isOpen && this.sockChannel.isOpen();
	}


	public int send(byte[] data, ByteBuffer sendBuffer) throws CommunicationException, IOException {
		int count = 0;
		lock.lock();
		try {
			sendBuffer.clear();
			sendBuffer.put(ByteConverter.IntToBytesBigEndian(data.length));
			sendBuffer.put(data);
			sendBuffer.put(Delimiter.end);
			sendBuffer.flip();

			int retryCount = 0;
			while (sendBuffer.hasRemaining()) {
				count += sockChannel.write(sendBuffer);
				if (retryCount++ > WLockConfig.SEND_RETRY_COUNT) {
					throw new CommunicationException(Version.INFO + " retry write count(" + retryCount + ") above SEND_RETRY_COUNT");
				}
			}
			return count;
		} finally {
			lock.unlock();
		}
	}
	
	public void asyncSend(WindowData cd) {
		nioHandler.offerWriteData(cd);
	}
	
	public byte[] receive(long sessionId, int timeout) throws TimeoutException, CommunicationException, InterruptedException, OperationCanceledException {
		WindowData cd = waitWindows.get(sessionId);
		if (cd == null) {
			throw new CommunicationException("sessionID:" + sessionId + " need invoke 'registerRec' method before invoke 'receive' method!");
		}
		if (!cd.getEvent().waitOne(timeout)) {
			throw new TimeoutException("receive data from wlock server timeout:" + timeout + "ms sessionID:" + sessionId + "[" + this.serverConfig.getIp() + ":" + this.serverConfig.getPort() + "]");
		}
		if (cd.isCanceled()) {
			throw new OperationCanceledException("canceled waitting data due to server dead:" + this.serverConfig.getIp() + ":" + this.serverConfig.getPort());
		}
		return cd.getData();
	}
	
	public void notify(long sid, byte[] response) {
		WindowData cd = waitWindows.get(sid);
		if (cd != null) {
			cd.appendData(response);
			cd.countDown();
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
	
	public void registerRec(long sessionID, WindowData cd) {
		waitWindows.addWd(sessionID, cd);
	}

	public void unregisterRec(long sessionID) {
		waitWindows.remove(sessionID);
	}

	public void close() throws IOException {
		isOpen = false;
		if (sockChannel != null) {
			logger.warn(Version.INFO + " close a wlock client socket: local addr:" + sockChannel.socket().getLocalAddress());
			sockChannel.close();
			sockChannel = null;
		}
	}

	/**
	 * 检测尾分隔符是否正常
	 * @param buf
	 * @return
	 */
	public boolean checkEndDelimiter(byte[] buf) {
		if (buf.length == Delimiter.end.length) {
			for (int i = 0; i < buf.length; i++) {
				if (buf[i] != Delimiter.end[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 跳过错误消息
	 */
	public void skipErrorMsg() {
		receiveMsg.reset();
		index = 0;
		while (receiveMsg.remaining() > 0) {
			byte b = receiveMsg.get();
			if (b == Delimiter.end[index]) {
				index++;
				if (index == Delimiter.end.length) {
					index = 0;
//					findEnd = true;
					break;
				}
			} else if (index != 0) {
				if (b == Delimiter.end[0]) {
					index = 1;
				} else {
					index = 0;
				}
			}
		}
	}
	
	/**
	 * 监听返回信息
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private volatile int index = 0;
//	private volatile boolean findEnd = true;
	private volatile byte[] remainBytes = null;

	public void frameHandleLen(ByteBuffer receiveBuffer) throws IOException, SerializeException, InterruptedException {
		receiveBuffer.clear();
		int ret = sockChannel.read(receiveBuffer);
		receiveBuffer.flip();
		receiveMsg.clear();

		if (remainBytes != null) {
			receiveMsg.put(remainBytes);
			remainBytes = null;
		}

		receiveMsg.put(receiveBuffer);
		receiveMsg.flip();
		receiveMsg.mark();

		while (receiveMsg.remaining() >= 8) {
			this.lastReadTime = System.currentTimeMillis();

			receiveMsg.mark();
			byte[] lenBuf = new byte[8];
			receiveMsg.get(lenBuf, 0, 8);
			int totalLen = ByteConverter.bytesToIntLittleEndian(lenBuf, 4);
			receiveMsg.reset();

			if (receiveMsg.remaining() >= (totalLen + Delimiter.end.length + 4)) {
				byte[] dataBuf = new byte[totalLen];
				byte[] endDelimiter = new byte[Delimiter.end.length];
				receiveMsg.position(receiveMsg.position() + 4); //跳过4个字节总长度
				receiveMsg.get(dataBuf, 0, totalLen);
				receiveMsg.get(endDelimiter, 0, Delimiter.end.length);

				if (checkEndDelimiter(endDelimiter)) {
					decode(dataBuf);
					continue;
				} else {
					skipErrorMsg();
					logger.error(Version.INFO + " skip a error msg......");
				}
			} else {
				receiveMsg.reset();
				break;
			}
		}

		if (receiveMsg.remaining() > 0) {
			receiveMsg.reset();
			remainBytes = new byte[receiveMsg.remaining()];
			receiveMsg.get(remainBytes, 0, remainBytes.length);
		}

		receiveMsg.clear();

		if(ret < 0){
			try {
				this.replaceChannel();
			} catch (IOException e1) {
				logger.error(Version.INFO + "close socket error", e1);
			}

			logger.error(Version.INFO + "server is close,client will close!");
		}
	}
	
	public void decode(byte[] databuf) {
		byte protocolType = databuf[6];
		long sessionID = ByteConverter.bytesToLongLittleEndian(databuf, WLockResponse.SESSIONID_OFFSET);
		if (protocolType == ProtocolType.HEARTBEAT) {
			HeartbeatService.getHbService().handleResponse(sessionID, databuf);
		} else if(protocolType == ProtocolType.REBOOT){
			logger.info(Version.INFO + " server restart ip: " + this.serverConfig.getIp() + " port: " + this.serverConfig.getPort());
			for (Server server: servers) {
				server.setState(ServerState.ReStart);
			}
		} else if (protocolType != ProtocolType.EVENT_NOTIFY) {
			WindowData wd = WaitWindow.getWindow().get(sessionID);
			if (wd != null) {
				wd.getChannel().notify(sessionID, databuf);
			}
		} else {
			// event notify
			byte commandType = databuf[5];
			if (commandType == 0) {
				notifyWatchEvent(databuf);
			}
		}
	}

	public void notifyWatchEvent(byte[] databuf) {
		Set<WLockClient> wLockClients = wLockClients();
		if (!wLockClients.isEmpty()) {
			for (WLockClient wlockClient: wLockClients) {
				LockService lockService = wlockClient.getLockService();
				WatchManager watchManager = lockService.getWatchManager();
				watchManager.notifyWatchEvent(databuf);
			}
		}
	}

	private Set<WLockClient> wLockClients() {
		Set<WLockClient> wLockClients = new HashSet<WLockClient>();
		for (Server server: servers) {
			wLockClients.add(server.getWlockClient());
		}
		return wLockClients;
	}

	public SocketChannel getSockChannel() {
		return sockChannel;
	}

	public synchronized void add(Server server) {
		servers.add(server);
	}

	public void destroy() throws IOException {
		if(this.isOpen()){
			this.close();
		}

		ChannelPool.getInstance().remove(node());
	}

	public void destroy(Server server) {
		servers.remove(server);
	}

	public void check() {
		for (Server server: servers) {
			server.check();
		}
	}

	public void replaceChannel() throws IOException {
		destroy();
		check();
	}

	public String node() {
		return serverConfig.getIp() + ":" + serverConfig.getPort();
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