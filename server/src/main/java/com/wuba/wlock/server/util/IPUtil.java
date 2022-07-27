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
package com.wuba.wlock.server.util;

public final class IPUtil {
	private IPUtil() {
	}

	public static String getIpStr(int ipInt) {
		String ip = byteToIp(intToByte(ipInt));
		return ip;
	}


	public static byte[] intToByte(int number){
		int temp = number;
		byte[] b =new byte[4];
		for(int i =0; i < b.length; i++){
			b[i]=new Integer(temp &0xff).byteValue();// 将最低位保存在最低位
			temp = temp >>8;// 向右移8位
		}
		return b;
	}
	public static String byteToIp(byte[] ipBuf) {
		int[] ipBufInt = new int[4];
		for(int i=0; i<4; i++) {
			if(ipBuf[i] < 0) {
				ipBufInt[i] = ipBuf[i] + 256;
			} else {
				ipBufInt[i] = ipBuf[i];
			}
		}
		StringBuilder sbIP = new StringBuilder();
		sbIP.append(ipBufInt[0]);
		sbIP.append(".");
		sbIP.append(ipBufInt[1]);
		sbIP.append(".");
		sbIP.append(ipBufInt[2]);
		sbIP.append(".");
		sbIP.append(ipBufInt[3]);

		return sbIP.toString();
	}
}
