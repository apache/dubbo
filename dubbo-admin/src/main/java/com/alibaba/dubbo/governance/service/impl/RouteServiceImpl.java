/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-4-15
 * $Id: RouteServiceImpl.java 182851 2012-06-28 09:39:16Z tony.chenl $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.governance.sync.RegistryServerSync.Pair;
import com.alibaba.dubbo.governance.sync.util.SyncUtils;
import com.alibaba.dubbo.registry.common.domain.Route;

/**
 * IbatisRouteService
 * 
 * @author william.liangf
 */
public class RouteServiceImpl extends AbstractService implements RouteService {

    public void createRoute(Route route) {
        registryService.register(route.toUrl());
    }

    public void updateRoute(Route route) {
        Long id = route.getId();
        if(id == null) {
            throw new IllegalStateException("no route id");
        }
        URL oldRoute = findRouteUrl(id);
        if(oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        
        registryService.unregister(oldRoute);
        registryService.register(route.toUrl());
    }

    public void deleteRoute(Long id) {
        URL oldRoute = findRouteUrl(id);
        if(oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        registryService.unregister(oldRoute);
    }

    public void enableRoute(Long id) {
        if(id == null) {
            throw new IllegalStateException("no route id");
        }
        
        URL oldRoute = findRouteUrl(id);
        if(oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        if(oldRoute.getParameter("enabled", true)) {
            return;
        }

        URL newRoute = oldRoute.removeParameter("enabled");
        registryService.unregister(oldRoute);
        registryService.register(newRoute);
        
    }

    public void disableRoute(Long id) {
        if(id == null) {
            throw new IllegalStateException("no route id");
        }
        
        URL oldRoute = findRouteUrl(id);
        if(oldRoute == null) {
            throw new IllegalStateException("Route was changed!");
        }
        if(!oldRoute.getParameter("enabled", true)) {
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

    public List<Route> findByService(String serviceName) {
        return SyncUtils.url2RouteList(findRouteUrlByService(serviceName));
    }
    
    private Map<Long, URL> findRouteUrlByService(String service) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);
        
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<Route> findByAddress(String address) {
        return SyncUtils.url2RouteList(findRouteUrlByAddress(address));
    }
    
    private Map<Long, URL> findRouteUrlByAddress(String address) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);
        
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public Route findRoute(Long id) {
        return SyncUtils.url2Route(findRouteUrlPair(id));
    }
    
    public Pair<Long, URL> findRouteUrlPair(Long id) {
        return SyncUtils.filterFromCategory(getRegistryCache(), Constants.ROUTERS_CATEGORY, id);
    }
    
    private URL findRouteUrl(Long id){
        return findRoute(id).toUrl();
    }
    
    private Map<Long, URL> findForceRouteUrlByService(String service) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);
        filter.put("force", "true");
        
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<Route> findForceRouteByService(String service) {
        return SyncUtils.url2RouteList(findForceRouteUrlByService(service));
    }
    
    private Map<Long, URL> findForceRouteUrlByAddress(String address) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);
        filter.put("force", "true");
        
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<Route> findForceRouteByAddress(String service) {
        return SyncUtils.url2RouteList(findForceRouteUrlByAddress(service));
    }

    private Map<Long, URL> findAllForceUrl() {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.ROUTERS_CATEGORY);
        filter.put("force", "true");
        
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }
    
    public List<Route> findAllForceRoute() {
        return SyncUtils.url2RouteList(findAllForceUrl());
    }

}
