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

package org.apache.dubbo.metrics.listener;

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;

import java.util.function.Consumer;

/**
 * According to the event template of {@link MetricsEventBus},
 * build a consistent static method for general and custom monitoring consume methods
 */
public abstract class AbstractMetricsListener implements MetricsLifeListener<TimeCounterEvent> {

    private final MetricsKey metricsKey;

    public AbstractMetricsListener(MetricsKey metricsKey) {
        this.metricsKey = metricsKey;
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event.isAvailable() && event.isAssignableFrom(metricsKey);
    }

    public static AbstractMetricsListener onEvent(MetricsKey metricsKey, Consumer<TimeCounterEvent> postFunc) {

        return new AbstractMetricsListener(metricsKey) {
            @Override
            public void onEvent(TimeCounterEvent event) {
                postFunc.accept(event);
            }
        };
    }

    public static AbstractMetricsListener onFinish(MetricsKey metricsKey, Consumer<TimeCounterEvent> finishFunc) {

        return new AbstractMetricsListener(metricsKey) {
            @Override
            public void onEventFinish(TimeCounterEvent event) {
                finishFunc.accept(event);
            }
        };
    }

    public static AbstractMetricsListener onError(MetricsKey metricsKey, Consumer<TimeCounterEvent> errorFunc) {

        return new AbstractMetricsListener(metricsKey) {
            @Override
            public void onEventError(TimeCounterEvent event) {
                errorFunc.accept(event);
            }
        };
    }


}
