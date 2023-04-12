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

package org.apache.dubbo.metrics.registry;

import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceType;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.metrics.registry.event.type.ServiceType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface RegistryMetricsConstants {

    MetricsPlaceType OP_TYPE_REGISTER = MetricsPlaceType.of("register", MetricsLevel.APP);
    MetricsPlaceType OP_TYPE_SUBSCRIBE = MetricsPlaceType.of("subscribe", MetricsLevel.APP);
    MetricsPlaceType OP_TYPE_NOTIFY = MetricsPlaceType.of("notify", MetricsLevel.APP);
    MetricsPlaceType OP_TYPE_REGISTER_SERVICE = MetricsPlaceType.of("register.service", MetricsLevel.SERVICE);
    MetricsPlaceType OP_TYPE_SUBSCRIBE_SERVICE = MetricsPlaceType.of("subscribe.service", MetricsLevel.SERVICE);

    // App-level
    List<MetricsKey> appKeys = Arrays.stream(ApplicationType.values()).map(ApplicationType::getMetricsKey).collect(Collectors.toList());

    // Service-level
    List<MetricsKey> serviceKeys = Arrays.stream(ServiceType.values()).map(ServiceType::getMetricsKey).collect(Collectors.toList());

}
