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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WindowData {
	private long sessionId;
	private WLockRequest request;
	private AutoResetEvent event;
	private Boolean result;
	private byte[] data;
	private boolean isCanceled;

	public WindowData(long sessionID, WLockRequest request) {
		this.event = new AutoResetEvent(1);
		this.sessionId = sessionID;
		this.request = request;
	}

	public void countDown() {
		event.countDown();
	}

	public void setResult(Boolean result) {
		this.result = result;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isCanceled() {
		return isCanceled;
	}

	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	public WLockRequest getRequest() {
		return request;
	}

	public void setRequest(WLockRequest request) {
		this.request = request;
	}

	public AutoResetEvent getEvent() {
		return event;
	}

	public void setEvent(AutoResetEvent event) {
		this.event = event;
	}

	public Boolean getResult() {
		return result;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public boolean getIsCanceled() {
		return isCanceled;
	}

	public void setIsCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}
	
	class AutoResetEvent {
	    private final CountDownLatch cdl;
	    private final int waitCount;
	    
	    public AutoResetEvent(){
	        cdl = new CountDownLatch(1);
	        this.waitCount = 1;
	    }
	    
	    public AutoResetEvent(int waitCount){
	        cdl = new CountDownLatch(waitCount);
	        this.waitCount = waitCount;
	    }

	    public void countDown() {
	        cdl.countDown();
	    }

	    public boolean waitOne(long time) throws InterruptedException {
	        return cdl.await(time, TimeUnit.MILLISECONDS);
	    }

		public int getWaitCount() {
			return waitCount;
		}
	}
}
