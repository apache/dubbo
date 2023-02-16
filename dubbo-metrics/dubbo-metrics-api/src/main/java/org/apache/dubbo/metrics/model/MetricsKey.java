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

package org.apache.dubbo.metrics.model;

public enum MetricsKey {
    APPLICATION_METRIC_INFO("dubbo.application.info.total", "Total Application Info"),

    // provider metrics key
    PROVIDER_METRIC_REQUESTS("dubbo.provider.requests.total", "Total Requests"),
    PROVIDER_METRIC_REQUESTS_SUCCEED("dubbo.provider.requests.succeed.total", "Succeed Requests"),
    PROVIDER_METRIC_REQUEST_BUSINESS_FAILED("dubbo.provider.requests.business.failed.total","Failed Business Requests"),
    PROVIDER_METRIC_REQUESTS_PROCESSING("dubbo.provider.requests.processing", "Processing Requests"),
    PROVIDER_METRIC_REQUESTS_TIMEOUT("dubbo.provider.requests.timeout.total", "Total Timeout Failed Requests"),
    PROVIDER_METRIC_REQUESTS_LIMIT("dubbo.provider.requests.limit.total", "Total Limit Failed Requests"),
    PROVIDER_METRIC_REQUESTS_FAILED("dubbo.provider.requests.unknown.failed.total", "Unknown Failed Requests"),
    PROVIDER_METRIC_REQUESTS_TOTAL_FAILED("dubbo.provider.requests.failed.total", "Total Failed Requests"),


    PROVIDER_METRIC_REQUESTS_TOTAL_AGG("dubbo.provider.requests.total.aggregate", "Aggregated Total Requests"),
    PROVIDER_METRIC_REQUESTS_SUCCEED_AGG("dubbo.provider.requests.succeed.aggregate", "Aggregated Succeed Requests"),
    PROVIDER_METRIC_REQUESTS_FAILED_AGG("dubbo.provider.requests.failed.aggregate", "Aggregated Failed Requests"),
    PROVIDER_METRIC_REQUESTS_BUSINESS_FAILED_AGG("dubbo.provider.requests.business.failed.aggregate", "Aggregated Business Failed Requests"),
    PROVIDER_METRIC_REQUESTS_TIMEOUT_AGG("dubbo.provider.requests.timeout.failed.aggregate", "Aggregated timeout Failed Requests"),
    PROVIDER_METRIC_REQUESTS_LIMIT_AGG("dubbo.provider.requests.limit.aggregate", "Aggregated limit Requests"),
    PROVIDER_METRIC_REQUESTS_TOTAL_FAILED_AGG("dubbo.provider.requests.failed.total.aggregate", "Aggregated failed total Requests"),

    PROVIDER_METRIC_QPS("dubbo.provider.qps.seconds", "Query Per Seconds"),
    PROVIDER_METRIC_RT_LAST("dubbo.provider.rt.seconds.last", "Last Response Time"),
    PROVIDER_METRIC_RT_MIN("dubbo.provider.rt.seconds.min", "Min Response Time"),
    PROVIDER_METRIC_RT_MAX("dubbo.provider.rt.seconds.max", "Max Response Time"),
    PROVIDER_METRIC_RT_SUM("dubbo.provider.rt.seconds.sum", "Sum Response Time"),
    PROVIDER_METRIC_RT_AVG("dubbo.provider.rt.seconds.avg", "Average Response Time"),
    PROVIDER_METRIC_RT_P99("dubbo.provider.rt.seconds.p99", "Response Time P99"),
    PROVIDER_METRIC_RT_P95("dubbo.provider.rt.seconds.p95", "Response Time P95"),

    GENERIC_METRIC_REQUESTS("dubbo.%s.requests.total", "Total %s Requests"),
    GENERIC_METRIC_REQUESTS_SUCCEED("dubbo.%s.requests.succeed.total", "Succeed %s Requests"),
    GENERIC_METRIC_REQUESTS_FAILED("dubbo.%s.requests.failed.total", "Failed %s Requests"),

    GENERIC_METRIC_RT_LAST("dubbo.%s.rt.seconds.last", "Last Response Time"),
    GENERIC_METRIC_RT_MIN("dubbo.%s.rt.seconds.min", "Min Response Time"),
    GENERIC_METRIC_RT_MAX("dubbo.%s.rt.seconds.max", "Max Response Time"),
    GENERIC_METRIC_RT_SUM("dubbo.%s.rt.seconds.sum", "Sum Response Time"),
    GENERIC_METRIC_RT_AVG("dubbo.%s.rt.seconds.avg", "Average Response Time"),
    GENERIC_METRIC_RT_P99("dubbo.%s.rt.seconds.p99", "Response Time P99"),
    GENERIC_METRIC_RT_P95("dubbo.%s.rt.seconds.p95", "Response Time P95"),

    THREAD_POOL_CORE_SIZE("dubbo.thread.pool.core.size","Thread Pool Core Size"),
    THREAD_POOL_LARGEST_SIZE("dubbo.thread.pool.largest.size","Thread Pool Largest Size"),
    THREAD_POOL_MAX_SIZE("dubbo.thread.pool.max.size","Thread Pool Max Size"),
    THREAD_POOL_ACTIVE_SIZE("dubbo.thread.pool.active.size","Thread Pool Active Size"),
    THREAD_POOL_THREAD_COUNT("dubbo.thread.pool.thread.count","Thread Pool Thread Count"),
    THREAD_POOL_QUEUE_SIZE("dubbo.thread.pool.queue.size","Thread Pool Queue Size"),

    // consumer metrics key
    ;

    private final String name;
    private final String description;

    public final String getName() {
        return this.name;
    }

    public final String getDescription() {
        return this.description;
    }

    MetricsKey(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
