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

package org.apache.dubbo.metrics.model.sample;

import org.apache.dubbo.metrics.model.MetricsCategory;

import java.util.Map;
import java.util.Objects;

/**
 * MetricSample.
 */
public class MetricSample {
    private String name;
    private String description;
    private Map<String, String> tags;
    private Type type;
    private MetricsCategory category;
    private String baseUnit;

    public MetricSample(String name, String description, Map<String, String> tags, Type type, MetricsCategory category) {
        this(name, description, tags, type, category, null);
    }

    public MetricSample(String name, String description, Map<String, String> tags, Type type, MetricsCategory category, String baseUnit) {
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.type = type;
        this.category = category;
        this.baseUnit = baseUnit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public MetricsCategory getCategory() {
        return category;
    }

    public void setCategory(MetricsCategory category) {
        this.category = category;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricSample that = (MetricSample) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description)
            && Objects.equals(baseUnit, that.baseUnit) && type == that.type
            && Objects.equals(category, that.category) && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, baseUnit, type, category, tags);
    }

    @Override
    public String toString() {
        return "MetricSample{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", baseUnit='" + baseUnit + '\'' +
            ", type=" + type +
            ", category=" + category +
            ", tags=" + tags +
            '}';
    }


    public enum Type {
        COUNTER,
        GAUGE,
        LONG_TASK_TIMER,
        TIMER,
        DISTRIBUTION_SUMMARY
    }
}
