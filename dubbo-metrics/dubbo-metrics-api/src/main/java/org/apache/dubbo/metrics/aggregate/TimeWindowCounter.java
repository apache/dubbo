/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.metrics.aggregate;

import java.util.concurrent.TimeUnit;

/**
 * Wrapper around Counter like Long and Integer.
 * <p>
 * Maintains a ring buffer of Counter to provide count over a sliding windows of time.
 */
public class TimeWindowCounter {
    private final Long[] ringBuffer;
    private final Long[] bucketStartTimeMillis;
    private int currentBucket;
    private long lastRotateTimestampMillis;
    private final long durationBetweenRotatesMillis;

    public TimeWindowCounter(int bucketNum, int timeWindowSeconds) {
        this.ringBuffer = new Long[bucketNum];
        this.bucketStartTimeMillis = new Long[bucketNum];
        for (int i = 0; i < bucketNum; i++) {
            this.ringBuffer[i] = 0L;
            this.bucketStartTimeMillis[i] = System.currentTimeMillis();
        }

        this.currentBucket = 0;
        this.lastRotateTimestampMillis = System.currentTimeMillis();
        this.durationBetweenRotatesMillis = TimeUnit.SECONDS.toMillis(timeWindowSeconds) / bucketNum;
    }

    public synchronized double get() {
        return rotate();
    }

    public long bucketLivedSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - bucketStartTimeMillis[currentBucket]);
    }

    public synchronized void increment() {
        this.increment(1L);
    }

    public synchronized void increment(Long step) {
        rotate();
        for (int i = 0; i < ringBuffer.length; i++) {
            ringBuffer[i] = ringBuffer[i] + step;
        }
    }

    public synchronized void decrement() {
        this.decrement(1L);
    }

    public synchronized void decrement(Long step) {
        rotate();
        for (int i = 0; i < ringBuffer.length; i++) {
            ringBuffer[i] = ringBuffer[i] - step;
        }
    }

    private Long rotate() {
        long timeSinceLastRotateMillis = System.currentTimeMillis() - lastRotateTimestampMillis;
        while (timeSinceLastRotateMillis > durationBetweenRotatesMillis) {
            ringBuffer[currentBucket] = 0L;
            bucketStartTimeMillis[currentBucket] = lastRotateTimestampMillis + durationBetweenRotatesMillis;
            if (++currentBucket >= ringBuffer.length) {
                currentBucket = 0;
            }
            timeSinceLastRotateMillis -= durationBetweenRotatesMillis;
            lastRotateTimestampMillis += durationBetweenRotatesMillis;
        }
        return ringBuffer[currentBucket];
    }
}
