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
package org.apache.dubbo.rpc.cluster.router.xds;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
import org.apache.dubbo.rpc.cluster.router.xds.rule.XdsRouteRule;

import io.envoyproxy.envoy.config.route.v3.VirtualHost;

public class RdsRouteRuleManager {


    private static final ConcurrentHashMap<String, Set<XdsRouteRuleListener>> RULE_LISTENERS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, List<XdsRouteRule>> ROUTE_DATA_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Consumer<Map<String, RouteResult>>> RDS_LISTENERS = new ConcurrentHashMap<>();

    public RdsRouteRuleManager() {
    }

    public synchronized void subscribeRds(String domain, XdsRouteRuleListener listener) {

        Set<XdsRouteRuleListener> listeners = RULE_LISTENERS.computeIfAbsent(domain, key ->
            new ConcurrentHashSet<>()
        );
        if (CollectionUtils.isEmpty(listeners)) {
            doSubscribeRds(domain);
        }
        listeners.add(listener);

        if (ROUTE_DATA_CACHE.containsKey(domain)) {
            listener.onRuleChange(domain, ROUTE_DATA_CACHE.get(domain));
        }
    }

    private void doSubscribeRds(String domain) {
        RDS_LISTENERS.computeIfAbsent(domain, key -> notify -> {
            List<VirtualHost> virtualHosts = notify.values()
                .stream()
                .map(r -> r.searchVirtualHost(domain))
                .collect(Collectors.toList());
            new RdsVirtualHostListener(domain, this).parseVirtualHost(virtualHosts.size() > 0 ? virtualHosts.get(0) : null);
        });
        Consumer<Map<String, RouteResult>> consumer = RDS_LISTENERS.get(domain);
        if (PilotExchanger.isEnabled()) {
           PilotExchanger.getInstance().observeRds(Collections.singleton(domain), consumer);
        }
    }

    public synchronized void unSubscribeRds(String domain, XdsRouteRuleListener listener) {
        Set<XdsRouteRuleListener> listeners = RULE_LISTENERS.get(domain);
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            RULE_LISTENERS.remove(domain);
            doUnsubscribeRds(domain);
        }
    }

    private void doUnsubscribeRds(String domain) {
        Consumer<Map<String, RouteResult>> consumer = RDS_LISTENERS.remove(domain);

        if (consumer != null && PilotExchanger.isEnabled()) {
            PilotExchanger.getInstance().unObserveRds(Collections.singleton(domain), consumer);
        }
        ROUTE_DATA_CACHE.remove(domain);
    }


    public void notifyRuleChange(String domain, List<XdsRouteRule> xdsRouteRules) {

        ROUTE_DATA_CACHE.put(domain, xdsRouteRules);

        Set<XdsRouteRuleListener> listeners = RULE_LISTENERS.get(domain);
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        boolean empty = CollectionUtils.isEmpty(xdsRouteRules);
        for (XdsRouteRuleListener listener : listeners) {
            if (empty) {
                listener.clearRule(domain);
            } else {
                listener.onRuleChange(domain, xdsRouteRules);
            }
        }
    }

    // for test
    static ConcurrentHashMap<String, Set<XdsRouteRuleListener>> getRuleListeners() {
        return RULE_LISTENERS;
    }

    // for test
    static ConcurrentHashMap<String, List<XdsRouteRule>> getRouteDataCache() {
        return ROUTE_DATA_CACHE;
    }

    // for test
    static ConcurrentHashMap<String, Consumer<Map<String, RouteResult>>> getRdsListeners() {
        return RDS_LISTENERS;
    }

}
