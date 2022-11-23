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

package org.apache.dubbo.common.metrics.model;

public enum MetricsKey {

    METRIC_REQUESTS_TOTAL("requests.total", "Total Requests"),
    METRIC_REQUESTS_SUCCEED("requests.succeed", "Succeed Requests"),
    METRIC_REQUESTS_FAILED("requests.failed", "Failed Requests"),
    METRIC_REQUEST_BUSINESS_FAILED("requests.business.failed","Failed Business Requests"),
    METRIC_REQUESTS_PROCESSING("requests.processing", "Processing Requests"),

    METRIC_REQUESTS_TOTAL_AGG("requests.total.aggregate", "Aggregated Total Requests"),
    METRIC_REQUESTS_SUCCEED_AGG("requests.succeed.aggregate", "Aggregated Succeed Requests"),
    METRIC_REQUESTS_FAILED_AGG("requests.failed.aggregate", "Aggregated Failed Requests"),
    METRIC_REQUESTS_BUSINESS_FAILED_AGG("requests.business.failed.aggregate", "Aggregated Business Failed Requests"),

    METRIC_QPS("qps", "Query Per Seconds"),
    METRIC_RT_LAST("rt.last", "Last Response Time"),
    METRIC_RT_MIN("rt.min", "Min Response Time"),
    METRIC_RT_MAX("rt.max", "Max Response Time"),
    METRIC_RT_TOTAL("rt.total", "Total Response Time"),
    METRIC_RT_AVG("rt.avg", "Average Response Time"),
    METRIC_RT_P99("rt.p99", "Response Time P99"),
    METRIC_RT_P95("rt.p95", "Response Time P95"),
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
