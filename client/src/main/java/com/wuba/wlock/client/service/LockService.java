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
package com.wuba.wlock.client.service;

import com.wuba.wlock.client.*;
import com.wuba.wlock.client.communication.SendReqResult;
import com.wuba.wlock.client.communication.ServerPoolHandler;
import com.wuba.wlock.client.communication.WatchPolicy;
import com.wuba.wlock.client.config.Factor;
import com.wuba.wlock.client.config.ParameterChecker;
import com.wuba.wlock.client.config.Version;
import com.wuba.wlock.client.exception.ParameterIllegalException;
import com.wuba.wlock.client.lockresult.AcquireLockResult;
import com.wuba.wlock.client.lockresult.GetLockResult;
import com.wuba.wlock.client.lockresult.LockResult;
import com.wuba.wlock.client.protocol.IProtocolFactory;
import com.wuba.wlock.client.protocol.ResponseStatus;
import com.wuba.wlock.client.util.InetAddressUtil;
import com.wuba.wlock.client.protocol.extend.*;
import com.wuba.wlock.client.watch.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class LockService {
	private final int PROCESS_LOCK_THREAD_ID = -1;

	private static final Log logger = LogFactory.getLog(LockService.class);
	private final IProtocolFactory protocolFactory;
	private final WLockClient wlockClient;
	private final ServerPoolHandler serverPoolHandler;
	private final LockManager lockManager;
	private final WatchManager watchManager;
	private final String registryKey;

	public LockService(WLockClient wlockClient) {
		this.wlockClient = wlockClient;
		this.serverPoolHandler = this.wlockClient.getServerPoolHandler();
		this.protocolFactory = ProtocolFactoryImpl.getInstance();
		this.lockManager = new LockManager(wlockClient);
		this.watchManager = new WatchManager(wlockClient, this.lockManager);
		this.registryKey = wlockClient.getRegistryKey().getRegistryKey();
	}

	/**
	 * 竞争锁逻辑
	 *
	 * @param lockkey
	 * @param lockOption 锁参数
	 * @return
	 * @throws ParameterIllegalException
	 */
	public AcquireLockResult tryAcquireLock(String lockkey, InternalLockOption lockOption) throws ParameterIllegalException {
		AcquireLockResult result = new AcquireLockResult();
		result.setRet(false);
		long startTimestamp = System.currentTimeMillis();
		lockParameterCheck(lockOption);
		if (this.lockManager.acquiredLockLocal(lockkey, lockOption)) {
			/*本地获取成功锁*/
			LockContext lockContext = lockManager.getLocalLockContext(lockkey, lockOption.getThreadID(), lockOption.getLockType(), lockOption.getLockOpcode());
			LockResult lockResult = renewLock(lockkey, lockContext.getLockVersion(), lockOption.getExpireTime(), lockOption.getThreadID(), lockOption.getLockType(), lockOption.getLockOpcode());
			if (!lockResult.isSuccess()) {
				this.lockManager.releaseLockLocal(lockkey, lockOption.getThreadID(), false, lockOption.getLockType(), lockOption.getLockOpcode());
			}

			result.setOwner(new LockOwner(InetAddressUtil.getIpInt(),lockOption.getThreadID(),lockOption.getPID()));
			result.setResponseStatus(lockResult.getResponseStatus());
			result.setRet(lockResult.isSuccess());
			result.setLockVersion(lockContext.getLockVersion());
			return result;
		}

		int timeout = (int) Math.min(this.wlockClient.getDefaultTimeoutForReq(), lockOption.getMaxWaitTime());
		WatchEvent watchEvent = null;
		if (lockOption.isWaitAcquire()) {
			/*优先注册watchevent*/
			watchEvent = new WatchEvent(lockkey, lockOption.getThreadID(),
					lockOption.getWatchID(), WatchType.ACQUIRE, startTimestamp);
			watchEvent.setLockOption(lockOption);
			watchEvent.setTimeout(lockOption.getMaxWaitTime());
			this.watchManager.registerWatchEvent(lockkey, watchEvent);
		}

		int groupId = this.wlockClient.getRegistryKey().getGroupId(lockkey);
		AcquireLockRequest acquireLockReq = protocolFactory.createAcquireReq(lockkey, groupId, lockOption);
		try {
			SendReqResult sendReqResult = this.serverPoolHandler.syncSendRequest(acquireLockReq, timeout, "tryAcquireLock " + lockkey);
			if (sendReqResult != null) {
				AcquireLockResponse resp = new AcquireLockResponse();
				resp.fromBytes(sendReqResult.getData());
				result.setResponseStatus(resp.getStatus());
				if (resp.getStatus() == ResponseStatus.LOCK_WAIT) {
					logger.debug(Version.INFO + ", tryAcquireLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + ", threadID : " + lockOption.getThreadID());
					NotifyEvent notifyEvent = this.watchManager.waitNotifyEvent(lockOption.getWatchID(), lockOption.getMaxWaitTime());
					if (notifyEvent != null && notifyEvent.getEventType() == EventType.LOCK_ACQUIRED.getType()) {
						this.lockManager.updateLockLocal(lockkey, notifyEvent.getFencingToken(), lockOption, true);
						EventCachedHandler.getInstance(wlockClient).unRegisterWatchEvent(lockkey, notifyEvent.getWatchID());
						AcquireEvent acquireEvent = new AcquireEvent(lockkey, resp.getFencingToken(), lockOption, lockOption.getThreadID());
						EventCachedHandler.getInstance(wlockClient).registerAcquireEvent(acquireEvent);
						result.setRet(true);
						result.setLockVersion(notifyEvent.getFencingToken());
						result.setOwner(new LockOwner(acquireLockReq.getHost(), acquireLockReq.getThreadID(), acquireLockReq.getPid()));
						result.setResponseStatus(ResponseStatus.SUCCESS);
					} else {
						result.setRet(false);
						logger.error(Version.INFO + ", tryAcquireLock blocked, timeout , lockkey : " + lockkey + ", timeout : " + lockOption.getMaxWaitTime());
					}

					return result;
				} else if (resp.getStatus() == ResponseStatus.SUCCESS) {
					this.lockManager.updateLockLocal(lockkey, resp.getFencingToken(), lockOption, false);
					result.setRet(true);
					result.setLockVersion(resp.getFencingToken());
					result.setOwner(new LockOwner(resp.getOwnerHost(), resp.getThreadID(), resp.getPid()));
					AcquireEvent acquireEvent = new AcquireEvent(lockkey, resp.getFencingToken(), lockOption, lockOption.getThreadID());
					EventCachedHandler.getInstance(wlockClient).registerAcquireEvent(acquireEvent);
					return result;
				} else if (resp.getStatus() == ResponseStatus.TIMEOUT) {
					result.setRet(false);
					logger.error(Version.INFO + ", tryAcquireLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + ", server : " + sendReqResult.getServer() + ", timeout : " + timeout);
					//retries++;
				} else {
					result.setRet(false);
					if (resp.getStatus() == ResponseStatus.LOCK_OCCUPIED) {
						logger.debug(Version.INFO + ", tryAcquireLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey);
					} else {
						logger.info(Version.INFO + ", tryAcquireLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey);
					}
					return result;
				}
			}
		} catch (Exception e) {
			logger.error(Version.INFO + ", tryAcquireLock error.", e);
		} finally {
			this.watchManager.unRegisterWatchEvent(lockkey, lockOption.getWatchID());
		}

		return result;
	}

	/**
	 * watch锁逻辑
	 *
	 * @param lockkey
	 * @param watchLockOption
	 * @return
	 * @throws ParameterIllegalException
	 */
	public LockResult watchLock(String lockkey, InternalLockOption watchLockOption) throws ParameterIllegalException {
		long startTimestamp = System.currentTimeMillis();
		lockParameterCheck(watchLockOption);

		int timeout = this.wlockClient.getDefaultTimeoutForReq();
		if (watchLockOption.getMaxWaitTime() > 0 && watchLockOption.getMaxWaitTime() < this.wlockClient.getDefaultTimeoutForReq()) {
			timeout = (int) watchLockOption.getMaxWaitTime();
		}

		WatchType watchType = watchLockOption.isWaitAcquire() ? WatchType.WATCH_AND_ACQUIRE : WatchType.WATCH;

		WatchEvent watchEvent = new WatchEvent(lockkey, watchLockOption.getThreadID(),
				watchLockOption.getWatchID(), watchType, startTimestamp);
		watchEvent.setLockOption(watchLockOption);
		watchEvent.setTimeout(watchLockOption.getMaxWaitTime());
		watchEvent.setWatchType(watchType);

		int groupId = this.wlockClient.getRegistryKey().getGroupId(lockkey);
		WatchLockRequest watchLockReq = protocolFactory.createWatchLockReq(lockkey, groupId, watchType, watchLockOption);
		try {
			SendReqResult sendReqResult = this.serverPoolHandler.syncSendRequest(watchLockReq, timeout, "watchLock " + lockkey);
			if (sendReqResult != null) {
				WatchLockResponse resp = new WatchLockResponse();
				resp.fromBytes(sendReqResult.getData());
				if (resp.getStatus() == ResponseStatus.SUCCESS) {
					EventCachedHandler.getInstance(wlockClient).registerWatchEvent(lockkey, watchEvent);
					return new LockResult(true, resp.getStatus());
				} else if (resp.getStatus() == ResponseStatus.TIMEOUT) {
					logger.error(Version.INFO + ", watchLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + ", server : " + sendReqResult.getServer() + ", timeout : " + timeout);
				} else {
					logger.error(Version.INFO + ", watchLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey);
				}
				
				return new LockResult(false, resp.getStatus());
			}
		} catch (Exception e) {
			logger.error(Version.INFO + ", watchLock error.", e);
		}

		return new LockResult(false, ResponseStatus.TIMEOUT);
	}

	/**
	 * 重新发送watch请求到服务端
	 *
	 * @param lockkey
	 * @param watchEvent watch 事件
	 * @return
	 * @throws ParameterIllegalException
	 */
	public LockResult reWatchLock(String lockkey, WatchEvent watchEvent) throws ParameterIllegalException {
		InternalLockOption watchLockOption = watchEvent.getLockOption();
		lockParameterCheck(watchLockOption);

		int timeout = this.wlockClient.getDefaultTimeoutForReq();
		if (watchLockOption.getMaxWaitTime() > 0 && watchLockOption.getMaxWaitTime() < this.wlockClient.getDefaultTimeoutForReq()) {
			timeout = (int) watchLockOption.getMaxWaitTime();
		}

		WatchType watchType = watchEvent.getWatchType();

		watchEvent.setLockOption(watchLockOption);

		int groupId = this.wlockClient.getRegistryKey().getGroupId(lockkey);
		WatchLockRequest watchLockReq = protocolFactory.createWatchLockReq(lockkey, groupId, watchType, watchLockOption);
		
		try {
			SendReqResult sendReqResult = this.serverPoolHandler.syncSendRequest(watchLockReq, timeout, "reWatchLock " + lockkey);
			if (sendReqResult != null) {
				WatchLockResponse resp = new WatchLockResponse();
				resp.fromBytes(sendReqResult.getData());
				if (resp.getStatus() == ResponseStatus.SUCCESS) {
					return new LockResult(true, resp.getStatus());
				} else if (resp.getStatus() == ResponseStatus.TIMEOUT) {
					logger.error(Version.INFO + ", watchLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + ", server : " + sendReqResult.getServer() + ", timeout : " + timeout);
				} else {
					logger.error(Version.INFO + ", watchLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey);
				}
				
				return new LockResult(false, resp.getStatus());
			}
		} catch (Exception e) {
			logger.error(Version.INFO + ", reWatchLock error.", e);
		}
		
		return new LockResult(false, ResponseStatus.TIMEOUT);
	}

	public boolean unWatchLock(String lockkey) {
		return false;
	}
	
	/**
	 * 锁续约逻辑
	 *
	 * @param lockkey
	 * @param expireTime
	 * @param ownerThreadID
	 * @return
	 * @throws ParameterIllegalException
	 */
	public LockResult renewLock(String lockkey, long lockVersion, long expireTime, long ownerThreadID, int lockType, int lockOpcode) throws ParameterIllegalException {
		return renewLock(lockkey, lockVersion, expireTime, ownerThreadID, false, lockType, lockOpcode);
	}

	/**
	 * 锁续约逻辑
	 *
	 * @param lockkey
	 * @param expireTime
	 * @param ownerThreadID
	 * @return
	 * @throws ParameterIllegalException
	 */
	public LockResult renewLock(String lockkey, long lockVersion, long expireTime, long ownerThreadID, boolean isRtouch, int lockType, int lockOpcode) throws ParameterIllegalException {
		if (!ParameterChecker.lockExpireTimeCheck(expireTime)) {
			logger.error("renew lock expireTime illegal, max " + Factor.LOCK_MAX_EXPIRETIME + ", you set : " + expireTime);
			throw new ParameterIllegalException("parameter expireTime illegal.");
		}

		if (lockVersion == -1) {
			LockContext lockContext = lockManager.getLocalLockContext(lockkey, ownerThreadID, lockType, lockOpcode);
			if (lockContext == null) {
				return new LockResult(false, ResponseStatus.LOCK_DELETED);
			}
			lockVersion = lockContext.getLockVersion();
		}

		int sexpireTime = isRtouch ? 0 :(int)  expireTime;
		int timeout = this.wlockClient.getDefaultTimeoutForReq();

		int groupId = this.wlockClient.getRegistryKey().getGroupId(lockkey);
		RenewLockRequest renewLockReq = protocolFactory.createRenewLockReq(lockkey, groupId, this.registryKey, lockVersion, sexpireTime, ownerThreadID, WLockClient.currentPid, lockType, lockOpcode);
		try {
			SendReqResult sendReqResult = this.serverPoolHandler.syncSendRequest(renewLockReq, timeout, "renewLock " + lockkey);
			if (sendReqResult != null) {
				RenewLockResponse resp = new RenewLockResponse();
				resp.fromBytes(sendReqResult.getData());
				if (resp.getStatus() == ResponseStatus.SUCCESS) {
					if (!isRtouch) {
						this.lockManager.updateExpireTime(lockkey, ownerThreadID, lockType, lockOpcode, expireTime);
					}
					return new LockResult(true, ResponseStatus.SUCCESS);
				} else if (resp.getStatus() == ResponseStatus.TIMEOUT) {
					logger.error(Version.INFO + ", renewLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + ", server : " + sendReqResult.getServer() + ", timeout : " + timeout);
				} else {
					logger.error(Version.INFO + ", renewLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + "version: " + lockVersion);
				}
				
				return new LockResult(false, resp.getStatus());
			}
		} catch (Exception e) {
			logger.error(Version.INFO + ", renewLock error.", e);
		}
		
		return new LockResult(false, ResponseStatus.TIMEOUT);
	}
	
	public LockResult releaseLock(String lockkey, long lockVersion, long threadID, int lockType, int opcode) {
		return releaseLock(lockkey, lockVersion, false, threadID, lockType, opcode);
	}

	@Deprecated
	public LockResult releaseLock(String lockkey, long lockVersion, int lockType, int opcode) {
		return releaseLock(lockkey, lockVersion, false, Thread.currentThread().getId(), lockType, opcode);
	}


	/**
	 * 释放锁逻辑
	 *
	 * @param lockkey
	 * @param lockVersion
	 * @param forced
	 * @param threadID
	 * @param lockType
	 * @return
	 */
	public LockResult releaseLock(String lockkey, long lockVersion, boolean forced, long threadID, int lockType, int opcode) {
		int timeout = this.wlockClient.getDefaultTimeoutForReq();
		
		LockContext lockContext = lockManager.getLocalLockContext(lockkey, threadID, lockType, opcode);
		if (lockContext == null) {
			return new LockResult(false, ResponseStatus.LOCK_DELETED);
		}

		long ownerThreadID = threadID;
		if (lockVersion == -1) {
			lockVersion = lockContext.getLockVersion();
		}
		if (ownerThreadID == -1) {
			ownerThreadID = lockContext.getLockOption().getThreadID();
		}

		int releaseRet = this.lockManager.releaseLockLocal(lockkey, ownerThreadID, forced, lockType, opcode);
		
		if (releaseRet > LockManager.ReleaseRet.SUCCESS) {
			/*本地释放锁success*/
			return new LockResult(true, ResponseStatus.SUCCESS);
		}

		if (releaseRet == LockManager.ReleaseRet.FAIL) {
			/*释放锁失败*/
			return new LockResult(false, ResponseStatus.ERROR);
		}

		/*远程释放锁*/
		int groupId = this.wlockClient.getRegistryKey().getGroupId(lockkey);
		ReleaseLockRequest releaseLockReq = protocolFactory.createReleaseLockReq(lockkey, groupId, this.registryKey, lockVersion, ownerThreadID, WLockClient.currentPid, lockType, opcode);
		try {
			SendReqResult sendReqResult = this.serverPoolHandler.syncSendRequest(releaseLockReq, timeout, "releaseLock " + lockkey);
			if (sendReqResult != null) {
				ReleaseLockResponse resp = new ReleaseLockResponse();
				resp.fromBytes(sendReqResult.getData());
				if (resp.getStatus() == ResponseStatus.SUCCESS) {
					EventCachedHandler.getInstance(wlockClient).unRegisterAcquireEvent(lockkey, ownerThreadID);
					return new LockResult(true, ResponseStatus.SUCCESS);
				} else if (resp.getStatus() == ResponseStatus.TIMEOUT) {
					logger.error(Version.INFO + ", releaseLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + ", server : " + sendReqResult.getServer() + ", timeout : " + timeout);
				} else if (resp.getStatus() == ResponseStatus.LOCK_DELETED) {
					logger.error(Version.INFO + ", releaseLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + " version: " + lockVersion);
					return new LockResult(false, resp.getStatus());
				} else {
					logger.error(Version.INFO + ", releaseLock status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + " version: " + lockVersion);
				}

				return new LockResult(false, resp.getStatus());
			}
		} catch (Exception e) {
			logger.error(Version.INFO + ", releaseLock error.", e);
		}

		return new LockResult(false, ResponseStatus.TIMEOUT);
	}

	/**
	 * 读取lock状态
	 *
	 * @param lockkey
	 * @return
	 */
	public GetLockResult getLockState(String lockkey) {
		GetLockResult getLockResult = new GetLockResult();
		getLockResult.setRet(false);
		int timeout = this.wlockClient.getDefaultTimeoutForReq();

		int groupId = this.wlockClient.getRegistryKey().getGroupId(lockkey);
		GetLockRequest getLockReq = protocolFactory.createGetLockReq(lockkey, groupId, this.registryKey);
		try {
			SendReqResult sendReqResult = this.serverPoolHandler.syncSendRequest(getLockReq, timeout, "getLockState " + lockkey);
			if (sendReqResult != null) {
				GetLockResponse resp = new GetLockResponse();
				resp.fromBytes(sendReqResult.getData());
				getLockResult.setResponseStatus(resp.getStatus());
				if (resp.getStatus() == ResponseStatus.SUCCESS) {
					getLockResult.setRet(true);
					getLockResult.setOwner(new LockOwner(resp.getOwnerHost(), resp.getOwnerThreadID(), resp.getOwnerPID()));
					getLockResult.setLockVersion(resp.getFencingToken());
					return getLockResult;
				} else if (resp.getStatus() == ResponseStatus.TIMEOUT) {
					logger.error(Version.INFO + ", getLockState status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey + ", server : " + sendReqResult.getServer() + ", timeout : " + timeout);
				} else {
					logger.error(Version.INFO + ", getLockState status : " + ResponseStatus.toStr(resp.getStatus()) + ", lockkey : " + lockkey);
				}
				return getLockResult;
			}
		} catch (Exception e) {
			logger.error(Version.INFO + ", getLockState error.", e);
		}
		
		return getLockResult;
	}

	public boolean lockParameterCheck(InternalLockOption lockOption) throws ParameterIllegalException {
		if (lockOption.getRealExpireMills() != 0 && lockOption.getHoldLockListener() == null) {
			logger.error("lock expireTime illegal, more than  "
					+ Factor.LOCK_MAX_EXPIRETIME + " must set hold lock listener");
			throw new ParameterIllegalException("parameter expireTime illegal.");
		}

		if (!ParameterChecker.lockExpireTimeCheck(lockOption.getExpireTime())) {
			logger.error("lock expireTime illegal, min "
					+ Factor.LOCK_MIN_EXPIRETIME + ", you set : " + lockOption.getExpireTime());
			throw new ParameterIllegalException("parameter expireTime illegal.");
		}

		if (!ParameterChecker.lockweightCheck(lockOption.getWeight())) {
			logger.error("lock weight illegal, max "
					+ Factor.ACQUIRE_LOCK_MAX_WEIGHT + ", min "
					+ Factor.ACQUIRE_LOCK_MIN_WEIGHT + ", you set : " + lockOption.getWeight());
			throw new ParameterIllegalException("parameter weight illegal.");
		}

		if (!lockOption.isAutoRenewEnabled() && lockOption.isAutoRenew()) {
			logger.error("lock auto renew enabled false, please contact admin.");
			throw new ParameterIllegalException("parameter renewInterval illegal.");
		}
		
		if (!ParameterChecker.lockRenewIntervalCheck(lockOption.getRenewInterval(), lockOption.getExpireTime())) {
			logger.error("lock renewInterval illegal, must less than expireTime " + lockOption.getExpireTime() 
					+ " and greater than MIN_RENEW_INTERVAL "  + Factor.LOCK_MIN_RENEWINTERVAL
					+", you set : " + lockOption.getRenewInterval());
			throw new ParameterIllegalException("parameter renewInterval illegal.");
		}

		if (lockOption.getWatchPolicy() == WatchPolicy.Continue && lockOption.getThreadID() != PROCESS_LOCK_THREAD_ID) {
			logger.error("lock policy illegal, must use process lock.");
			throw new ParameterIllegalException("parameter lockPolicy illegal.");
		}

		return true;
	}

	public WatchManager getWatchManager() {
		return watchManager;
	}

	public LockManager getLockManager() {
		return lockManager;
	}
}