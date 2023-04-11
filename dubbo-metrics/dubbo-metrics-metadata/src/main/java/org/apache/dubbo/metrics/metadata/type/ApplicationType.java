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

package org.apache.dubbo.metrics.metadata.type;


import org.apache.dubbo.metrics.model.key.MetricsKey;

public enum ApplicationType {
    P_TOTAL(MetricsKey.METADATA_PUSH_METRIC_NUM),
    P_SUCCEED(MetricsKey.METADATA_PUSH_METRIC_NUM_SUCCEED),
    P_FAILED(MetricsKey.METADATA_PUSH_METRIC_NUM_FAILED),

    S_TOTAL(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM),
    S_SUCCEED(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED),
    S_FAILED(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_FAILED),

    ;
    private final MetricsKey metricsKey;
    private final boolean isIncrement;

    ApplicationType(MetricsKey metricsKey) {
        this(metricsKey, true);
    }

    ApplicationType(MetricsKey metricsKey, boolean isIncrement) {
        this.metricsKey = metricsKey;
        this.isIncrement = isIncrement;
    }

    public MetricsKey getMetricsKey() {
        return metricsKey;
    }

    public boolean isIncrement() {
        return isIncrement;
    }
}
