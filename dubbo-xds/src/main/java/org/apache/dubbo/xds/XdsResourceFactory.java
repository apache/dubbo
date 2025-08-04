package org.apache.dubbo.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.xds.XdsResourceFactory.LdsUpdateWatcher.CdsUpdateNodeDirectory;
import org.apache.dubbo.xds.XdsResourceFactory.LdsUpdateWatcher.EdsUpdateLeafDirectory;
import org.apache.dubbo.xds.XdsResourceFactory.LdsUpdateWatcher.RdsUpdateWatcher;
import org.apache.dubbo.xds.listener.CdsListener;
import org.apache.dubbo.xds.registry.EdsListener;
import org.apache.dubbo.xds.resource.XdsClusterResource;
import org.apache.dubbo.xds.resource.XdsEndpointResource;
import org.apache.dubbo.xds.resource.XdsListenerResource;
import org.apache.dubbo.xds.resource.XdsRouteConfigureResource;
import org.apache.dubbo.xds.resource.common.Locality;
import org.apache.dubbo.xds.resource.endpoint.DropOverload;
import org.apache.dubbo.xds.resource.endpoint.LbEndpoint;
import org.apache.dubbo.xds.resource.endpoint.LocalityLbEndpoints;
import org.apache.dubbo.xds.resource.filter.NamedFilterConfig;
import org.apache.dubbo.xds.resource.listener.HttpConnectionManager;
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
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

public class XdsResourceFactory {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsResourceFactory.class);
    private final PilotExchanger pilotExchanger = PilotExchanger.getInstance();
    private final Map<String, List<EdsListener>> edsListeners = new ConcurrentHashMap<>();
    private final Map<String, VirtualHost> xdsVirtualHostMap = new ConcurrentHashMap<>();
    private final Map<String, CdsUpdate> xdsClusterMap = new ConcurrentHashMap<>();
    private final Map<String, EdsUpdate> xdsEdsMap = new ConcurrentHashMap<>();
    private final Map<String, LdsUpdateWatcher> ldsWatchers = new ConcurrentHashMap<>();
    private final Map<String, RdsUpdateWatcher> rdsWatchers = new ConcurrentHashMap<>();
    private final Map<String, CdsUpdateNodeDirectory> cdsWatchers = new ConcurrentHashMap<>();
    private final Map<String, EdsUpdateLeafDirectory> edsWatchers = new ConcurrentHashMap<>();
    
    private final static XdsResourceFactory instance = new XdsResourceFactory();

    public static XdsResourceFactory getInstance() {
        return instance;
    }

    public Map<String, List<EdsListener>> getEdsListeners() {
        return edsListeners;
    }

    public Map<String, VirtualHost> getXdsVirtualHostMap() {
        return xdsVirtualHostMap;
    }

    public Map<String, CdsUpdate> getXdsClusterMap() {
        return xdsClusterMap;
    }

    public Map<String, EdsUpdate> getXdsEdsMap() {
        return xdsEdsMap;
    }

    public Map<String, LdsUpdateWatcher> getLdsWatchers() {
        return ldsWatchers;
    }

    public Map<String, RdsUpdateWatcher> getRdsWatchers() {
        return rdsWatchers;
    }

    public Map<String, CdsUpdateNodeDirectory> getCdsWatchers() {
        return cdsWatchers;
    }

    public Map<String, EdsUpdateLeafDirectory> getEdsWatchers() {
        return edsWatchers;
    }

    public void subscribeApp(String appName, EdsListener listener) {
        if (!appName.contains(":")) {
            String errorMsg = "Service name must include a port in the format 'service:port'. Got: " + appName;
            logger.error("[XDS] " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        registerEdsListener(appName, listener);
    }

    private void registerEdsListener(String name, EdsListener listener) {
        edsListeners.compute(name, (key, listeners) -> {
            if (listeners == null) {
                listeners = new ArrayList<>();
                listeners.add(listener);
                LdsUpdateWatcher ldsUpdateWatcher = new LdsUpdateWatcher(name);
                ldsWatchers.putIfAbsent(name, ldsUpdateWatcher);
                pilotExchanger.subscribeXdsResource(name, XdsListenerResource.getInstance(), ldsUpdateWatcher);
            } else {
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
            return listeners;
        });
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
            if (update == null) {
                return;
            }
            HttpConnectionManager httpConnectionManager = update.getHttpConnectionManager();
            List<VirtualHost> virtualHosts = httpConnectionManager.getVirtualHosts();
            String rdsName = httpConnectionManager.getRdsName();

            if (virtualHosts != null) {
                updateRoutes(virtualHosts, httpConnectionManager.getHttpMaxStreamDurationNano(), httpConnectionManager.getHttpFilterConfigs());
            } else {
                rdsUpdateWatcher = new RdsUpdateWatcher(rdsName, httpConnectionManager.getHttpMaxStreamDurationNano(), httpConnectionManager.getHttpFilterConfigs());
                rdsWatchers.putIfAbsent(rdsName, rdsUpdateWatcher);
                pilotExchanger.subscribeXdsResource(rdsName, XdsRouteConfigureResource.getInstance(), rdsUpdateWatcher);
            }
        }
        
        private void updateRoutes(List<VirtualHost> virtualHosts, long httpMaxStreamDurationNano, List<NamedFilterConfig> filterConfigs) {
            VirtualHost matchedVirtualHost = null;
            for (VirtualHost virtualHost : virtualHosts) {
                if (virtualHost.getName().equals(ldsResourceName)) {
                    matchedVirtualHost = virtualHost;
                    break;
                }
            }
            
            if (matchedVirtualHost == null) {
                logger.error("[XDS] No matching VirtualHost found for: {}", ldsResourceName);
                return;
            }
            
            xdsVirtualHostMap.put(ldsResourceName, matchedVirtualHost);
            List<Route> routes = matchedVirtualHost.getRoutes();
            Set<String> clusters = new HashSet<>();
            Map<String, String> clusterNameMap = new HashMap<>();
            
            for (Route route : routes) {
                RouteAction action = route.getRouteAction();
                if (action != null) {
                    if (action.getCluster() != null) {
                        String clusterName = action.getCluster();
                        clusters.add(clusterName);
                        clusterNameMap.put(clusterName, clusterName);
                    } else if (action.getWeightedClusters() != null) {
                        for (ClusterWeight weightedCluster : action.getWeightedClusters()) {
                            String clusterName = weightedCluster.getName();
                            clusters.add(clusterName);
                            clusterNameMap.put(clusterName, clusterName);
                        }
                    }
                }
            }
            
            Set<String> addedClusters = existingClusters == null ? clusters : Sets.difference(clusters, existingClusters);
            Set<String> deletedClusters = existingClusters == null ? Collections.emptySet() : Sets.difference(existingClusters, clusters);
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

            public RdsUpdateWatcher(String rdsName, long httpMaxStreamDurationNano,
                    @Nullable List<NamedFilterConfig> filterConfigs) {
                this.rdsName = rdsName;
                this.httpMaxStreamDurationNano = httpMaxStreamDurationNano;
                this.filterConfigs = filterConfigs;
            }

            @Override
            public void onResourceUpdate(RdsUpdate update) {
                if (RdsUpdateWatcher.this != rdsUpdateWatcher) {
                    logger.warn("[XDS] Ignoring RDS update because this is not the current watcher");
                    return;
                }
                
                updateRoutes(update.getVirtualHosts(), httpMaxStreamDurationNano, filterConfigs);
            }
        }

        public class CdsUpdateNodeDirectory implements XdsResourceListener<CdsUpdate> {

            @Override
            public void onResourceUpdate(CdsUpdate update) {
                if (update == null) {
                    return;
                }
                
                if (update.getClusterType() == ClusterType.EDS) {
                    xdsClusterMap.put(update.getClusterName(), update);
                    String edsResourceName = update.getEdsServiceName() != null ? update.getEdsServiceName() : update.getClusterName();
                    
                    if (edsWatchers.containsKey(edsResourceName)) {
                        logger.info("[XDS] EDS watcher already exists for: {}", edsResourceName);
                        return;
                    }
                    
                    EdsUpdateLeafDirectory edsUpdateWatcher = new EdsUpdateLeafDirectory(update.getClusterName());
                    edsWatchers.putIfAbsent(edsResourceName, edsUpdateWatcher);
                    pilotExchanger.subscribeXdsResource(edsResourceName, XdsEndpointResource.getInstance(), edsUpdateWatcher);
                } else if (update.getClusterType() == ClusterType.AGGREGATE) {
                    for (String cluster : update.getPrioritizedClusterNames()) {
                        if (cdsWatchers.containsKey(cluster)) {
                            logger.info("[XDS] CDS watcher already exists for: {}", cluster);
                            continue;
                        }
                        CdsUpdateNodeDirectory cdsUpdateWatcher = new CdsUpdateNodeDirectory();
                        cdsWatchers.putIfAbsent(cluster, cdsUpdateWatcher);
                        pilotExchanger.subscribeXdsResource(cluster, XdsClusterResource.getInstance(), cdsUpdateWatcher);
                    }
                } else if (update.getClusterType() == ClusterType.LOGICAL_DNS) {

                }
            }
        }

        public class EdsUpdateLeafDirectory implements XdsResourceListener<EdsUpdate> {

            private final String clusterName;
            // private final String edsResourceName;
            //
            // @Nullable
            // protected final Long maxConcurrentRequests;
            //
            // @Nullable
            // protected final UpstreamTlsContext tlsContext;
            //
            // @Nullable
            // protected final OutlierDetection outlierDetection;

            private Map<Locality, String> localityPriorityNames = Collections.emptyMap();

            int priorityNameGenId = 1;

            public EdsUpdateLeafDirectory(String clusterName) {
                this.clusterName = clusterName;
            }

            @Override
            public void onResourceUpdate(EdsUpdate update) {
                if (update == null) {
                    logger.warn("[XDS] Received null EdsUpdate for cluster: {}", clusterName);
                    return;
                }
                xdsEdsMap.put(update.getClusterName(), update);
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
                        logger.info("[XDS] Discard locality {} with 0 healthy endpoints", locality);
                        continue;
                    }
                    if (!prioritizedLocalityWeights.containsKey(priorityName)) {
                        prioritizedLocalityWeights.put(priorityName, new HashMap<>());
                    }
                    prioritizedLocalityWeights.get(priorityName).put(locality, localityLbInfo.getLocalityWeight());
                }

                for (EdsListener edsListener : edsListeners.get(ldsResourceName)) {
                    edsListener.onNotify(addresses.stream().map(address -> URL.valueOf(address.toString()).setProtocol("tri").addParameter("clusterID", clusterName)).collect(Collectors.toList()));
                }

                sortedPriorityNames.retainAll(prioritizedLocalityWeights.keySet());
            }

            private List<String> generatePriorityNames(
                    String name,
                    Map<Locality, LocalityLbEndpoints> localityLbEndpoints) {
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

            /**
             * 根据endpoints生成invoker
             * @param addresses
             */
            //            private void generateInvokersFromEndpoints(List<URLAddress> addresses) {
            //
            //                List<Invoker<T>> invokers = new ArrayList<>();
            //                addresses.forEach(address -> {
            //                    URL url = new URL(
            //                            protocolName,
            //                            address.getIp(),
            //                            address.getPort(),
            //                            serviceType.getName(),
            //                            oriUrl.getParameters());
            //                    // set cluster name
            //                    url = url.addParameter("clusterID", clusterName);
            //                    // set load balance policy
            //                    //            url = url.addParameter("loadbalance", lbPolicy);
            //                    //  cluster to invoker
            //                    try {
            //                        Invoker<T> invoker = protocol.refer(serviceType, url);
            //
            //                        invokers.add(invoker);
            //                    } catch (Throwable e) {
            //                        logger.error("Failed to refer invoker from address " + address, e);
            //                    }
            //                });
            //                // TODO: Consider cases where some clients are not available
            //                // TODO: Need add new api which can add invokers, because a XdsDirectory need monitor multi clusters.
            //
            //                // 设置新的invokers到xdsCluster中
            //                BitList<Invoker<T>> bitList = new BitList<>(invokers);
            //                refreshRouter(bitList.clone(), () -> setInvokers(bitList));
            //            }
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
