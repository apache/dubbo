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

import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.Objects;

import io.micrometer.common.lang.Nullable;

/**
 * Let {@link MetricsKey MetricsKey}  output dynamic, custom string content
 */
public class MetricsKeyWrapper {

    /**
     * Metrics key when exporting
     */
    private final MetricsKey metricsKey;

    /**
     * The value corresponding to the MetricsKey placeholder (if exist)
     */
    private final MetricsPlaceValue placeType;

    /**
     * Exported sample type
     */
    private MetricSample.Type sampleType = MetricSample.Type.COUNTER;

    /**
     * When the MetricsPlaceType is null, it is equivalent to a single MetricsKey.
     * Use the decorator mode to share a container with MetricsKey
     */
    public MetricsKeyWrapper(MetricsKey metricsKey, @Nullable MetricsPlaceValue placeType) {
        this.metricsKey = metricsKey;
        this.placeType = placeType;
    }

    public MetricsKeyWrapper setSampleType(MetricSample.Type sampleType) {
        this.sampleType = sampleType;
        return this;
    }

    public MetricSample.Type getSampleType() {
        return sampleType;
    }

    public MetricsPlaceValue getPlaceType() {
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

    public MetricsLevel getLevel() {
        return getPlaceType().getMetricsLevel();
    }

    public String targetKey() {
        if (placeType == null) {
            return metricsKey.getName();
        }
        try {
            return String.format(metricsKey.getName(), getType());
        } catch (Exception ignore) {
            return metricsKey.getName();
        }
    }

    public String targetDesc() {
        if (placeType == null) {
            return metricsKey.getDescription();
        }
        try {
            return String.format(metricsKey.getDescription(), getType());
        } catch (Exception ignore) {
            return metricsKey.getDescription();
        }
    }

    public static MetricsKeyWrapper wrapper(MetricsKey metricsKey) {
        return new MetricsKeyWrapper(metricsKey, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricsKeyWrapper wrapper = (MetricsKeyWrapper) o;

        if (metricsKey != wrapper.metricsKey) return false;
        return Objects.equals(placeType, wrapper.placeType);
    }

    @Override
    public int hashCode() {
        int result = metricsKey != null ? metricsKey.hashCode() : 0;
        result = 31 * result + (placeType != null ? placeType.hashCode() : 0);
        return result;
    }
}
