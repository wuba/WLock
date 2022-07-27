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
package com.wuba.wlock.client.communication;

import com.wuba.wlock.client.config.Version;

public enum WatchPolicy {
	/**
	 * watch 一次
	 */
	Once(0),
	/**
	 * 持续 watch
	 */
	Continue(1);

	private int watchPolicy;

	WatchPolicy(int watchPolicy) {
		this.watchPolicy = watchPolicy;
	}

	public int getWatchPolicy() {
		return watchPolicy;
	}

	public static WatchPolicy parse(short value) {
		for (WatchPolicy tmp : values()) {
			if (value == tmp.watchPolicy) {
				return tmp;
			}
		}
		throw new IllegalArgumentException(Version.INFO + ", Illegal Argument [" + value + "] for WatchPolicy");
	}
}
