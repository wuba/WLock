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
package com.wuba.wlock.server.collector;

import com.wuba.wlock.server.collector.entity.QpsVO;


public final class QpsCounter {

	private QpsCounter() {
	}

	private static QpsVO qpsEntity = QpsVO.getInstance();

	public static void incrServerOtherQps() {
		qpsEntity.incrServerOtherQps();
	}

	public static void incrServerAcquireQps() {
		qpsEntity.incrServerAcquireQps();
	}

	public static void incrServerRenewQps() {
		qpsEntity.incrServerRenewQps();
	}

	public static void incrServerReleaseQps() {
		qpsEntity.incrServerReleaseQps();
	}

	public static void incrServerWatchQps() {
		qpsEntity.incrServerWatchQps();
	}

	public static void incrServerGetQps() {
		qpsEntity.incrServerGetQps();
	}

	public static void incrServerAcquireFailQps() {
		qpsEntity.incrServerAcquireFailQps();
	}

	public static void incrServerRenewFailQps() {
		qpsEntity.incrServerRenewFailQps();
	}

	public static void incrServerReleaseFailQps() {
		qpsEntity.incrServerReleaseFailQps();
	}

	public static void incrServerWatchFailQps() {
		qpsEntity.incrServerWatchFailQps();
	}

	public static void incrServerGetFailQps() {
		qpsEntity.incrServerGetFailQps();
	}

	public static void incrServerDeleteQps() {
		qpsEntity.incrServerDeleteQps();
	}

	public static void incrServerDeleteFailQps() {
		qpsEntity.incrServerDeleteFailQps();
	}

	public static void incrServerAbandonQps() {
		qpsEntity.incrServerAbandonQps();
	}

	public static void incrGroupOtherQps(int group) {
		qpsEntity.incrGroupOtherQps(group);
	}

	public static void incrGroupAcquireQps(int group) {
		qpsEntity.incrGroupAcquireQps(group);
	}

	public static void incrGroupRenewQps(int group) {
		qpsEntity.incrGroupRenewQps(group);
	}

	public static void incrGroupReleaseQps(int group) {
		qpsEntity.incrGroupReleaseQps(group);
	}

	public static void incrGroupWatchQps(int group) {
		qpsEntity.incrGroupWatchQps(group);
	}

	public static void incrGroupGetQps(int group) {
		qpsEntity.incrGroupGetQps(group);
	}

	public static void incrGroupAcquireFailQps(int group) {
		qpsEntity.incrGroupAcquireFailQps(group);
	}

	public static void incrGroupRenewFailQps(int group) {
		qpsEntity.incrGroupRenewFailQps(group);
	}

	public static void incrGroupReleaseFailQps(int group) {
		qpsEntity.incrGroupReleaseFailQps(group);
	}

	public static void incrGroupWatchFailQps(int group) {
		qpsEntity.incrGroupWatchFailQps(group);
	}

	public static void incrGroupGetFailQps(int group) {
		qpsEntity.incrGroupGetFailQps(group);
	}

	public static void incrGroupDeleteQps(int group) {
		qpsEntity.incrGroupDeleteQps(group);
	}

	public static void incrGroupDeleteFailQps(int group) {
		qpsEntity.incrGroupDeleteFailQps(group);
	}

	public static void incrGroupAbandonQps(int group) {
		qpsEntity.incrGroupAbandonQps(group);
	}

	public static void incrKeyOtherQps(String key, int groupId) {
		qpsEntity.incrKeyOtherQps(key, groupId);
	}

	public static void incrKeyAcquireQps(String key, int groupId) {
		qpsEntity.incrKeyAcquireQps(key, groupId);
	}

	public static void incrKeyRenewQps(String key, int groupId) {
		qpsEntity.incrKeyRenewQps(key, groupId);
	}

	public static void incrKeyReleaseQps(String key, int groupId) {
		qpsEntity.incrKeyReleaseQps(key,groupId);
	}

	public static void incrKeyWatchQps(String key, int groupId) {
		qpsEntity.incrKeyWatchQps(key,groupId);
	}

	public static void incrKeyGetQps(String key, int groupId) {
		qpsEntity.incrKeyGetQps(key, groupId);
	}

	public static void incrKeyAcquireFailQps(String key, int groupId) {
		qpsEntity.incrKeyAcquireFailQps(key, groupId);
	}

	public static void incrKeyRenewFailQps(String key, int groupId) {
		qpsEntity.incrKeyRenewFailQps(key, groupId);
	}

	public static void incrKeyReleaseFailQps(String key, int groupId) {
		qpsEntity.incrKeyReleaseFailQps(key, groupId);
	}

	public static void incrKeyWatchFailQps(String key, int groupId) {
		qpsEntity.incrKeyWatchFailQps(key, groupId);
	}

	public static void incrKeyGetFailQps(String key, int groupId) {
		qpsEntity.incrKeyGetFailQps(key, groupId);
	}

	public static void incrKeyDeleteQps(String key, int groupId) {
		qpsEntity.incrKeyDeleteQps(key, groupId);
	}

	public static void incrKeyDeleteFailQps(String key, int groupId) {
		qpsEntity.incrKeyDeleteFailQps(key, groupId);
	}

	public static void incrKeyAnandonQps(String key, int groupId) {
		qpsEntity.incrKeyAbandonQps(key, groupId);
	}
}
