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

public interface MetricsConstants {

    String PROTOCOL_PROMETHEUS = "prometheus";

    String TAG_IP = "ip";

    String TAG_HOSTNAME = "hostname";

    String TAG_APPLICATION_NAME = "application.name";

    String TAG_INTERFACE_KEY = "interface";

    String TAG_METHOD_KEY = "method";

    String TAG_GROUP_KEY = "group";

    String TAG_VERSION_KEY = "version";

    String ENABLE_JVM_METRICS_KEY = "enable.jvm.metrics";

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

    int PROMETHEUS_DEFAULT_METRICS_PORT = 20888;

    String PROMETHEUS_DEFAULT_METRICS_PATH = "/metrics";

    int PROMETHEUS_DEFAULT_PUSH_INTERVAL = 30;

    String PROMETHEUS_DEFAULT_JOB_NAME = "default_dubbo_job";

    String METRIC_REQUESTS_TOTAL_NAME = "requests.total";

    String METRIC_REQUESTS_TOTAL_DESC = "Total Requests";

    String METRIC_REQUESTS_SUCCEED_NAME = "requests.succeed";

    String METRIC_REQUESTS_SUCCEED_DESC = "Succeed Requests";

    String METRIC_REQUESTS_FAILED_NAME = "requests.failed";

    String METRIC_REQUESTS_FAILED_DESC = "Failed Requests";

    String METRIC_REQUESTS_PROCESSING_NAME = "requests.processing";

    String METRIC_REQUESTS_PROCESSING_DESC = "Processing Requests";

    String METRIC_REQUESTS_TOTAL_AGG_NAME = "requests.total.aggregate";

    String METRIC_REQUESTS_TOTAL_AGG_DESC = "Aggregated Total Requests";

    String METRIC_REQUESTS_SUCCEED_AGG_NAME = "requests.succeed.aggregate";

    String METRIC_REQUESTS_SUCCEED_AGG_DESC = "Aggregated Succeed Requests";

    String METRIC_REQUESTS_FAILED_AGG_NAME = "requests.failed.aggregate";

    String METRIC_REQUESTS_FAILED_AGG_DESC = "Aggregated Failed Requests";

    String METRIC_QPS_NAME = "qps";

    String METRIC_QPS_DESC = "Query Per Seconds";

    String METRIC_RT_LAST_NAME = "rt.last";

    String METRIC_RT_LAST_DESC = "Last Response Time";

    String METRIC_RT_MIN_NAME = "rt.min";

    String METRIC_RT_MIN_DESC = "Min Response Time";

    String METRIC_RT_MAX_NAME = "rt.max";

    String METRIC_RT_MAX_DESC = "Max Response Time";

    String METRIC_RT_TOTAL_NAME = "rt.total";

    String METRIC_RT_TOTAL_DESC = "Total Response Time";

    String METRIC_RT_AVG_NAME = "rt.avg";

    String METRIC_RT_AVG_DESC = "Average Response Time";

    String METRIC_RT_P99_NAME = "rt.p99";

    String METRIC_RT_P99_DESC = "Response Time P99";

    String METRIC_RT_P95_NAME = "rt.p95";

    String METRIC_RT_P95_DESC = "Response Time P95";
}
