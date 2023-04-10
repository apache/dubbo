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

import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.listener.MetricsListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A simple event publisher that defines lifecycle events and supports rt events
 */
public class SimpleMetricsEventMulticaster implements MetricsEventMulticaster {
    private final List<MetricsListener<?>> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addListener(MetricsListener<?> listener) {
        listeners.add(listener);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void publishEvent(MetricsEvent event) {
        if (event instanceof EmptyEvent) {
            return;
        }
        if (validateIfApplicationConfigExist(event)) return;
        for (MetricsListener listener : listeners) {
            if (listener.isSupport(event)) {
                listener.onEvent(event);
            }
        }
    }

    private boolean validateIfApplicationConfigExist(MetricsEvent event) {
        if (event.getSource() != null) {
            // Check if exist application config
            return event.getSource().NotExistApplicationConfig();
        }
        return false;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void publishFinishEvent(MetricsEvent event) {
        publishTimeEvent(event, metricsLifeListener -> metricsLifeListener.onEventFinish(event));
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void publishErrorEvent(MetricsEvent event) {
        publishTimeEvent(event, metricsLifeListener -> metricsLifeListener.onEventError(event));
    }

    @SuppressWarnings({"rawtypes"})
    private void publishTimeEvent(MetricsEvent event, Consumer<MetricsLifeListener> consumer) {
        if (validateIfApplicationConfigExist(event)) return;
        if (event instanceof EmptyEvent) {
            return;
        }
        if (event instanceof TimeCounterEvent) {
            ((TimeCounterEvent) event).getTimePair().end();
        }
        for (MetricsListener listener : listeners) {
            if (listener instanceof MetricsLifeListener && listener.isSupport(event)) {
                consumer.accept(((MetricsLifeListener) listener));
            }
        }
    }
}
