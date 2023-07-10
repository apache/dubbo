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

package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;
import org.apache.dubbo.metrics.model.key.CategoryOverall;
import org.apache.dubbo.metrics.model.key.MetricsCat;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.registry.collector.NettyMetricsCollector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.NETTY_METRICS_MAP;
import static org.apache.dubbo.metrics.registry.NettyMetricsConstants.OP_TYPE_MEMORY;

public final class NettySubDispatcher extends SimpleMetricsEventMulticaster {


    public NettySubDispatcher(NettyMetricsCollector collector) {

        CategorySet.ALL.forEach(categorySet ->
        {
            super.addListener(categorySet.getPost().getEventFunc().apply(collector));
            if (categorySet.getFinish() != null) {
                super.addListener(categorySet.getFinish().getEventFunc().apply(collector));
            }
            if (categorySet.getError() != null) {
                super.addListener(categorySet.getError().getEventFunc().apply(collector));
            }
        });
    }

    /**
     * A closer aggregation of MetricsCat, a summary collection of certain types of events
     */
    interface CategorySet {
        CategoryOverall APPLICATION_HEAP_MEMORY_USED = new CategoryOverall(OP_TYPE_MEMORY, MCat.HEAP_MEMORY_USED, null, null);


        List<CategoryOverall> ALL = Arrays.asList(APPLICATION_HEAP_MEMORY_USED);
    }


    /**
     * {@link MetricsCat} MetricsCat collection, for better classification processing
     * Except for a few custom functions, most of them can build standard event listening functions through the static methods of MetricsApplicationListener
     */
    interface MCat {
        // MetricsNotifyListener
        MetricsCat HEAP_MEMORY_USED = new MetricsCat(MetricsKey.NETTY_ALLOCATOR_HEAP_MEMORY_USED,
            (key, placeType, collector) -> AbstractMetricsKeyListener.onFinish(key,
                event -> {
                    collector.addRt(event.appName(), placeType.getType(), event.getTimePair().calc());
                    Map<String, Long> lastNumMap = Collections.unmodifiableMap(event.getAttachmentValue(NETTY_METRICS_MAP));
                    lastNumMap.forEach(
                        (k, v) -> {
                            MetricsKey metricsKey = MetricsKey.getMetricsByName(k);
                            collector.setAppNum(metricsKey, v);
                        });
                }
            ));

    }


}
