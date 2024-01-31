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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_CODEC_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_LIMIT;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_NETWORK_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_PROCESSING;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_TIMEOUT;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_TOTAL_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUEST_BUSINESS_FAILED;

public interface DefaultConstants {

    String METRIC_FILTER_EVENT = "metric_filter_event";

    String METRIC_THROWABLE = "metric_filter_throwable";

    List<MetricsKeyWrapper> METHOD_LEVEL_KEYS = Arrays.asList(
            new MetricsKeyWrapper(METRIC_REQUESTS, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(METRIC_REQUESTS, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            // METRIC_REQUESTS_PROCESSING use GAUGE
            new MetricsKeyWrapper(
                            METRIC_REQUESTS_PROCESSING,
                            MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD))
                    .setSampleType(MetricSample.Type.GAUGE),
            new MetricsKeyWrapper(
                            METRIC_REQUESTS_PROCESSING,
                            MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD))
                    .setSampleType(MetricSample.Type.GAUGE),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_SUCCEED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_SUCCEED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUEST_BUSINESS_FAILED,
                    MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUEST_BUSINESS_FAILED,
                    MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_TIMEOUT, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_TIMEOUT, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_LIMIT, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_LIMIT, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_TOTAL_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_TOTAL_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_NETWORK_FAILED,
                    MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_NETWORK_FAILED,
                    MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED,
                    MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED,
                    MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_CODEC_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD)),
            new MetricsKeyWrapper(
                    METRIC_REQUESTS_CODEC_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD)));

    List<MetricsKey> INIT_AGG_METHOD_KEYS = Arrays.asList(
            MetricsKey.METRIC_REQUESTS_TOTAL_AGG,
            MetricsKey.METRIC_REQUESTS_SUCCEED_AGG,
            MetricsKey.METRIC_REQUESTS_FAILED_AGG,
            MetricsKey.METRIC_REQUEST_BUSINESS_FAILED_AGG,
            MetricsKey.METRIC_REQUESTS_TIMEOUT_AGG,
            MetricsKey.METRIC_REQUESTS_LIMIT_AGG,
            MetricsKey.METRIC_REQUESTS_TOTAL_FAILED_AGG,
            MetricsKey.METRIC_REQUESTS_NETWORK_FAILED_AGG,
            MetricsKey.METRIC_REQUESTS_CODEC_FAILED_AGG,
            MetricsKey.METRIC_REQUESTS_TOTAL_SERVICE_UNAVAILABLE_FAILED_AGG);

    List<MetricsKey> INIT_DEFAULT_METHOD_KEYS = Arrays.asList(
            MetricsKey.METRIC_REQUESTS,
            MetricsKey.METRIC_REQUESTS_PROCESSING,
            MetricsKey.METRIC_REQUESTS_FAILED_AGG,
            MetricsKey.METRIC_REQUESTS_SUCCEED,
            MetricsKey.METRIC_REQUESTS_TOTAL_FAILED,
            MetricsKey.METRIC_REQUEST_BUSINESS_FAILED);
}
