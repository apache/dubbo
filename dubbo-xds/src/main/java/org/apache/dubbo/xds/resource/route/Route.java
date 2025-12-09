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

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.filter.FilterConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Route {

    private final RouteMatch routeMatch;

    @Nullable
    private final RouteAction routeAction;

    private final Map<String, FilterConfig> filterConfigOverrides;

    public static Route forAction(
            RouteMatch routeMatch, RouteAction routeAction, Map<String, FilterConfig> filterConfigOverrides) {
        return create(routeMatch, routeAction, filterConfigOverrides);
    }

    public static Route forNonForwardingAction(RouteMatch routeMatch, Map<String, FilterConfig> filterConfigOverrides) {
        return create(routeMatch, null, filterConfigOverrides);
    }

    public static Route create(
            RouteMatch routeMatch, @Nullable RouteAction routeAction, Map<String, FilterConfig> filterConfigOverrides) {
        return new Route(routeMatch, routeAction, filterConfigOverrides);
    }

    Route(RouteMatch routeMatch, @Nullable RouteAction routeAction, Map<String, FilterConfig> filterConfigOverrides) {
        if (routeMatch == null) {
            throw new NullPointerException("Null routeMatch");
        }
        this.routeMatch = routeMatch;
        this.routeAction = routeAction;
        if (filterConfigOverrides == null) {
            throw new NullPointerException("Null filterConfigOverrides");
        }
        this.filterConfigOverrides = Collections.unmodifiableMap(new HashMap<>(filterConfigOverrides));
    }

    public RouteMatch getRouteMatch() {
        return routeMatch;
    }

    @Nullable
    public RouteAction getRouteAction() {
        return routeAction;
    }

    public Map<String, FilterConfig> getFilterConfigOverrides() {
        return filterConfigOverrides;
    }

    public String toString() {
        return "Route{" + "routeMatch=" + routeMatch + ", " + "routeAction=" + routeAction + ", "
                + "filterConfigOverrides=" + filterConfigOverrides + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Route) {
            Route that = (Route) o;
            return this.routeMatch.equals(that.getRouteMatch())
                    && (this.routeAction == null
                            ? that.getRouteAction() == null
                            : this.routeAction.equals(that.getRouteAction()))
                    && this.filterConfigOverrides.equals(that.getFilterConfigOverrides());
        }
        return false;
    }

    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= routeMatch.hashCode();
        h$ *= 1000003;
        h$ ^= (routeAction == null) ? 0 : routeAction.hashCode();
        h$ *= 1000003;
        h$ ^= filterConfigOverrides.hashCode();
        return h$;
    }
}
