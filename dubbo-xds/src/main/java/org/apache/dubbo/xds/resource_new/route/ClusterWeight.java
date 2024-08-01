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
package org.apache.dubbo.xds.resource_new.route;

import org.apache.dubbo.xds.resource_new.filter.FilterConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClusterWeight {

    private final String name;

    private final int weight;

    private final Map<String, FilterConfig> filterConfigOverrides;

    public ClusterWeight(String name, int weight, Map<String, FilterConfig> filterConfigOverrides) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        this.name = name;
        this.weight = weight;
        if (filterConfigOverrides == null) {
            throw new NullPointerException("Null filterConfigOverrides");
        }
        this.filterConfigOverrides = Collections.unmodifiableMap(new HashMap<>(filterConfigOverrides));
    }

    String name() {
        return name;
    }

    int weight() {
        return weight;
    }

    Map<String, FilterConfig> filterConfigOverrides() {
        return filterConfigOverrides;
    }

    public String toString() {
        return "ClusterWeight{" + "name=" + name + ", " + "weight=" + weight + ", " + "filterConfigOverrides="
                + filterConfigOverrides + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof ClusterWeight) {
            ClusterWeight that = (ClusterWeight) o;
            return this.name.equals(that.name())
                    && this.weight == that.weight()
                    && this.filterConfigOverrides.equals(that.filterConfigOverrides());
        }
        return false;
    }

    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= name.hashCode();
        h$ *= 1000003;
        h$ ^= weight;
        h$ *= 1000003;
        h$ ^= filterConfigOverrides.hashCode();
        return h$;
    }
}
