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

package org.apache.dubbo.metrics.model.key;

/**
 * Please follow a unified naming format as follows:
 * dubbo_type_action_unit_otherfun
 */
public enum MetricsKey {
    APPLICATION_METRIC_INFO("application.info.total", "Total Application Info"),

    CONFIGCENTER_METRIC_TOTAL("configcenter.total", "Config Changed Total"),

    // provider metrics key
    METRIC_REQUESTS(".requests.total", "Total Requests"),
    METRIC_REQUESTS_SUCCEED(".requests.succeed.total", "Total Succeed Requests"),
    METRIC_REQUEST_BUSINESS_FAILED(".requests.business.failed.total", "Total Failed Business Requests"),

    METRIC_REQUESTS_PROCESSING(".requests.processing.total", "Processing Requests"),
    METRIC_REQUESTS_TIMEOUT(".requests.timeout.total", "Total Timeout Failed Requests"),
    METRIC_REQUESTS_LIMIT(".requests.limit.total", "Total Limit Failed Requests"),
    METRIC_REQUESTS_FAILED(".requests.unknown.failed.total", "Total Unknown Failed Requests"),
    METRIC_REQUESTS_TOTAL_FAILED(".requests.failed.total", "Total Failed Requests"),
    METRIC_REQUESTS_NETWORK_FAILED(".requests.failed.network.total", "Total network Failed Requests"),
    METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED(".requests.failed.service.unavailable.total", "Total Service Unavailable Failed Requests"),
    METRIC_REQUESTS_CODEC_FAILED(".requests.failed.codec.total", "Total Codec Failed Requests"),

    METRIC_REQUESTS_TOTAL_AGG(".requests.total.aggregate", "Aggregated Total Requests"),
    METRIC_REQUESTS_SUCCEED_AGG(".requests.succeed.aggregate", "Aggregated Succeed Requests"),
    METRIC_REQUESTS_FAILED_AGG(".requests.failed.aggregate", "Aggregated Failed Requests"),
    METRIC_REQUEST_BUSINESS_FAILED_AGG(".requests.business.failed.aggregate", "Aggregated Business Failed Requests"),
    METRIC_REQUESTS_TIMEOUT_AGG(".requests.timeout.failed.aggregate", "Aggregated timeout Failed Requests"),
    METRIC_REQUESTS_LIMIT_AGG(".requests.limit.aggregate", "Aggregated limit Requests"),
    METRIC_REQUESTS_TOTAL_FAILED_AGG(".requests.failed.total.aggregate", "Aggregated failed total Requests"),
    METRIC_REQUESTS_NETWORK_FAILED_AGG(".requests.failed.network.total.aggregate", "Aggregated failed network total Requests"),
    METRIC_REQUESTS_CODEC_FAILED_AGG(".requests.failed.codec.total.aggregate", "Aggregated failed codec total Requests"),
    METRIC_REQUESTS_TOTAL_SERVICE_UNAVAILABLE_FAILED_AGG(".requests.failed.service.unavailable.total.aggregate", "Aggregated failed codec total Requests"),

    METRIC_QPS(".qps.total", "Query Per Seconds"),
    METRIC_RT_LAST(".rt.milliseconds.last", "Last Response Time"),
    METRIC_RT_MIN(".rt.milliseconds.min", "Min Response Time"),
    METRIC_RT_MAX(".rt.milliseconds.max", "Max Response Time"),
    METRIC_RT_SUM(".rt.milliseconds.sum", "Sum Response Time"),
    METRIC_RT_AVG(".rt.milliseconds.avg", "Average Response Time"),
    METRIC_RT_P99(".rt.milliseconds.p99", "Response Time P99"),
    METRIC_RT_P95(".rt.milliseconds.p95", "Response Time P95"),
    METRIC_RT_P90(".rt.milliseconds.p90", "Response Time P90"),
    METRIC_RT_P50(".rt.milliseconds.p50", "Response Time P50"),
    METRIC_RT_MIN_AGG(".rt.milliseconds.min.aggregate", "Aggregated Min Response"),
    METRIC_RT_MAX_AGG(".rt.milliseconds.max.aggregate", "Aggregated Max Response"),
    METRIC_RT_AVG_AGG(".rt.milliseconds.avg.aggregate", "Aggregated Avg Response"),

    // register metrics key
    REGISTER_METRIC_REQUESTS("registry.register.requests.total", "Total Register Requests"),
    REGISTER_METRIC_REQUESTS_SUCCEED("registry.register.requests.succeed.total", "Succeed Register Requests"),
    REGISTER_METRIC_REQUESTS_FAILED("registry.register.requests.failed.total", "Failed Register Requests"),
    METRIC_RT_HISTOGRAM(".rt.milliseconds.histogram", "Response Time Histogram"),


    GENERIC_METRIC_REQUESTS(".requests.total", "Total %s Requests"),
    GENERIC_METRIC_REQUESTS_SUCCEED(".requests.succeed.total", "Succeed %s Requests"),
    GENERIC_METRIC_REQUESTS_FAILED(".requests.failed.total", "Failed %s Requests"),

    // subscribe metrics key
    SUBSCRIBE_METRIC_NUM("registry.subscribe.num.total", "Total Subscribe Num"),
    SUBSCRIBE_METRIC_NUM_SUCCEED("registry.subscribe.num.succeed.total", "Succeed Subscribe Num"),
    SUBSCRIBE_METRIC_NUM_FAILED("registry.subscribe.num.failed.total", "Failed Subscribe Num"),

    // directory metrics key
    DIRECTORY_METRIC_NUM_ALL("registry.directory.num.all", "All Directory Urls"),
    DIRECTORY_METRIC_NUM_VALID("registry.directory.num.valid.total", "Valid Directory Urls"),
    DIRECTORY_METRIC_NUM_TO_RECONNECT("registry.directory.num.to_reconnect.total", "ToReconnect Directory Urls"),
    DIRECTORY_METRIC_NUM_DISABLE("registry.directory.num.disable.total", "Disable Directory Urls"),

    NOTIFY_METRIC_REQUESTS("registry.notify.requests.total", "Total Notify Requests"),
    NOTIFY_METRIC_NUM_LAST("registry.notify.num.last", "Last Notify Nums"),

    THREAD_POOL_CORE_SIZE("thread.pool.core.size", "Thread Pool Core Size"),
    THREAD_POOL_LARGEST_SIZE("thread.pool.largest.size", "Thread Pool Largest Size"),
    THREAD_POOL_MAX_SIZE("thread.pool.max.size", "Thread Pool Max Size"),
    THREAD_POOL_ACTIVE_SIZE("thread.pool.active.size", "Thread Pool Active Size"),
    THREAD_POOL_THREAD_COUNT("thread.pool.thread.count", "Thread Pool Thread Count"),
    THREAD_POOL_QUEUE_SIZE("thread.pool.queue.size", "Thread Pool Queue Size"),
    THREAD_POOL_THREAD_REJECT_COUNT("thread.pool.reject.thread.count", "Thread Pool Reject Thread Count"),

    // metadata push metrics key
    METADATA_PUSH_METRIC_NUM("metadata.push.num.total", "Total Push Num"),
    METADATA_PUSH_METRIC_NUM_SUCCEED("metadata.push.num.succeed.total", "Succeed Push Num"),
    METADATA_PUSH_METRIC_NUM_FAILED("metadata.push.num.failed.total", "Failed Push Num"),

    // metadata subscribe metrics key
    METADATA_SUBSCRIBE_METRIC_NUM("metadata.subscribe.num.total", "Total Metadata Subscribe Num"),
    METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED("metadata.subscribe.num.succeed.total", "Succeed Metadata Subscribe Num"),
    METADATA_SUBSCRIBE_METRIC_NUM_FAILED("metadata.subscribe.num.failed.total", "Failed Metadata Subscribe Num"),

    // register service metrics key
    SERVICE_REGISTER_METRIC_REQUESTS("registry.register.service.total", "Total Service-Level Register Requests"),
    SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED("registry.register.service.succeed.total", "Succeed Service-Level Register Requests"),
    SERVICE_REGISTER_METRIC_REQUESTS_FAILED("registry.register.service.failed.total", "Failed Service-Level Register Requests"),

    // subscribe metrics key
    SERVICE_SUBSCRIBE_METRIC_NUM("registry.subscribe.service.num.total", "Total Service-Level Subscribe Num"),
    SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED("registry.subscribe.service.num.succeed.total", "Succeed Service-Level Num"),
    SERVICE_SUBSCRIBE_METRIC_NUM_FAILED("registry.subscribe.service.num.failed.total", "Failed Service-Level Num"),
    // store provider metadata service key
    STORE_PROVIDER_METADATA("metadata.store.provider.total", "Store Provider Metadata"),

    STORE_PROVIDER_METADATA_SUCCEED("metadata.store.provider.succeed.total", "Succeed Store Provider Metadata"),

    STORE_PROVIDER_METADATA_FAILED("metadata.store.provider.failed.total", "Failed Store Provider Metadata"),
    METADATA_GIT_COMMITID_METRIC("git.commit.id", "Git Commit Id Metrics"),

    // consumer metrics key
    INVOKER_NO_AVAILABLE_COUNT("consumer.invoker.no.available.count", "Request Throw No Invoker Available Exception Count"),
    ;

    private String namePrefix;
    private String nameSuffix;
    private String description;

    public final String getName() {
        return "dubbo." + nameSuffix;
    }

    public final String getNameByType(String type) {
        return "dubbo." + type + nameSuffix;
    }

    public final String getDescription() {
        return this.description;
    }


    MetricsKey(String nameSuffix, String description) {
        this.nameSuffix = nameSuffix;
        this.description = description;
    }
}
