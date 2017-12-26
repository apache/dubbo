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
package com.alibaba.dubbo.governance.service;

import com.alibaba.dubbo.registry.common.domain.Route;

import java.util.List;

/**
 * RouteService
 *
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
