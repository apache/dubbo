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
import org.apache.dubbo.metrics.listener.MetricsLifeListener;

import static org.apache.dubbo.metrics.registry.collector.stat.RegistryStatComposite.OP_TYPE_REGISTER;

public class MetricsRegisterListener implements MetricsLifeListener<RegistryEvent.MetricsApplicationRegisterEvent> {


    @Override
    public boolean isSupport(MetricsEvent event) {
        return event instanceof RegistryEvent.MetricsApplicationRegisterEvent;
    }

    @Override
    public void onEvent(RegistryEvent.MetricsApplicationRegisterEvent event) {
        if (!event.isAvailable()) {
            return;
        }
        event.getCollector().increment(event.getSource().getApplicationName(), RegistryEvent.ApplicationType.R_TOTAL);
    }

    @Override
    public void onEventFinish(RegistryEvent.MetricsApplicationRegisterEvent event) {
        event.getCollector().increment(event.getSource().getApplicationName(), RegistryEvent.ApplicationType.R_SUCCEED);
        event.getCollector().addApplicationRT(event.getSource().getApplicationName(), OP_TYPE_REGISTER, event.getTimePair().calc());
    }

    @Override
    public void onEventError(RegistryEvent.MetricsApplicationRegisterEvent event) {
        event.getCollector().increment(event.getSource().getApplicationName(), RegistryEvent.ApplicationType.R_FAILED);
        event.getCollector().addApplicationRT(event.getSource().getApplicationName(), OP_TYPE_REGISTER, event.getTimePair().calc());
    }
}
