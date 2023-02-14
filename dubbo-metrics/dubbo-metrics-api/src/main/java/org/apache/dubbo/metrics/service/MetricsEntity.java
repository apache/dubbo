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

package org.apache.dubbo.metrics.service;

import org.apache.dubbo.metrics.model.MetricsCategory;

import java.util.Map;
import java.util.Objects;

/**
 * Metrics response entity.
 */
public class MetricsEntity {

    private String name;
    private Map<String, String> tags;
    private MetricsCategory category;
    private Object value;

    public MetricsEntity() {

    }

    public MetricsEntity(String name, Map<String, String> tags, MetricsCategory category, Object value) {
        this.name = name;
        this.tags = tags;
        this.category = category;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public MetricsCategory getCategory() {
        return category;
    }

    public void setCategory(MetricsCategory category) {
        this.category = category;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricsEntity entity = (MetricsEntity) o;
        return Objects.equals(name, entity.name) && Objects.equals(tags, entity.tags)
            && Objects.equals(category, entity.category) && Objects.equals(value, entity.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags, category, value);
    }
}
