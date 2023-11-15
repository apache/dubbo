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

import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.ErrorCodeMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This sampler is used to count the number of occurrences of each error code.
 */
public class ErrorCodeSampler extends MetricsNameCountSampler<String, String, ErrorCodeMetric> {

    private final ErrorCodeMetricsListenRegister register;

    /**
     * Map<ErrorCode,Metric>
     */
    private final Map<String, ErrorCodeMetric> errorCodeMetrics;

    public ErrorCodeSampler(DefaultMetricsCollector collector) {
        super(collector, MetricsCategory.ERROR_CODE, MetricsKey.ERROR_CODE_COUNT);
        this.register = new ErrorCodeMetricsListenRegister(this);
        this.errorCodeMetrics = new ConcurrentHashMap<>();
    }

    @Override
    protected MetricSample provideMetricsSample(
            ErrorCodeMetric metric, AtomicLong count, MetricsKey metricsKey, MetricsCategory metricsCategory) {
        return new CounterMetricSample<>(
                metricsKey.getNameByType(metric.getErrorCode()),
                metricsKey.getDescription(),
                metric.getTags(),
                metricsCategory,
                count);
    }

    @Override
    protected void countConfigure(MetricsCountSampleConfigurer<String, String, ErrorCodeMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> {
            String errorCode = configure.getSource();
            ErrorCodeMetric metric = errorCodeMetrics.get(errorCode);

            if (metric == null) {
                metric = new ErrorCodeMetric(collector.getApplicationModel().getApplicationName(), errorCode);
                errorCodeMetrics.put(errorCode, metric);
            }
            return metric;
        });
    }
}
