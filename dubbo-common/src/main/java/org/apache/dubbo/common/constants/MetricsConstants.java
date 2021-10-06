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

package org.apache.dubbo.common.constants;

/**
 * Metrics Constants.
 */
public interface MetricsConstants {

    String PROTOCOL_PROMETHEUS = "prometheus";

    String TAG_INTERFACE_KEY = "interface";

    String TAG_METHOD_KEY = "method";

    String TAG_GROUP_KEY = "group";

    String TAG_VERSION_KEY = "version";

    String AGGREGATION_COLLECTOR_KEY = "aggregation";

    String AGGREGATION_ENABLED_KEY = "aggregation.enabled";

    String AGGREGATION_BUCKET_NUM_KEY = "aggregation.bucket.num";

    String AGGREGATION_TIME_WINDOW_SECONDS_KEY = "aggregation.time.window.seconds";

    String PROMETHEUS_EXPORTER_ENABLED_KEY = "prometheus.exporter.enabled";

    String PROMETHEUS_EXPORTER_ENABLE_HTTP_SERVICE_DISCOVERY_KEY = "prometheus.exporter.enable.http.service.discovery";

    String PROMETHEUS_EXPORTER_HTTP_SERVICE_DISCOVERY_URL_KEY = "prometheus.exporter.http.service.discovery.url";

    String PROMETHEUS_EXPORTER_METRICS_PORT_KEY = "prometheus.exporter.metrics.port";

    String PROMETHEUS_EXPORTER_METRICS_PATH_KEY = "prometheus.exporter.metrics.path";

    String PROMETHEUS_PUSHGATEWAY_ENABLED_KEY = "prometheus.pushgateway.enabled";

    String PROMETHEUS_PUSHGATEWAY_BASE_URL_KEY = "prometheus.pushgateway.base.url";

    String PROMETHEUS_PUSHGATEWAY_USERNAME_KEY = "prometheus.pushgateway.username";

    String PROMETHEUS_PUSHGATEWAY_PASSWORD_KEY = "prometheus.pushgateway.password";

    String PROMETHEUS_PUSHGATEWAY_PUSH_INTERVAL_KEY = "prometheus.pushgateway.push.interval";

    String PROMETHEUS_PUSHGATEWAY_JOB_KEY = "prometheus.pushgateway.job";

}
