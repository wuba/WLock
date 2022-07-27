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
package com.wuba.wlock.client.config;

public class Factor {
	
	public final static int SEQ_LIMIT = (1 << 31) - 2048;
	
	public final static int DEFAULT_ACQUIRE_LOCK_WEIGHT = 1;
	public final static int ACQUIRE_LOCK_MAX_WEIGHT = 10;	
	public final static int ACQUIRE_LOCK_MIN_WEIGHT = 1;
	
	public final static int LOCK_MAX_EXPIRETIME = 5*60*1000;
	public final static int LOCK_MIN_EXPIRETIME = 5000;
	
	public final static int LOCK_NOT_RENEWINTERVAL = Integer.MAX_VALUE; // <=0 means not auto renew
	public final static int LOCK_MIN_RENEWINTERVAL = 1000;
	
	public final static long WATCH_MAX_WAIT_TIME_MARK = Long.MAX_VALUE;
	/**
	 * 长期持有锁场景下,锁续约间隔 holdLock
	 */
	public final static int HOLD_LOCK_RENEWINTERVAL = 60*1000;
}
