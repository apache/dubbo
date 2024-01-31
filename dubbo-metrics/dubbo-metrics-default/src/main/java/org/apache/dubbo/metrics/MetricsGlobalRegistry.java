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
package org.apache.dubbo.metrics;

import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Optional;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

/**
 * Get the micrometer meter registry, can choose spring, micrometer, dubbo
 */
public class MetricsGlobalRegistry {

    private static CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();

    /**
     * Use CompositeMeterRegistry according to the following priority
     * 1. If useGlobalRegistry is configured, use the micrometer global CompositeMeterRegistry
     * 2. If there is a spring actuator, use spring's CompositeMeterRegistry
     * 3. Dubbo's own CompositeMeterRegistry is used by default
     */
    public static CompositeMeterRegistry getCompositeRegistry(ApplicationModel applicationModel) {
        Optional<MetricsConfig> configOptional =
                applicationModel.getApplicationConfigManager().getMetrics();
        if (configOptional.isPresent()
                && configOptional.get().getUseGlobalRegistry() != null
                && configOptional.get().getUseGlobalRegistry()) {
            return Metrics.globalRegistry;
        } else {
            return compositeRegistry;
        }
    }

    public static CompositeMeterRegistry getCompositeRegistry() {
        return getCompositeRegistry(ApplicationModel.defaultModel());
    }

    public static void setCompositeRegistry(CompositeMeterRegistry compositeRegistry) {
        MetricsGlobalRegistry.compositeRegistry = compositeRegistry;
    }
}
