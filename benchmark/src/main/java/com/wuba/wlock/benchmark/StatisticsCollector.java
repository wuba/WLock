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

import java.io.RandomAccessFile;

public class StatisticsCollector {

    private static final int abandonCount = 2;

    String name;
    int threadNum;
    volatile int sampleCount;
    Data[] datas;

    public StatisticsCollector(String name, int threadNum, int sampleCount) {
        this.name = name;
        this.threadNum = threadNum;
        this.sampleCount = sampleCount + abandonCount;
        datas = new Data[this.sampleCount];
    }

    public boolean upload(Data data) {
        sampleCount--;
        datas[sampleCount] = data;
        if (sampleCount == 0) {
            System.out.println("aggregateResult");
            aggregateResult();
            return false;
        }


        return true;
    }

    private void aggregateResult() {
        try {
            long qps = 0;
            long avg = 0;
            long tp99 = 0;
            long median = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            long error = 0;

            int count = datas.length - abandonCount - 1;
            for (int i = 1; i <= count; i++) {
                Data data = datas[i];

                qps += data.qps;
                avg += data.avg * data.qps;
                tp99 += data.tp99 * data.qps;
                median += data.median * data.qps;
                min = Math.min(min, data.min);
                max = Math.max(max, data.max);
                error += data.error;
            }
            String format = "qps is 0";
            if (qps > 0) {
                format = String.format("%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d", name + "-" + threadNum, qps / count, avg / qps, tp99 / qps, median / qps, min, max, error/count);
            }
            System.err.println(format);

            RandomAccessFile randomFile = new RandomAccessFile("/opt/press.log", "rw");
            long fileLength = randomFile.length();
            randomFile.seek(fileLength);
            randomFile.write((format+"\r\n").getBytes());
            randomFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
