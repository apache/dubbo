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
 * The value corresponding to the placeholder in {@link MetricsKey}
 */
public class MetricsPlaceValue {

    private final String type;
    private final MetricsLevel metricsLevel;

    private MetricsPlaceValue(String type, MetricsLevel metricsLevel) {
        this.type = type;
        this.metricsLevel = metricsLevel;
    }

    public static MetricsPlaceValue of(String type, MetricsLevel metricsLevel) {
        return new MetricsPlaceValue(type, metricsLevel);
    }

    public String getType() {
        return type;
    }

    public MetricsLevel getMetricsLevel() {
        return metricsLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricsPlaceValue that = (MetricsPlaceValue) o;

        if (!type.equals(that.type)) return false;
        return metricsLevel == that.metricsLevel;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + metricsLevel.hashCode();
        return result;
    }
}
