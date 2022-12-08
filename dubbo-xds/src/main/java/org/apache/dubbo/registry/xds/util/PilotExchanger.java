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
import org.apache.dubbo.registry.xds.util.protocol.impl.EdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.impl.LdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.impl.RdsProtocol;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PilotExchanger {

    private final XdsChannel xdsChannel;

    private final RdsProtocol rdsProtocol;

    private final EdsProtocol edsProtocol;

    private ListenerResult listenerResult;

    private RouteResult routeResult;
    private final AtomicBoolean isRdsObserve = new AtomicBoolean(false);
    private final HashSet<String> domainObserveRequest = new HashSet<>();

    private final Map<String, Set<Consumer<Set<Endpoint>>>> domainObserveConsumer = new ConcurrentHashMap<>();

    private PilotExchanger(URL url) {
        xdsChannel = new XdsChannel(url);
        int pollingTimeout = url.getParameter("pollingTimeout", 10);
        LdsProtocol ldsProtocol = new LdsProtocol(xdsChannel, NodeBuilder.build(), pollingTimeout);
        this.rdsProtocol = new RdsProtocol(xdsChannel, NodeBuilder.build(), pollingTimeout);
        this.edsProtocol = new EdsProtocol(xdsChannel, NodeBuilder.build(), pollingTimeout);

        this.listenerResult = ldsProtocol.getListeners();
        this.routeResult = rdsProtocol.getResource(listenerResult.getRouteConfigNames());

        // Observer RDS update
        if (CollectionUtils.isNotEmpty(listenerResult.getRouteConfigNames())) {
            createRouteObserve();
            isRdsObserve.set(true);
        }

        // Observe LDS updated
        ldsProtocol.observeListeners((newListener) -> {
            // update local cache
            if (!newListener.equals(listenerResult)) {
                this.listenerResult = newListener;
                // update RDS observation
                if (isRdsObserve.get()) {
                    createRouteObserve();
                }
            }
        });
    }

    private void createRouteObserve() {
         rdsProtocol.observeResource(listenerResult.getRouteConfigNames(), (newResult) -> {
             // check if observed domain update ( will update endpoint observation )
             domainObserveConsumer.forEach((domain, consumer) -> {
                 Set<String> newRoute = newResult.searchDomain(domain);
                 if (!routeResult.searchDomain(domain).equals(newRoute)) {
                     // routers in observed domain has been updated
//                    Long domainRequest = domainObserveRequest.get(domain);
                     // router list is empty when observeEndpoints() called and domainRequest has not been created yet
                     // create new observation
                     doObserveEndpoints(domain);
                 }
             });
             // update local cache
             routeResult = newResult;
         });
    }

    public static PilotExchanger initialize(URL url) {
        return new PilotExchanger(url);
    }

    public void destroy() {
        xdsChannel.destroy();
    }

    public Set<String> getServices() {
        return routeResult.getDomains();
    }

    public Set<Endpoint> getEndpoints(String domain) {
        Set<String> cluster = routeResult.searchDomain(domain);
        if (CollectionUtils.isNotEmpty(cluster)) {
            EndpointResult endpoint = edsProtocol.getResource(cluster);
            return endpoint.getEndpoints();
        } else {
            return Collections.emptySet();
        }
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
        Set<String> router = routeResult.searchDomain(domain);
        // if router is empty, do nothing
        // observation will be created when RDS updates
        if (CollectionUtils.isNotEmpty(router)) {
            edsProtocol.observeResource(
                router,
                endpointResult ->
                    // notify consumers
                    domainObserveConsumer.get(domain).forEach(
                        consumer1 -> consumer1.accept(endpointResult.getEndpoints())));
            domainObserveRequest.add(domain);
        }
    }
}
