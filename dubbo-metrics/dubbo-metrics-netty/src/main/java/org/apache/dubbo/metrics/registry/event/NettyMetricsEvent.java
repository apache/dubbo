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

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.utils.TimePair;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.metrics.registry.collector.NettyMetricsCollector;
import org.apache.dubbo.remoting.transport.netty4.NettyEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.NETTY_METRICS_MAP;

/**
 * Netty related events
 */
public class NettyMetricsEvent extends TimeCounterEvent {
    public NettyMetricsEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel, typeWrapper, TimePair.start());
        ScopeBeanFactory beanFactory = getSource().getBeanFactory();
        NettyMetricsCollector collector;
        if (!beanFactory.isDestroyed()) {
            collector = beanFactory.getBean(NettyMetricsCollector.class);
            super.setAvailable(collector != null && collector.isCollectEnabled());
        }
    }

    public static NettyMetricsEvent toNettyEvent(NettyEvent event) {
        ApplicationModel applicationModel = event.getApplicationModel();
        NettyEvent.MetricsData postResult = event.getPostResult();

        NettyMetricsEvent nettyMetricsEvent =
                new NettyMetricsEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, null, null, null));

        Map<String, Long> dataMap = new HashMap<>();
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_HEAP_MEMORY_USED.getName(), postResult.usedHeapMemory);
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_DIRECT_MEMORY_USED.getName(), postResult.usedDirectMemory);
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_HEAP_ARENAS_NUM.getName(), postResult.numHeapArenas);
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_DIRECT_ARENAS_NUM.getName(), postResult.numDirectArenas);
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_NORMAL_CACHE_SIZE.getName(), postResult.normalCacheSize);
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_SMALL_CACHE_SIZE.getName(), postResult.smallCacheSize);
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_THREAD_LOCAL_CACHES_NUM.getName(), postResult.numThreadLocalCaches);
        dataMap.put(MetricsKey.NETTY_ALLOCATOR_CHUNK_SIZE.getName(), postResult.chunkSize);
        nettyMetricsEvent.putAttachment(NETTY_METRICS_MAP, dataMap);

        return nettyMetricsEvent;
    }
}
