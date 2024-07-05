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
package org.apache.dubbo.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.xds.directory.XdsDirectory;
import org.apache.dubbo.xds.protocol.XdsResourceListener;
import org.apache.dubbo.xds.protocol.impl.CdsProtocol;
import org.apache.dubbo.xds.protocol.impl.EdsProtocol;
import org.apache.dubbo.xds.protocol.impl.LdsProtocol;
import org.apache.dubbo.xds.protocol.impl.RdsProtocol;
import org.apache.dubbo.xds.resource.XdsCluster;
import org.apache.dubbo.xds.resource.XdsEndpoint;
import org.apache.dubbo.xds.resource.XdsRouteConfiguration;
import org.apache.dubbo.xds.resource.XdsVirtualHost;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;

public class PilotExchanger {

    protected final AdsObserver adsObserver;

    protected final LdsProtocol ldsProtocol;

    protected final RdsProtocol rdsProtocol;

    protected final EdsProtocol edsProtocol;

    protected final CdsProtocol cdsProtocol;

    private final Set<String> domainObserveRequest = new ConcurrentHashSet<String>();

    private static PilotExchanger GLOBAL_PILOT_EXCHANGER = null;

    private static final Map<String, XdsVirtualHost> xdsVirtualHostMap = new ConcurrentHashMap<>();

    private static final Map<String, XdsCluster> xdsClusterMap = new ConcurrentHashMap<>();

    private final Map<String, Set<XdsDirectory>> rdsListeners = new ConcurrentHashMap<>();

    private final Map<String, Set<XdsDirectory>> cdsListeners = new ConcurrentHashMap<>();

    protected PilotExchanger(URL url) {
        int pollingTimeout = url.getParameter("pollingTimeout", 10);
        adsObserver = new AdsObserver(url, NodeBuilder.build());

        this.rdsProtocol =
                new RdsProtocol(adsObserver, NodeBuilder.build(), pollingTimeout, url.getOrDefaultApplicationModel());
        this.edsProtocol =
                new EdsProtocol(adsObserver, NodeBuilder.build(), pollingTimeout, url.getOrDefaultApplicationModel());
        this.ldsProtocol =
                new LdsProtocol(adsObserver, NodeBuilder.build(), pollingTimeout, url.getOrDefaultApplicationModel());
        this.cdsProtocol =
                new CdsProtocol(adsObserver, NodeBuilder.build(), pollingTimeout, url.getOrDefaultApplicationModel());

        XdsResourceListener<XdsRouteConfiguration> pilotRdsListener =
                xdsRouteConfigurations -> xdsRouteConfigurations.forEach(xdsRouteConfiguration -> xdsRouteConfiguration
                        .getVirtualHosts()
                        .forEach((serviceName, xdsVirtualHost) -> {
                            this.xdsVirtualHostMap.put(serviceName, xdsVirtualHost);
                            // when resource update, notify subscribers
                            if (rdsListeners.containsKey(serviceName)) {
                                for (XdsDirectory listener : rdsListeners.get(serviceName)) {
                                    listener.onRdsChange(serviceName, xdsVirtualHost);
                                }
                            }
                        }));

        XdsResourceListener<ClusterLoadAssignment> pilotEdsListener = clusterLoadAssignments -> {
            List<XdsCluster> xdsClusters =
                    clusterLoadAssignments.stream().map(this::parseCluster).collect(Collectors.toList());
            xdsClusters.forEach(xdsCluster -> {
                this.xdsClusterMap.put(xdsCluster.getName(), xdsCluster);
                if (cdsListeners.containsKey(xdsCluster.getName())) {
                    for (XdsDirectory listener : cdsListeners.get(xdsCluster.getName())) {
                        listener.onEdsChange(xdsCluster.getName(), xdsCluster);
                    }
                }
            });
        };

        this.rdsProtocol.registerListen(pilotRdsListener);
        this.edsProtocol.registerListen(pilotEdsListener);
        // lds resources callback，listen to all rds resources in the callback function
        this.ldsProtocol.registerListen(rdsProtocol.getLdsListener());
        this.cdsProtocol.registerListen(edsProtocol.getCdsListener());

        // cds resources callback，listen to all cds resources in the callback function
        this.cdsProtocol.subscribeClusters();
        this.ldsProtocol.subscribeListeners();
    }

    public static Map<String, XdsVirtualHost> getXdsVirtualHostMap() {
        return xdsVirtualHostMap;
    }

    public static Map<String, XdsCluster> getXdsClusterMap() {
        return xdsClusterMap;
    }

    public void subscribeRds(String applicationName, XdsDirectory listener) {
        rdsListeners.computeIfAbsent(applicationName, key -> new ConcurrentHashSet<>());
        rdsListeners.get(applicationName).add(listener);
        if (xdsVirtualHostMap.containsKey(applicationName)) {
            listener.onRdsChange(applicationName, this.xdsVirtualHostMap.get(applicationName));
        }
    }

    public void unSubscribeRds(String applicationName, XdsDirectory listener) {
        rdsListeners.get(applicationName).remove(listener);
    }

    public void subscribeCds(String clusterName, XdsDirectory listener) {
        cdsListeners.computeIfAbsent(clusterName, key -> new ConcurrentHashSet<>());
        cdsListeners.get(clusterName).add(listener);
        if (xdsClusterMap.containsKey(clusterName)) {
            listener.onEdsChange(clusterName, xdsClusterMap.get(clusterName));
        }
    }

    public void unSubscribeCds(String clusterName, XdsDirectory listener) {
        cdsListeners.get(clusterName).remove(listener);
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

    public static PilotExchanger createInstance(URL url) {
        return new PilotExchanger(url);
    }

    public static boolean isEnabled() {
        return GLOBAL_PILOT_EXCHANGER != null;
    }

    public void destroy() {
        this.adsObserver.destroy();
    }

    private XdsCluster parseCluster(ClusterLoadAssignment cluster) {
        XdsCluster xdsCluster = new XdsCluster();

        xdsCluster.setName(cluster.getClusterName());

        List<XdsEndpoint> xdsEndpoints = cluster.getEndpointsList().stream()
                .flatMap(e -> e.getLbEndpointsList().stream())
                .map(LbEndpoint::getEndpoint)
                .map(this::parseEndpoint)
                .collect(Collectors.toList());

        xdsCluster.setXdsEndpoints(xdsEndpoints);

        return xdsCluster;
    }

    private XdsEndpoint parseEndpoint(io.envoyproxy.envoy.config.endpoint.v3.Endpoint endpoint) {
        XdsEndpoint xdsEndpoint = new XdsEndpoint();
        xdsEndpoint.setAddress(endpoint.getAddress().getSocketAddress().getAddress());
        xdsEndpoint.setPortValue(endpoint.getAddress().getSocketAddress().getPortValue());
        return xdsEndpoint;
    }
}
