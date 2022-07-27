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

import java.util.Date;

public class BatchPress {

    public static void main(String[] args) throws Exception {
//        args = new String[]{"1", "5", "RedLockPress"};

        int sampleCount = Integer.parseInt(args[0]);
        String[] threadNums = args[1].split(",");
        String[] names = args[2].split(",");
        String[] limits = null;
        if (args.length > 3) {
            limits = args[3].split(",");
        }
        if (limits != null) {
            for (String limit: limits) {
                runTask(sampleCount, threadNums, names, limit);
            }
        } else {
            runTask(sampleCount, threadNums, names, null);
        }
    }

    private static void runTask(int sampleCount, String[] threadNums, String[] names, String limit) throws Exception{
        for (String threadNum: threadNums) {
            for (String name: names) {
                System.out.println(String.format("PressTask execute date: %s name: %s threadNum: %s sampleCount: %d limit: %s", new Date().toString(),name, threadNum, sampleCount, limit));
                PressTask pressTask = new PressTask(name, Integer.parseInt(threadNum), sampleCount, limit);
                pressTask.execute();
                Thread.sleep(30000);
            }
        }
    }
}
