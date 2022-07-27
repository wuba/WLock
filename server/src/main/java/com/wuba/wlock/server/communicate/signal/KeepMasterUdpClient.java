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
package com.wuba.wlock.server.communicate.signal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class KeepMasterUdpClient {
	private String encode;
	private DatagramSocket sock = null;
	private InetSocketAddress addr = null;

	public static KeepMasterUdpClient getInstance(String ip, int port, String encode) throws SocketException {
		KeepMasterUdpClient client = new KeepMasterUdpClient();
		client.encode = encode;
		client.sock = new DatagramSocket();
		client.addr = new InetSocketAddress(ip, port);
		return client;
	}

	private KeepMasterUdpClient() {
	}
	
	public void send(String msg) throws IOException {
		byte[] buf = msg.getBytes(encode);
		send(buf);
	}

	public void send(byte[] buf) throws IOException {
		DatagramPacket dp = new DatagramPacket(buf, buf.length, addr);
		sock.send(dp);
	}

}
