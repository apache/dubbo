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
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.registry.collector.NettyMetricsCollector;

import java.util.Collections;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.NETTY_METRICS_MAP;

public final class NettySubDispatcher extends SimpleMetricsEventMulticaster {

    public NettySubDispatcher(NettyMetricsCollector collector) {
        super.addListener(new AbstractMetricsKeyListener(null) {
            @Override
            public boolean isSupport(MetricsEvent event) {
                return true;
            }

            @Override
            public void onEventFinish(TimeCounterEvent event) {
                Map<String, Long> lastNumMap = Collections.unmodifiableMap(event.getAttachmentValue(NETTY_METRICS_MAP));
                lastNumMap.forEach((k, v) -> {
                    MetricsKey metricsKey = MetricsKey.getMetricsByName(k);
                    collector.setAppNum(metricsKey, v);
                });
            }
        });
    }
}
