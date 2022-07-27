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
package com.wuba.wlock.server.expire;

import com.wuba.wlock.server.expire.event.ExpireEvent;
import com.wuba.wlock.server.expire.event.ExpireEventType;
import com.wuba.wlock.server.expire.queue.all.QueueAllExpireManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class ExpireStrategyFactory {

	private static Logger logger = LoggerFactory.getLogger(ExpireStrategyFactory.class);

	public static String QUEUE_ALL_PATTERN = "queue_all_pattern";

	private ExpireManager expireManager;
	
	private String pattern;
	
	private volatile boolean isStarted = false;
	
	private volatile boolean isNeedDeleteExpireEvent = false;
	
	public static AtomicLong acquireKeyCount = new AtomicLong(0);

	public static AtomicLong deleteKeyCount = new AtomicLong(0);
	
	private ExpireStrategyFactory() {
	}

	private static ExpireStrategyFactory instance =  new ExpireStrategyFactory();

	public static ExpireStrategyFactory getInstance() {
		return instance;
	}

	public synchronized void start(String pattern) throws Exception {
		if (!this.isStarted) {
			this.isStarted = true;
			if (expireManager == null) {
				if (QUEUE_ALL_PATTERN.equals(pattern)) {
					this.pattern = pattern;
					expireManager = QueueAllExpireManager.getInstance();
				} else {
					throw new Exception("the pattern " + pattern + " is error!");
				}
			}
			expireManager.start();
			logger.info("ExpireStrategyFactory started, pattern is {}", pattern);
		}
	}

	public void addExpireEvent(ExpireEvent expireEvent) {
		if (null == this.expireManager || null == expireEvent) {
			return;
		}
		if (ExpireEventType.EXPIRE_LOCK_EVENT == expireEvent.getExpireType()) {
			acquireKeyCount.incrementAndGet();
		}
		this.expireManager.addExpireEvent(expireEvent);
	}
	
	public ExpireManager getExpireManager() {
		return expireManager;
	}

	public void shutdown() {
		if (null == this.expireManager) {
			return;
		}
		this.expireManager.stop();
	}
	
	public void paused(int groupId) {
		if (null == this.expireManager) {
			return;
		}
 		Runnable target = () -> {
 			this.expireManager.pause(groupId);
 		};
 		new Thread(target).start();
	}
	
	public void resume(int groupId) {
		if (null == this.expireManager) {
			return;
		}
		this.expireManager.resume(groupId);
	}
	
	public void learnMaster(int groupId) {
		if (null == this.expireManager) {
			return;
		}
		this.expireManager.learnMaster(groupId);
	}


	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

}
