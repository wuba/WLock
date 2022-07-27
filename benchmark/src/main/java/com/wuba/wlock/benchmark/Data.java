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

public class Data {
    long qps;
    long avg;
    long tp99;
    long median;
    long min;
    long max;
    long error;

    public Data(long qps, long avg, long tp99, long median, long min, long max, long error) {
        this.qps = qps;
        this.avg = avg;
        this.tp99 = tp99;
        this.median = median;
        this.min = min;
        this.max = max;
        this.error = error;
    }

    @Override
    public String toString() {
        return String.format("qps: %d avg: %d tp99: %d median: %d min: %d max: %d error: %d", qps, avg, tp99, median, min, max, error);
    }
}
