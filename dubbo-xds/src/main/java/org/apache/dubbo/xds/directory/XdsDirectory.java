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
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.cluster.directory.AbstractDirectory;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.xds.PilotExchanger;
import org.apache.dubbo.xds.directory.XdsDirectory.LdsUpdateWatcher.RdsUpdateWatcher;
import org.apache.dubbo.xds.resource.XdsClusterResource;
import org.apache.dubbo.xds.resource.XdsListenerResource;
import org.apache.dubbo.xds.resource.XdsRouteConfigureResource;
import org.apache.dubbo.xds.resource.cluster.OutlierDetection;
import org.apache.dubbo.xds.resource.common.Locality;
import org.apache.dubbo.xds.resource.endpoint.DropOverload;
import org.apache.dubbo.xds.resource.endpoint.LbEndpoint;
import org.apache.dubbo.xds.resource.endpoint.LocalityLbEndpoints;
import org.apache.dubbo.xds.resource.filter.NamedFilterConfig;
import org.apache.dubbo.xds.resource.listener.HttpConnectionManager;
import org.apache.dubbo.xds.resource.listener.security.UpstreamTlsContext;
import org.apache.dubbo.xds.resource.route.ClusterWeight;
import org.apache.dubbo.xds.resource.route.Route;
import org.apache.dubbo.xds.resource.route.RouteAction;
import org.apache.dubbo.xds.resource.route.VirtualHost;
import org.apache.dubbo.xds.resource.update.CdsUpdate;
import org.apache.dubbo.xds.resource.update.CdsUpdate.ClusterType;
import org.apache.dubbo.xds.resource.update.EdsUpdate;
import org.apache.dubbo.xds.resource.update.LdsUpdate;
import org.apache.dubbo.xds.resource.update.RdsUpdate;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Sets;

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

    private Map<String, LdsUpdateWatcher> ldsWatchers = new HashMap<>();
    private Map<String, RdsUpdateWatcher> rdsWatchers = new HashMap<>();
    private Map<String, CdsUpdateNodeDirectory> cdsWatchers = new HashMap<>();
    private Map<String, EdsUpdateLeafDirectory> edsWatchers = new HashMap<>();

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
            LdsUpdateWatcher ldsUpdateWatcher = new LdsUpdateWatcher(applicationName);
            ldsWatchers.putIfAbsent(applicationName, ldsUpdateWatcher);
            pilotExchanger.subscribeXdsResource(applicationName, XdsListenerResource.getInstance(), ldsUpdateWatcher);
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

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
        //
        //        pilotExchanger.unSubscribeXdsResource(resourceName, this);
    }

    public class LdsUpdateWatcher implements XdsResourceListener<LdsUpdate> {
        private final String ldsResourceName;

        @Nullable
        private Set<String> existingClusters; // clusters to which new requests can be routed

        @Nullable
        private RdsUpdateWatcher rdsUpdateWatcher;

        public LdsUpdateWatcher(String ldsResourceName) {
            this.ldsResourceName = ldsResourceName;
        }

        @Override
        public void onResourceUpdate(LdsUpdate update) {
            HttpConnectionManager httpConnectionManager = update.getHttpConnectionManager();
            List<VirtualHost> virtualHosts = httpConnectionManager.getVirtualHosts();
            String rdsName = httpConnectionManager.getRdsName();

            if (virtualHosts != null) {
                updateRoutes(
                        virtualHosts,
                        httpConnectionManager.getHttpMaxStreamDurationNano(),
                        httpConnectionManager.getHttpFilterConfigs());
            } else {
                rdsUpdateWatcher = new RdsUpdateWatcher(
                        rdsName,
                        httpConnectionManager.getHttpMaxStreamDurationNano(),
                        httpConnectionManager.getHttpFilterConfigs());
                rdsWatchers.putIfAbsent(rdsName, rdsUpdateWatcher);
                pilotExchanger.subscribeXdsResource(rdsName, XdsRouteConfigureResource.getInstance(), rdsUpdateWatcher);
            }
        }

        private void updateRoutes(
                List<VirtualHost> virtualHosts,
                long httpMaxStreamDurationNano,
                @Nullable List<NamedFilterConfig> filterConfigs) {
            //            String authority = overrideAuthority != null ? overrideAuthority : ldsResourceName;
            String authority = ldsResourceName;
            VirtualHost virtualHost = RoutingUtils.findVirtualHostForHostName(virtualHosts, authority);
            if (virtualHost == null) {
                return;
            }

            List<Route> routes = virtualHost.getRoutes();

            // Populate all clusters to which requests can be routed to through the virtual host.
            Set<String> clusters = new HashSet<>();
            // uniqueName -> clusterName
            Map<String, String> clusterNameMap = new HashMap<>();
            for (Route route : routes) {
                RouteAction action = route.getRouteAction();
                String clusterName;
                if (action != null) {
                    if (action.getCluster() != null) {
                        clusterName = action.getCluster();
                        clusters.add(clusterName);
                        clusterNameMap.put(clusterName, action.getCluster());
                    } else if (action.getWeightedClusters() != null) {
                        for (ClusterWeight weighedCluster : action.getWeightedClusters()) {
                            clusterName = weighedCluster.getName();
                            clusters.add(clusterName);
                            clusterNameMap.put(clusterName, weighedCluster.getName());
                        }
                    }
                }
            }

            boolean shouldUpdateResult = existingClusters == null;
            Set<String> addedClusters =
                    existingClusters == null ? clusters : Sets.difference(clusters, existingClusters);
            Set<String> deletedClusters =
                    existingClusters == null ? Collections.emptySet() : Sets.difference(existingClusters, clusters);
            existingClusters = clusters;
            for (String cluster : addedClusters) {
                CdsUpdateNodeDirectory cdsUpdateWatcher = new CdsUpdateNodeDirectory();
                cdsWatchers.putIfAbsent(cluster, cdsUpdateWatcher);
                pilotExchanger.subscribeXdsResource(cluster, XdsClusterResource.getInstance(), cdsUpdateWatcher);
            }
        }

        public class RdsUpdateWatcher implements XdsResourceListener<RdsUpdate> {
            private String rdsName;

            private final long httpMaxStreamDurationNano;

            @Nullable
            private final List<NamedFilterConfig> filterConfigs;

            public RdsUpdateWatcher(
                    String rdsName, long httpMaxStreamDurationNano, @Nullable List<NamedFilterConfig> filterConfigs) {
                this.rdsName = rdsName;
                this.httpMaxStreamDurationNano = httpMaxStreamDurationNano;
                this.filterConfigs = filterConfigs;
            }

            @Override
            public void onResourceUpdate(RdsUpdate update) {
                if (RdsUpdateWatcher.this != rdsUpdateWatcher) {
                    return;
                }
                updateRoutes(update.getVirtualHosts(), httpMaxStreamDurationNano, filterConfigs);
            }
        }
    }

    /**
     * This is the internal node of the Directory tree, which is responsible for creating invokers from clusters.
     *
     * Each invoker instance created in this should be representing a cluster pointing to another Directory instead of a specific instance invoker.
     */
    public class CdsUpdateNodeDirectory implements XdsResourceListener<CdsUpdate> {
        @Override
        public void onResourceUpdate(CdsUpdate update) {
            // 啥都不干，就是把 aggregate logicalDns eds 三种做个分类处理，其中eds的不用做什么事情
            if (update.getClusterType() == ClusterType.AGGREGATE) {
                String clusterName = update.getClusterName();
                for (String cluster : update.getPrioritizedClusterNames()) {
                    // create internal node directory.
                }
            } else if (update.getClusterType() == ClusterType.EDS) {
                // create leaf directory.
            } else {

            }
        }
    }

    /**
     * This is the leaf node of the Directory tree, which is responsible for creating invokers from endpoints.
     *
     * Each invoker instance created in this  should be representing a specific dubbo provider instance.
     */
    public class EdsUpdateLeafDirectory implements XdsResourceListener<EdsUpdate> {
        private final String clusterName;
        private final String edsResourceName;

        @Nullable
        protected final Long maxConcurrentRequests;

        @Nullable
        protected final UpstreamTlsContext tlsContext;

        @Nullable
        protected final OutlierDetection outlierDetection;

        private Map<Locality, String> localityPriorityNames = Collections.emptyMap();

        int priorityNameGenId = 1;

        public EdsUpdateLeafDirectory(
                String clusterName,
                String edsResourceName,
                @Nullable Long maxConcurrentRequests,
                @Nullable UpstreamTlsContext tlsContext,
                @Nullable OutlierDetection outlierDetection) {
            this.clusterName = clusterName;
            this.edsResourceName = edsResourceName;
            this.maxConcurrentRequests = maxConcurrentRequests;
            this.tlsContext = tlsContext;
            this.outlierDetection = outlierDetection;
        }

        @Override
        public void onResourceUpdate(EdsUpdate update) {
            Map<Locality, LocalityLbEndpoints> localityLbEndpoints = update.getLocalityLbEndpointsMap();
            List<DropOverload> dropOverloads = update.getDropPolicies();
            List<URLAddress> addresses = new ArrayList<>();
            Map<String, Map<Locality, Integer>> prioritizedLocalityWeights = new HashMap<>();
            List<String> sortedPriorityNames = generatePriorityNames(clusterName, localityLbEndpoints);
            for (Locality locality : localityLbEndpoints.keySet()) {
                LocalityLbEndpoints localityLbInfo = localityLbEndpoints.get(locality);
                String priorityName = localityPriorityNames.get(locality);
                boolean discard = true;
                for (LbEndpoint endpoint : localityLbInfo.getEndpoints()) {
                    if (endpoint.isHealthy()) {
                        discard = false;
                        long weight = localityLbInfo.getLocalityWeight();
                        if (endpoint.getLoadBalancingWeight() != 0) {
                            weight *= endpoint.getLoadBalancingWeight();
                        }
                        addresses.add(endpoint.getAddresses().get(0));
                    }
                }
                if (discard) {
                    logger.info("Discard locality {0} with 0 healthy endpoints", locality);
                    continue;
                }
                if (!prioritizedLocalityWeights.containsKey(priorityName)) {
                    prioritizedLocalityWeights.put(priorityName, new HashMap<Locality, Integer>());
                }
                prioritizedLocalityWeights.get(priorityName).put(locality, localityLbInfo.getLocalityWeight());
            }

            sortedPriorityNames.retainAll(prioritizedLocalityWeights.keySet());
        }

        private List<String> generatePriorityNames(
                String name, Map<Locality, LocalityLbEndpoints> localityLbEndpoints) {
            TreeMap<Integer, List<Locality>> todo = new TreeMap<>();
            for (Locality locality : localityLbEndpoints.keySet()) {
                int priority = localityLbEndpoints.get(locality).getPriority();
                if (!todo.containsKey(priority)) {
                    todo.put(priority, new ArrayList<>());
                }
                todo.get(priority).add(locality);
            }
            Map<Locality, String> newNames = new HashMap<>();
            Set<String> usedNames = new HashSet<>();
            List<String> ret = new ArrayList<>();
            for (Integer priority : todo.keySet()) {
                String foundName = "";
                for (Locality locality : todo.get(priority)) {
                    if (localityPriorityNames.containsKey(locality)
                            && usedNames.add(localityPriorityNames.get(locality))) {
                        foundName = localityPriorityNames.get(locality);
                        break;
                    }
                }
                if ("".equals(foundName)) {
                    foundName = String.format(Locale.US, "%s[child%d]", name, priorityNameGenId++);
                }
                for (Locality locality : todo.get(priority)) {
                    newNames.put(locality, foundName);
                }
                ret.add(foundName);
            }
            localityPriorityNames = newNames;
            return ret;
        }
    }

    //
    //    public void onResourceUpdate(CdsUpdate cdsUpdate) {
    //        // for eds cluster, do nothing
    //
    //        // for aggregate clusters, do subscription
    //        String clusterName = cdsUpdate.getClusterName();
    //        this.pilotExchanger.subscribeCds(clusterName, this);
    //    }
    //
    //    public void onResourceUpdate(String clusterName, EdsUpdate edsUpdate) {
    //        xdsEndpointMap.put(clusterName, edsUpdate);
    //        //        String lbPolicy = xdsCluster.getLbPolicy();
    //        List<LbEndpoint> xdsEndpoints = edsUpdate.getLocalityLbEndpointsMap().values().stream()
    //                .flatMap(e -> e.getEndpoints().stream())
    //                .collect(Collectors.toList());
    //        BitList<Invoker<T>> invokers = new BitList<>(Collections.emptyList());
    //        xdsEndpoints.forEach(e -> {
    //            String ip = e.getAddresses().get(0).getAddress();
    //            int port = e.getAddresses().get(0).getPort();
    //            URL url = new URL(this.protocolName, ip, port, this.serviceType.getName(), this.url.getParameters());
    //            // set cluster name
    //            url = url.addParameter("clusterID", clusterName);
    //            // set load balance policy
    //            //            url = url.addParameter("loadbalance", lbPolicy);
    //            //  cluster to invoker
    //            Invoker<T> invoker = this.protocol.refer(this.serviceType, url);
    //            invokers.add(invoker);
    //        });
    //        // TODO: Consider cases where some clients are not available
    //        // super.getInvokers().addAll(invokers);
    //        // TODO: Need add new api which can add invokers, because a XdsDirectory need monitor multi clusters.
    //        super.setInvokers(invokers);
    //        //        xdsCluster.setInvokers(invokers);
    //    }
}
