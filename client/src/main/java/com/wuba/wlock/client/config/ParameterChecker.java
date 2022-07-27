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

public class ParameterChecker {
	
	/**
	 * lock weight 合法性检测
	 * @param weight
	 * @return
	 */
	public static boolean lockweightCheck(int weight) {
		if (weight > Factor.ACQUIRE_LOCK_MAX_WEIGHT || weight < Factor.ACQUIRE_LOCK_MIN_WEIGHT) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * lock 过期时间合法性检测
	 * @param expireTime
	 * @return
	 */
	public static boolean lockExpireTimeCheck(int expireTime) {
		if (expireTime < Factor.LOCK_MIN_EXPIRETIME || expireTime > Factor.LOCK_MAX_EXPIRETIME) {
			return false;
		}
		
		return true;
	}
	
	public static boolean lockRenewIntervalCheck(int renewInterval, int expireTime) {
		if (renewInterval == Factor.LOCK_NOT_RENEWINTERVAL) {
			return true;
		}
		if (renewInterval < Factor.LOCK_MIN_RENEWINTERVAL || renewInterval > expireTime) {
			return false;
		}
		
		return true;
	}
}
