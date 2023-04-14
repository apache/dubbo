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

import java.util.Arrays;
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

    MetricsPlaceType OP_TYPE_REGISTER = MetricsPlaceType.of("register", MetricsLevel.APP);
    MetricsPlaceType OP_TYPE_SUBSCRIBE = MetricsPlaceType.of("subscribe", MetricsLevel.APP);
    MetricsPlaceType OP_TYPE_NOTIFY = MetricsPlaceType.of("notify", MetricsLevel.APP);
    MetricsPlaceType OP_TYPE_DIRECTORY = MetricsPlaceType.of("directory", MetricsLevel.APP);
    MetricsPlaceType OP_TYPE_REGISTER_SERVICE = MetricsPlaceType.of("register.service", MetricsLevel.SERVICE);
    MetricsPlaceType OP_TYPE_SUBSCRIBE_SERVICE = MetricsPlaceType.of("subscribe.service", MetricsLevel.SERVICE);

    // App-level
    List<MetricsKey> APP_LEVEL_KEYS = Arrays.asList(REGISTER_METRIC_REQUESTS, REGISTER_METRIC_REQUESTS_SUCCEED, REGISTER_METRIC_REQUESTS_FAILED,
        SUBSCRIBE_METRIC_NUM, SUBSCRIBE_METRIC_NUM_SUCCEED, SUBSCRIBE_METRIC_NUM_FAILED,
        NOTIFY_METRIC_REQUESTS);

    // Service-level
    List<MetricsKey> SERVICE_LEVEL_KEYS = Arrays.asList(NOTIFY_METRIC_NUM_LAST,
        SERVICE_REGISTER_METRIC_REQUESTS, SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED, SERVICE_REGISTER_METRIC_REQUESTS_FAILED,
        SERVICE_SUBSCRIBE_METRIC_NUM, SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED, SERVICE_SUBSCRIBE_METRIC_NUM_FAILED,
        DIRECTORY_METRIC_NUM_VALID, DIRECTORY_METRIC_NUM_TO_RECONNECT, DIRECTORY_METRIC_NUM_DISABLE, DIRECTORY_METRIC_NUM_ALL
    );


}
