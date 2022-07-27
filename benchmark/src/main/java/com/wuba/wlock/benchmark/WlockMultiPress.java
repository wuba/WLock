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
package com.wuba.wlock.benchmark;


import com.wuba.wlock.client.WDistributedLock;
import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.client.lockresult.AcquireLockResult;

public class WlockMultiPress implements Press{
    static WLockClient wlockClient = null;

    static {
        try {
            wlockClient =  new WLockClient("test123_9_all", "127.0.0.1", 22020);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean run(String lockKey) throws Exception{
        WDistributedLock lock = wlockClient.newDistributeLock(lockKey);
        AcquireLockResult result = lock.tryAcquireLock(5000, 100000);
        lock.releaseLock();
        return result.isSuccess();
    }
}
