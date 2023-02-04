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

import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MethodMetric;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

public class MetricsStatComposite {

    public Map<RequestEvent.Type, MetricsStatHandler> stats = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, AtomicLong> lastRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, LongAccumulator> minRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, LongAccumulator> maxRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, AtomicLong> avgRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, AtomicLong> totalRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, AtomicLong> rtCount = new ConcurrentHashMap<>();
    private final List<MetricsListener> listeners;
    private DefaultMetricsCollector collector;

    public MetricsStatComposite(DefaultMetricsCollector collector) {
        this.listeners = collector.getListener();
        this.collector = collector;
        this.init();
    }

    public MetricsStatHandler getHandler(RequestEvent.Type statType) {
        return stats.get(statType);
    }

    public Map<MethodMetric, AtomicLong> getLastRT() {
        return this.lastRT;
    }

    public Map<MethodMetric, LongAccumulator> getMinRT() {
        return this.minRT;
    }

    public Map<MethodMetric, LongAccumulator> getMaxRT() {
        return this.maxRT;
    }

    public Map<MethodMetric, AtomicLong> getAvgRT() {
        return this.avgRT;
    }

    public Map<MethodMetric, AtomicLong> getTotalRT() {
        return this.totalRT;
    }

    public Map<MethodMetric, AtomicLong> getRtCount() {
        return this.rtCount;
    }

    public void addRT(String applicationName, String interfaceName, String methodName, String group, String version, Long responseTime) {
        if (collector.isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);

            AtomicLong last = ConcurrentHashMapUtils.computeIfAbsent(lastRT, metric, k -> new AtomicLong());
            last.set(responseTime);

            LongAccumulator min = ConcurrentHashMapUtils.computeIfAbsent(minRT, metric, k -> new LongAccumulator(Long::min, Long.MAX_VALUE));
            min.accumulate(responseTime);

            LongAccumulator max = ConcurrentHashMapUtils.computeIfAbsent(maxRT, metric, k -> new LongAccumulator(Long::max, Long.MIN_VALUE));
            max.accumulate(responseTime);

            AtomicLong total = ConcurrentHashMapUtils.computeIfAbsent(totalRT, metric, k -> new AtomicLong());
            total.addAndGet(responseTime);

            AtomicLong count = ConcurrentHashMapUtils.computeIfAbsent(rtCount, metric, k -> new AtomicLong());
            count.incrementAndGet();

            ConcurrentHashMapUtils.computeIfAbsent(avgRT, metric, k -> new AtomicLong());

            publishEvent(new RTEvent(metric, responseTime));
        }
    }

    private void init() {
        stats.put(RequestEvent.Type.TOTAL, new DefaultMetricsStatHandler() {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.TOTAL));
            }
        });

        stats.put(RequestEvent.Type.SUCCEED, new DefaultMetricsStatHandler() {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.SUCCEED));
            }
        });

        stats.put(RequestEvent.Type.UNKNOWN_FAILED, new DefaultMetricsStatHandler() {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.UNKNOWN_FAILED));
            }
        });

        stats.put(RequestEvent.Type.BUSINESS_FAILED, new DefaultMetricsStatHandler() {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.BUSINESS_FAILED));
            }
        });

        stats.put(RequestEvent.Type.PROCESSING, new DefaultMetricsStatHandler());

        stats.put(RequestEvent.Type.REQUEST_LIMIT, new DefaultMetricsStatHandler() {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.REQUEST_LIMIT));
            }
        });

        stats.put(RequestEvent.Type.REQUEST_TIMEOUT, new DefaultMetricsStatHandler() {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.REQUEST_TIMEOUT));
            }
        });

        stats.put(RequestEvent.Type.TOTAL_FAILED, new DefaultMetricsStatHandler() {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.TOTAL_FAILED));
            }
        });
    }

    private void publishEvent(MetricsEvent event) {
        for (MetricsListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
