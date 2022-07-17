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
package org.apache.dubbo.service;

import org.apache.dubbo.metrices.MethodMetrics;
import org.apache.dubbo.metrices.Metrics;
import org.apache.dubbo.metrices.MetricsBucket;
import org.apache.dubbo.metrices.MetricsCollector;
import org.apache.dubbo.quantile.Aggregation;
import org.apache.dubbo.quantile.Quantile;
import org.apache.dubbo.quantile.TDigestAggregation;

import java.util.LinkedList;

public class MetricsServiceImpl implements MetricsService {
    /**
     * Metrics local aggregation switch.
     */
    private final boolean enable;

    /**
     * Metrics storage, based on sliding Windows.
     */
    private final MetricsCollector collector;

    /**
     * Metrics local aggregation algorithm.
     */
    private final Aggregation aggregation;

    public MetricsServiceImpl(boolean enable, int buckets, int ageSeconds) {
        this.enable = enable;
        this.collector = new MetricsCollector(buckets, ageSeconds);
        this.aggregation = new TDigestAggregation();
    }

    @Override
    public MethodMetrics getMethodMetrics(String uniqueInterfaceName, String methodName) {
        MethodMetrics methodMetrics = new MethodMetrics();
        MetricsBucket metricsBucket = collector.getMetricsBucket();
        LinkedList<Metrics> store = metricsBucket.getStore(uniqueInterfaceName, methodName);
        long duration = store.getLast().getStartTime() - store.getFirst().getStartTime();
        int qps = (int) (store.size() / (duration / 1000));
        methodMetrics.setQps(qps);
        int succeed = 0, failed = 0, processing = 0;
        for (Metrics metrics : store) {
            switch (metrics.getStatus()) {
                case 0:
                    processing++;
                case 1:
                    failed++;
                case 2:
                    succeed++;
            }
        }
        methodMetrics.setSucceed(succeed);
        methodMetrics.setFailed(failed);
        methodMetrics.setProcessing(processing);
        if (enable) {
            Quantile quantile = this.aggregation.getQuantile(store);
            methodMetrics.setRt(quantile);
        }
        return methodMetrics;
    }

    @Override
    public Metrics start(String uniqueInterfaceName, String methodName) {
        return collector.start(uniqueInterfaceName, methodName);
    }
}
