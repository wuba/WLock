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
package com.wuba.wlock.registry.server.util;

import com.wuba.wlock.registry.constant.CommonConstant;

import java.util.ArrayList;
import java.util.List;

public class ConversionUtil {
	private static final char TRUE_T = 'T';
	private static final char TRUE_Y = 'Y';

	public static int toInt(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else {
			return Integer.parseInt(String.valueOf(value));
		}
	}

	public static boolean toBoolean(Object value) {
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		} else {
			String s = String.valueOf(value);
			if (s.length() == 0) {
				return false;
			}

			try {
				return Integer.parseInt(s) != 0;
			} catch (NumberFormatException e) {

			}

			if (Character.toUpperCase(s.charAt(0)) == TRUE_T || Character.toUpperCase(s.charAt(0)) == TRUE_Y) {
				return true;
			}
			return false;
		}
	}

	public static String[] toStringArray(Object value) {
		if (value instanceof String[]) {
			return (String[]) value;
		}

		if (value instanceof Iterable<?>) {
			List<String> answer = new ArrayList<String>();
			for (Object v : (Iterable<?>) value) {
				if (v == null) {
					answer.add(null);
				} else {
					answer.add(String.valueOf(v));
				}
			}
			return answer.toArray(new String[answer.size()]);
		}

		return String.valueOf(value).split("[, \\t\\n\\r\\f\\e\\a]");
	}

	private static final String[] INTEGERS = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "10", "11", "12", "13", "14", "15", };

	public static String toString(int value) {
		if (value >= 0 && value < INTEGERS.length) {
			return INTEGERS[value];
		} else {
			return Integer.toString(value);
		}
	}

	private ConversionUtil() {

	}

	/**
	 * byte array to short (little endian)
	 * 
	 * @param buf
	 * @return
	 */
	public static short bytesToShortLittleEndian(byte[] buf) {
		return (short) (((buf[0] << 8) & 0xff00) | (buf[1] & 0xff));
	}

	/**
	 * byte array to int (little endian)
	 * 
	 * @param buf
	 * @return
	 */
	public static int bytesToIntLittleEndian(byte[] buf) {
		return ((buf[0] << 24) & 0xff000000) | ((buf[1] << 16) & 0xff0000)
				| ((buf[2] << 8) & 0xff00) | (buf[3] & 0xff);
	}

	/**
	 * byte array to int (little endian)
	 * 
	 * @param buf
	 * @return
	 */
	public static long bytesToLongLittleEndian(byte[] buf) {
		return (((long) buf[0] << 56) & 0xff00000000000000L)
				| (((long) buf[1] << 48) & 0xff000000000000L)
				| (((long) buf[2] << 40) & 0xff0000000000L)
				| (((long) buf[3] << 32) & 0xff00000000L)
				| (((long) buf[4] << 24) & 0xff000000L)
				| (((long) buf[5] << 16) & 0xff0000L)
				| (((long) buf[6] << 8) & 0xff00L) | ((long) buf[7] & 0xffL);
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static short bytesToShortBigEndian(byte[] buf) {
		return (short) (buf[0] & 0xff | ((buf[1] << 8) & 0xff00));
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static int bytesToIntBigEndian(byte[] buf) {
		return buf[0] & 0xff | ((buf[1] << 8) & 0xff00)
				| ((buf[2] << 16) & 0xff0000) | ((buf[3] << 24) & 0xff000000);
	}

	/**
	 * byte array to int (big endian)
	 * 
	 * @param buf
	 * @return
	 */
	public static long bytesToLongBigEndian(byte[] buf) {
		return (long) buf[0] & 0xffL | (((long) buf[1] << 8) & 0xff00L)
				| (((long) buf[2] << 16) & 0xff0000L)
				| (((long) buf[3] << 24) & 0xff000000L)
				| (((long) buf[4] << 32) & 0xff00000000L)
				| (((long) buf[5] << 40) & 0xff0000000000L)
				| (((long) buf[6] << 48) & 0xff000000000000L)
				| (((long) buf[7] << 56) & 0xff00000000000000L);
	}

	public static byte[] shortToBytesLittleEndian(short n) {
		byte[] buf = new byte[2];
		for (int i = 0; i < buf.length; i++) {
			buf[buf.length - i - 1] = (byte) (n >> (8 * i));
		}
		return buf;
	}

	/**
	 * int to byte array (little endian)
	 * 
	 * @param n
	 * @return
	 */
	public static byte[] intToBytesLittleEndian(int n) {
		byte[] buf = new byte[4];
		for (int i = 0; i < buf.length; i++) {
			buf[buf.length - i - 1] = (byte) (n >> (8 * i));
		}
		return buf;
	}

	public static byte[] longToBytesLittleEndian(long n) {
		byte[] buf = new byte[8];
		for (int i = 0; i < buf.length; i++) {
			buf[buf.length - i - 1] = (byte) (n >> (8 * i));
		}
		return buf;
	}

	public static byte[] shortToBytesBigEndian(short n) {
		byte[] buf = new byte[2];
		for (int i = 0; i < buf.length; i++) {
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
	public static byte[] intToBytesBigEndian(int n) {
		byte[] buf = new byte[4];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = (byte) (n >> (8 * i));
		}
		return buf;
	}

	public static byte[] longToBytesBigEndian(long n) {
		byte[] buf = new byte[8];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = (byte) (n >> (8 * i));
		}
		return buf;
	}

	public static short bytesToShortLittleEndian(byte[] buf, int offset) {
		return (short) (((buf[offset] << 8) & 0xff00) | (buf[offset + 1] & 0xff));
	}

	public static short bytesToShortBigEndian(byte[] buf, int offset) {
		return (short) (buf[offset] & 0xff | ((buf[offset + 1] << 8) & 0xff00));
	}

	public static int bytesToIntLittleEndian(byte[] buf, int offset) {
		return ((buf[offset] << 24) & 0xff000000)
				| ((buf[offset + 1] << 16) & 0xff0000)
				| ((buf[offset + 2] << 8) & 0xff00) | (buf[offset + 3] & 0xff);
	}

	public static int bytesToIntBigEndian(byte[] buf, int offset) {
		return buf[offset] & 0xff | ((buf[offset + 1] << 8) & 0xff00)
				| ((buf[offset + 2] << 16) & 0xff0000)
				| ((buf[offset + 3] << 24) & 0xff000000);
	}

	public static long bytesToLongLittleEndian(byte[] buf, int offset) {
		return (((long) buf[offset] << 56) & 0xff00000000000000L)
				| (((long) buf[offset + 1] << 48) & 0xff000000000000L)
				| (((long) buf[offset + 2] << 40) & 0xff0000000000L)
				| (((long) buf[offset + 3] << 32) & 0xff00000000L)
				| ((buf[offset + 4] << 24) & 0xff000000L)
				| ((buf[offset + 5] << 16) & 0xff0000L)
				| ((buf[offset + 6] << 8) & 0xff00L)
				| (buf[offset + 7] & 0xffL);
	}

	public static long bytesToLongBigEndian(byte[] buf, int offset) {
		return (long) buf[offset] & 0xffL
				| (((long) buf[offset + 1] << 8) & 0xff00L)
				| (((long) buf[offset + 2] << 16) & 0xff0000L)
				| (((long) buf[offset + 3] << 24) & 0xff000000L)
				| (((long) buf[offset + 4] << 32) & 0xff00000000L)
				| (((long) buf[offset + 5] << 40) & 0xff0000000000L)
				| (((long) buf[offset + 6] << 48) & 0xff000000000000L)
				| (((long) buf[offset + 7] << 56) & 0xff00000000000000L);
	}

	/**
	 * IP转换为INT
	 * 
	 * @param ip
	 * @return
	 * @throws Exception
	 */
	public static int ipToInt(String ip) throws Exception {
		String[] ipAry = ip.split("\\.");
		if (ipAry.length != CommonConstant.FOUR) {
			throw new Exception("ipToInt error ip:" + ip);
		}
		byte[] ipBuf = new byte[4];
		for (int i = 0; i < CommonConstant.FOUR; i++) {
			int item = Integer.parseInt(ipAry[i]);
			if (item > 127) {
				item -= 256;
			}
			ipBuf[i] = (byte) item;
		}

		int s = 0;
		int s0 = ipBuf[0] & 0xff;// 最低位
		int s1 = ipBuf[1] & 0xff;
		int s2 = ipBuf[2] & 0xff;
		int s3 = ipBuf[3] & 0xff;
		s3 <<= 24;
		s2 <<= 16;
		s1 <<= 8;
		s = s0 | s1 | s2 | s3;
		return s;
	}

	/**
	 * IP转换为byte[]
	 * 
	 * @param ip
	 * @return
	 * @throws Exception
	 */
	public static byte[] ipToTyte(String ip) throws Exception {
		String[] ipAry = ip.split("\\.");
		if (ipAry.length != CommonConstant.FOUR) {
			throw new Exception("ip2int error ip:" + ip);
		}
		byte[] ipBuf = new byte[4];
		for (int i = 0; i < CommonConstant.FOUR; i++) {
			int item = Integer.parseInt(ipAry[i]);
			if (item > 127) {
				item -= 256;
			}
			ipBuf[i] = (byte) item;
		}
		return ipBuf;
	}

	/**
	 * int转ip
	 * 
	 * @param ipBuf
	 * @return
	 */
	public static String byteToIp(byte[] ipBuf) {
		int[] ipBufInt = new int[4];
		for (int i = 0; i < CommonConstant.FOUR; i++) {
			if (ipBuf[i] < 0) {
				ipBufInt[i] = ipBuf[i] + 256;
			} else {
				ipBufInt[i] = ipBuf[i];
			}
		}
		StringBuilder sbip = new StringBuilder();
		sbip.append(ipBufInt[0]);
		sbip.append(".");
		sbip.append(ipBufInt[1]);
		sbip.append(".");
		sbip.append(ipBufInt[2]);
		sbip.append(".");
		sbip.append(ipBufInt[3]);

		return sbip.toString();
	}

	/**
	 * 注释：字节数组到int的转换！
	 * 
	 * @param b
	 * @return
	 */
	public static int byteToInt(byte[] b) {
		int s = 0;
		int s0 = b[0] & 0xff;// 最低位
		int s1 = b[1] & 0xff;
		int s2 = b[2] & 0xff;
		int s3 = b[3] & 0xff;
		s3 <<= 24;
		s2 <<= 16;
		s1 <<= 8;
		s = s0 | s1 | s2 | s3;
		return s;
	}

	/**
	 * 注释：int到字节数组的转换！
	 * 
	 * @param number
	 * @return
	 */
	public static byte[] intToByte(int number) {
		int temp = number;
		byte[] b = new byte[4];
		for (int i = 0; i < b.length; i++) {
			b[i] = new Integer(temp & 0xff).byteValue();// 将最低位保存在最低位
			temp = temp >> 8;// 向右移8位
		}
		return b;
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	public static short shortFromBigEndian(byte[] array, int index) {
		return (short) (((array[index + 0] & 0xff) << 8) | (array[index + 1] & 0xff));
	}

	public static int intFromBigEndian(byte[] array, int index) {
		int n = (((array[index + 0] & 0xff) << 24)
				| ((array[index + 1] & 0xff) << 16)
				| ((array[index + 2] & 0xff) << 8) | (array[index + 3] & 0xff));

		return n;
	}

	public static long longFromBigEndian(byte[] array, int index) {
		int hi = intFromBigEndian(array, index);
		int lo = intFromBigEndian(array, index + 4);
		return (((long) hi & 0xFFFFFFFFL) << 32) | (((long) lo << 32) >>> 32);
	}

	public static void shortToBigEndian(short value, byte[] array, int index) {
		array[index + 0] = (byte) (value >>> 8);
		array[index + 1] = (byte) (value);
	}

	public static void intToBigEndian(int value, byte[] array, int index) {
		array[index + 0] = (byte) (value >>> 24);
		array[index + 1] = (byte) (value >>> 16);
		array[index + 2] = (byte) (value >>> 8);
		array[index + 3] = (byte) (value);
	}

	public static void longToBigEndian(long value, byte[] array, int index) {
		intToBigEndian((int) (value >>> 32), array, index);
		intToBigEndian((int) (value & 0xFFFFFFFFL), array, index + 4);
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

}
