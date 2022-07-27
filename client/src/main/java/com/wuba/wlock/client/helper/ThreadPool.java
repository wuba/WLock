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

import com.wuba.wlock.client.util.ThreadPoolUtil;
import com.wuba.wlock.client.util.ThreadRenameFactory;

import java.util.concurrent.ScheduledExecutorService;

public class ThreadPool {

	public static final ScheduledExecutorService registryScheduler = ThreadPoolUtil.newScheduledThreadPool(1, new ThreadRenameFactory("registryScheduler"));
	
}
