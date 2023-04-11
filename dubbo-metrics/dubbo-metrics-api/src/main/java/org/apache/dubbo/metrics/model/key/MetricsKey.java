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

public enum MetricsKey {
    APPLICATION_METRIC_INFO("dubbo.application.info.total", "Total Application Info"),

    CONFIGCENTER_METRIC_TOTAL("dubbo.configcenter.total", "Config Changed Total"),

    // provider metrics key
    METRIC_REQUESTS("dubbo.%s.requests.total", "Total Requests"),
    METRIC_REQUESTS_SUCCEED("dubbo.%s.requests.succeed.total", "Total Succeed Requests"),
    METRIC_REQUEST_BUSINESS_FAILED("dubbo.%s.requests.business.failed.total", "Total Failed Business Requests"),

    METRIC_REQUESTS_PROCESSING("dubbo.%s.requests.processing", "Processing Requests"),
    METRIC_REQUESTS_TIMEOUT("dubbo.%s.requests.timeout.total", "Total Timeout Failed Requests"),
    METRIC_REQUESTS_LIMIT("dubbo.%s.requests.limit.total", "Total Limit Failed Requests"),
    METRIC_REQUESTS_FAILED("dubbo.%s.requests.unknown.failed.total", "Total Unknown Failed Requests"),
    METRIC_REQUESTS_TOTAL_FAILED("dubbo.%s.requests.failed.total", "Total Failed Requests"),
    METRIC_REQUESTS_NETWORK_FAILED("dubbo.%s.requests.failed.network.total", "Total network Failed Requests"),
    METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED("dubbo.%s.requests.failed.service.unavailable.total", "Total Service Unavailable Failed Requests"),
    METRIC_REQUESTS_CODEC_FAILED("dubbo.%s.requests.failed.codec.total", "Total Codec Failed Requests"),

    METRIC_REQUESTS_TOTAL_AGG("dubbo.%s.requests.total.aggregate", "Aggregated Total Requests"),
    METRIC_REQUESTS_SUCCEED_AGG("dubbo.%s.requests.succeed.aggregate", "Aggregated Succeed Requests"),
    METRIC_REQUESTS_FAILED_AGG("dubbo.%s.requests.failed.aggregate", "Aggregated Failed Requests"),
    METRIC_REQUESTS_BUSINESS_FAILED_AGG("dubbo.%s.requests.business.failed.aggregate", "Aggregated Business Failed Requests"),
    METRIC_REQUESTS_TIMEOUT_AGG("dubbo.%s.requests.timeout.failed.aggregate", "Aggregated timeout Failed Requests"),
    METRIC_REQUESTS_LIMIT_AGG("dubbo.%s.requests.limit.aggregate", "Aggregated limit Requests"),
    METRIC_REQUESTS_TOTAL_FAILED_AGG("dubbo.%s.requests.failed.total.aggregate", "Aggregated failed total Requests"),
    METRIC_REQUESTS_TOTAL_NETWORK_FAILED_AGG("dubbo.%s.requests.failed.network.total.aggregate", "Aggregated failed network total Requests"),
    METRIC_REQUESTS_TOTAL_CODEC_FAILED_AGG("dubbo.%s.requests.failed.codec.total.aggregate", "Aggregated failed codec total Requests"),
    METRIC_REQUESTS_TOTAL_SERVICE_UNAVAILABLE_FAILED_AGG("dubbo.%s.requests.failed.service.unavailable.total.aggregate", "Aggregated failed codec total Requests"),

    METRIC_QPS("dubbo.%s.qps.total", "Query Per Seconds"),
    METRIC_RT_LAST("dubbo.%s.rt.milliseconds.last", "Last Response Time"),
    METRIC_RT_MIN("dubbo.%s.rt.milliseconds.min", "Min Response Time"),
    METRIC_RT_MAX("dubbo.%s.rt.milliseconds.max", "Max Response Time"),
    METRIC_RT_SUM("dubbo.%s.rt.milliseconds.sum", "Sum Response Time"),
    METRIC_RT_AVG("dubbo.%s.rt.milliseconds.avg", "Average Response Time"),
    METRIC_RT_P99("dubbo.%s.rt.milliseconds.p99", "Response Time P99"),
    METRIC_RT_P95("dubbo.%s.rt.milliseconds.p95", "Response Time P95"),


    // register metrics key
    REGISTER_METRIC_REQUESTS("dubbo.registry.register.requests.total", "Total Register Requests"),
    REGISTER_METRIC_REQUESTS_SUCCEED("dubbo.registry.register.requests.succeed.total", "Succeed Register Requests"),
    REGISTER_METRIC_REQUESTS_FAILED("dubbo.registry.register.requests.failed.total", "Failed Register Requests"),
    METRIC_RT_HISTOGRAM("dubbo.%s.rt.milliseconds.histogram", "Response Time Histogram"),


    GENERIC_METRIC_REQUESTS("dubbo.%s.requests.total", "Total %s Requests"),
    GENERIC_METRIC_REQUESTS_SUCCEED("dubbo.%s.requests.succeed.total", "Succeed %s Requests"),
    GENERIC_METRIC_REQUESTS_FAILED("dubbo.%s.requests.failed.total", "Failed %s Requests"),

    // subscribe metrics key
    SUBSCRIBE_METRIC_NUM("dubbo.registry.subscribe.num.total", "Total Subscribe Num"),
    SUBSCRIBE_METRIC_NUM_SUCCEED("dubbo.registry.subscribe.num.succeed.total", "Succeed Subscribe Num"),
    SUBSCRIBE_METRIC_NUM_FAILED("dubbo.registry.subscribe.num.failed.total", "Failed Subscribe Num"),

    // directory metrics key
    DIRECTORY_METRIC_NUM_ALL("dubbo.registry.directory.num.all", "All Directory Urls"),
    DIRECTORY_METRIC_NUM_VALID("dubbo.registry.directory.num.valid.total", "Valid Directory Urls"),
    DIRECTORY_METRIC_NUM_TO_RECONNECT("dubbo.registry.directory.num.to_reconnect.total", "ToReconnect Directory Urls"),
    DIRECTORY_METRIC_NUM_DISABLE("dubbo.registry.directory.num.disable.total", "Disable Directory Urls"),

    NOTIFY_METRIC_REQUESTS("dubbo.registry.notify.requests.total", "Total Notify Requests"),
    NOTIFY_METRIC_NUM_LAST("dubbo.registry.notify.num.last", "Last Notify Nums"),

    THREAD_POOL_CORE_SIZE("dubbo.thread.pool.core.size", "Thread Pool Core Size"),
    THREAD_POOL_LARGEST_SIZE("dubbo.thread.pool.largest.size", "Thread Pool Largest Size"),
    THREAD_POOL_MAX_SIZE("dubbo.thread.pool.max.size", "Thread Pool Max Size"),
    THREAD_POOL_ACTIVE_SIZE("dubbo.thread.pool.active.size", "Thread Pool Active Size"),
    THREAD_POOL_THREAD_COUNT("dubbo.thread.pool.thread.count", "Thread Pool Thread Count"),
    THREAD_POOL_QUEUE_SIZE("dubbo.thread.pool.queue.size", "Thread Pool Queue Size"),
    THREAD_POOL_THREAD_REJECT_COUNT("dubbo.thread.pool.reject.thread.count", "Thread Pool Reject Thread Count"),

    // metadata push metrics key
    METADATA_PUSH_METRIC_NUM("dubbo.metadata.push.num.total", "Total Push Num"),
    METADATA_PUSH_METRIC_NUM_SUCCEED("dubbo.metadata.push.num.succeed.total", "Succeed Push Num"),
    METADATA_PUSH_METRIC_NUM_FAILED("dubbo.metadata.push.num.failed.total", "Failed Push Num"),

    // metadata subscribe metrics key
    METADATA_SUBSCRIBE_METRIC_NUM("dubbo.metadata.subscribe.num.total", "Total Metadata Subscribe Num"),
    METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED("dubbo.metadata.subscribe.num.succeed.total", "Succeed Metadata Subscribe Num"),
    METADATA_SUBSCRIBE_METRIC_NUM_FAILED("dubbo.metadata.subscribe.num.failed.total", "Failed Metadata Subscribe Num"),

    // register service metrics key
    SERVICE_REGISTER_METRIC_REQUESTS("dubbo.registry.register.service.total", "Total Service-Level Register Requests"),
    SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED("dubbo.registry.register.service.succeed.total", "Succeed Service-Level Register Requests"),
    SERVICE_REGISTER_METRIC_REQUESTS_FAILED("dubbo.registry.register.service.failed.total", "Failed Service-Level Register Requests"),

    // subscribe metrics key
    SERVICE_SUBSCRIBE_METRIC_NUM("dubbo.registry.subscribe.service.num.total", "Total Service-Level Subscribe Num"),
    SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED("dubbo.registry.subscribe.service.num.succeed.total", "Succeed Service-Level Num"),
    SERVICE_SUBSCRIBE_METRIC_NUM_FAILED("dubbo.registry.subscribe.service.num.failed.total", "Failed Service-Level Num"),
    // store provider metadata service key
    STORE_PROVIDER_METADATA("dubbo.metadata.store.provider.total", "Store Provider Metadata"),

    STORE_PROVIDER_METADATA_SUCCEED("dubbo.metadata.store.provider.succeed.total", "Succeed Store Provider Metadata"),

    STORE_PROVIDER_METADATA_FAILED("dubbo.metadata.store.provider.failed.total", "Failed Store Provider Metadata"),
    METADATA_GIT_COMMITID_METRIC("git.commit.id", "Git Commit Id Metrics"),

    // consumer metrics key
    ;

    private String name;
    private String description;

    public final String getName() {
        return this.name;
    }

    public final String getNameByType(String type) {
        return String.format(name, type);
    }


    public final MetricsKey formatName(String type) {
        this.name = String.format(name, type);
        return this;
    }

    public final String getDescription() {
        return this.description;
    }

    MetricsKey(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
