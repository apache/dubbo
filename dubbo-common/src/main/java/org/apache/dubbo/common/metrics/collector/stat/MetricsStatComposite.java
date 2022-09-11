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

package org.apache.dubbo.common.metrics.collector.stat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

import org.apache.dubbo.common.metrics.event.MetricsEvent;
import org.apache.dubbo.common.metrics.event.RTEvent;
import org.apache.dubbo.common.metrics.event.RequestEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MethodMetric;
public class MetricsStatComposite{

    public Map<StatType, MetricsStatHandler> stats = new ConcurrentHashMap<>();

    private final Map<MethodMetric, AtomicLong>     lastRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, LongAccumulator> minRT  = new ConcurrentHashMap<>();
    private final Map<MethodMetric, LongAccumulator> maxRT  = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> avgRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> totalRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> rtCount = new ConcurrentHashMap<>();

    private final String applicationName;
    private final List<MetricsListener> listeners;

    public MetricsStatComposite(String applicationName, List<MetricsListener> listeners){
        this.applicationName = applicationName;
        this.listeners = listeners;
        this.init();
    }

    public MetricsStatHandler getHandler(StatType statType) {
        return stats.get(statType);
    }

    public Map<MethodMetric, AtomicLong> getLastRT(){
        return this.lastRT;
    }
    public Map<MethodMetric, LongAccumulator> getMinRT(){
        return this.minRT;
    }

    public Map<MethodMetric, LongAccumulator> getMaxRT(){
        return this.maxRT;
    }
    public Map<MethodMetric, AtomicLong> getAvgRT(){
        return this.avgRT;
    }
    public Map<MethodMetric, AtomicLong> getTotalRT(){
        return this.totalRT;
    }
    public Map<MethodMetric, AtomicLong> getRtCount(){
        return this.rtCount;
    }

    private void publishEvent(MetricsEvent event) {
        for (MetricsListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    public void addRT(String interfaceName, String methodName, String group, String version, Long responseTime) {

        MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);

        AtomicLong last = lastRT.computeIfAbsent(metric, k -> new AtomicLong());
        last.set(responseTime);

        LongAccumulator min = minRT.computeIfAbsent(metric, k -> new LongAccumulator(Long::min, Long.MAX_VALUE));
        min.accumulate(responseTime);

        LongAccumulator max = maxRT.computeIfAbsent(metric, k -> new LongAccumulator(Long::max, Long.MIN_VALUE));
        max.accumulate(responseTime);

        AtomicLong total = totalRT.computeIfAbsent(metric, k -> new AtomicLong());
        total.addAndGet(responseTime);

        AtomicLong count = rtCount.computeIfAbsent(metric, k -> new AtomicLong());
        count.incrementAndGet();

        avgRT.computeIfAbsent(metric, k -> new AtomicLong());

        publishEvent(new RTEvent(metric, responseTime));

    }

    private void init() {
        stats.put(StatType.TOTAL, new DefaultMetricStatHandler(applicationName){
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.TOTAL));
            }
        });

        stats.put(StatType.SUCCEED, new DefaultMetricStatHandler(applicationName) {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.SUCCEED));
            }
        });

        stats.put(StatType.FAILED, new DefaultMetricStatHandler(applicationName) {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.FAILED));
            }
        });

        stats.put(StatType.BUSINESS_FAILED, new DefaultMetricStatHandler(applicationName) {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.BUSINESS_FAILED));
            }
        });

        stats.put(StatType.PROCESSING, new DefaultMetricStatHandler(applicationName));
    }
}
