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
package org.apache.dubbo.xds.resource.route;

import org.apache.dubbo.xds.resource.filter.FilterConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VirtualHost {

    private String name;
    private List<String> domains;
    private List<Route> routes;
    private Map<String, FilterConfig> filterConfigOverrides;

    public VirtualHost(
            String name, List<String> domains, List<Route> routes, Map<String, FilterConfig> filterConfigOverrides) {
        this.name = name;
        this.domains = Collections.unmodifiableList(new ArrayList<>(domains));
        this.routes = Collections.unmodifiableList(new ArrayList<>(routes));
        this.filterConfigOverrides = Collections.unmodifiableMap(new HashMap<>(filterConfigOverrides));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = new ArrayList<>(domains);
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = new ArrayList<>(routes);
    }

    public Map<String, FilterConfig> getFilterConfigOverrides() {
        return filterConfigOverrides;
    }

    public void setFilterConfigOverrides(Map<String, FilterConfig> filterConfigOverrides) {
        this.filterConfigOverrides = new HashMap<>(filterConfigOverrides);
    }

    @Override
    public String toString() {
        return "VirtualHost{" + "name=" + name + ", " + "domains=" + domains + ", " + "routes=" + routes + ", "
                + "filterConfigOverrides=" + filterConfigOverrides + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VirtualHost that = (VirtualHost) o;
        return Objects.equals(name, that.name)
                && Objects.equals(domains, that.domains)
                && Objects.equals(routes, that.routes)
                && Objects.equals(filterConfigOverrides, that.filterConfigOverrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, domains, routes, filterConfigOverrides);
    }

    public static VirtualHost create(
            String name, List<String> domains, List<Route> routes, Map<String, FilterConfig> filterConfigOverrides) {
        return new VirtualHost(name, domains, routes, filterConfigOverrides);
    }
}
