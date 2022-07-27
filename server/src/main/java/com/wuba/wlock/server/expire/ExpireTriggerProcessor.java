
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

import com.wuba.wlock.server.communicate.ProtocolType;
import com.wuba.wlock.server.communicate.constant.LockContext;
import com.wuba.wlock.server.communicate.protocol.DeleteLockRequest;
import com.wuba.wlock.server.config.ServerConfig;
import com.wuba.wlock.server.expire.event.ExpireEvent;
import com.wuba.wlock.server.expire.event.ExpireEventType;
import com.wuba.wlock.server.expire.event.LockExpireEvent;
import com.wuba.wlock.server.expire.event.WatchExpireEvent;
import com.wuba.wlock.server.lock.protocol.LockOwnerInfo;
import com.wuba.wlock.server.lock.protocol.LockTypeEnum;
import com.wuba.wlock.server.lock.protocol.OpcodeEnum;
import com.wuba.wlock.server.lock.protocol.ReentrantLockValue;
import com.wuba.wlock.server.lock.repository.LockRepositoryImpl;
import com.wuba.wlock.server.lock.repository.base.ILockRepository;
import com.wuba.wlock.server.util.TimeUtil;
import com.wuba.wlock.server.watch.impl.WatchServiceImpl;
import com.wuba.wlock.server.worker.LockWorker;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExpireTriggerProcessor {
	
	private static Logger logger = LoggerFactory.getLogger(ExpireTriggerProcessor.class);
	
	private LinkedBlockingQueue<ExpireEvent> taskQueue;

	private Worker worker;
	
	private final Thread workerThread;
	
	private static LockWorker lockWorker = LockWorker.getInstance();
	
	private static WatchServiceImpl watchServiceImpl = WatchServiceImpl.getInstance(); 
	
	private static ILockRepository lockRepository = LockRepositoryImpl.getInstance();
	
	private volatile boolean isShutDown = false;
	
	private volatile AtomicBoolean isStopped = new AtomicBoolean(false);
	
	private int groupId;
	
	private ExpireManager expireManager;
	
	private volatile CountDownLatch countDownLatch;
	
	private volatile List<WatchExpireEvent> writeWatchEventList;
	
	private volatile List<WatchExpireEvent> readWatchEventList;
	
	private ScheduledExecutorService scheduledExcutorService;
	
	private static final long TASK_START_DELAY = 50; //ms

	private static final long TASK_EXECUTE_PERIOD = 50; //ms
	
	private final ThreadFactory threadFactory;
	
	private ExecutorService executorService;
	
	// 生成删除锁请求限速
	private double grouplimitQps;
	private RateLimiter limiter;
	private double storeLimitQps;
	private long cacheExpireEvent;
	private long warmupTimeMillSecond;
	
	public ExpireTriggerProcessor(int groupId, ExpireManager expireManager) {
		this.groupId = groupId;
		this.expireManager = expireManager;
		this.countDownLatch = new CountDownLatch(1);
		this.writeWatchEventList = new ArrayList<WatchExpireEvent>(100);
		this.readWatchEventList = new ArrayList<WatchExpireEvent>(100);
		this.scheduledExcutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("WatchExpireEventDispatchThread-" + groupId + "-"));
		threadFactory = new ThreadFactoryImpl("ExpireTriggerWorker-" + groupId + "-");
		
		worker = new Worker();
		workerThread = threadFactory.newThread(worker);
		workerThread.setDaemon(true);

		executorService = new ThreadPoolExecutor(4, 8, 1500L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryImpl("ExpireTriggerWorker_check_expireTime_" + groupId + "_"));
		
		storeLimitQps = ServerConfig.getInstance().getStoreLimitQps();
		cacheExpireEvent = ServerConfig.getInstance().getCacheExpireEvent();
		warmupTimeMillSecond = ServerConfig.getInstance().getWarmUpTimeMillSecond();
		
		this.taskQueue = new LinkedBlockingQueue<ExpireEvent>();
		this.grouplimitQps = storeLimitQps / expireManager.getWpaxosService().getOptions().getGroupCount();
		this.limiter = RateLimiter.create(grouplimitQps, warmupTimeMillSecond, TimeUnit.MILLISECONDS);
	}
	
	public void notifyWorker() {
		worker.notifyWorker();
	}
	
	public synchronized void offer(ExpireEvent expireEvent) {
		if (ServerConfig.getInstance().getExpireLimitStart()) { 
			// 超过最大阈值并且是锁过期事件则进行抛弃
			if (taskQueue.size() > cacheExpireEvent && ExpireEventType.EXPIRE_LOCK_EVENT == expireEvent.getExpireType()) {
				logger.error("ExpireTriggerProcessor current groupId {}, cacheQueue size is to Max {}", this.groupId, getTaskQSize());
			} else {
				taskQueue.offer(expireEvent);
			}
		} else {
			taskQueue.offer(expireEvent);
		}
	}
	
	public int getTaskQSize() {
		return taskQueue.size();
	}
	
	private void checkExpireLockEvent(ExpireEvent expireEvent) {
		this.executorService.execute(() -> {
			try {
				LockExpireEvent lockExpireEvent = (LockExpireEvent) expireEvent;
				String lockKey = lockExpireEvent.getLockKey();
				long lockVersion = lockExpireEvent.getLockVersion();
				Optional<ReentrantLockValue> lock = lockRepository.getLock(lockKey, lockExpireEvent.getGroupId());
				if (lock.isPresent()) {
					ReentrantLockValue reentrantLockValue = lock.get();
					LockOwnerInfo lockOwnerInfo = reentrantLockValue.getLockOwnerInfo();
					if (lockExpireEvent.getLockType() == LockTypeEnum.readWriteReentrantLock.getValue()
							&& lockExpireEvent.getOpcode() == OpcodeEnum.ReadWriteOpcode.READ.getValue()) {
						lockOwnerInfo = reentrantLockValue.getReadLockOwner(lockExpireEvent.getHost(), lockExpireEvent.getThreadID(), lockExpireEvent.getPid());
					}

					if (lockOwnerInfo != null && lockOwnerInfo.getLockVersion() <= lockVersion && lockOwnerInfo.isExpire()) {
						//this.transferOffer(expireEvent);
						if (ServerConfig.getInstance().getExpireLimitStart()) {
							limiter.acquire();
						}
						DeleteLockRequest deleteLockRequest = createDeleteLockRequest(lockExpireEvent);
						LockContext lockContext = new LockContext(deleteLockRequest.toBytes());
						lockContext.setLockkey(lockExpireEvent.getLockKey());
						lockWorker.offer(lockContext, lockExpireEvent.getGroupId());
						ExpireStrategyFactory.deleteKeyCount.incrementAndGet();
						logger.debug("ExpireTriggerProcessor handler LockExpireEvent, lockKey is {}, groupId {}, expireTimeStamp {}", expireEvent.getLockKey(), expireEvent.getGroupId(), expireEvent.getExpireTimestamp());
					}
				}
			} catch (Exception e) {
				logger.info(e.getMessage(), e);
			}
		});
	}
	
	private DeleteLockRequest createDeleteLockRequest(LockExpireEvent lockExpireEvent) {
		DeleteLockRequest deleteLockRequest = new DeleteLockRequest();
		deleteLockRequest.setProtocolType(ProtocolType.DELETE_LOCK);
		deleteLockRequest.setSessionID(0L);
		deleteLockRequest.setLockKey(lockExpireEvent.getLockKey());
		deleteLockRequest.setRegistryKey(lockExpireEvent.getRegistryKey());
		deleteLockRequest.setLockKeyLen((short) (lockExpireEvent.getLockKey().length()));
		deleteLockRequest.setTimestamp(TimeUtil.getCurrentTimestamp());
		deleteLockRequest.setLockType(lockExpireEvent.getLockType());
		deleteLockRequest.setOpcode(lockExpireEvent.getOpcode());
		deleteLockRequest.setHost(lockExpireEvent.getHost());
		deleteLockRequest.setThreadID(lockExpireEvent.getThreadID());
		deleteLockRequest.setPid(lockExpireEvent.getPid());
		return deleteLockRequest;
	}
	
	public void putWatchExpireEvent(WatchExpireEvent wacthExpireEvent) {
		this.writeWatchEventList.add(wacthExpireEvent);
	}
	
	public void swapWatchExpireEventList() {
		List<WatchExpireEvent> tempList = this.writeWatchEventList;
		this.writeWatchEventList = this.readWatchEventList;
		this.readWatchEventList = tempList;
	}
	
	public void start() {
		if (expireManager.getWpaxosService().isIMMaster(groupId)) {
			isStopped.set(false);
		} else {
			isStopped.set(true);
		}
		
		workerThread.start();
		
		scheduledExcutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					if (isStopped.get()) {
						return;
					}
					swapWatchExpireEventList();
					if (!readWatchEventList.isEmpty()) {
						watchServiceImpl.timeoutTrigger(new ArrayList<WatchExpireEvent>(readWatchEventList));
					}
					readWatchEventList.clear();
				} catch (Exception e) {
					logger.info("WatchExpireEventDispatchThread error", e);
				}
			}

		}, TASK_START_DELAY, TASK_EXECUTE_PERIOD, TimeUnit.MILLISECONDS);

		scheduledExcutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				logger.info("ExpireTriggerProcessor taskQueue size: {}", getTaskQSize());
			}
		}, 1, 1, TimeUnit.MINUTES);
		
		logger.info("ExpireTriggerProcessor worker {} start.", groupId);
	}
	
	public void resume() {
		if (!isStopped.compareAndSet(true,false)) {
			return;
		}
		this.notifyWorker();
		logger.info("ExpireTriggerProcessor worker {} resume.", groupId);
	}
	
	public void pause() {
		if (!isStopped.compareAndSet(false,true)) {
			return;
		}
		countDownLatch = new CountDownLatch(1);
		taskQueue.clear();
		logger.info("ExpireTriggerProcessor worker {} pause.", groupId);
		try {
			countDownLatch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void shutdown() {
		this.isStopped.set(true);
		this.isShutDown = true;
		if (!scheduledExcutorService.isShutdown()) {
			scheduledExcutorService.shutdown();
			try {
				if (scheduledExcutorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
					scheduledExcutorService.shutdownNow();
				}
			} catch (InterruptedException e) {
			} finally {
				if (!scheduledExcutorService.isShutdown()) {
					scheduledExcutorService.shutdownNow();
				}
			}
		}
		boolean interrupted = false;
		resume();
		try {
			//让处理线程执行完成
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		while (workerThread.isAlive()) {
			workerThread.interrupt();
			try {
				workerThread.join(100);
			} catch (InterruptedException ignored) {
				interrupted = true;
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
		this.taskQueue.clear();
		this.readWatchEventList.clear();
		this.writeWatchEventList.clear();
		logger.info("ExpireTriggerProcessor worker {} stop", groupId);
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public ExpireManager getExpireManager() {
		return expireManager;
	}

	public void setExpireManager(ExpireManager expireManager) {
		this.expireManager = expireManager;
	}
	
	public class Worker implements Runnable {
		
		public void notifyWorker() {
			synchronized(this) {
				if (expireManager.getWpaxosService().isIMMaster(groupId) || isShutDown) {
					this.notifyAll();
				}
			}
		}
		
		@Override
		public void run() {
			do {
				try {
					synchronized (this) {
						while (isStopped.get()) {
							countDownLatch.countDown();
							this.wait();
						}
					}
					ExpireEvent expireEvent = taskQueue.poll(1000, TimeUnit.MILLISECONDS);
					if (expireEvent == null) {
						continue;
					}
					byte expireEventType = expireEvent.getExpireType();
					switch (expireEventType) {
						case ExpireEventType.EXPIRE_LOCK_EVENT : {
							checkExpireLockEvent(expireEvent);
							break;
						}
						case ExpireEventType.EXPIRE_WATCH_EVENT : {
							putWatchExpireEvent((WatchExpireEvent) expireEvent);
							break;
						}
						default : {
							logger.warn("unknow ExpireEventType value is {}, lockKey is {}", expireEventType, expireEvent.getLockKey());
						}
					}
				} catch (Exception e) {
					logger.error("ExpireTriggerProcessor error:", e);
				}
			} while (!isShutDown);
		}
		
	}
	
}