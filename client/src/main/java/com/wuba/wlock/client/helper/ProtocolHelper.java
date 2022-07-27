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
package com.wuba.wlock.client.helper;

import com.wuba.wlock.client.registryclient.protocal.ProtocolConstant;

public class ProtocolHelper {

	/**
	 * 获得协义的版本号
	 * 
	 * @param buffer
	 * @return
	 */
	public static int getVersion(byte[] buffer) {
		return buffer[0];
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static boolean checkHeadDelimiter(byte[] buf) {
		if (buf.length == ProtocolConstant.P_START_TAG.length) {
			for (int i = 0; i < buf.length; i++) {
				if (buf[i] != ProtocolConstant.P_START_TAG[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static boolean checkEndDelimiter(byte[] buf) {
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

}
