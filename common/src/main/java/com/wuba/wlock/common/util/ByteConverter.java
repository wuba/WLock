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

public class ByteConverter {

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static short bytesToShortLittleEndian(byte[] buf) {
		return (short) (buf[0] & 0xff | ((buf[1] << 8) & 0xff00));
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static int bytesToIntLittleEndian(byte[] buf) {
		return buf[0] & 0xff | ((buf[1] << 8) & 0xff00)
				| ((buf[2] << 16) & 0xff0000) | ((buf[3] << 24) & 0xff000000);
	}

	/**
	 * byte array to int (big endian)
	 * 
	 * @param buf
	 * @return
	 */
	public static long bytesToLongLittleEndian(byte[] buf) {
		return (long)buf[0] & 0xffL
				| (((long)buf[1] << 8)  & 0xff00L)
				| (((long)buf[2] << 16) & 0xff0000L)
				| (((long)buf[3] << 24) & 0xff000000L)
				| (((long)buf[4] << 32) & 0xff00000000L)
				| (((long)buf[5] << 40) & 0xff0000000000L)
				| (((long)buf[6] << 48) & 0xff000000000000L)
				| (((long)buf[7] << 56) & 0xff00000000000000L);
	}

	public static byte[] shortToBytesLittleEndian(short n) {
		byte[] buf = new byte[2];
		for(int i=0; i<buf.length; i++) {
			buf[i] = (byte) (n >> (8 * i));
		}
		return buf;
	}

	/**
	 * int to byte array (big endian)
	 * 
	 * @param n
	 * @return
	 */
	public static byte[] intToBytesLittleEndian(int n) {
		byte[] buf = new byte[4];
		for(int i=0; i<buf.length; i++) {
			buf[i] = (byte) (n >> (8 * i));
		}
		return buf;
	}

	public static byte[] longToBytesLittleEndian(long n) {
		byte[] buf = new byte[8];
		for(int i=0; i<buf.length; i++) {
			buf[i] = (byte) (n >> (8 * i));
		}
		return buf;
	}
	
	public static short bytesToShortLittleEndian(byte[] buf, int offset) {
		return (short) (buf[offset] & 0xff | ((buf[offset + 1] << 8) & 0xff00));
	}


	public static int bytesToIntLittleEndian(byte[] buf, int offset) {
		return buf[offset] & 0xff 
				| ((buf[offset + 1] << 8)  & 0xff00)
				| ((buf[offset + 2] << 16) & 0xff0000)
				| ((buf[offset + 3] << 24) & 0xff000000);
	}

	public static long bytesToLongLittleEndian(byte[] buf, int offset) {
		return (long)buf[offset] & 0xffL
				| (((long)buf[offset + 1] << 8) & 0xff00L)
				| (((long)buf[offset + 2] << 16) & 0xff0000L)
				| (((long)buf[offset + 3] << 24) & 0xff000000L)
				| (((long)buf[offset + 4] << 32) & 0xff00000000L)
				| (((long)buf[offset + 5] << 40) & 0xff0000000000L)
				| (((long)buf[offset + 6] << 48) & 0xff000000000000L)
				| (((long)buf[offset + 7] << 56) & 0xff00000000000000L);
	}
}