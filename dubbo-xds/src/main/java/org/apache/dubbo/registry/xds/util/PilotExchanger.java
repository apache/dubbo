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
package org.apache.dubbo.registry.xds.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.xds.util.protocol.AbstractProtocol;
import org.apache.dubbo.registry.xds.util.protocol.impl.EdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.impl.LdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.impl.RdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.cluster.router.xds.RdsVirtualHostListener;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PilotExchanger {

    protected final XdsChannel xdsChannel;

    protected final LdsProtocol ldsProtocol;

    protected final RdsProtocol rdsProtocol;

    protected final EdsProtocol edsProtocol;

    protected Map<String, ListenerResult> listenerResult;

    protected Map<String, RouteResult> routeResult;

    private final AtomicBoolean isRdsObserve = new AtomicBoolean(false);
    private final HashSet<String> domainObserveRequest = new HashSet<>();

    private final Map<String, Set<Consumer<Set<Endpoint>>>> domainObserveConsumer = new ConcurrentHashMap<>();

    private final Map<String, Consumer<RdsVirtualHostListener>> rdsObserveConsumer = new ConcurrentHashMap<>();

    private static  PilotExchanger GLOBAL_PILOTEXCHANGER = null;

    protected PilotExchanger(URL url) {
        xdsChannel = new XdsChannel(url);
        int pollingTimeout = url.getParameter("pollingTimeout", 10);
        ApplicationModel applicationModel = url.getOrDefaultApplicationModel();
        this.ldsProtocol = new LdsProtocol(xdsChannel, NodeBuilder.build(), pollingTimeout, applicationModel);
        this.rdsProtocol = new RdsProtocol(xdsChannel, NodeBuilder.build(), pollingTimeout, applicationModel);
        this.edsProtocol = new EdsProtocol(xdsChannel, NodeBuilder.build(), pollingTimeout, applicationModel);

        this.listenerResult = ldsProtocol.getListeners();
        this.routeResult = rdsProtocol.getResource(listenerResult.values().iterator().next().getRouteConfigNames());

        Set<String> ldsResourcesName = new HashSet<>();
        ldsResourcesName.add(AbstractProtocol.emptyResourceName);
        // Observer RDS update
        if (CollectionUtils.isNotEmpty(listenerResult.values().iterator().next().getRouteConfigNames())) {
            createRouteObserve();
            isRdsObserve.set(true);
        }

        // Observe LDS updated
        ldsProtocol.observeResource(ldsResourcesName,(newListener) -> {
            // update local cache
            if (!newListener.equals(listenerResult)) {
                this.listenerResult = newListener;
                // update RDS observation
                if (isRdsObserve.get()) {
                    createRouteObserve();
                }
            }
        }, false);
    }

    private void createRouteObserve() {
        rdsProtocol.observeResource(listenerResult.values().iterator().next().getRouteConfigNames(), (newResult) -> {
            // check if observed domain update ( will update endpoint observation )
            domainObserveConsumer.forEach((domain, consumer) -> {
                newResult.values().forEach(o -> {
                    Set<String> newRoute = o.searchDomain(domain);
                    for (Map.Entry<String, RouteResult> entry: routeResult.entrySet()) {
                        if (!entry.getValue().searchDomain(domain).equals(newRoute)) {
                            // routers in observed domain has been updated
//                    Long domainRequest = domainObserveRequest.get(domain);
                            // router list is empty when observeEndpoints() called and domainRequest has not been created yet
                            // create new observation
                            doObserveEndpoints(domain);
                        }
                    }
                });
            });
            routeResult = newResult;
        }, false);
    }

    public static PilotExchanger initialize(URL url) {
        if (GLOBAL_PILOTEXCHANGER != null) {
            return GLOBAL_PILOTEXCHANGER;
        }
        return (GLOBAL_PILOTEXCHANGER = new PilotExchanger(url));
    }

    public void destroy() {
        xdsChannel.destroy();
    }

    public Set<String> getServices() {
        Set<String> domains = new HashSet<>();
        for (Map.Entry<String, RouteResult> entry: routeResult.entrySet()) {
            domains.addAll(entry.getValue().getDomains());
        }
        return domains;
    }

    public Set<Endpoint> getEndpoints(String domain) {
        Set<Endpoint> endpoints = new HashSet<>();
        for (Map.Entry<String, RouteResult> entry: routeResult.entrySet()) {
            Set<String> cluster = entry.getValue().searchDomain(domain);
            if (CollectionUtils.isNotEmpty(cluster)) {
                Map<String, EndpointResult> endpointResultList = edsProtocol.getResource(cluster);
                endpointResultList.forEach((k, v) -> endpoints.addAll(v.getEndpoints()));
            } else {
                return Collections.emptySet();
            }
        }
        return endpoints;
    }

    public void observeEndpoints(String domain, Consumer<Set<Endpoint>> consumer) {
        // store Consumer
        domainObserveConsumer.compute(domain, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashSet<>();
            }
            // support multi-consumer
            v.add(consumer);
            return v;
        });
        if (!domainObserveRequest.contains(domain)) {
            doObserveEndpoints(domain);
        }
    }

    private void doObserveEndpoints(String domain) {
        for (Map.Entry<String, RouteResult> entry: routeResult.entrySet()) {
            Set<String> router = entry.getValue().searchDomain(domain);
            // if router is empty, do nothing
            // observation will be created when RDS updates
            if (CollectionUtils.isNotEmpty(router)) {
                edsProtocol.observeResource(
                    router,
                    (endpointResultMap) -> {
                        endpointResultMap.forEach((k, v) -> {
                            // notify consumers
                            domainObserveConsumer.get(domain).forEach(
                                consumer1 -> consumer1.accept(v.getEndpoints()));
                        });
                    }, false);
                domainObserveRequest.add(domain);
            }
        }

    }

    public void unObserveEndpoints(String domain, Consumer<Set<Endpoint>> consumer) {
        domainObserveConsumer.get(domain).remove(consumer);
        domainObserveRequest.remove(domain);
    }

    public VirtualHost getVirtualHost(String domain) {
        for (Map.Entry<String, RouteResult> entry : routeResult.entrySet()) {
            if (entry.getValue().searchVirtualHost(domain) != null) {
                return entry.getValue().searchVirtualHost(domain);
            }
        }
        return null;
    }

    public void unObserveRds(String domain) {
        for (Map.Entry<String, RouteResult> entry : routeResult.entrySet()) {
            entry.getValue().removeVirtualHost(domain);
        }
    }
}
