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
package com.wuba.wlock.server.collector.entity;

import com.wuba.wlock.common.collector.protocol.QpsEntity;

import java.util.concurrent.atomic.AtomicLong;

public class QpsLockStatCounter {
	private AtomicLong acquireQps = new AtomicLong(0L);
	private AtomicLong acquireFailQps = new AtomicLong(0L);
	private AtomicLong renewQps = new AtomicLong(0L);
	private AtomicLong renewFailQps = new AtomicLong(0L);
	private AtomicLong releaseQps = new AtomicLong(0L);
	private AtomicLong releaseFailQps = new AtomicLong(0L);
	private AtomicLong watchQps = new AtomicLong(0L);
	private AtomicLong watchFailQps = new AtomicLong(0L);
	private AtomicLong getQps = new AtomicLong(0L);
	private AtomicLong getFailQps = new AtomicLong(0L);
	private AtomicLong deleteQps = new AtomicLong(0L);
	private AtomicLong deleteFailQps = new AtomicLong(0L);
	private AtomicLong otherQps = new AtomicLong(0L);
	private AtomicLong abandonQps = new AtomicLong(0L);

	public QpsLockStatCounter() {
	}

	public QpsLockStatCounter(QpsLockStatCounter qpsLockStatCounter) {
		this.acquireQps.set(qpsLockStatCounter.acquireQps.longValue());
		this.acquireFailQps.set(qpsLockStatCounter.acquireFailQps.longValue());
		this.renewQps.set(qpsLockStatCounter.renewQps.longValue());
		this.renewFailQps.set(qpsLockStatCounter.renewFailQps.longValue());
		this.releaseQps.set(qpsLockStatCounter.releaseQps.longValue());
		this.releaseFailQps.set(qpsLockStatCounter.releaseFailQps.longValue());
		this.watchQps.set(qpsLockStatCounter.watchQps.longValue());
		this.watchFailQps.set(qpsLockStatCounter.watchFailQps.longValue());
		this.otherQps.set(qpsLockStatCounter.otherQps.longValue());
		this.getQps.set(qpsLockStatCounter.getQps.longValue());
		this.getFailQps.set(qpsLockStatCounter.getFailQps.longValue());
		this.deleteQps.set(qpsLockStatCounter.deleteQps.longValue());
		this.deleteFailQps.set(qpsLockStatCounter.deleteFailQps.longValue());
		this.abandonQps.set(qpsLockStatCounter.abandonQps.longValue());
	}

	public void replaceBy(QpsLockStatCounter qpsLockStatCounter) {
		this.acquireQps.set(qpsLockStatCounter.acquireQps.longValue());
		this.acquireFailQps.set(qpsLockStatCounter.acquireFailQps.longValue());
		this.renewQps.set(qpsLockStatCounter.renewQps.longValue());
		this.renewFailQps.set(qpsLockStatCounter.renewFailQps.longValue());
		this.releaseQps.set(qpsLockStatCounter.releaseQps.longValue());
		this.releaseFailQps.set(qpsLockStatCounter.releaseFailQps.longValue());
		this.watchQps.set(qpsLockStatCounter.watchQps.longValue());
		this.watchFailQps.set(qpsLockStatCounter.watchFailQps.longValue());
		this.otherQps.set(qpsLockStatCounter.otherQps.longValue());
		this.getQps.set(qpsLockStatCounter.getQps.longValue());
		this.getFailQps.set(qpsLockStatCounter.getFailQps.longValue());
		this.deleteQps.set(qpsLockStatCounter.deleteQps.longValue());
		this.deleteFailQps.set(qpsLockStatCounter.deleteFailQps.longValue());
		this.abandonQps.set(qpsLockStatCounter.abandonQps.longValue());
	}

	public QpsEntity getRealTimeQps(QpsLockStatCounter lastQpsCounter) {
		QpsEntity qpsEntity = new QpsEntity();
		qpsEntity.setAcquireQps((int) (this.acquireQps.longValue() - lastQpsCounter.acquireQps.longValue()));
		qpsEntity.setAcquireFailQps((int) (this.acquireFailQps.longValue() - lastQpsCounter.acquireFailQps.longValue()));
		qpsEntity.setRenewQps((int) (this.renewQps.longValue() - lastQpsCounter.renewQps.longValue()));
		qpsEntity.setRenewFailQps((int) (this.renewFailQps.longValue() - lastQpsCounter.renewFailQps.longValue()));
		qpsEntity.setReleaseQps((int) (this.releaseQps.longValue() - lastQpsCounter.releaseQps.longValue()));
		qpsEntity.setReleaseFailQps((int) (this.releaseFailQps.longValue() - lastQpsCounter.releaseFailQps.longValue()));
		qpsEntity.setWatchQps((int) (this.watchQps.longValue() - lastQpsCounter.watchQps.longValue()));
		qpsEntity.setWatchFailQps((int) (this.watchFailQps.longValue() - lastQpsCounter.watchFailQps.longValue()));
		qpsEntity.setOtherQps((int) (this.otherQps.longValue() - lastQpsCounter.otherQps.longValue()));
		qpsEntity.setGetQps((int) (this.getQps.longValue() - lastQpsCounter.getQps.longValue()));
		qpsEntity.setGetFailQps((int) (this.getFailQps.longValue() - lastQpsCounter.getFailQps.longValue()));
		qpsEntity.setDeleteQps((int) (this.deleteQps.longValue() - lastQpsCounter.deleteQps.longValue()));
		qpsEntity.setDeleteFailQps((int) (this.deleteFailQps.longValue() - lastQpsCounter.deleteFailQps.longValue()));
		qpsEntity.setAbandonQps((int) (this.abandonQps.longValue() - lastQpsCounter.abandonQps.longValue()));
		return qpsEntity;
	}

	public void incrOtherQps() {
		otherQps.incrementAndGet();
	}

	public void incrAcquireQps() {
		acquireQps.incrementAndGet();
	}

	public void incrRenewQps() {
		renewQps.incrementAndGet();
	}

	public void incrReleaseQps() {
		releaseQps.incrementAndGet();
	}

	public void incrWatchQps() {
		watchQps.incrementAndGet();
	}

	public void incrGetQps() {
		getQps.incrementAndGet();
	}

	public void incrAcquireFailQps() {
		acquireFailQps.incrementAndGet();
	}

	public void incrRenewFailQps() {
		renewFailQps.incrementAndGet();
	}

	public void incrReleaseFailQps() {
		releaseFailQps.incrementAndGet();
	}

	public void incrWatchFailQps() {
		watchFailQps.incrementAndGet();
	}

	public void incrGetFailQps() {
		getFailQps.incrementAndGet();
	}

	public void incrDeleteQps() {
		deleteQps.incrementAndGet();
	}

	public void incrDeleteFailQps() {
		deleteFailQps.incrementAndGet();
	}

	public void incrAbandonQps(){
		abandonQps.incrementAndGet();
	}
}
