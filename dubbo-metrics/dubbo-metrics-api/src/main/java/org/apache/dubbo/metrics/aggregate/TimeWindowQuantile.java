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

import com.tdunning.math.stats.TDigest;

import java.util.concurrent.TimeUnit;

/**
 * Wrapper around TDigest.
 * <p>
 * Maintains a ring buffer of TDigest to provide quantiles over a sliding windows of time.
 */
public class TimeWindowQuantile {
    private final double compression;
    private final TDigest[] ringBuffer;
    private int currentBucket;
    private long lastRotateTimestampMillis;
    private final long durationBetweenRotatesMillis;

    public TimeWindowQuantile(double compression, int bucketNum, int timeWindowSeconds) {
        this.compression = compression;
        this.ringBuffer = new TDigest[bucketNum];
        for (int i = 0; i < bucketNum; i++) {
            this.ringBuffer[i] = TDigest.createDigest(compression);
        }

        this.currentBucket = 0;
        this.lastRotateTimestampMillis = System.currentTimeMillis();
        this.durationBetweenRotatesMillis = TimeUnit.SECONDS.toMillis(timeWindowSeconds) / bucketNum;
    }

    public synchronized double quantile(double q) {
        TDigest currentBucket = rotate();

        // This may return Double.NaN, and it's correct behavior.
        // see: https://github.com/prometheus/client_golang/issues/85
        return currentBucket.quantile(q);
    }

    public synchronized void add(double value) {
        rotate();
        for (TDigest bucket : ringBuffer) {
            bucket.add(value);
        }
    }

    private TDigest rotate() {
        long timeSinceLastRotateMillis = System.currentTimeMillis() - lastRotateTimestampMillis;
        while (timeSinceLastRotateMillis > durationBetweenRotatesMillis) {
            ringBuffer[currentBucket] = TDigest.createDigest(compression);
            if (++currentBucket >= ringBuffer.length) {
                currentBucket = 0;
            }
            timeSinceLastRotateMillis -= durationBetweenRotatesMillis;
            lastRotateTimestampMillis += durationBetweenRotatesMillis;
        }
        return ringBuffer[currentBucket];
    }
}
