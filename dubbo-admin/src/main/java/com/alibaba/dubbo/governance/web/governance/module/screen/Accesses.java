/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Access;
import com.alibaba.dubbo.registry.common.domain.Route;
import com.alibaba.dubbo.registry.common.route.RouteRule;
import com.alibaba.dubbo.registry.common.route.RouteRule.MatchPair;
import com.alibaba.dubbo.registry.common.util.Tool;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Providers. URI: /services/$service/accesses
 *
 * @author william.liangf
 * @author ding.lid
 * @author tony.chenl
 */
public class Accesses extends Restful {

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");
    private static final Pattern ALL_IP_PATTERN = Pattern.compile("0{1,3}(\\.0{1,3}){3}$");
    @Autowired
    private RouteService routeService;
    @Autowired
    private ProviderService providerService;

    public void index(Map<String, Object> context) throws Exception {
        String service = (String) context.get("service");
        String address = (String) context.get("address");
        address = Tool.getIP(address);
        List<Route> routes;
        if (service != null && service.length() > 0) {
            routes = routeService.findForceRouteByService(service);
        } else if (address != null && address.length() > 0) {
            routes = routeService.findForceRouteByAddress(address);
        } else {
            routes = routeService.findAllForceRoute();
        }
        List<Access> accesses = new ArrayList<Access>();
        if (routes == null) {
            context.put("accesses", accesses);
            return;
        }
        for (Route route : routes) {
            Map<String, MatchPair> rule = RouteRule.parseRule(route.getMatchRule());
            MatchPair pair = rule.get("consumer.host");
            if (pair != null) {
                for (String host : pair.getMatches()) {
                    Access access = new Access();
                    access.setAddress(host);
                    access.setService(route.getService());
                    access.setAllow(false);
                    accesses.add(access);
                }
                for (String host : pair.getUnmatches()) {
                    Access access = new Access();
                    access.setAddress(host);
                    access.setService(route.getService());
                    access.setAllow(true);
                    accesses.add(access);
                }
            }
        }
        context.put("accesses", accesses);
    }

    public void add(Map<String, Object> context) {
        List<String> serviceList = Tool.sortSimpleName(providerService.findServices());
        context.put("serviceList", serviceList);
    }

    public boolean create(Map<String, Object> context) throws Exception {
        String addr = (String) context.get("consumerAddress");
        String services = (String) context.get("service");
        Set<String> consumerAddresses = toAddr(addr);
        Set<String> aimServices = toService(services);
        for (String aimService : aimServices) {
            boolean isFirst = false;
            List<Route> routes = routeService.findForceRouteByService(aimService);
            Route route = null;
            if (routes == null || routes.size() == 0) {
                isFirst = true;
                route = new Route();
                route.setService(aimService);
                route.setForce(true);
                route.setName(aimService + " blackwhitelist");
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
            for (String consumerAddress : consumerAddresses) {
                if (Boolean.valueOf((String) context.get("allow"))) {
                    matchPair.getUnmatches().add(Tool.getIP(consumerAddress));

                } else {
                    matchPair.getMatches().add(Tool.getIP(consumerAddress));
                }
            }
            StringBuilder sb = new StringBuilder();
            RouteRule.contidionToString(sb, when);
            route.setMatchRule(sb.toString());
            route.setUsername(operator);
            if (isFirst) {
                routeService.createRoute(route);
            } else {
                routeService.updateRoute(route);
            }

        }
        return true;
    }

    private Set<String> toAddr(String addr) throws IOException {
        Set<String> consumerAddresses = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new StringReader(addr));
        while (true) {
            String line = reader.readLine();
            if (null == line)
                break;

            String[] split = line.split("[\\s,;]+");
            for (String s : split) {
                if (s.length() == 0)
                    continue;
                if (!IP_PATTERN.matcher(s).matches()) {
                    throw new IllegalStateException("illegal IP: " + s);
                }
                if (LOCAL_IP_PATTERN.matcher(s).matches() || ALL_IP_PATTERN.matcher(s).matches()) {
                    throw new IllegalStateException("local IP or any host ip is illegal: " + s);
                }

                consumerAddresses.add(s);
            }
        }
        return consumerAddresses;
    }

    private Set<String> toService(String services) throws IOException {
        Set<String> aimServices = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new StringReader(services));
        while (true) {
            String line = reader.readLine();
            if (null == line)
                break;

            String[] split = line.split("[\\s,;]+");
            for (String s : split) {
                if (s.length() == 0)
                    continue;
                aimServices.add(s);
            }
        }
        return aimServices;
    }

    /**
     * 删除动作
     *
     * @throws ParseException
     */
    public boolean delete(Map<String, Object> context) throws ParseException {
        String accesses = (String) context.get("accesses");
        String[] temp = accesses.split(" ");
        Map<String, Set<String>> prepareToDeleate = new HashMap<String, Set<String>>();
        for (String s : temp) {
            String service = s.split("=")[0];
            String address = s.split("=")[1];
            Set<String> addresses = prepareToDeleate.get(service);
            if (addresses == null) {
                prepareToDeleate.put(service, new HashSet<String>());
                addresses = prepareToDeleate.get(service);
            }
            addresses.add(address);
        }
        for (Entry<String, Set<String>> entry : prepareToDeleate.entrySet()) {

            String service = entry.getKey();
            List<Route> routes = routeService.findForceRouteByService(service);
            if (routes == null || routes.size() == 0) {
                continue;
            }
            for (Route blackwhitelist : routes) {
                MatchPair pairs = RouteRule.parseRule(blackwhitelist.getMatchRule()).get("consumer.host");
                Set<String> matches = new HashSet<String>();
                matches.addAll(pairs.getMatches());
                Set<String> unmatches = new HashSet<String>();
                unmatches.addAll(pairs.getUnmatches());
                for (String pair : pairs.getMatches()) {
                    for (String address : entry.getValue()) {
                        if (pair.equals(address)) {
                            matches.remove(pair);
                            break;
                        }
                    }
                }
                for (String pair : pairs.getUnmatches()) {
                    for (String address : entry.getValue()) {
                        if (pair.equals(address)) {
                            unmatches.remove(pair);
                            break;
                        }
                    }
                }
                if (matches.size() == 0 && unmatches.size() == 0) {
                    routeService.deleteRoute(blackwhitelist.getId());
                } else {
                    Map<String, MatchPair> condition = new HashMap<String, MatchPair>();
                    condition.put("consumer.host", new MatchPair(matches, unmatches));
                    StringBuilder sb = new StringBuilder();
                    RouteRule.contidionToString(sb, condition);
                    blackwhitelist.setMatchRule(sb.toString());
                    routeService.updateRoute(blackwhitelist);
                }
            }

        }
        return true;
    }

    public void show(Map<String, Object> context) {
    }

    public void edit(Map<String, Object> context) {
    }

    public String update(Map<String, Object> context) {
        return null;
    }

}
