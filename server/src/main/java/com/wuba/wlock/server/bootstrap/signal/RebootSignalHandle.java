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
package com.wuba.wlock.server.bootstrap.signal;

import com.wuba.wlock.server.communicate.TcpServer;
import com.wuba.wlock.server.communicate.protocol.ProtocolFactoryImpl;
import com.wuba.wlock.server.communicate.protocol.RebootRequest;
import com.wuba.wlock.server.communicate.registry.RegistryClient;
import com.wuba.wlock.server.constant.ServerState;
import com.wuba.wlock.server.exception.ProtocolException;
import com.wuba.wlock.server.worker.KeepMasterWorker;
import com.wuba.wlock.server.wpaxos.WpaxosService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.Iterator;

public class RebootSignalHandle implements SignalHandler {

	private static final Logger logger = LogManager.getLogger(RebootSignalHandle.class);

	public static RebootRequest comand = ProtocolFactoryImpl.getInstance().createRebootReq();

	@Override
	public void handle(Signal arg0) {
		logger.error("server state is set to be rebooting ......");
		ServerState.setRebooting(true);
		Iterator<Channel> iter = TcpServer.ALL_CHANNELS.iterator();
		while (iter.hasNext()) {
			Channel nettyChannel = iter.next();
			if (nettyChannel != null && nettyChannel.isOpen()) {
				try {
					nettyChannel.write(ChannelBuffers.copiedBuffer(comand.toBytes()));
				} catch (ProtocolException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		RegistryClient.getInstance().stop();
		WpaxosService.getInstance().dropAllMaster();
		KeepMasterWorker.getInstance().shutdown();

		try {
			Thread.sleep(1000 * 10);
		} catch (InterruptedException e1) {
			logger.error(e1.getMessage(), e1);
		}

		System.exit(0); // 退出
	}
}
