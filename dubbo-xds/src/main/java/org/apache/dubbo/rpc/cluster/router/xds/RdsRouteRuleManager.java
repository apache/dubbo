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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
import org.apache.dubbo.rpc.cluster.router.xds.rule.XdsRouteRule;

import io.envoyproxy.envoy.config.route.v3.VirtualHost;

public class RdsRouteRuleManager {


    private static final ConcurrentHashMap<String, Set<XdsRouteRuleListener>> RULE_LISTENERS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, List<XdsRouteRule>> ROUTE_DATA_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, RdsVirtualHostListener> RDS_LISTENERS = new ConcurrentHashMap<>();

    private static volatile Consumer<Map<String, ListenerResult>> LDS_LISTENER;

    private static volatile Consumer<Map<String, RouteResult>> RDS_LISTENER;

    private static Map<String, RouteResult> RDS_RESULT;

    public RdsRouteRuleManager() {
    }

    public synchronized void subscribeRds(String domain, XdsRouteRuleListener listener) {

        Set<XdsRouteRuleListener> listeners = ConcurrentHashMapUtils.computeIfAbsent(RULE_LISTENERS, domain, key ->
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
        synchronized (RdsRouteRuleManager.class) {
            if (RDS_LISTENER == null) {
                RDS_LISTENER = rds -> {
                    if (rds == null) {
                        return;
                    }
                    for (RouteResult routeResult : rds.values()) {
                        for (String domainToNotify : RDS_LISTENERS.keySet()) {
                            VirtualHost virtualHost = routeResult.searchVirtualHost(domainToNotify);
                            if (virtualHost != null) {
                                RDS_LISTENERS.get(domainToNotify).parseVirtualHost(virtualHost);
                            }
                        }
                    }
                    RDS_RESULT = rds;
                };
            }
            if (LDS_LISTENER == null) {
                LDS_LISTENER = new Consumer<Map<String, ListenerResult>>() {
                    private volatile Set<String> configNames = null;

                    @Override
                    public void accept(Map<String, ListenerResult> listenerResults) {
                        if (listenerResults.size() == 1) {
                            for (ListenerResult listenerResult : listenerResults.values()) {
                                Set<String> newConfigNames = listenerResult.getRouteConfigNames();
                                if (configNames == null) {
                                    PilotExchanger.getInstance().observeRds(newConfigNames, RDS_LISTENER);
                                } else if (!configNames.equals(newConfigNames)) {
                                    PilotExchanger.getInstance().unObserveRds(configNames, RDS_LISTENER);
                                    PilotExchanger.getInstance().observeRds(newConfigNames, RDS_LISTENER);
                                }
                                configNames = newConfigNames;
                            }
                        }
                    }
                };
                if (PilotExchanger.isEnabled()) {
                    PilotExchanger.getInstance().observeLds(LDS_LISTENER);
                }
            }
        }
        ConcurrentHashMapUtils.computeIfAbsent(RDS_LISTENERS, domain, key -> new RdsVirtualHostListener(domain, this));
        RDS_LISTENER.accept(RDS_RESULT);
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
        RDS_LISTENERS.remove(domain);
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
    static Map<String, RdsVirtualHostListener> getRdsListeners() {
        return RDS_LISTENERS;
    }

}
