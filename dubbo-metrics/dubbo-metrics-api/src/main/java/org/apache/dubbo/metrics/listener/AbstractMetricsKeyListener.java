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

import org.apache.dubbo.common.event.DubboEventBus;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;

import java.util.function.Consumer;

/**
 * According to the event template of {@link DubboEventBus},
 * build a consistent static method for general and custom monitoring consume methods
 */
public abstract class AbstractMetricsKeyListener {

    private final MetricsKey metricsKey;

    public AbstractMetricsKeyListener(MetricsKey metricsKey) {
        this.metricsKey = metricsKey;
    }

    /**
     * The MetricsKey type determines whether events are supported
     */
    public boolean support(TimeCounterEvent event) {
        return event.isAssignableFrom(metricsKey);
    }

    public void onEvent(TimeCounterEvent event) {}

    public void onEventFinish(TimeCounterEvent event) {}

    public void onEventError(TimeCounterEvent event) {}

    public static AbstractMetricsKeyListener onEvent(MetricsKey metricsKey, Consumer<TimeCounterEvent> postFunc) {

        return new AbstractMetricsKeyListener(metricsKey) {
            @Override
            public void onEvent(TimeCounterEvent event) {
                postFunc.accept(event);
            }
        };
    }

    public static AbstractMetricsKeyListener onFinish(MetricsKey metricsKey, Consumer<TimeCounterEvent> finishFunc) {

        return new AbstractMetricsKeyListener(metricsKey) {
            @Override
            public void onEventFinish(TimeCounterEvent event) {
                finishFunc.accept(event);
            }
        };
    }

    public static AbstractMetricsKeyListener onError(MetricsKey metricsKey, Consumer<TimeCounterEvent> errorFunc) {

        return new AbstractMetricsKeyListener(metricsKey) {
            @Override
            public void onEventError(TimeCounterEvent event) {
                errorFunc.accept(event);
            }
        };
    }
}
