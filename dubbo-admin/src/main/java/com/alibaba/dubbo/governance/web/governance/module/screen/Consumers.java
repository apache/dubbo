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
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Consumer;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.domain.Route;
import com.alibaba.dubbo.registry.common.route.OverrideUtils;
import com.alibaba.dubbo.registry.common.route.RouteRule;
import com.alibaba.dubbo.registry.common.route.RouteRule.MatchPair;
import com.alibaba.dubbo.registry.common.route.RouteUtils;
import com.alibaba.dubbo.registry.common.util.Tool;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consumers. URI: /services/$service/consumers
 *
 */
public class Consumers extends Restful {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private OverrideService overrideService;

    @Autowired
    private RouteService routeService;

    public void index(Map<String, Object> context) throws Exception {
        String service = (String) context.get("service");
        String application = (String) context.get("application");
        String address = (String) context.get("address");
        List<Consumer> consumers;
        List<Override> overrides;
        List<Provider> providers = null;
        List<Route> routes = null;
        // service
        if (service != null && service.length() > 0) {
            consumers = consumerService.findByService(service);
            overrides = overrideService.findByService(service);
            providers = providerService.findByService(service);
            routes = routeService.findByService(service);
        }
        // address
        else if (address != null && address.length() > 0) {
            consumers = consumerService.findByAddress(address);
            overrides = overrideService.findByAddress(Tool.getIP(address));
        }
        // application
        else if (application != null && application.length() > 0) {
            consumers = consumerService.findByApplication(application);
            overrides = overrideService.findByApplication(application);
        }
        // all
        else {
            consumers = consumerService.findAll();
            overrides = overrideService.findAll();
        }
        if (consumers != null && consumers.size() > 0) {
            for (Consumer consumer : consumers) {
                if (service == null || service.length() == 0) {
                    providers = providerService.findByService(consumer.getService());
                    routes = routeService.findByService(consumer.getService());
                }
                List<Route> routed = new ArrayList<Route>();
                consumer.setProviders(RouteUtils.route(consumer.getService(), consumer.getAddress(), consumer.getParameters(), providers, overrides, routes, null, routed));
                consumer.setRoutes(routed);
                OverrideUtils.setConsumerOverrides(consumer, overrides);
            }
        }
        context.put("consumers", consumers);
    }

    public void show(Long id, Map<String, Object> context) {
        Consumer consumer = consumerService.findConsumer(id);
        List<Provider> providers = providerService.findByService(consumer.getService());
        List<Route> routes = routeService.findByService(consumer.getService());
        List<Override> overrides = overrideService.findByService(consumer.getService());
        List<Route> routed = new ArrayList<Route>();
        consumer.setProviders(RouteUtils.route(consumer.getService(), consumer.getAddress(), consumer.getParameters(), providers, overrides, routes, null, routed));
        consumer.setRoutes(routed);
        OverrideUtils.setConsumerOverrides(consumer, overrides);
        context.put("consumer", consumer);
        context.put("providers", consumer.getProviders());
        context.put("routes", consumer.getRoutes());
        context.put("overrides", consumer.getOverrides());
    }

    public void edit(Long id, Map<String, Object> context) {
        show(id, context);
    }

    public boolean update(Consumer newConsumer, Map<String, Object> context) {
        Long id = newConsumer.getId();
        String parameters = newConsumer.getParameters();
        Consumer consumer = consumerService.findConsumer(id);
        if (consumer == null) {
            context.put("message", getMessage("NoSuchOperationData", id));
            return false;
        }
        String service = consumer.getService();
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        Map<String, String> oldMap = StringUtils.parseQueryString(consumer.getParameters());
        Map<String, String> newMap = StringUtils.parseQueryString(parameters);
        for (Map.Entry<String, String> entry : oldMap.entrySet()) {
            if (entry.getValue().equals(newMap.get(entry.getKey()))) {
                newMap.remove(entry.getKey());
            }
        }
        String address = consumer.getAddress();
        List<Override> overrides = overrideService.findByServiceAndAddress(consumer.getService(), consumer.getAddress());
        OverrideUtils.setConsumerOverrides(consumer, overrides);
        Override override = consumer.getOverride();
        if (override != null) {
            if (newMap.size() > 0) {
                override.setParams(StringUtils.toQueryString(newMap));
                override.setEnabled(true);
                override.setOperator(operator);
                override.setOperatorAddress(operatorAddress);
                overrideService.updateOverride(override);
            } else {
                overrideService.deleteOverride(override.getId());
            }
        } else {
            override = new Override();
            override.setService(service);
            override.setAddress(address);
            override.setParams(StringUtils.toQueryString(newMap));
            override.setEnabled(true);
            override.setOperator(operator);
            override.setOperatorAddress(operatorAddress);
            overrideService.saveOverride(override);
        }
        return true;
    }

    public void routed(Long id, Map<String, Object> context) {
        show(id, context);
    }

    public void notified(Long id, Map<String, Object> context) {
        show(id, context);
    }

    public void overrided(Long id, Map<String, Object> context) {
        show(id, context);
    }

    public boolean shield(Long[] ids, Map<String, Object> context) throws Exception {
        return mock(ids, context, "force:return null");
    }

    public boolean tolerant(Long[] ids, Map<String, Object> context) throws Exception {
        return mock(ids, context, "fail:return null");
    }

    public boolean recover(Long[] ids, Map<String, Object> context) throws Exception {
        return mock(ids, context, "");
    }

    private boolean mock(Long[] ids, Map<String, Object> context, String mock) throws Exception {
        if (ids == null || ids.length == 0) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        List<Consumer> consumers = new ArrayList<Consumer>();
        for (Long id : ids) {
            Consumer c = consumerService.findConsumer(id);
            if (c != null) {
                consumers.add(c);
                if (!super.currentUser.hasServicePrivilege(c.getService())) {
                    context.put("message", getMessage("HaveNoServicePrivilege", c.getService()));
                    return false;
                }
            }
        }
        for (Consumer consumer : consumers) {
            String service = consumer.getService();
            String address = Tool.getIP(consumer.getAddress());
            List<Override> overrides = overrideService.findByServiceAndAddress(service, address);
            if (overrides != null && overrides.size() > 0) {
                for (Override override : overrides) {
                    Map<String, String> map = StringUtils.parseQueryString(override.getParams());
                    if (mock == null || mock.length() == 0) {
                        map.remove("mock");
                    } else {
                        map.put("mock", URL.encode(mock));
                    }
                    if (map.size() > 0) {
                        override.setParams(StringUtils.toQueryString(map));
                        override.setEnabled(true);
                        override.setOperator(operator);
                        override.setOperatorAddress(operatorAddress);
                        overrideService.updateOverride(override);
                    } else {
                        overrideService.deleteOverride(override.getId());
                    }
                }
            } else if (mock != null && mock.length() > 0) {
                Override override = new Override();
                override.setService(service);
                override.setAddress(address);
                override.setParams("mock=" + URL.encode(mock));
                override.setEnabled(true);
                override.setOperator(operator);
                override.setOperatorAddress(operatorAddress);
                overrideService.saveOverride(override);
            }
        }
        return true;
    }

    public boolean allshield(Map<String, Object> context) throws Exception {
        return allmock(context, "force:return null");
    }

    public boolean alltolerant(Map<String, Object> context) throws Exception {
        return allmock(context, "fail:return null");
    }

    public boolean allrecover(Map<String, Object> context) throws Exception {
        return allmock(context, "");
    }

    private boolean allmock(Map<String, Object> context, String mock) throws Exception {
        String service = (String) context.get("service");
        if (service == null || service.length() == 0) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        List<Override> overrides = overrideService.findByService(service);
        Override allOverride = null;
        if (overrides != null && overrides.size() > 0) {
            for (Override override : overrides) {
                if (override.isDefault()) {
                    allOverride = override;
                    break;
                }
            }
        }
        if (allOverride != null) {
            Map<String, String> map = StringUtils.parseQueryString(allOverride.getParams());
            if (mock == null || mock.length() == 0) {
                map.remove("mock");
            } else {
                map.put("mock", URL.encode(mock));
            }
            if (map.size() > 0) {
                allOverride.setParams(StringUtils.toQueryString(map));
                allOverride.setEnabled(true);
                allOverride.setOperator(operator);
                allOverride.setOperatorAddress(operatorAddress);
                overrideService.updateOverride(allOverride);
            } else {
                overrideService.deleteOverride(allOverride.getId());
            }
        } else if (mock != null && mock.length() > 0) {
            Override override = new Override();
            override.setService(service);
            override.setParams("mock=" + URL.encode(mock));
            override.setEnabled(true);
            override.setOperator(operator);
            override.setOperatorAddress(operatorAddress);
            overrideService.saveOverride(override);
        }
        return true;
    }

    public boolean allow(Long[] ids, Map<String, Object> context) throws Exception {
        return access(ids, context, true, false);
    }

    public boolean forbid(Long[] ids, Map<String, Object> context) throws Exception {
        return access(ids, context, false, false);
    }

    public boolean onlyallow(Long[] ids, Map<String, Object> context) throws Exception {
        return access(ids, context, true, true);
    }

    public boolean onlyforbid(Long[] ids, Map<String, Object> context) throws Exception {
        return access(ids, context, false, true);
    }

    private boolean access(Long[] ids, Map<String, Object> context, boolean allow, boolean only) throws Exception {
        if (ids == null || ids.length == 0) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        List<Consumer> consumers = new ArrayList<Consumer>();
        for (Long id : ids) {
            Consumer c = consumerService.findConsumer(id);
            if (c != null) {
                consumers.add(c);
                if (!super.currentUser.hasServicePrivilege(c.getService())) {
                    context.put("message", getMessage("HaveNoServicePrivilege", c.getService()));
                    return false;
                }
            }
        }
        Map<String, Set<String>> serviceAddresses = new HashMap<String, Set<String>>();
        for (Consumer consumer : consumers) {
            String service = consumer.getService();
            String address = Tool.getIP(consumer.getAddress());
            Set<String> addresses = serviceAddresses.get(service);
            if (addresses == null) {
                addresses = new HashSet<String>();
                serviceAddresses.put(service, addresses);
            }
            addresses.add(address);
        }
        for (Map.Entry<String, Set<String>> entry : serviceAddresses.entrySet()) {
            String service = entry.getKey();
            boolean isFirst = false;
            List<Route> routes = routeService.findForceRouteByService(service);
            Route route = null;
            if (routes == null || routes.size() == 0) {
                isFirst = true;
                route = new Route();
                route.setService(service);
                route.setForce(true);
                route.setName(service + " blackwhitelist");
                route.setFilterRule("false");
                route.setEnabled(true);
            } else {
                route = routes.get(0);
            }
            Map<String, MatchPair> when = null;
            MatchPair matchPair = null;
            if (isFirst) {
                when = new HashMap<String, MatchPair>();
                matchPair = new MatchPair(new HashSet<String>(), new HashSet<String>());
                when.put("consumer.host", matchPair);
            } else {
                when = RouteRule.parseRule(route.getMatchRule());
                matchPair = when.get("consumer.host");
            }
            if (only) {
                matchPair.getUnmatches().clear();
                matchPair.getMatches().clear();
                if (allow) {
                    matchPair.getUnmatches().addAll(entry.getValue());
                } else {
                    matchPair.getMatches().addAll(entry.getValue());
                }
            } else {
                for (String consumerAddress : entry.getValue()) {
                    if (matchPair.getUnmatches().size() > 0) { // whitelist take effect
                        matchPair.getMatches().remove(consumerAddress); // remove data in blacklist
                        if (allow) { // if allowed
                            matchPair.getUnmatches().add(consumerAddress); // add to whitelist
                        } else { // if not allowed
                            matchPair.getUnmatches().remove(consumerAddress); // remove from whitelist
                        }
                    } else { // blacklist take effect
                        if (allow) { // if allowed
                            matchPair.getMatches().remove(consumerAddress); // remove from blacklist
                        } else { // if not allowed
                            matchPair.getMatches().add(consumerAddress); // add to blacklist
                        }
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            RouteRule.contidionToString(sb, when);
            route.setMatchRule(sb.toString());
            route.setUsername(operator);
            if (matchPair.getMatches().size() > 0 || matchPair.getUnmatches().size() > 0) {
                if (isFirst) {
                    routeService.createRoute(route);
                } else {
                    routeService.updateRoute(route);
                }
            } else if (!isFirst) {
                routeService.deleteRoute(route.getId());
            }
        }
        return true;
    }
}
