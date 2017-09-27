/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-4-15
 * $Id: RouteService.java 182337 2012-06-27 09:04:15Z tony.chenl $
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.service;

import com.alibaba.dubbo.registry.common.domain.Route;

import java.util.List;

/**
 * RouteService
 *
 * @author william.liangf
 */
public interface RouteService {

    void createRoute(Route route);

    void updateRoute(Route route);

    void deleteRoute(Long id);

    void enableRoute(Long id);

    void disableRoute(Long id);

    Route findRoute(Long id);

    List<Route> findAll();

    List<Route> findByService(String serviceName);

    List<Route> findByAddress(String address);

    List<Route> findByServiceAndAddress(String service, String address);

    List<Route> findForceRouteByService(String service);

    List<Route> findForceRouteByAddress(String address);

    List<Route> findForceRouteByServiceAndAddress(String service, String address);

    List<Route> findAllForceRoute();

}
