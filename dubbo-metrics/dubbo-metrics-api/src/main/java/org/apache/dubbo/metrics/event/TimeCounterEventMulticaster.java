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
package org.apache.dubbo.metrics.event;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A simple event publisher that defines lifecycle events and supports rt events
 */
public class TimeCounterEventMulticaster {
    private final List<AbstractMetricsKeyListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public void addListener(AbstractMetricsKeyListener listener) {
        listeners.add(listener);
    }

    public void publishEvent(TimeCounterEvent event) {
        if (validateIfApplicationConfigExist(event)) return;
        for (AbstractMetricsKeyListener listener : listeners) {
            if (listener.support(event)) {
                listener.onEvent(event);
            }
        }
    }

    private boolean validateIfApplicationConfigExist(TimeCounterEvent event) {
        if (event.getSource() != null) {
            // Check if exist application config
            return StringUtils.isEmpty(event.appName());
        }
        return false;
    }

    public void publishFinishEvent(TimeCounterEvent event) {
        publishTimeEvent(event, metricsLifeListener -> metricsLifeListener.onEventFinish(event));
    }

    public void publishErrorEvent(TimeCounterEvent event) {
        publishTimeEvent(event, metricsLifeListener -> metricsLifeListener.onEventError(event));
    }

    private void publishTimeEvent(TimeCounterEvent event, Consumer<AbstractMetricsKeyListener> consumer) {
        if (validateIfApplicationConfigExist(event)) {
            return;
        }
        event.getTimePair().end();
        for (AbstractMetricsKeyListener listener : listeners) {
            if (listener.support(event)) {
                consumer.accept(listener);
            }
        }
    }
}
