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
package org.apache.dubbo.registry.xds.util.protocol.message;

import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.Objects;
import java.util.Set;

public class ListenerResult {
    private Set<String> routeConfigNames;

    public ListenerResult() {
        this.routeConfigNames = new ConcurrentHashSet<>();
    }

    public ListenerResult(Set<String> routeConfigNames) {
        this.routeConfigNames = routeConfigNames;
    }

    public Set<String> getRouteConfigNames() {
        return routeConfigNames;
    }

    public void setRouteConfigNames(Set<String> routeConfigNames) {
        this.routeConfigNames = routeConfigNames;
    }

    public void mergeRouteConfigNames(Set<String> names) {
        this.routeConfigNames.addAll(names);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListenerResult listenerResult = (ListenerResult) o;
        return Objects.equals(routeConfigNames, listenerResult.routeConfigNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeConfigNames);
    }

    @Override
    public String toString() {
        return "ListenerResult{" +
            "routeConfigNames=" + routeConfigNames +
            '}';
    }
}
