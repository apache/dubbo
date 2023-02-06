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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.function.BiConsumer;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

public class SubscribeListener implements MetricsLifeListener<RegistrySubscribeEvent> {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    @Override
    public boolean isSupport(MetricsEvent<?> event) {
        return event instanceof RegistrySubscribeEvent;
    }

    @Override
    public void onEvent(RegistrySubscribeEvent event) {
        handleIncrementEvent(event, RegistrySubscribeEvent.Type.R_TOTAL);
    }

    @Override
    public void onEventFinish(RegistrySubscribeEvent event) {
        handleIncrementEvent(event, RegistrySubscribeEvent.Type.R_SUCCEED);
        handleTimeEvent(event);
    }

    @Override
    public void onEventError(RegistrySubscribeEvent event) {
        handleIncrementEvent(event, RegistrySubscribeEvent.Type.R_FAILED);
        handleTimeEvent(event);
    }

    public void handleIncrementEvent(RegistrySubscribeEvent event, RegistrySubscribeEvent.Type type) {
        handleEvent(event, (applicationModel, collector) -> collector.increment(type, applicationModel.getApplicationName()));
    }

    public void handleTimeEvent(RegistrySubscribeEvent event) {
        handleEvent(event, (applicationModel, collector) -> collector.addRT(applicationModel.getApplicationName(), event.getTimePair().calc()));
    }

    public void handleEvent(RegistrySubscribeEvent event, BiConsumer<ApplicationModel, RegistryMetricsCollector> consumer) {
        ApplicationModel applicationModel = event.getSource();
        RegistryMetricsCollector collector = applicationModel.getBeanFactory().getBean(RegistryMetricsCollector.class);
        if (collector == null) {
            logger.error(INTERNAL_ERROR, "unknown error in registry module", "", "RegisterListener invoked but no collector found");
            return;
        }
        if (!collector.isCollectEnabled()) {
            return;
        }
        consumer.accept(applicationModel, collector);

    }
}
