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
package org.apache.dubbo.xds.directory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.cluster.directory.AbstractDirectory;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.xds.PilotExchanger;
import org.apache.dubbo.xds.resource.endpoint.LbEndpoint;
import org.apache.dubbo.xds.resource.route.ClusterWeight;
import org.apache.dubbo.xds.resource.route.Route;
import org.apache.dubbo.xds.resource.route.RouteAction;
import org.apache.dubbo.xds.resource.route.VirtualHost;
import org.apache.dubbo.xds.resource.update.EdsUpdate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class XdsDirectory<T> extends AbstractDirectory<T> {

    private final URL url;

    private final Class<T> serviceType;

    private final String[] applicationNames;

    private final String protocolName;

    PilotExchanger pilotExchanger;

    private Protocol protocol;

    private final Map<String, VirtualHost> xdsVirtualHostMap = new ConcurrentHashMap<>();

    private final Map<String, EdsUpdate> xdsEndpointMap = new ConcurrentHashMap<>();

    private static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsDirectory.class);

    public XdsDirectory(Directory<T> directory) {
        super(directory.getUrl(), null, true, directory.getConsumerUrl());
        this.serviceType = directory.getInterface();
        this.url = directory.getConsumerUrl();
        this.applicationNames = url.getParameter("provided-by").split(",");
        this.protocolName = url.getParameter("protocol", "tri");
        this.protocol = directory.getProtocol();
        super.routerChain = directory.getRouterChain();
        this.pilotExchanger =
                url.getOrDefaultApplicationModel().getBeanFactory().getBean(PilotExchanger.class);

        // subscribe resource
        for (String applicationName : applicationNames) {
            pilotExchanger.subscribeRds(applicationName, this);
        }
    }

    public Map<String, VirtualHost> getXdsVirtualHostMap() {
        return xdsVirtualHostMap;
    }

    public Map<String, EdsUpdate> getXdsEndpointMap() {
        return xdsEndpointMap;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public Class<T> getInterface() {
        return serviceType;
    }

    public List<Invoker<T>> doList(
            SingleRouterChain<T> singleRouterChain, BitList<Invoker<T>> invokers, Invocation invocation) {
        List<Invoker<T>> result = singleRouterChain.route(this.getConsumerUrl(), invokers, invocation);
        return (List) (result == null ? BitList.emptyList() : result);
    }

    @Override
    public List<Invoker<T>> getAllInvokers() {
        return super.getInvokers();
    }

    public void onRdsChange(String applicationName, VirtualHost xdsVirtualHost) {
        Set<String> oldCluster = getAllCluster();
        xdsVirtualHostMap.put(applicationName, xdsVirtualHost);
        Set<String> newCluster = getAllCluster();
        changeClusterSubscribe(oldCluster, newCluster);
    }

    private Set<String> getAllCluster() {
        if (CollectionUtils.isEmptyMap(xdsVirtualHostMap)) {
            return new HashSet<>();
        }
        Set<String> clusters = new HashSet<>();
        xdsVirtualHostMap.forEach((applicationName, xdsVirtualHost) -> {
            for (Route xdsRoute : xdsVirtualHost.getRoutes()) {
                RouteAction action = xdsRoute.getRouteAction();
                if (action.getCluster() != null) {
                    clusters.add(action.getCluster());
                } else if (CollectionUtils.isNotEmpty(action.getWeightedClusters())) {
                    for (ClusterWeight weightedCluster : action.getWeightedClusters()) {
                        clusters.add(weightedCluster.getName());
                    }
                }
            }
        });
        return clusters;
    }

    private void changeClusterSubscribe(Set<String> oldCluster, Set<String> newCluster) {
        Set<String> removeSubscribe = new HashSet<>(oldCluster);
        Set<String> addSubscribe = new HashSet<>(newCluster);

        removeSubscribe.removeAll(newCluster);
        addSubscribe.removeAll(oldCluster);

        // remove subscribe cluster
        for (String cluster : removeSubscribe) {
            pilotExchanger.unSubscribeCds(cluster, this);
            xdsEndpointMap.remove(cluster);
            // TODO: delete invokers which belong unsubscribed cluster
        }
        // add subscribe cluster
        for (String cluster : addSubscribe) {
            pilotExchanger.subscribeCds(cluster, this);
        }
    }

    public void onEdsChange(String clusterName, EdsUpdate edsUpdate) {
        xdsEndpointMap.put(clusterName, edsUpdate);
        //        String lbPolicy = xdsCluster.getLbPolicy();
        List<LbEndpoint> xdsEndpoints = edsUpdate.getLocalityLbEndpointsMap().values().stream()
                .flatMap(e -> e.getEndpoints().stream())
                .collect(Collectors.toList());
        BitList<Invoker<T>> invokers = new BitList<>(Collections.emptyList());
        xdsEndpoints.forEach(e -> {
            String ip = e.getAddresses().getFirst().getAddress();
            int port = e.getAddresses().getFirst().getPort();
            URL url = new URL(this.protocolName, ip, port, this.serviceType.getName(), this.url.getParameters());
            // set cluster name
            url = url.addParameter("clusterID", clusterName);
            // set load balance policy
            //            url = url.addParameter("loadbalance", lbPolicy);
            //  cluster to invoker
            Invoker<T> invoker = this.protocol.refer(this.serviceType, url);
            invokers.add(invoker);
        });
        // TODO: Consider cases where some clients are not available
        // super.getInvokers().addAll(invokers);
        // TODO: Need add new api which can add invokers, because a XdsDirectory need monitor multi clusters.
        super.setInvokers(invokers);
        //        xdsCluster.setInvokers(invokers);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
