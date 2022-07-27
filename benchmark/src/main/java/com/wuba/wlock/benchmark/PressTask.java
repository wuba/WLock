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

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PressTask {
    String name;
    int threadNum;

    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
    StatisticsCollector statisticsCollector;

    volatile AtomicLong[] timearray = timeArray();
    volatile AtomicLong[] timearray2 = timeArray();
    volatile boolean timeOpen = true;
    AtomicLong count = new AtomicLong();
    AtomicLong error = new AtomicLong();
    volatile boolean stop = false;
    RateLimiter limiter = null;
    boolean isLimit = false;

    CountDownLatch countDownLatch = new CountDownLatch(1);

    public PressTask(String name, int threadNum, int sampleCount, String limit) {
        this.name = name;
        this.threadNum = threadNum;
        statisticsCollector = new StatisticsCollector(name, threadNum, sampleCount);
        if (limit != null) {
            isLimit = true;
            limiter = RateLimiter.create(Integer.parseInt(limit));
        }
    }

    public void execute() throws Exception {
        scheduledExecutorService.scheduleAtFixedRate(new Statistics(), 30, 60, TimeUnit.SECONDS);

        final AtomicLong idx = new AtomicLong(0);
        final String lockKeyBase = String.valueOf(System.currentTimeMillis());

        for (int i = 0; i < threadNum; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Press press = press(name);
                        while (!stop) {
                            if (isLimit) {
                                limiter.acquire();
                            }

                            long startTime = System.nanoTime();
                            boolean run = press.run(lockKeyBase + idx.incrementAndGet());
                            long endTime = System.nanoTime();
                            count.incrementAndGet();
                            if (!run) {
                                error.incrementAndGet();
                            }
                            int cost = (int) ((endTime - startTime) / 1000);
                            if (cost < timearray.length) {
                                if (timeOpen) {
                                    timearray[cost].incrementAndGet();
                                } else {
                                    timearray2[cost].incrementAndGet();
                                }
                            } else {
                                System.out.println("cost: " + cost);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

        countDownLatch.await();
    }

    private static AtomicLong[] timeArray() {
        AtomicLong[] timearray = new AtomicLong[10000000];
        for (int i = 0; i < timearray.length; i++) {
            timearray[i] = new AtomicLong();
        }
        return timearray;
    }

    private class Statistics implements Runnable {

        @Override
        public void run() {

            try {
                boolean upload = true;
                long allCount = count.getAndSet(0);
                long errorCount = error.getAndSet(0);
                if (allCount != 0) {
                    timeOpen = !timeOpen;
                    AtomicLong[] timeArray = timearray;
                    if (timeOpen) {
                        timeArray = timearray2;
                    }

                    long avgsum = 0;
                    long medianLimt = allCount / 2;
                    long tp99Limt = allCount / 100;
                    long min = Long.MAX_VALUE;
                    long max = Long.MIN_VALUE;

                    long tp99 = -1;
                    long median = -1;

                    long costCount = 0;

                    for (int cost = timeArray.length - 1; cost >= 0; cost--) {
                        long sum = timeArray[cost].getAndSet(0);

                        costCount += sum;

                        avgsum+= cost * sum;
                        if (sum > 0) {
                            if (cost < min) {
                                min = cost;
                            }

                            if (cost > max) {
                                max = cost;
                            }
                        }

                        if (tp99 < 0 && costCount >= tp99Limt) {
                            tp99 = cost;
                        }

                        if (median < 0 && costCount >= medianLimt) {
                            median = cost;
                        }
                    }

                    long avg = avgsum / allCount;
                    Data data = new Data(allCount / 60, avg, tp99, median, min, max, errorCount);
                    System.out.println(data.toString());
                    upload = statisticsCollector.upload(data);
                } else {
                    Data data = new Data(0, 0, 0, 0, 0, 0, errorCount);
                    System.out.println(data.toString());
                    upload = statisticsCollector.upload(data);
                }

                if (!upload) {
                    System.out.println("shutdownNow");
                    stop = true;
                    scheduledExecutorService.shutdownNow();
                    countDownLatch.countDown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Press press(String name) {
        if (name.equals("RedisPress")) {
            return new RedisPress();
        }

        if (name.equals("RedissonPress")) {
            return new RedissonPress();
        }

        if (name.equals("WlockMultiPress")) {
            return new WlockMultiPress();
        }

        if (name.equals("ZookeeperPress")) {
            return new ZookeeperPress();
        }

        if (name.equals("RedLockPress")) {
            return new RedLockPress();
        }

        if (name.equals("EtcdPress")) {
            return new EtcdPress();
        }

        throw new RuntimeException("压测类不存在");
    }
}
