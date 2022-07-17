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
package org.apache.dubbo.metrices;

import java.util.concurrent.TimeUnit;

public class MetricsCollector {
    private int currentBucket;
    private long lastRotateTime;
    private final MetricsBucket[] ringBuffer;
    private final long duration;

    public MetricsCollector(int buckets, int ageSeconds) {
        this.ringBuffer = new MetricsBucket[buckets];
        for (int i = 0; i < buckets; i++) {
            this.ringBuffer[i] = new MetricsBucket();
        }
        this.currentBucket = 0;
        this.lastRotateTime = System.currentTimeMillis();
        this.duration = TimeUnit.SECONDS.toMillis(ageSeconds);
    }

    public MetricsBucket getMetricsBucket() {
        return rotate();
    }

    public Metrics start(String uniqueInterfaceName, String methodName) {
        Metrics metrics = new Metrics();
        insert(uniqueInterfaceName, methodName, metrics);
        return metrics;
    }

    private synchronized void insert(String uniqueInterfaceName, String methodName, Metrics metrics) {
        rotate();
        for (MetricsBucket metricsBucket : ringBuffer) {
            metricsBucket.insert(uniqueInterfaceName, methodName, metrics);
        }
    }

    private MetricsBucket rotate() {
        long timeSinceLastRotate = System.currentTimeMillis() - lastRotateTime;
        while (timeSinceLastRotate > duration) {
            ringBuffer[currentBucket] = new MetricsBucket();
            if (++currentBucket >= ringBuffer.length) {
                currentBucket = 0;
            }
            timeSinceLastRotate -= duration;
            lastRotateTime += duration;
        }
        return ringBuffer[currentBucket];
    }
}
