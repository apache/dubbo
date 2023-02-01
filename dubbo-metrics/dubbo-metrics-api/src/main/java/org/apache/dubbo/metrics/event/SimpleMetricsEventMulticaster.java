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

import org.apache.dubbo.metrics.listener.MetricsListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleMetricsEventMulticaster implements MetricsEventMulticaster {

    private static volatile SimpleMetricsEventMulticaster instance;

    private final List<MetricsListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public static SimpleMetricsEventMulticaster getInstance() {
        if (instance == null) {
            synchronized (SimpleMetricsEventMulticaster.class) {
                if (instance == null) {
                    instance = new SimpleMetricsEventMulticaster();
                }
            }
        }
        return instance;
    }

    @Override
    public void addListener(MetricsListener listener) {
        listeners.add(listener);
    }

    @Override
    public void publishEvent(MetricsEvent event) {
        if (event instanceof EmptyEvent) {
            return;
        }
        for (MetricsListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
