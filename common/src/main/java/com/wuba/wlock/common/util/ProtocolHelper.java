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
package com.wuba.wlock.common.util;

import com.wuba.wlock.common.registry.protocol.ProtocolConstant;

public class ProtocolHelper {

	public static ProtocolHelper instance = new ProtocolHelper();

	private ProtocolHelper() {
	}

	public static ProtocolHelper getInstance() {
		return instance;
	}

	public boolean checkEndDelimiter(byte[] buf) {
		if (buf.length == ProtocolConstant.P_END_TAG.length) {
			for (int i = 0; i < buf.length; i++) {
				if (buf[i] != ProtocolConstant.P_END_TAG[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean checkHeadDelimiter(byte[] buf) {
		if (buf.length > ProtocolConstant.P_START_TAG.length) {
			for (int i = 0; i < 5; i++) {
				if (buf[i] != ProtocolConstant.P_START_TAG[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

}
