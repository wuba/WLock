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
package com.wuba.wlock.client.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UniqueCodeGenerator {
	
	private static final AtomicLong codeID = new AtomicLong();
	private static long INFOID_FLAG = 1260000000000L;
	private static final Lock infoLock = new ReentrantLock();
	
	public static long getUniqueCode(){
		infoLock.lock();
		try {
			long infoid = System.nanoTime() - INFOID_FLAG;
			infoid = (infoid<<7) | codeID.getAndIncrement();
			return Math.abs(infoid);
		} finally {
			infoLock.unlock();
		}
	}
	
}
