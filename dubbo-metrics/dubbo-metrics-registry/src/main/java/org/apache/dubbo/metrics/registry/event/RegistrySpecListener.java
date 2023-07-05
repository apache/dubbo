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

import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;
import org.apache.dubbo.metrics.listener.MetricsApplicationListener;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_DIRECTORY_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_LAST_NUM_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_DIRECTORY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;

/**
 * Different from the general-purpose listener constructor {@link MetricsApplicationListener} ,
 * it provides registry custom listeners
 */
public class RegistrySpecListener {

    /**
     * Perform auto-increment on the monitored key,
     * Can use a custom listener instead of this generic operation
     */
    public static AbstractMetricsKeyListener onPostOfRegister(MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onEvent(metricsKey,
            event -> ((RegistryMetricsCollector) collector).incrRegisterNum(metricsKey, getRgs(event))
        );
    }

    public static AbstractMetricsKeyListener onFinishOfRegister(MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onFinish(metricsKey,
            event -> ((RegistryMetricsCollector) collector).incrRegisterFinishNum(metricsKey, OP_TYPE_REGISTER.getType(), getRgs(event), event.getTimePair().calc())
        );
    }

    public static AbstractMetricsKeyListener onErrorOfRegister(MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onError(metricsKey,
            event -> ((RegistryMetricsCollector) collector).incrRegisterFinishNum(metricsKey, OP_TYPE_REGISTER.getType(), getRgs(event), event.getTimePair().calc())
        );
    }

    public static AbstractMetricsKeyListener onPostOfServiceRegister(MetricsKey metricsKey, MetricsPlaceValue placeType, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onEvent(metricsKey,
            event -> ((RegistryMetricsCollector) collector).incrServiceRegisterNum(new MetricsKeyWrapper(metricsKey, placeType), getServiceKey(event), getRgs(event), getSize(event))
        );
    }

    public static AbstractMetricsKeyListener onFinishOfServiceRegister(MetricsKey metricsKey, MetricsPlaceValue placeType, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onFinish(metricsKey,
            event -> ((RegistryMetricsCollector) collector).incrServiceRegisterFinishNum(new MetricsKeyWrapper(metricsKey, placeType), getServiceKey(event), getRgs(event), getSize(event), event.getTimePair().calc())
        );
    }

    /**
     * Every time an event is triggered, multiple serviceKey related to notify are increment
     */
    public static AbstractMetricsKeyListener onFinishOfNotify(MetricsKey metricsKey, MetricsPlaceValue placeType, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onEvent(metricsKey,
            event ->
            {
                collector.addRt(event.appName(), placeType.getType(), event.getTimePair().calc());
                Map<String, Integer> lastNumMap = Collections.unmodifiableMap(event.getAttachmentValue(ATTACHMENT_KEY_LAST_NUM_MAP));
                lastNumMap.forEach(
                    (k, v) -> collector.setNum(new MetricsKeyWrapper(metricsKey, OP_TYPE_NOTIFY), k, v));
            }
        );
    }

    /**
     * Every time an event is triggered, multiple fixed key related to directory are increment, which has nothing to do with the monitored key
     */
    public static AbstractMetricsKeyListener onPostOfDirectory(MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onFinish(metricsKey,
            event -> {
                Map<MetricsKey, Map<String, Integer>> summaryMap = event.getAttachmentValue(ATTACHMENT_DIRECTORY_MAP);
                summaryMap.forEach((summaryKey, map) ->
                    map.forEach(
                        (k, v) -> collector.setNum(new MetricsKeyWrapper(summaryKey, OP_TYPE_DIRECTORY), k, v)));

            }
        );
    }

    public static AbstractMetricsKeyListener onErrorOfServiceRegister(MetricsKey metricsKey, MetricsPlaceValue placeType, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onError(metricsKey,
            event -> ((RegistryMetricsCollector) collector).incrServiceRegisterFinishNum(new MetricsKeyWrapper(metricsKey, placeType), getServiceKey(event), getRgs(event), getSize(event), event.getTimePair().calc())
        );
    }

    /**
     * Get the number of multiple registries
     */
    public static List<String> getRgs(MetricsEvent event) {
        return event.getAttachmentValue(RegistryMetricsConstants.ATTACHMENT_KEY_MULTI_REGISTRY);
    }

    /**
     * Get the exposed number of the protocol
     */
    public static int getSize(MetricsEvent event) {
        return event.getAttachmentValue(ATTACHMENT_KEY_SIZE);
    }

    public static String getServiceKey(MetricsEvent event) {
        return event.getAttachmentValue(ATTACHMENT_KEY_SERVICE);
    }
}
