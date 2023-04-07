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

import org.apache.dubbo.metrics.model.MetricsSupport;

import java.util.Map;

/**
 * Let {@link MetricsKey MetricsKey}  output dynamic, custom string content
 */
public class MetricsKeyWrapper {

    /**
     * Metrics key when exporting
     */
    private final MetricsKey metricsKey;


    private final MetricsPlaceType placeType;

    public MetricsKeyWrapper(String type, MetricsKey metricsKey) {
        this(metricsKey, MetricsPlaceType.of(type, MetricsLevel.APP));
    }

    public MetricsKeyWrapper(MetricsKey metricsKey, MetricsPlaceType placeType) {
        this.metricsKey = metricsKey;
        this.placeType = placeType;
    }

    public MetricsPlaceType getPlaceType() {
        return placeType;
    }

    public String getType() {
        return getPlaceType().getType();
    }

    public MetricsKey getMetricsKey() {
        return metricsKey;
    }

    public boolean isKey(MetricsKey metricsKey, String registryOpType) {
        return metricsKey == getMetricsKey() && registryOpType.equals(getType());
    }

    public boolean isServiceLevel() {
        return getPlaceType().getMetricsLevel().equals(MetricsLevel.SERVICE);
    }

    public String targetKey() {
        try {
            return String.format(metricsKey.getName(), getType());
        } catch (Exception ignore) {
            return metricsKey.getName();
        }
    }

    public String targetDesc() {
        try {
            return String.format(metricsKey.getDescription(), getType());
        } catch (Exception ignore) {
            return metricsKey.getDescription();
        }
    }

    public Map<String, String> tagName(String key) {
        return isServiceLevel() ? MetricsSupport.serviceTags(key) : MetricsSupport.applicationTags(key);
    }
}
