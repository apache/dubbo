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

package org.apache.dubbo.common.metrics;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

import static org.apache.dubbo.common.metrics.MetricsService.DEFAULT_EXTENSION_NAME;

/**
 * Metrics Service.
 * Provide an interface to get metrics from {@link org.apache.dubbo.common.metrics.collector.MetricsCollector}
 */
@SPI(value = DEFAULT_EXTENSION_NAME, scope = ExtensionScope.APPLICATION)
public interface MetricsService {

    /**
     * Default {@link MetricsService} extension name.
     */
    String DEFAULT_EXTENSION_NAME = "default";

    /**
     * The contract version of {@link MetricsService}, the future update must make sure compatible.
     */
    String VERSION = "1.0.0";
}
