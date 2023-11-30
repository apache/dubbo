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

public interface MetricsConstants {

    String INVOCATION = "metric_filter_invocation";
    String METHOD_METRICS = "metric_filter_method_metrics";
    String INVOCATION_METRICS_COUNTER = "metric_filter_invocation_counter";
    String INVOCATION_SIDE = "metric_filter_side";
    String INVOCATION_REQUEST_ERROR = "metric_request_error";

    String ATTACHMENT_KEY_SERVICE = "serviceKey";
    String ATTACHMENT_KEY_SIZE = "size";
    String ATTACHMENT_KEY_LAST_NUM_MAP = "lastNumMap";
    String ATTACHMENT_DIRECTORY_MAP = "dirNum";

    int SELF_INCREMENT_SIZE = 1;
}
