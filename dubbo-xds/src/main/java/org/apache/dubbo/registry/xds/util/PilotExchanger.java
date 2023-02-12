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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
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
import org.apache.dubbo.rpc.cluster.router.xds.RdsVirtualHostListener;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class PilotExchanger {

    protected final XdsChannel xdsChannel;

    protected final LdsProtocol ldsProtocol;

    protected final RdsProtocol rdsProtocol;

    protected final EdsProtocol edsProtocol;

    protected Map<String, ListenerResult> listenerResult;

    protected Map<String, RouteResult> routeResult;

    private final AtomicBoolean isRdsObserve = new AtomicBoolean(false);
    private final Set<String> domainObserveRequest = new ConcurrentHashSet<String>();

    private final Map<String, Set<Consumer<Set<Endpoint>>>> domainObserveConsumer = new ConcurrentHashMap<>();

    private final Map<String, Consumer<RdsVirtualHostListener>> rdsObserveConsumer = new ConcurrentHashMap<>();

    private static PilotExchanger GLOBAL_PILOT_EXCHANGER = null;

    private final ApplicationModel applicationModel;

    protected PilotExchanger(URL url) {
        xdsChannel = new XdsChannel(url);
        int pollingTimeout = url.getParameter("pollingTimeout", 10);
        this.applicationModel = url.getOrDefaultApplicationModel();
        AdsObserver adsObserver = new AdsObserver(url, NodeBuilder.build());
        this.ldsProtocol = new LdsProtocol(adsObserver, NodeBuilder.build(), pollingTimeout);
        this.rdsProtocol = new RdsProtocol(adsObserver, NodeBuilder.build(), pollingTimeout);
        this.edsProtocol = new EdsProtocol(adsObserver, NodeBuilder.build(), pollingTimeout);

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
        ldsProtocol.observeResource(ldsResourcesName, (newListener) -> {
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
            List<String> domainsToUpdate = new LinkedList<>();
            domainObserveConsumer.forEach((domain, consumer) -> {
                newResult.values().forEach(o -> {
                    Set<String> newRoute = o.searchDomain(domain);
                    for (Map.Entry<String, RouteResult> entry : routeResult.entrySet()) {
                        if (!entry.getValue().searchDomain(domain).equals(newRoute)) {
                            // routers in observed domain has been updated
//                    Long domainRequest = domainObserveRequest.get(domain);
                            // router list is empty when observeEndpoints() called and domainRequest has not been created yet
                            // create new observation
                            domainsToUpdate.add(domain);
//                            doObserveEndpoints(domain);
                        }
                    }
                });
            });
            routeResult = newResult;
            ExecutorService executorService = applicationModel.getFrameworkModel().getBeanFactory()
                .getBean(FrameworkExecutorRepository.class).getSharedExecutor();
            executorService.submit(() -> domainsToUpdate.forEach(this::doObserveEndpoints));
        }, false);
    }

    public static PilotExchanger initialize(URL url) {
        synchronized (PilotExchanger.class) {
            if (GLOBAL_PILOT_EXCHANGER != null) {
                return GLOBAL_PILOT_EXCHANGER;
            }
            return (GLOBAL_PILOT_EXCHANGER = new PilotExchanger(url));
        }
    }

    public static PilotExchanger getInstance() {
        synchronized (PilotExchanger.class) {
            return GLOBAL_PILOT_EXCHANGER;
        }
    }

    public static boolean isEnabled() {
        return GLOBAL_PILOT_EXCHANGER != null;
    }


    public void destroy() {
        xdsChannel.destroy();
    }

    public Set<String> getServices() {
        Set<String> domains = new HashSet<>();
        for (Map.Entry<String, RouteResult> entry : routeResult.entrySet()) {
            domains.addAll(entry.getValue().getDomains());
        }
        return domains;
    }

    public Set<Endpoint> getEndpoints(String domain) {
        Set<Endpoint> endpoints = new HashSet<>();
        for (Map.Entry<String, RouteResult> entry : routeResult.entrySet()) {
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
        for (Map.Entry<String, RouteResult> entry : routeResult.entrySet()) {
            Set<String> router = entry.getValue().searchDomain(domain);
            // if router is empty, do nothing
            // observation will be created when RDS updates
            if (CollectionUtils.isNotEmpty(router)) {
                edsProtocol.observeResource(
                    router,
                    (endpointResultMap) -> {
                        Set<Endpoint> endpoints = endpointResultMap.values().stream()
                            .map(EndpointResult::getEndpoints)
                            .flatMap(Set::stream)
                            .collect(Collectors.toSet());
                        for (Consumer<Set<Endpoint>> consumer : domainObserveConsumer.get(domain)) {
                            consumer.accept(endpoints);
                        }
                    }, false);
                domainObserveRequest.add(domain);
            }
        }

    }

    public void unObserveEndpoints(String domain, Consumer<Set<Endpoint>> consumer) {
        domainObserveConsumer.get(domain).remove(consumer);
        domainObserveRequest.remove(domain);
    }

    public void observeEds(Set<String> clusterNames, Consumer<Map<String, EndpointResult>> consumer) {
        edsProtocol.observeResource(clusterNames, consumer, false);
    }

    public void unObserveEds(Set<String> clusterNames, Consumer<Map<String, EndpointResult>> consumer) {
        edsProtocol.unobserveResource(clusterNames, consumer);
    }

    public void observeRds(Set<String> clusterNames, Consumer<Map<String, RouteResult>> consumer) {
        rdsProtocol.observeResource(clusterNames, consumer, false);
    }

    public void unObserveRds(Set<String> clusterNames, Consumer<Map<String, RouteResult>> consumer) {
        rdsProtocol.unobserveResource(clusterNames, consumer);
    }

    public void observeLds(Consumer<Map<String, ListenerResult>> consumer) {
        ldsProtocol.observeResource(Collections.singleton(AbstractProtocol.emptyResourceName), consumer, false);
    }

    public void unObserveLds(Consumer<Map<String, ListenerResult>> consumer) {
        ldsProtocol.unobserveResource(Collections.singleton(AbstractProtocol.emptyResourceName), consumer);
    }

}
