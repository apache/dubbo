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
    METRIC_REQUESTS("dubbo.%s.requests.total", "Total Requests"),
    METRIC_REQUESTS_SUCCEED("dubbo.%s.requests.succeed.total", "Total Succeed Requests"),
    METRIC_REQUEST_BUSINESS_FAILED("dubbo.%s.requests.business.failed.total","Total Failed Business Requests"),
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

    METRIC_QPS("dubbo.%s.qps.seconds", "Query Per Seconds"),
    METRIC_RT_LAST("dubbo.%s.rt.seconds.last", "Last Response Time"),
    METRIC_RT_MIN("dubbo.%s.rt.seconds.min", "Min Response Time"),
    METRIC_RT_MAX("dubbo.%s.rt.seconds.max", "Max Response Time"),
    METRIC_RT_SUM("dubbo.%s.rt.seconds.sum", "Sum Response Time"),
    METRIC_RT_AVG("dubbo.%s.rt.seconds.avg", "Average Response Time"),
    METRIC_RT_P99("dubbo.%s.rt.seconds.p99", "Response Time P99"),
    METRIC_RT_P95("dubbo.%s.rt.seconds.p95", "Response Time P95"),

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
    THREAD_POOL_QUEUE_SIZE("dubbo.thread.pool.queue.size","Thread Pool Queue Size");

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
