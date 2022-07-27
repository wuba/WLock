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
package com.wuba.wlock.common.collector.protocol;


import com.alibaba.fastjson.annotation.JSONField;

public class QpsEntity {
	private int acquireQps;
	private int acquireFailQps;
	private int releaseQps;
	private int releaseFailQps;
	private int renewQps;
	private int renewFailQps;
	private int watchQps;
	private int watchFailQps;
	private int getQps;
	private int getFailQps;
	private int otherQps;
	private int deleteQps;
	private int deleteFailQps;
	private int abandonQps;

	public QpsEntity() {
	}

	@JSONField(ordinal = 3)
	public int getAcquireQps() {
		return acquireQps;
	}

	public void setAcquireQps(int acquireQps) {
		this.acquireQps = acquireQps;
	}

	@JSONField(ordinal = 4)
	public int getReleaseQps() {
		return releaseQps;
	}

	public void setReleaseQps(int releaseQps) {
		this.releaseQps = releaseQps;
	}

	@JSONField(ordinal = 5)
	public int getRenewQps() {
		return renewQps;
	}

	public void setRenewQps(int renewQps) {
		this.renewQps = renewQps;
	}

	@JSONField(ordinal = 6)
	public int getWatchQps() {
		return watchQps;
	}

	public void setWatchQps(int watchQps) {
		this.watchQps = watchQps;
	}

	@JSONField(serialize = false)
	public int getOtherQps() {
		return otherQps;
	}

	public void setOtherQps(int otherQps) {
		this.otherQps = otherQps;
	}

	@JSONField(ordinal = 7)
	public int getGetQps() {
		return getQps;
	}

	public void setGetQps(int getQps) {
		this.getQps = getQps;
	}

	@JSONField(ordinal = 8)
	public int getAbandonQps() {
		return abandonQps;
	}

	public void setAbandonQps(int abandonQps) {
		this.abandonQps = abandonQps;
	}

	@JSONField(ordinal = 9)
	public int getDeleteQps() {
		return deleteQps;
	}

	public void setDeleteQps(int deleteQps) {
		this.deleteQps = deleteQps;
	}

	@JSONField(ordinal = 10)
	public int getAcquireFailQps() {
		return acquireFailQps;
	}

	public void setAcquireFailQps(int acquireFailQps) {
		this.acquireFailQps = acquireFailQps;
	}

	@JSONField(ordinal = 11)
	public int getReleaseFailQps() {
		return releaseFailQps;
	}

	public void setReleaseFailQps(int releaseFailQps) {
		this.releaseFailQps = releaseFailQps;
	}

	@JSONField(ordinal = 12)
	public int getRenewFailQps() {
		return renewFailQps;
	}

	public void setRenewFailQps(int renewFailQps) {
		this.renewFailQps = renewFailQps;
	}

	@JSONField(ordinal = 13)
	public int getWatchFailQps() {
		return watchFailQps;
	}

	public void setWatchFailQps(int watchFailQps) {
		this.watchFailQps = watchFailQps;
	}

	@JSONField(ordinal = 14)
	public int getGetFailQps() {
		return getFailQps;
	}

	public void setGetFailQps(int getFailQps) {
		this.getFailQps = getFailQps;
	}


	@JSONField(ordinal = 15)
	public int getDeleteFailQps() {
		return deleteFailQps;
	}

	public void setDeleteFailQps(int deleteFailQps) {
		this.deleteFailQps = deleteFailQps;
	}

	@JSONField(ordinal = 1)
	public int getAllQps() {
		return this.acquireQps + this.releaseQps + this.renewQps + this.watchQps + this.getQps + this.otherQps + this.deleteQps;
	}

	@JSONField(ordinal = 2)
	public int getAllFailQps() {
		return this.acquireFailQps + this.releaseFailQps + this.renewFailQps + this.watchFailQps + this.getFailQps + this.deleteFailQps;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("QpsEntity{");
		sb.append("acquireQps=").append(acquireQps);
		sb.append(", acquireFailQps=").append(acquireFailQps);
		sb.append(", releaseQps=").append(releaseQps);
		sb.append(", releaseFailQps=").append(releaseFailQps);
		sb.append(", renewQps=").append(renewQps);
		sb.append(", renewFailQps=").append(renewFailQps);
		sb.append(", watchQps=").append(watchQps);
		sb.append(", watchFailQps=").append(watchFailQps);
		sb.append(", getQps=").append(getQps);
		sb.append(", getFailQps=").append(getFailQps);
		sb.append(", otherQps=").append(otherQps);
		sb.append(", deleteQps=").append(deleteQps);
		sb.append(", deleteFailQps=").append(deleteFailQps);
		sb.append(", abandonQps=").append(abandonQps);
		sb.append('}');
		return sb.toString();
	}

	public void merge(QpsEntity qpsEntity) {
		if (qpsEntity == null) {
			return;
		}
		this.acquireQps += qpsEntity.acquireQps;
		this.acquireFailQps += qpsEntity.acquireFailQps;
		this.releaseQps += qpsEntity.releaseQps;
		this.releaseFailQps += qpsEntity.releaseFailQps;
		this.renewQps += qpsEntity.renewQps;
		this.renewFailQps += qpsEntity.renewFailQps;
		this.watchQps += qpsEntity.watchQps;
		this.watchFailQps += qpsEntity.watchFailQps;
		this.getQps += qpsEntity.getQps;
		this.getFailQps += qpsEntity.getFailQps;
		this.otherQps += qpsEntity.otherQps;
		this.deleteQps += qpsEntity.deleteQps;
		this.deleteFailQps += qpsEntity.deleteFailQps;
		this.abandonQps += qpsEntity.abandonQps;
	}
}
