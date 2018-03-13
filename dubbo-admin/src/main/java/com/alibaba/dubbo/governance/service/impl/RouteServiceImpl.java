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
package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.governance.sync.util.Pair;
import com.alibaba.dubbo.governance.sync.util.SyncUtils;
import com.alibaba.dubbo.registry.common.domain.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IbatisRouteService
 *
 */
public class RouteServiceImpl extends AbstractService implements RouteService {

    public void createRoute(Route route) {
        registryService.register(route.toUrl());
    }

    public void updateRoute(Route route) {
        Long id = route.getId();
        if (id == null) {
            throw new IllegalStateException("no route id");
        }
        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }

        registryService.unregister(oldRoute);
        registryService.register(route.toUrl());
    }

    public void deleteRoute(Long id) {
        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        registryService.unregister(oldRoute);
    }

    public void enableRoute(Long id) {
        if (id == null) {
            throw new IllegalStateException("no route id");
        }

        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        if (oldRoute.getParameter("enabled", true)) {
            return;
        }

        registryService.unregister(oldRoute);
        URL newRoute = oldRoute.addParameter("enabled", true);
        registryService.register(newRoute);

    }

    public void disableRoute(Long id) {
        if (id == null) {
            throw new IllegalStateException("no route id");
        }

        URL oldRoute = findRouteUrl(id);
        if (oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        if (!oldRoute.getParameter("enabled", true)) {
            return;
        }

        URL newRoute = oldRoute.addParameter("enabled", false);
        registryService.unregister(oldRoute);
        registryService.register(newRoute);

    }

    public List<Route> findAll() {
        return SyncUtils.url2RouteList(findAllUrl());
    }

    private Map<Long, URL> findAllUrl() {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);

        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public Route findRoute(Long id) {
        return SyncUtils.url2Route(findRouteUrlPair(id));
    }

    public Pair<Long, URL> findRouteUrlPair(Long id) {
        return SyncUtils.filterFromCategory(getRegistryCache(), Constants.ROUTERS_CATEGORY, id);
    }

    private URL findRouteUrl(Long id) {
        return findRoute(id).toUrl();
    }

    private Map<Long, URL> findRouteUrl(String service, String address, boolean force) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);
        if (service != null && service.length() > 0) {
            filter.put(SyncUtils.SERVICE_FILTER_KEY, service);
        }
        if (address != null && address.length() > 0) {
            filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);
        }
        if (force) {
            filter.put("force", "true");
        }
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<Route> findByService(String serviceName) {
        return SyncUtils.url2RouteList(findRouteUrl(serviceName, null, false));
    }

    public List<Route> findByAddress(String address) {
        return SyncUtils.url2RouteList(findRouteUrl(null, address, false));
    }

    public List<Route> findByServiceAndAddress(String service, String address) {
        return SyncUtils.url2RouteList(findRouteUrl(service, address, false));
    }

    public List<Route> findForceRouteByService(String service) {
        return SyncUtils.url2RouteList(findRouteUrl(service, null, true));
    }

    public List<Route> findForceRouteByAddress(String address) {
        return SyncUtils.url2RouteList(findRouteUrl(null, address, true));
    }

    public List<Route> findForceRouteByServiceAndAddress(String service, String address) {
        return SyncUtils.url2RouteList(findRouteUrl(service, address, true));
    }

    public List<Route> findAllForceRoute() {
        return SyncUtils.url2RouteList(findRouteUrl(null, null, true));
    }

}
