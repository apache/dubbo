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

import java.util.Objects;

public class BaseMetric {
    private String name;
    private String description;
    private String baseUnit;

    public BaseMetric() {

    }

    public BaseMetric(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public BaseMetric(String name, String description, String baseUnit) {
        this.name = name;
        this.description = description;
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
        BaseMetric that = (BaseMetric) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(baseUnit, that.baseUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, baseUnit);
    }

    @Override
    public String toString() {
        return "BaseMetric{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", baseUnit='" + baseUnit + '\'' +
            '}';
    }
}
