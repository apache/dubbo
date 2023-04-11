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

package org.apache.dubbo.metrics.registry.event.type;

import org.apache.dubbo.metrics.model.key.MetricsKey;

public enum ServiceType {

    N_LAST_NUM(MetricsKey.NOTIFY_METRIC_NUM_LAST),

    R_SERVICE_TOTAL(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS),
    R_SERVICE_SUCCEED(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED),
    R_SERVICE_FAILED(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED),

    S_SERVICE_TOTAL(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM),
    S_SERVICE_SUCCEED(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED),
    S_SERVICE_FAILED(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED),

    D_VALID(MetricsKey.DIRECTORY_METRIC_NUM_VALID),
    D_TO_RECONNECT(MetricsKey.DIRECTORY_METRIC_NUM_TO_RECONNECT),
    D_DISABLE(MetricsKey.DIRECTORY_METRIC_NUM_DISABLE),
    D_ALL(MetricsKey.DIRECTORY_METRIC_NUM_ALL),
    ;

    private final MetricsKey metricsKey;

    ServiceType(MetricsKey metricsKey) {
        this.metricsKey = metricsKey;
    }

    public MetricsKey getMetricsKey() {
        return metricsKey;
    }

}
