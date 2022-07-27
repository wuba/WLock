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

import com.wuba.wlock.server.communicate.WLockRequest;
import com.wuba.wlock.server.communicate.WLockResponse;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.retrans.RetransWaitWindow.RetransWindowData;
import com.wuba.wlock.server.exception.CommunicationException;
import com.wuba.wlock.server.exception.OperationCanceledException;
import com.wuba.wlock.server.exception.RetransRuntimeException;
import com.wuba.wlock.server.util.ByteConverter;
import com.wuba.wlock.server.util.TimeUtil;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RetransChannel {
	private static final Logger logger = LoggerFactory.getLogger(RetransChannel.class);
	private Channel channel;
	private AtomicLong sessionId;
	private String ip;
	private int port;
	private RetransServer retransServer;
	private Map<Long, WindowData> waitWindows;
	private AtomicInteger errorTimes = new AtomicInteger(0);
	private RetransWaitWindow retransWaitWindow = new RetransWaitWindow();
	public static ConcurrentHashMap<Integer, RetransChannel> channelMapper = new ConcurrentHashMap<Integer, RetransChannel>();

	public RetransChannel(String ip, int port, RetransServer retransServer) {
		this.ip = ip;
		this.port = port;
		sessionId = new AtomicLong(0L);
		waitWindows = new ConcurrentHashMap<Long, WindowData>(1024);
		this.retransServer = retransServer;
	}
	
	public static void recordChannel(int channelId, RetransChannel retransChannel) {
		channelMapper.put(channelId, retransChannel);
	}
	
	public static RetransChannel getChannelById(int channleId) {
		return channelMapper.get(channleId);
	}
	
	public static void removeRecordChannel(int channleId) {
		channelMapper.remove(channleId);
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public RetransServer getRetransServer() {
		return retransServer;
	}

	public void setRetransServer(RetransServer retransServer) {
		this.retransServer = retransServer;
	}

	public void asyncSend(LockContext lockContext, WLockRequest wlockRequest) throws RetransRuntimeException, IOException {
		if (channel == null || !channel.isOpen()) {
			throw new IOException("channel is null or close " + ip);
		}
		long sid = sessionId.getAndIncrement();
		wlockRequest.setSessionID(sid);
		RetransWindowData retransWindowData = new RetransWindowData(lockContext, TimeUtil.getCurrentTimestamp());
		this.retransWaitWindow.regisWindowData(sid, retransWindowData);
		
		try {
			ChannelFuture future = channel.write(ChannelBuffers.copiedBuffer(wlockRequest.toBytes()));
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						errorTimes.incrementAndGet();
					}
				}
			});
		} catch(Exception e) {
			errorTimes.incrementAndGet();
			throw new RetransRuntimeException(e);
		}
	}

	public byte[] syncSend(WLockRequest wlockRequest, int timeout) throws RetransRuntimeException, IOException {
		if (channel == null || !channel.isOpen()) {
			throw new IOException("channel is null or close " + ip);
		}
		long sid = sessionId.getAndIncrement();
		WindowData windowData = new WindowData(sid, wlockRequest);
		wlockRequest.setSessionID(sid);
		waitWindows.put(sid, windowData);
		byte[] data;
		try {
			ChannelFuture future = channel.write(ChannelBuffers.copiedBuffer(wlockRequest.toBytes()));
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						errorTimes.incrementAndGet();
					}
				}
			});
			data = receive(sid, timeout);
			errorTimes.set(0);
			return data;
		} catch(Exception e) {
			errorTimes.incrementAndGet();
			throw new RetransRuntimeException(e);
		} finally {
			waitWindows.remove(sid);
		}
	}

	public void notify(long sid, byte[] response) {
		WindowData cd = waitWindows.get(sid);
		if (cd != null) {
			cd.setData(response);
			cd.countDown();
		}
		
		RetransWindowData rWindowData = this.retransWaitWindow.getWindowData(sid);
		if (rWindowData != null) {
			handAck(sid, response);
		}
	}
	
	public void handAck(long sid, byte[] response) {
		RetransWindowData rWindowData = this.retransWaitWindow.getWindowData(sid);
		if (rWindowData != null) {
			LockContext lockContext = rWindowData.getLockContext();
			long sessionId = lockContext.getSessionId();
			byte[] sidBuf = ByteConverter.longToBytesLittleEndian(sessionId);
			for (int i = 0; i < sidBuf.length; i++) {
				response[WLockResponse.SESSION_ID_POS + i] = sidBuf[i];
			}

			Channel channel = lockContext.getChannel();	
			ChannelFuture future = channel.write(ChannelBuffers.copiedBuffer(response));
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						logger.error("Retrans response send to Server failed.");
					}
				}
			});
			
			this.retransWaitWindow.unregisWindowData(sid);
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

	public void cancelAllWd() {
		for (WindowData wd : waitWindows.values()) {
			if (wd != null && !wd.isCanceled()) {
				for (int i = 0; i < wd.getEvent().getWaitCount(); i++) {
					wd.countDown();
				}
				wd.setCanceled(true);
			}
		}
		waitWindows.clear();
	}

	private byte[] receive(long sessionId, int timeout) throws CommunicationException, OperationCanceledException, TimeoutException, InterruptedException {
		WindowData cd = waitWindows.get(sessionId);
		if (cd == null) {
			throw new CommunicationException("sessionID:" + sessionId + " need invoke 'registerRec' method before invoke 'receive' method!");
		}
		if (!cd.getEvent().waitOne(timeout)) {
			throw new TimeoutException("retrans request timeout:" + timeout + "ms sessionID:" + sessionId
					+ "[" + ip + ":" + port + "]");
		}
		if (cd.isCanceled()) {
			throw new OperationCanceledException("canceled waitting data due to server dead:" + ip + ":" + port);
		}
		return cd.getData();
	}
	
	public AtomicLong getSessionId() {
		return sessionId;
	}

	public void setSessionId(AtomicLong sessionId) {
		this.sessionId = sessionId;
	}

	public Map<Long, WindowData> getWaitWindows() {
		return waitWindows;
	}

	public void setWaitWindows(Map<Long, WindowData> waitWindows) {
		this.waitWindows = waitWindows;
	}

	public int getErrorTimes() {
		return errorTimes.get();
	}

	public void setErrorTimes(int errorTimes) {
		this.errorTimes.set(errorTimes);
	}
	
	public void closed() {
		if (this.channel != null && this.channel.isOpen()) {
			this.channel.close();
			this.channel = null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RetransChannel other = (RetransChannel) obj;
		if (ip == null) {
			if (other.ip != null) {
				return false;
			}
		} else if (!ip.equals(other.ip)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "RegisterChannel{" +
				"ip='" + ip + '\'' +
				", port=" + port +
				'}';
	}
}
