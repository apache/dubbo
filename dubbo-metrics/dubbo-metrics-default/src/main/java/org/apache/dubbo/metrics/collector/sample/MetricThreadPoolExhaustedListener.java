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

package org.apache.dubbo.metrics.collector.sample;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedEvent;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedListener;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;

public class MetricThreadPoolExhaustedListener implements ThreadPoolExhaustedListener {

    private final ThreadRejectMetricsCountSampler threadRejectMetricsCountSampler;

    private final String threadPoolExecutorName;

    public MetricThreadPoolExhaustedListener(String threadPoolExecutorName,DefaultMetricsCollector collector) {
        this.threadPoolExecutorName=threadPoolExecutorName;
        this.threadRejectMetricsCountSampler = new ThreadRejectMetricsCountSampler(collector);
    }

    public MetricThreadPoolExhaustedListener(String threadPoolExecutorName,ThreadRejectMetricsCountSampler sampler) {
        this.threadPoolExecutorName=threadPoolExecutorName;
        this.threadRejectMetricsCountSampler=sampler;
    }
    @Override
    public void onEvent(ThreadPoolExhaustedEvent event) {
        threadRejectMetricsCountSampler.addMetricName(threadPoolExecutorName);
        threadRejectMetricsCountSampler.incOnEvent(threadPoolExecutorName,threadPoolExecutorName);
    }
}
