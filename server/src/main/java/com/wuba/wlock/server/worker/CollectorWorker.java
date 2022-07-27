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
package com.wuba.wlock.server.worker;

import com.wuba.wlock.common.collector.protocol.GroupQps;
import com.wuba.wlock.common.collector.protocol.KeyQps;
import com.wuba.wlock.common.collector.protocol.ServerQps;
import com.wuba.wlock.server.collector.QpsAbandon;
import com.wuba.wlock.server.collector.entity.QpsVO;
import com.wuba.wlock.server.collector.log.GroupLog;
import com.wuba.wlock.server.collector.log.KeyGroupLog;
import com.wuba.wlock.server.collector.log.KeyLog;
import com.wuba.wlock.server.collector.log.ServerLog;
import com.wuba.wpaxos.utils.ThreadFactoryImpl;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class CollectorWorker {
	private static final Logger logger = LoggerFactory.getLogger(CollectorWorker.class);
	private static ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(2, new ThreadFactoryImpl("collector_worker"));
	private static CollectorWorker collectorWorker = new CollectorWorker();

	public static CollectorWorker getInstance() {
		return collectorWorker;
	}

	private CollectorWorker() {
	}

	public void start() {
		/*
		 *  收集器借助该上报作为心跳,所以没有初始化延时
		 */
		scheduledExecutorService.scheduleAtFixedRate(this::uploadQps, 0, 1, TimeUnit.MINUTES);
		QpsAbandon.init();
		logger.info("CollectorWorker start end");
	}


	private void uploadQps() {
		try {
			String nowMinue = DateFormatUtils.format(new Date(), "yyyyMMddHHmm");
			ServerQps serverQps = QpsVO.getInstance().calculateServerQps();
			KeyQps keyQps = QpsVO.getInstance().calculateKeyQps();
			GroupQps groupQps = QpsVO.getInstance().calcutateGroupQps();

			ServerLog.write(nowMinue, serverQps);
			GroupLog.write(nowMinue, groupQps);
			KeyLog.write(nowMinue, keyQps);
			KeyGroupLog.write(nowMinue, keyQps);
		} catch (Throwable e) {
			logger.error("CollectorWorker.uploadQps error", e);
		}
	}

	public void shutdown() {
		scheduledExecutorService.shutdown();
		try {
			if (!scheduledExecutorService.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
				scheduledExecutorService.shutdownNow();
			}
		} catch (InterruptedException e) {
		} finally {
			scheduledExecutorService.shutdownNow();
		}
	}

}
