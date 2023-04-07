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
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.model.key.MetricsKey;

import java.util.function.BiConsumer;

public abstract class RegistryListener implements MetricsLifeListener<RegistryEvent> {

    private final MetricsKey keyType;

    public RegistryListener(MetricsKey keyType) {
        this.keyType = keyType;
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event.isAvailable() && event.isAssignableFrom(keyType);
    }

    static RegistryListener onEvent(MetricsKey metricsKey, BiConsumer<RegistryEvent, MetricsKey> postFunc) {

        return new RegistryListener(metricsKey) {
            @Override
            public void onEvent(RegistryEvent event) {
                postFunc.accept(event, metricsKey);
            }
        };
    }

    static RegistryListener onFinish(MetricsKey enumType, BiConsumer<TimeCounterEvent, MetricsKey> finishFunc) {

        return new RegistryListener(enumType) {
            @Override
            public void onEventFinish(RegistryEvent event) {
                finishFunc.accept(event, enumType);
            }
        };
    }

    static RegistryListener onError(MetricsKey metricsKey, BiConsumer<TimeCounterEvent, MetricsKey> errorFunc) {

        return new RegistryListener(metricsKey) {
            @Override
            public void onEventError(RegistryEvent event) {
                errorFunc.accept(event, metricsKey);
            }
        };
    }
}
