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
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.dubbo.metrics.model.key.MetricsKey.DIRECTORY_METRIC_NUM_ALL;
import static org.apache.dubbo.metrics.model.key.MetricsKey.DIRECTORY_METRIC_NUM_DISABLE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.DIRECTORY_METRIC_NUM_TO_RECONNECT;
import static org.apache.dubbo.metrics.model.key.MetricsKey.DIRECTORY_METRIC_NUM_VALID;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NOTIFY_METRIC_NUM_LAST;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NOTIFY_METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.REGISTER_METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.REGISTER_METRIC_REQUESTS_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SUBSCRIBE_METRIC_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED;

public interface RegistryMetricsConstants {

    String ATTACHMENT_REGISTRY_KEY = "registryKey";
    String ATTACHMENT_REGISTRY_SINGLE_KEY = "registrySingleKey";

    MetricsPlaceValue OP_TYPE_REGISTER = MetricsPlaceValue.of("register", MetricsLevel.APP);
    MetricsPlaceValue OP_TYPE_SUBSCRIBE = MetricsPlaceValue.of("subscribe", MetricsLevel.APP);
    MetricsPlaceValue OP_TYPE_NOTIFY = MetricsPlaceValue.of("notify", MetricsLevel.APP);
    MetricsPlaceValue OP_TYPE_DIRECTORY = MetricsPlaceValue.of("directory", MetricsLevel.APP);
    MetricsPlaceValue OP_TYPE_REGISTER_SERVICE = MetricsPlaceValue.of("register.service", MetricsLevel.REGISTRY);
    MetricsPlaceValue OP_TYPE_SUBSCRIBE_SERVICE = MetricsPlaceValue.of("subscribe.service", MetricsLevel.SERVICE);

    // App-level
    List<MetricsKey> APP_LEVEL_KEYS = Collections.singletonList(NOTIFY_METRIC_REQUESTS);

    // Registry-level
    List<MetricsKey> REGISTER_LEVEL_KEYS = Arrays.asList(
            REGISTER_METRIC_REQUESTS,
            REGISTER_METRIC_REQUESTS_SUCCEED,
            REGISTER_METRIC_REQUESTS_FAILED,
            SUBSCRIBE_METRIC_NUM,
            SUBSCRIBE_METRIC_NUM_SUCCEED,
            SUBSCRIBE_METRIC_NUM_FAILED);

    // Service-level
    List<MetricsKeyWrapper> SERVICE_LEVEL_KEYS = Arrays.asList(
            new MetricsKeyWrapper(NOTIFY_METRIC_NUM_LAST, OP_TYPE_NOTIFY),
            new MetricsKeyWrapper(SERVICE_REGISTER_METRIC_REQUESTS, OP_TYPE_REGISTER_SERVICE),
            new MetricsKeyWrapper(SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED, OP_TYPE_REGISTER_SERVICE),
            new MetricsKeyWrapper(SERVICE_REGISTER_METRIC_REQUESTS_FAILED, OP_TYPE_REGISTER_SERVICE),
            new MetricsKeyWrapper(SERVICE_SUBSCRIBE_METRIC_NUM, OP_TYPE_SUBSCRIBE_SERVICE),
            new MetricsKeyWrapper(SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED, OP_TYPE_SUBSCRIBE_SERVICE),
            new MetricsKeyWrapper(SERVICE_SUBSCRIBE_METRIC_NUM_FAILED, OP_TYPE_SUBSCRIBE_SERVICE),
            new MetricsKeyWrapper(DIRECTORY_METRIC_NUM_VALID, OP_TYPE_DIRECTORY),
            new MetricsKeyWrapper(DIRECTORY_METRIC_NUM_TO_RECONNECT, OP_TYPE_DIRECTORY),
            new MetricsKeyWrapper(DIRECTORY_METRIC_NUM_DISABLE, OP_TYPE_DIRECTORY),
            new MetricsKeyWrapper(DIRECTORY_METRIC_NUM_ALL, OP_TYPE_DIRECTORY));
}
