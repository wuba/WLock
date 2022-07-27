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

import com.wuba.wlock.client.config.Factor;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OpaqueGenerator {
	
	private final static AtomicLong seq = new AtomicLong(0);
	
	static final Lock seqLock = new ReentrantLock();
	
	public static long getOpaque() {
		long msg_seq = seq.getAndIncrement();
		if (msg_seq > Factor.SEQ_LIMIT){
			seqLock.lock();
			try {
				long f = seq.get();
				if ((f > Factor.SEQ_LIMIT) || (f < 0)){
					seq.set(0);
				}
				if (msg_seq < 0) {
					msg_seq = seq.getAndIncrement();
				}
			} finally {
				seqLock.unlock();
			}
		}
		return msg_seq;
	}
	
}
