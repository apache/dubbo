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
package org.apache.dubbo.metrics.metadata;

import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_PUSH_METRIC_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_PUSH_METRIC_NUM_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_PUSH_METRIC_NUM_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.STORE_PROVIDER_METADATA;
import static org.apache.dubbo.metrics.model.key.MetricsKey.STORE_PROVIDER_METADATA_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.STORE_PROVIDER_METADATA_SUCCEED;

public interface MetadataMetricsConstants {

    MetricsPlaceValue OP_TYPE_PUSH = MetricsPlaceValue.of("push", MetricsLevel.APP);
    MetricsPlaceValue OP_TYPE_SUBSCRIBE = MetricsPlaceValue.of("subscribe", MetricsLevel.APP);
    MetricsPlaceValue OP_TYPE_STORE_PROVIDER_INTERFACE =
            MetricsPlaceValue.of("store.provider.interface", MetricsLevel.SERVICE);

    // App-level
    List<MetricsKey> APP_LEVEL_KEYS = Arrays.asList(
            METADATA_PUSH_METRIC_NUM,
            METADATA_PUSH_METRIC_NUM_SUCCEED,
            METADATA_PUSH_METRIC_NUM_FAILED,
            METADATA_SUBSCRIBE_METRIC_NUM,
            METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED,
            METADATA_SUBSCRIBE_METRIC_NUM_FAILED);

    // Service-level
    List<MetricsKeyWrapper> SERVICE_LEVEL_KEYS = Arrays.asList(
            new MetricsKeyWrapper(STORE_PROVIDER_METADATA, OP_TYPE_STORE_PROVIDER_INTERFACE),
            new MetricsKeyWrapper(STORE_PROVIDER_METADATA_SUCCEED, OP_TYPE_STORE_PROVIDER_INTERFACE),
            new MetricsKeyWrapper(STORE_PROVIDER_METADATA_FAILED, OP_TYPE_STORE_PROVIDER_INTERFACE));
}
