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

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedLockPress implements Press{
    static RedissonClient redissonClient;
    static RedissonClient redissonClient2;
    static RedissonClient redissonClient3;

    static {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        redissonClient = Redisson.create(config);

        Config config2 = new Config();
        config2.useSingleServer().setAddress("redis://127.0.0.1:6379");
        redissonClient2 = Redisson.create(config2);

        Config config3 = new Config();
        config3.useSingleServer().setAddress("redis://127.0.0.1:6379");
        redissonClient3 = Redisson.create(config3);
    }

    private RedissonRedLock redLock(String lockKey) {
        return new RedissonRedLock(redissonClient.getLock(lockKey), redissonClient2.getLock(lockKey), redissonClient3.getLock(lockKey));
    }

    @Override
    public boolean run(String lockKey) throws Exception{
        try {
            RLock lock = redLock(lockKey);
            lock.lock();
            lock.unlock();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
