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

import redis.clients.jedis.Jedis;

public class RedisPress implements Press{
    Jedis jedis = new Jedis("127.0.0.1", 6379);

    @Override
    public boolean run(String lockName) throws Exception{
        try {
            jedis.setnx(lockName, String.valueOf(Thread.currentThread()));
            jedis.del(lockName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
