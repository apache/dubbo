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
import org.apache.dubbo.metrics.listener.MetricsListener;

public class MetricsDirectoryListener implements MetricsListener<RegistryEvent.MetricsDirectoryEvent> {

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event instanceof RegistryEvent.MetricsDirectoryEvent;
    }

    @Override
    public void onEvent(RegistryEvent.MetricsDirectoryEvent event) {
        if (!event.isAvailable()) {
            return;
        }
        if (event.getType().isIncrement()) {
            event.getCollector().increment(event.getSource().getApplicationName(), event.getType());
        } else {
            event.getCollector().setNum(event.getType(), event.getSource().getApplicationName(), event.getSize());
        }
    }


}
