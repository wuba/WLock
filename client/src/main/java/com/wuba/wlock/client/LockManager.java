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
package com.wuba.wlock.client;

import com.wuba.wlock.client.communication.LockTypeEnum;
import com.wuba.wlock.client.communication.ReadWriteLockTypeEnum;
import com.wuba.wlock.client.communication.WatchPolicy;
import com.wuba.wlock.client.config.Factor;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.listener.HoldLockListener;
import com.wuba.wlock.client.listener.LockExpireListener;
import com.wuba.wlock.client.listener.RenewListener;
import com.wuba.wlock.client.listener.WatchListener;
import com.wuba.wlock.client.lockresult.LockResult;
import com.wuba.wlock.client.protocol.ResponseStatus;
import com.wuba.wlock.client.service.LockService;
import com.wuba.wlock.client.watch.EventCachedHandler;
import com.wuba.wlock.client.watch.EventType;
import com.wuba.wlock.client.watch.NotifyEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
	private static final Log logger = LogFactory.getLog(LockManager.class);
	private final WLockClient wlockClient;
	public static String lockkey_threadID_seperator = "@";
	private final int renewThdCount;
	private final ConcurrentHashMap<String/*lockkey@threadID*/, LockContext> lockContextMap = new ConcurrentHashMap<String, LockContext>();
	private final ConcurrentHashMap<String/*lockkey@threadID*/, LockContext> readLockContextMap = new ConcurrentHashMap<String, LockContext>();
	private final ConcurrentHashMap<String/*lockkey@threadID*/, LockContext> writeLockContextMap = new ConcurrentHashMap<String, LockContext>();

	private final ConcurrentHashMap<String/*lockkey@threadID@lockType@opcode*/, RenewLockTask> renewTaskMap = new ConcurrentHashMap<String, RenewLockTask>();
	private TreeSet<LockContext> expireCheckSet = null;
	private int expireCheckInterval = 50;
	private ReentrantLock renewLock = new ReentrantLock();
	private ReentrantLock expireLock = new ReentrantLock();

	private ScheduledExecutorService scheduledExecutorService = null;

	public LockManager(WLockClient wlockClient) {
		this.wlockClient = wlockClient;
		this.renewThdCount = this.wlockClient.getAutoRenewThreadCount();
		expireCheckSet = new TreeSet<LockContext>(new Comparator<LockContext>() {
			@Override
			public int compare(final LockContext o1, final LockContext o2) {
				return o1.compareTo(o2);
			}
		});
		init();
	}

	public void init() {
		scheduledExecutorService = Executors
				.newScheduledThreadPool(renewThdCount, new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, "LockManagerScheduledThread");
					}
				});

		scheduleExpireCheck();
	}

	public void scheduleExpireCheck() {
		scheduledExecutorService.scheduleWithFixedDelay(new ExpireCheckTask(), expireCheckInterval, expireCheckInterval, TimeUnit.MILLISECONDS);
	}


	private ConcurrentHashMap<String, LockContext> lockContextMap(int lockType, int lockOpcode) {
		if (lockType == LockTypeEnum.reentrantLock.getValue()) {
			return lockContextMap;
		}

		if (lockType == LockTypeEnum.readWriteReentrantLock.getValue()) {
			if (lockOpcode == ReadWriteLockTypeEnum.Read.getOpcode()) {
				return readLockContextMap;
			}

			if (lockOpcode == ReadWriteLockTypeEnum.Write.getOpcode()) {
				return writeLockContextMap;
			}
		}

		return null;
	}

	private ConcurrentHashMap<String, LockContext> lockContextMap(int eventType) {
		int lockType = LockTypeEnum.reentrantLock.getValue();
		int lockOpcode = 0;
		if (eventType == EventType.LOCK_EXPIRED.getType()) {
			lockType = LockTypeEnum.reentrantLock.getValue();
		} else if (eventType == EventType.READ_LOCK_EXPIRED.getType()) {
			lockType = LockTypeEnum.readWriteReentrantLock.getValue();
			lockOpcode = ReadWriteLockTypeEnum.Read.getOpcode();
		} else if (eventType == EventType.WRITE_LOCK_EXPIRED.getType()) {
			lockType = LockTypeEnum.readWriteReentrantLock.getValue();
			lockOpcode = ReadWriteLockTypeEnum.Write.getOpcode();
		}

		return lockContextMap(lockType, lockOpcode);
	}
	/**
	 * 读取本地锁信息
	 * @param lockkey
	 * @return
	 */
	public LockContext getLocalLockContext(String lockkey, long ownerId, int lockType, int lockOpcode) {
		String contextKey = lockkey + lockkey_threadID_seperator + ownerId;
		return lockContextMap(lockType, lockOpcode).get(contextKey);
	}

	/**
	 * 从远程获取锁之前，先从本地获取
	 * @param lockkey
	 * @return 获取成功否
	 */
	public boolean acquiredLockLocal(String lockkey, InternalLockOption lockOption) {
		String contextKey = lockkey + lockkey_threadID_seperator + lockOption.getThreadID();

		LockContext lockContext = lockContextMap(lockOption.getLockType(), lockOption.getLockOpcode()).get(contextKey);

		/*已从远程获取到锁*/
		if (lockContext != null && lockContext.getAquiredCount() > 0) {
			lockContext.incrAquiredCount();
			// updateExpireTime(lockContext);
			return true;
		}

		/*本地获取失败，必须远程获取*/
		return false;
	}

	/**
	 * 从远程获取到锁之后，强制更新本地锁
	 * @param lockkey 锁名称
	 * @param lockVersion 锁版本号
	 * @param lockOption 锁属性选项
	 * @param needTouch 是否需要立即续约，为 true时立即续约
	 * @return 更新成功否
	 */
	public boolean updateLockLocal(String lockkey, long lockVersion, InternalLockOption lockOption, boolean needTouch) {
		String contextKey = lockkey + lockkey_threadID_seperator + lockOption.getThreadID();

		LockContext lockContext = new LockContext(lockkey, contextKey, lockVersion, 1, lockOption);
		// 更新过期时长
		lockContext.setExpireMills(lockOption.getExpireTime());
		lockContextMap(lockOption.getLockType(), lockOption.getLockOpcode()).put(contextKey, lockContext);
		updateExpireTime(lockContext, lockOption.getExpireTime());

		lockContext.updateRealExpireTime();

		if (needTouch) {
			startTouchRenew(lockkey, lockContext, lockOption.getLockType(), lockOption.getLockOpcode());
		} else if (lockOption.isAutoRenew() || lockOption.getRealExpireTimeStamp() > System.currentTimeMillis()) {
			startAutoRenew(lockkey, lockContext, lockOption.getLockType(), lockOption.getLockOpcode());
		}

		return true;
	}

	/**
	 * 释放本地锁
	 * @param lockkey
	 * @return 释放锁，大于0:释放成功，等于0:释放成功，需要远程释放，-1:释放锁失败; -2 锁不存在
	 */
	public int releaseLockLocal(String lockkey, long ownerThreadID, boolean forced, int lockType, int opcode) {
		String contextKey = lockkey + lockkey_threadID_seperator + ownerThreadID;
		return releaseLockLocal(lockkey, ownerThreadID, contextKey, forced, lockType, opcode);
	}

	/**
	 * 释放本地锁
	 * @param lockContextKey
	 * @return 释放锁，大于0:释放成功，等于0:释放成功，需要远程释放，-1:释放锁失败; -2 锁不存在
	 */
	public int releaseLockLocal(String lockkey, long ownerThreadID, String lockContextKey, boolean forced, int lockType, int opcode) {
		ConcurrentHashMap<String, LockContext> lockContextMap = lockContextMap(lockType, opcode);

		if (!lockContextMap.containsKey(lockContextKey)) {
			logger.debug(Version.INFO + " releaseLockLocal may has error, as local has been released all, lockkey : " + lockkey);
			return ReleaseRet.NOT_EXIST;
		}

		LockContext lockContext = lockContextMap.get(lockContextKey);

		if (forced) {
			/* 过期强制释放锁 */
			lockContext.getReleased().compareAndSet(false, true);
			logger.warn(Version.INFO + " releaseLock forced, lockkey : " + lockkey + ", ownerID : " + ownerThreadID);
			lockContextMap.remove(lockContextKey);
			cancelRenewTask(lockkey, ownerThreadID, lockType, opcode);
			return ReleaseRet.SUCCESS;
		}

		if (lockContext.getLockOption().getThreadID() != ownerThreadID) {
			logger.error(Version.INFO + " release lock failed, owner threadid maybe wrong, owner : " + lockContext.getLockOption().getThreadID() + ", release threadID : " + ownerThreadID);
			return ReleaseRet.FAIL;
		}

		if (lockContext.getAquiredCount() <= 0) {
			logger.error(Version.INFO + " releaseLockLocal may has error, as local has been released all, lockkey : " + lockkey);
			lockContextMap.remove(lockContextKey);
			return ReleaseRet.NOT_EXIST;
		}

		int acquireCount = lockContext.decrAquiredCount();
		if (acquireCount == 0) {
			lockContext.getReleased().compareAndSet(false, true);
			lockContextMap.remove(lockContextKey);
			cancelRenewTask(lockkey, ownerThreadID, lockType, opcode);
		}

		return acquireCount;
	}

	/**
	 * 处理锁过期广播事件
	 * @param notifyEvent
	 */
	public void dealBroadcastEvent(final NotifyEvent notifyEvent) {
		final String lockkey = notifyEvent.getLockkey();
		long threadID = notifyEvent.getThreadID();
		final String contextKey = lockkey + LockManager.lockkey_threadID_seperator + threadID;
		scheduledExecutorService.execute(new Runnable() {

			@Override
			public void run() {
				// 锁被释放；
				if (notifyEvent.isExpiredEvent()) {
					LockContext lockContext = LockManager.this.lockContextMap(notifyEvent.getEventType()).get(contextKey);
					if (lockContext == null) {
						return;
					}

					if (lockContext.getReleased().compareAndSet(false, true)) {
						releaseLockLocal(lockkey, lockContext.getLockOption().getThreadID(), lockContext.getLockContextkey(), true, lockContext.getLockOption().lockType, lockContext.getLockOption().lockOpcode);
						onLockExpired(lockContext);
					}
				}
			}
		});
	}



	/**
	 * 锁过期回调
	 * @param lockContext
	 */
	public void onLockExpired(LockContext lockContext) {
		InternalLockOption lockOption = lockContext.getLockOption();

		String lockkey = lockContext.getLockkey();
		try {
			EventCachedHandler.getInstance(wlockClient).unRegisterAcquireEvent(lockkey, lockOption.getThreadID());
			LockExpireListener lockExpirelistener = lockOption.getLockExpireListener();
			if (lockExpirelistener != null) {
				lockExpirelistener.onExpire(lockkey);
			}
			logger.warn(Version.INFO + " lock expired, lockkey : " + lockkey + ", expireTime:" + lockOption.getExpireTime());
		} catch(Exception e) {
			logger.error(Version.INFO + " lock expired callback error.", e);
		}
	}

	/**
	 * 更新锁过期时间
	 * @param lockkey
	 * @param threadID
	 */
	public void updateExpireTime(String lockkey, long threadID, int lockType, int lockOpcode, long expireTime) {
		if (lockkey == null) {
			logger.error(Version.INFO + " update lock expire time failed.");
			return;
		}
		String contextKey = lockkey + LockManager.lockkey_threadID_seperator + threadID;
		LockContext lockContext = LockManager.this.lockContextMap(lockType, lockOpcode).get(contextKey);
		if (lockContext != null) {
			updateExpireTime(lockContext, expireTime);
		}
	}

	/**
	 * 更新锁过期时间
	 * @param lockContext
	 */
	public void updateExpireTime(LockContext lockContext, long expireTime) {
		lockContext.updateExpireTime(expireTime);
		expireLock.lock();
		try {
			this.expireCheckSet.remove(lockContext);
			this.expireCheckSet.add(lockContext);
		} finally {
			expireLock.unlock();
		}
	}

	/**
	 * 自动续约
	 * @param lockkey
	 * @param lockContext
	 */
	public void startAutoRenew(String lockkey, LockContext lockContext, int lockType, int lockOpcode) {
		if (!containsRenewTask(lockContext.getLockContextkey(), lockType, lockOpcode)) {
			this.renewLock.lock();
			try {
				if (!containsRenewTask(lockContext.getLockContextkey(), lockType, lockOpcode)) {
					RenewLockTask renewLockTask = new RenewLockTask(lockkey, lockContext, this.wlockClient.getLockService(), false, lockType, lockOpcode);
					putRenewTask(lockContext.getLockContextkey(), lockType, lockOpcode, renewLockTask);
					sheduleRenew(renewLockTask, lockContext.getLockOption().getRenewInterval());
				}
			} finally {
				this.renewLock.unlock();
			}
		}
	}

	/**
	 * 首次获取到锁，需要立即续约
	 * @param lockkey
	 * @param lockContext
	 */
	public void startTouchRenew(String lockkey, LockContext lockContext, int lockType, int lockOpcode) {
		if (!containsRenewTask(lockContext.getLockContextkey(), lockType, lockOpcode)) {
			this.renewLock.lock();
			try {
				if (!containsRenewTask(lockContext.getLockContextkey(), lockType, lockOpcode)) {
					RenewLockTask renewLockTask = new RenewLockTask(lockkey, lockContext, this.wlockClient.getLockService(), true, lockType, lockOpcode);
					putRenewTask(lockContext.getLockContextkey(), lockType, lockOpcode, renewLockTask);
					sheduleRenew(renewLockTask, 0);
				}
			} finally {
				this.renewLock.unlock();
			}
		}
	}

	public void sheduleRenew(RenewLockTask renewTask, int interval) {
		scheduledExecutorService.schedule(renewTask, interval, TimeUnit.MILLISECONDS);
	}

	/**
	 * 取消自动续约
	 * @param lockkey
	 * @param ownerThreadID
	 */
	public void cancelRenewTask(String lockkey, long ownerThreadID, int lockType, int opcode) {
		String contextKey = lockkey + lockkey_threadID_seperator + ownerThreadID;

		if (containsRenewTask(contextKey, lockType, opcode)) {
			this.renewLock.lock();
			try {
				if (containsRenewTask(contextKey, lockType, opcode)) {
					RenewLockTask renewLockTask = getRenewTask(contextKey, lockType, opcode);
					renewLockTask.setCanceled(true);
					removeRenewTask(contextKey, lockType, opcode);
				}
			} finally {
				this.renewLock.unlock();
			}
		}
	}



	private String renewTaskKey(String contextKey, int lockType, int opcode) {
		return contextKey + lockkey_threadID_seperator + lockType + lockkey_threadID_seperator + opcode;
	}

	private RenewLockTask getRenewTask(String contextKey, int lockType, int opcode) {
		return this.renewTaskMap.get(renewTaskKey(contextKey, lockType, opcode));
	}

	private boolean containsRenewTask(String contextKey, int lockType, int opcode) {
		return renewTaskMap.containsKey(renewTaskKey(contextKey, lockType, opcode));
	}

	private void removeRenewTask(String contextKey, int lockType, int opcode) {
		this.renewTaskMap.remove(renewTaskKey(contextKey, lockType, opcode));
	}

	private void putRenewTask(String contextKey, int lockType, int opcode, RenewLockTask renewLockTask) {
		this.renewTaskMap.put(renewTaskKey(contextKey, lockType, opcode), renewLockTask);
	}

	public  class RenewLockTask implements Runnable {
		private String lockkey;
		private long lockversion;
		private long ownerThreadID;
		private int renewInterval;
		private RenewListener renewListener;
		private HoldLockListener holdLockListener;
		private WatchListener watchListener;
		private long expireTime;
		private LockContext lockContext;
		private LockService lockService;
		private boolean isCanceled = false;
		private boolean isAutoRenew = false;
		private boolean isTouch = false;
		private int touchTimes = 0;
		private long realExpireTimeStamp;
		public int MAX_TOUCH_TIMES = 10;

		private int lockType;
		private int lockOpcode;

		private RenewLockTask(String lockkey, LockContext lockContext, LockService lockService, boolean isTouch, int lockType, int lockOpcode) {
			this.lockkey = lockkey;
			this.lockContext = lockContext;
			this.lockversion = lockContext.getLockVersion();
			this.watchListener = lockContext.getLockOption().getWatchPolicy() == WatchPolicy.Continue ? lockContext.getLockOption().getWatchListener() : null;
			this.ownerThreadID = this.lockContext.getLockOption().getThreadID();
			this.renewInterval = this.lockContext.getLockOption().getRenewInterval();
			this.renewListener = this.lockContext.getLockOption().getRenewListener();
			this.holdLockListener = this.lockContext.getLockOption().getHoldLockListener();
			this.expireTime = this.lockContext.getLockOption().getExpireTime();
			this.lockService = lockService;
			this.isTouch = isTouch;
			this.isAutoRenew = this.lockContext.getLockOption().isAutoRenew();
			this.realExpireTimeStamp = this.lockContext.getLockOption().getRealExpireTimeStamp();
			this.lockType = lockType;
			this.lockOpcode = lockOpcode;
		}

		@Override
		public void run() {
			LockResult renewRes = null;
			try {
				if (isCanceled) {
					return;
				}

				if (isNeedRenew()) {
					renewRes = this.lockService.renewLock(lockkey, lockversion, this.expireTime, ownerThreadID, lockType, lockOpcode);
					if (this.renewListener != null) {
						if (renewRes.isSuccess()) {
							renewListener.onRenewSuccess(lockkey);
						} else {
							renewListener.onRenewFailed(lockkey);
						}
					}
					if (!renewRes.isSuccess()) {
						if (renewRes.getResponseStatus() == ResponseStatus.LOCK_CHANGED_OWNER
								|| renewRes.getResponseStatus() == ResponseStatus.TOKEN_ERROR
								|| renewRes.getResponseStatus() == ResponseStatus.LOCK_DELETED) {
							cancelRenewTask(this.lockkey, this.ownerThreadID, lockType, lockOpcode);
							if (holdLockListener != null) {
								holdLockListener.onOwnerChange(lockkey, ResponseStatus.toStr(renewRes.getResponseStatus()));
							}
							if (watchListener != null) {
								watchListener.onLockChange(lockkey, lockversion);
							}
							if (this.isTouch) {
								logger.error(Version.INFO + ", lock acquired, but not touch success within 10s, lockkey : " + this.lockkey);
							} else {
								logger.error(Version.INFO + ", renew lock failed, as lock state has changed, so stoped, lockkey : " + this.lockkey);
							}
							return;
						}
						logger.error(Version.INFO + ",renew lock failed, lockkey : " + this.lockkey);
					}
				}
			} catch (ParameterIllegalException e) {
				logger.error(Version.INFO + ", renew lock error.", e);
			} catch (Throwable th) {
				logger.error(Version.INFO + ", renew lock unknown error.", th);
			}

			if (!this.isTouch && realExpireTimeStamp > 0) {
				dealHoldExpire();
				return;
			}

			if (!this.isTouch && isAutoRenew) {
				// 自动续约
				LockManager.this.sheduleRenew(this, this.renewInterval);
				return;
			}

			if (this.isTouch) {
				touchTimes++;
				if (renewRes == null || !renewRes.isSuccess()) {
					// 续约失败
					if (touchTimes <= MAX_TOUCH_TIMES) {
						// 首次获取到锁后，一直没续约成功
						int interval = (int) Math.max(this.renewInterval, this.expireTime/3);
						LockManager.this.sheduleRenew(this, Math.min(1000, interval));
						return;
					} else {
						// touch达到上限，放弃续约
						cancelRenewTask(this.lockkey, this.ownerThreadID, lockType, lockOpcode);
						logger.error(Version.INFO + ", lock acquired, but not touch success within 10s, lockkey : " + this.lockkey);
					}
				} else {
					// 续约成功
					if (isAutoRenew) {
						LockManager.this.sheduleRenew(this, this.renewInterval);
						isTouch = false;
						return;
					}
					if (realExpireTimeStamp > 0) {
						dealHoldExpire();
						return;
					}
					cancelRenewTask(this.lockkey, this.ownerThreadID, lockType, lockOpcode);
				}
			}


		}

		private void dealHoldExpire() {
			long lastTime = realExpireTimeStamp - System.currentTimeMillis() - renewInterval;
			this.expireTime = lastTime > Factor.LOCK_MAX_EXPIRETIME ? Factor.LOCK_MAX_EXPIRETIME : (int) lastTime;
			if (lastTime > Factor.LOCK_MIN_EXPIRETIME + renewInterval) {
				LockManager.this.sheduleRenew(this, renewInterval);
			}
		}

		private boolean isNeedRenew() {
			return realExpireTimeStamp == 0 || realExpireTimeStamp - System.currentTimeMillis() > renewInterval;
		}

		public boolean isCanceled() {
			return isCanceled;
		}

		public void setCanceled(boolean isCanceled) {
			this.isCanceled = isCanceled;
		}
	}

	class ExpireCheckTask implements Runnable {

		@Override
		public void run() {
			try {
				Set<LockContext> removeSet = new HashSet<LockContext>();
				Set<LockContext> expireSet = new HashSet<LockContext>();

				TreeSet<LockContext> checkCopyset = new TreeSet<LockContext>(new Comparator<LockContext>() {
					@Override
					public int compare(final LockContext o1, final LockContext o2) {
						return o1.compareTo(o2);
					}
				});
				expireLock.lock();
				try {
					checkCopyset.addAll(expireCheckSet);
				} finally {
					expireLock.unlock();
				}
				Iterator<LockContext> iter = checkCopyset.iterator();
				while(iter.hasNext()) {
					LockContext lockContext = iter.next();
					if (!lockContext.isExpired()) {
						break;
					}

					if (lockContext.getReleased().compareAndSet(false, true)) {
						expireSet.add(lockContext);
					}
					removeSet.add(lockContext);
				}

				expireLock.lock();
				try {
					expireCheckSet.removeAll(removeSet);
				} finally {
					expireLock.unlock();
				}

				for (LockContext lockContext : expireSet) {
					String lockkey = lockContext.getLockkey();
					long lockVersion = lockContext.getLockVersion();
					long threadID = lockContext.getLockOption().getThreadID();
					wlockClient.getLockService().releaseLock(lockkey, lockVersion, true, threadID, lockContext.getLockOption().lockType, lockContext.getLockOption().getLockOpcode());
					onLockExpired(lockContext);
					logger.warn(Version.INFO + ", Local check lockkey " + lockkey + " expired.");
				}
			} catch(Throwable th) {
				logger.error(Version.INFO + ", ExpireCheckTask error.", th);
			}
		}
	}

	public interface ReleaseRet {
		/**
		 * 大于0:释放成功
		 */
		int SUCCESS = 0;

		/**
		 * 释放锁失败
		 */
		int FAIL = -1;

		/**
		 * 锁不存在
		 */
		int NOT_EXIST = -2;
	}
}
