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
package org.apache.dubbo.metrics.collector.stat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.apache.dubbo.metrics.event.EmptyEvent;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.rpc.Invocation;


public class DefaultMetricsStatHandler implements MetricsStatHandler {

    private final Map<MethodMetric, AtomicLong> counts = new ConcurrentHashMap<>();

    private final Map<ApplicationMetric, AtomicLong> applicationMetrics = new ConcurrentHashMap<>();

    public DefaultMetricsStatHandler() {
    }

    @Override
    public MetricsEvent increase(String applicationName, Invocation invocation) {
        return this.doIncrExecute(applicationName, invocation);
    }

    public MetricsEvent decrease(String applicationName, Invocation invocation) {
        return this.doDecrExecute(applicationName,invocation);
    }
    @Override
    public MetricsEvent addApplication(String applicationName, String version) {
        ApplicationMetric applicationMetric = new ApplicationMetric(applicationName, version);
        AtomicLong count = applicationMetrics.computeIfAbsent(applicationMetric, k -> new AtomicLong(0L));
        count.incrementAndGet();
        return EmptyEvent.instance();
    }
    protected MetricsEvent doExecute(String applicationName, Invocation invocation, BiConsumer<MethodMetric, Map<MethodMetric, AtomicLong>> execute) {
        MethodMetric metric = new MethodMetric(applicationName, invocation);
        execute.accept(metric, counts);
        return this.retrieveMetricsEvent(metric);
    }

    protected MetricsEvent doIncrExecute(String applicationName, Invocation invocation) {
        return this.doExecute(applicationName, invocation, (metric, counts) -> {
            AtomicLong count = counts.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();
        });
    }


    protected MetricsEvent doDecrExecute(String applicationName, Invocation invocation) {
        return this.doExecute(applicationName, invocation, (metric, counts) -> {
            AtomicLong count = counts.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.decrementAndGet();
        });
    }

    @Override
    public Map<MethodMetric, AtomicLong> get() {
        return counts;
    }

    public MetricsEvent retrieveMetricsEvent(MethodMetric metric) {
        return EmptyEvent.instance();
    }

    public Map<ApplicationMetric, AtomicLong> getApplicationMetric() {
        return applicationMetrics;
    }


}
