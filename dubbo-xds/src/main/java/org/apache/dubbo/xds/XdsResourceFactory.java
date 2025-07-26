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
        logger.info("[XDS] Current thread: {}, Listener: {}", Thread.currentThread().getName(), listener);
        logger.info("[XDS] Current edsListeners: {}", edsListeners);

        // 同时注册多种可能的服务名格式
        String serviceNameWithoutPort = appName;
        String portOnly = null;
        
        if (appName.contains(":")) {
            serviceNameWithoutPort = appName.substring(0, appName.indexOf(":"));
            portOnly = appName.substring(appName.indexOf(":") + 1);
            logger.info("[XDS] Extracted service name without port: {} and port: {} from {}", serviceNameWithoutPort, portOnly, appName);
        }

        // 1. 注册原始appName (例如: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local)
        registerEdsListener(appName, listener);

        // 2. 如果appName包含端口，也注册不带端口的服务名
        if (!appName.equals(serviceNameWithoutPort)) {
            registerEdsListener(serviceNameWithoutPort, listener);
        }

        // 3. 额外注册可能的端口号格式（解决LDS rdsName只有端口号的问题）
        if (portOnly != null) {
            logger.info("[XDS] Also registering port-only format: {}", portOnly);
            registerEdsListener(portOnly, listener);
        }

        // 4. 如果appName是完整的FQDN格式，也注册短服务名格式
        if (appName.contains(".")) {
            String shortServiceName = appName.substring(0, appName.indexOf("."));
            if (!shortServiceName.equals(serviceNameWithoutPort)) {
                logger.info("[XDS] Also registering short service name: {}", shortServiceName);
                registerEdsListener(shortServiceName, listener);
                
                // 如果有端口，也注册短服务名+端口的格式
                if (portOnly != null) {
                    String shortWithPort = shortServiceName + ":" + portOnly;
                    logger.info("[XDS] Also registering short service name with port: {}", shortWithPort);
                    registerEdsListener(shortWithPort, listener);
                }
            }
        }

        // 5. 为没有端口号的服务名自动注册常用端口格式（重要！）
        if (portOnly == null) {
            // 对于Dubbo services，自动尝试注册常用端口
            String[] commonPorts = {"50051", "50052", "20880"};  // Triple, gRPC, Dubbo默认端口
            for (String port : commonPorts) {
                logger.info("[XDS] Auto-registering common port format: {}", port);
                registerEdsListener(port, listener);
                
                // 也注册完整服务名+端口的格式
                String serviceWithPort = appName + ":" + port;
                logger.info("[XDS] Auto-registering service with port: {}", serviceWithPort);
                registerEdsListener(serviceWithPort, listener);
            }
        }

        logger.info("[XDS] After subscription, edsListeners: {}", edsListeners);
    }

    private void registerEdsListener(String name, EdsListener listener) {
        logger.info("[XDS] Registering EDS listener for name: {}", name);
        edsListeners.compute(name, (key, listeners) -> {
            if (listeners == null) {
                logger.info("[XDS] Creating new listeners list for {}", name);
                listeners = new ArrayList<>();
                listeners.add(listener);
                LdsUpdateWatcher ldsUpdateWatcher = new LdsUpdateWatcher(name);
                ldsWatchers.putIfAbsent(name, ldsUpdateWatcher);
                logger.info("[XDS] Subscribing to XDS LDS resource for {} with watcher {}", name, ldsUpdateWatcher);
                pilotExchanger.subscribeXdsResource(name, XdsListenerResource.getInstance(), ldsUpdateWatcher);
                
                // 对于端口号格式，额外订阅可能的完整服务名格式
                if (name.matches("\\d+")) { // 如果是纯端口号
                    logger.info("[XDS] Name {} appears to be a port number, will try to match with port-based listeners", name);
                }
            } else {
                logger.info("[XDS] Adding listener to existing listeners for {}", name);
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
            logger.info("[XDS-DIAG] === LdsUpdateWatcher received update for resource: {} ===", ldsResourceName);
            logger.info("[XDS-DIAG] LdsUpdate details: {}", update);
            if (update == null) {
                return;
            }
            HttpConnectionManager httpConnectionManager = update.getHttpConnectionManager();
            logger.info("[XDS-DIAG] HttpConnectionManager: {}", httpConnectionManager);

            // If top-level HCM is null, search within the listener's filter chains
            if (httpConnectionManager == null && update.getListener() != null) {
                // First, check the regular filter chains
                if (CollectionUtils.isNotEmpty(update.getListener().getFilterChains())) {
                    for (org.apache.dubbo.xds.resource.listener.FilterChain filterChain : update.getListener()
                        .getFilterChains()) {
                        if (filterChain.getHttpConnectionManager() != null) {
                            // Use the first valid HttpConnectionManager found
                            httpConnectionManager = filterChain.getHttpConnectionManager();
                            logger.info("[XDS] Found HttpConnectionManager in a FilterChain.");
                            break;
                        }
                    }
                }

                // As a fallback, check the default filter chain
                if (httpConnectionManager == null && update.getListener().getDefaultFilterChain() != null) {
                    httpConnectionManager = update.getListener().getDefaultFilterChain().getHttpConnectionManager();
                    if (httpConnectionManager != null) {
                        logger.info("[XDS] Found HttpConnectionManager in the DefaultFilterChain.");
                    }
                }
            }

            if (httpConnectionManager != null) {
                List<VirtualHost> virtualHosts = httpConnectionManager.getVirtualHosts();
                String rdsName = httpConnectionManager.getRdsName();
                
                logger.info("[XDS-DIAG] === HttpConnectionManager Analysis ===");
                logger.info("[XDS-DIAG] VirtualHosts count: {}", (virtualHosts != null ? virtualHosts.size() : "null"));
                logger.info("[XDS-DIAG] RDS name: '{}'", rdsName);
                
                if (virtualHosts != null && !virtualHosts.isEmpty()) {
                    logger.info("[XDS-DIAG] === Inline VirtualHosts Details ===");
                    for (int i = 0; i < virtualHosts.size(); i++) {
                        VirtualHost vh = virtualHosts.get(i);
                        logger.info("[XDS-DIAG] VirtualHost[{}]: name='{}', domains={}, routes={}",
                            i, vh.getName(), vh.getDomains(), vh.getRoutes().size());
                        for (int j = 0; j < vh.getRoutes().size(); j++) {
                            Route route = vh.getRoutes().get(j);
                            if (route.getRouteAction() != null) {
                                RouteAction action = route.getRouteAction();
                                if (action.getCluster() != null) {
                                    logger.info("[XDS-DIAG]   Route[{}]: cluster='{}'", j, action.getCluster());
                                } else if (action.getWeightedClusters() != null) {
                                    logger.info("[XDS-DIAG]   Route[{}]: weightedClusters={}", j, 
                                        action.getWeightedClusters().stream()
                                            .map(wc -> wc.getName() + ":" + wc.getWeight())
                                            .collect(Collectors.toList()));
                                }
                            }
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(virtualHosts)) {
                    logger.info("[XDS] LdsUpdateWatcher using inline VirtualHosts, count: {} (ignoring RDS if present)", virtualHosts.size());
                    updateRoutes(virtualHosts, httpConnectionManager.getHttpMaxStreamDurationNano(), httpConnectionManager.getHttpFilterConfigs());
                    
                    // 如果同时存在RDS配置，记录但不订阅，因为内联VirtualHosts优先级更高
                    if (StringUtils.isNotEmpty(rdsName)) {
                        logger.info("[XDS] Note: RDS name '{}' is present but ignored due to inline VirtualHosts", rdsName);
                    }
                } else if (StringUtils.isNotEmpty(rdsName)) {
                    logger.info("[XDS] LdsUpdateWatcher subscribing to RDS resource: {} (no inline VirtualHosts)", rdsName);
                    
                    // 检查是否已经有RDS watcher了，避免重复订阅
                    if (rdsWatchers.containsKey(rdsName)) {
                        logger.info("[XDS] RDS watcher already exists for: {}, reusing existing watcher", rdsName);
                        rdsUpdateWatcher = rdsWatchers.get(rdsName);
                    } else {
                        rdsUpdateWatcher = new RdsUpdateWatcher(
                            LdsUpdateWatcher.this.ldsResourceName,
                            httpConnectionManager.getHttpMaxStreamDurationNano(),
                            httpConnectionManager.getHttpFilterConfigs()
                        );
                        rdsWatchers.put(rdsName, rdsUpdateWatcher);
                        logger.info("[XDS] Created new RDS watcher for: {}", rdsName);
                        pilotExchanger.subscribeXdsResource(rdsName, XdsRouteConfigureResource.getInstance(), rdsUpdateWatcher);
                    }
                } else {
                    logger.warn("[XDS] HttpConnectionManager has neither VirtualHosts nor RDS name");
                }
            } else {
                logger.warn("[XDS] LdsUpdateWatcher received LdsUpdate with null HttpConnectionManager and no valid fallback.");
            }
        }

        private void updateRoutes(
                List<VirtualHost> virtualHosts,
                long httpMaxStreamDurationNano,
                @Nullable List<NamedFilterConfig> filterConfigs) {
            logger.info("[XDS] LdsUpdateWatcher.updateRoutes called with {} virtualHosts for ldsResourceName: {}", virtualHosts.size(), ldsResourceName);
            //            String authority = overrideAuthority != null ? overrideAuthority : ldsResourceName;
            VirtualHost virtualHost = RoutingUtils.findVirtualHostForHostName(virtualHosts, ldsResourceName);
            if (virtualHost == null) {
                logger.warn("[XDS] LdsUpdateWatcher.updateRoutes: No VirtualHost found for: {}", ldsResourceName);
                return;
            }
            logger.info("[XDS] LdsUpdateWatcher.updateRoutes: Found VirtualHost: {} for: {}", virtualHost.getName(), ldsResourceName);
            
            // 存储VirtualHost时，需要使用多种可能的key格式，以便XdsRouter能够找到
            xdsVirtualHostMap.put(ldsResourceName, virtualHost);
            
            // 如果ldsResourceName包含端口号，也存储不带端口号的版本
            if (ldsResourceName.contains(":")) {
                String nameWithoutPort = ldsResourceName.substring(0, ldsResourceName.indexOf(":"));
                logger.info("[XDS] Also storing VirtualHost with key without port: {}", nameWithoutPort);
                xdsVirtualHostMap.put(nameWithoutPort, virtualHost);
            }
            
            // 从VirtualHost的domains中提取可能的服务名并存储
            for (String domain : virtualHost.getDomains()) {
                if (!domain.equals(ldsResourceName) && !xdsVirtualHostMap.containsKey(domain)) {
                    logger.info("[XDS] Also storing VirtualHost with domain key: {}", domain);
                    xdsVirtualHostMap.put(domain, virtualHost);
                }
            }
            
            logger.info("[XDS] Current xdsVirtualHostMap keys: {}", xdsVirtualHostMap.keySet());
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
            Set<String> addedClusters = existingClusters == null ? clusters : Sets.difference(clusters, existingClusters);
            Set<String> deletedClusters = existingClusters == null ? Collections.emptySet() : Sets.difference(existingClusters, clusters);
            existingClusters = clusters;
            // 发现新集群，订阅CDS资源
            for (String cluster : addedClusters) {
                CdsUpdateNodeDirectory cdsUpdateWatcher = new CdsUpdateNodeDirectory();
                cdsWatchers.putIfAbsent(cluster, cdsUpdateWatcher);
                pilotExchanger.subscribeXdsResource(cluster, XdsClusterResource.getInstance(), cdsUpdateWatcher);
            }
        }

        public class RdsUpdateWatcher implements XdsResourceListener<RdsUpdate> {

            private String rdsName;

            // 添加existingClusters变量定义
            @Nullable
            private Set<String> existingClusters; // clusters to which new requests can be routed

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
                logger.info("[XDS-DIAG] === RdsUpdateWatcher received update for resource: {} ===", rdsName);
                logger.info("[XDS-DIAG] RdsUpdate details: {}", update);
                logger.info("[XDS-DIAG] Current rdsUpdateWatcher == this: {}", RdsUpdateWatcher.this == rdsUpdateWatcher);
                
                if (RdsUpdateWatcher.this != rdsUpdateWatcher) {
                    logger.warn("[XDS-DIAG] Ignoring RDS update because this is not the current watcher");
                    return;
                }
                
                if (update != null && update.getVirtualHosts() != null) {
                    logger.info("[XDS-DIAG] === RDS VirtualHosts Details ===");
                    List<VirtualHost> virtualHosts = update.getVirtualHosts();
                    for (int i = 0; i < virtualHosts.size(); i++) {
                        VirtualHost vh = virtualHosts.get(i);
                        logger.info("[XDS-DIAG] RDS VirtualHost[{}]: name='{}', domains={}, routes={}",
                            i, vh.getName(), vh.getDomains(), vh.getRoutes().size());
                        for (int j = 0; j < vh.getRoutes().size(); j++) {
                            Route route = vh.getRoutes().get(j);
                            if (route.getRouteAction() != null) {
                                RouteAction action = route.getRouteAction();
                                if (action.getCluster() != null) {
                                    logger.info("[XDS-DIAG]   RDS Route[{}]: cluster='{}'", j, action.getCluster());
                                } else if (action.getWeightedClusters() != null) {
                                    logger.info("[XDS-DIAG]   RDS Route[{}]: weightedClusters={}", j, 
                                        action.getWeightedClusters().stream()
                                            .map(wc -> wc.getName() + ":" + wc.getWeight())
                                            .collect(Collectors.toList()));
                                }
                            }
                        }
                    }
                }
                
                updateRoutes(update.getVirtualHosts(), httpMaxStreamDurationNano, filterConfigs);
            }

            private void updateRoutes(
                List<VirtualHost> virtualHosts, long httpMaxStreamDurationNano, List<NamedFilterConfig> filterConfigs) {
                logger.info("[XDS] RdsUpdateWatcher.updateRoutes called with {} virtualHosts for ldsResourceName: {}", virtualHosts.size(), ldsResourceName);

                // 首先记录所有可用的VirtualHost信息
                logger.info("[XDS] Available VirtualHosts:");
                for (VirtualHost vh : virtualHosts) {
                    logger.info("[XDS]   VirtualHost: {} with domains: {}", vh.getName(), vh.getDomains());
                }

                // 尝试多种方式匹配VirtualHost
                VirtualHost matchedVirtualHost = null;

                // 1. 首先尝试不带端口号的精确匹配（这是正确的方式）
                if (ldsResourceName.contains(":")) {
                    String nameWithoutPort = ldsResourceName.substring(0, ldsResourceName.indexOf(":"));
                    logger.info("[XDS] Trying exact match for name without port: '{}'", nameWithoutPort);
                    
                    // 详细日志：检查每个VirtualHost的域名匹配
                    for (VirtualHost vh : virtualHosts) {
                        logger.info("[XDS] Checking VirtualHost: '{}' with domains: {}", vh.getName(), vh.getDomains());
                        for (String domain : vh.getDomains()) {
                            boolean matches = domain.equals(nameWithoutPort);
                            logger.info("[XDS]   Domain '{}' matches '{}': {}", domain, nameWithoutPort, matches);
                            if (matches) {
                                logger.info("[XDS]   --> This should be a match!");
                            }
                        }
                    }
                    
                    matchedVirtualHost = RoutingUtils.findVirtualHostForHostName(virtualHosts, nameWithoutPort);
                    if (matchedVirtualHost != null) {
                        logger.info("[XDS] Found exact match VirtualHost: '{}' for name without port: '{}'", matchedVirtualHost.getName(), nameWithoutPort);
                    } else {
                        logger.warn("[XDS] No match found for name without port: '{}'", nameWithoutPort);
                    }
                }
                
                // 2. 如果没找到，尝试完全匹配（包含端口号）
                if (matchedVirtualHost == null) {
                    logger.info("[XDS] Trying to find VirtualHost for full ldsResourceName: '{}'", ldsResourceName);
                    matchedVirtualHost = RoutingUtils.findVirtualHostForHostName(virtualHosts, ldsResourceName);
                    if (matchedVirtualHost != null) {
                        logger.info("[XDS] Found match VirtualHost: '{}' for full name: '{}'", matchedVirtualHost.getName(), ldsResourceName);
                        // 如果找到的是allow_any，不要使用它，继续寻找更好的匹配
                        if ("allow_any".equals(matchedVirtualHost.getName())) {
                            logger.warn("[XDS] Found allow_any VirtualHost, but we want a more specific match. Continuing search.");
                            matchedVirtualHost = null;
                        }
                    } else {
                        logger.warn("[XDS] No match found for full name: '{}'", ldsResourceName);
                    }
                }

                if (matchedVirtualHost == null) {
                    // 2. 尝试前缀匹配 (例如，对于"dubbo-demo-xds-provider:50051"，尝试匹配"dubbo-demo-xds-provider."开头的域名)
                    String prefixToMatch = null;
                    if (ldsResourceName.contains(":")) {
                        prefixToMatch = ldsResourceName.substring(0, ldsResourceName.indexOf(":")) + ".";
                    } else {
                        prefixToMatch = ldsResourceName + ".";
                    }

                    logger.info("[XDS] Trying to find VirtualHost with prefix: {}", prefixToMatch);
                    for (VirtualHost vh : virtualHosts) {
                        for (String domain : vh.getDomains()) {
                            if (domain.startsWith(prefixToMatch)) {
                                logger.info("[XDS] Found prefix match VirtualHost: {} with domain: {}", vh.getName(), domain);
                                matchedVirtualHost = vh;
                                break;
                            }
                        }
                        if (matchedVirtualHost != null) {
                            break;
                        }
                    }

                    // 3. 已经在上面尝试过不带端口号的匹配了，这里跳过

                    // 4. 尝试通过域名包含关系匹配
                    if (matchedVirtualHost == null) {
                        String serviceToMatch = ldsResourceName;
                        if (serviceToMatch.contains(":")) {
                            serviceToMatch = serviceToMatch.substring(0, serviceToMatch.indexOf(":"));
                        }
                        
                        logger.info("[XDS] Trying domain matching for service: {}", serviceToMatch);
                        for (VirtualHost vh : virtualHosts) {
                            if (vh.getName().contains(serviceToMatch)) {
                                logger.info("[XDS] Found VirtualHost by name matching: {} contains {}", vh.getName(), serviceToMatch);
                                matchedVirtualHost = vh;
                                break;
                            }
                            
                            // 检查域名列表
                            for (String domain : vh.getDomains()) {
                                if (domain.contains(serviceToMatch) || serviceToMatch.contains(domain)) {
                                    logger.info("[XDS] Found VirtualHost by domain matching: {} matches {}", domain, serviceToMatch);
                                    matchedVirtualHost = vh;
                                    break;
                                }
                            }
                            if (matchedVirtualHost != null) {
                                break;
                            }
                        }
                    }

                    // 5. 最后，如果还没找到，记录所有可用的VirtualHost但不使用fallback
                    if (matchedVirtualHost == null) {
                        logger.warn("[XDS] No matching VirtualHost found for: {}. Available VirtualHosts:", ldsResourceName);
                        for (VirtualHost vh : virtualHosts) {
                            logger.warn("[XDS] Available VirtualHost: {} with domains: {}", vh.getName(), vh.getDomains());
                        }
                        
                        // 不要使用allow_any作为fallback，而是返回
                        logger.error("[XDS] Cannot find matching VirtualHost for: {}, will not use fallback", ldsResourceName);
                        return;
                    }
                }

                if (matchedVirtualHost == null) {
                    logger.warn("[XDS] No matching VirtualHost found for: {}", ldsResourceName);
                    return;
                }

                // 记录匹配到的VirtualHost
                logger.info("[XDS] Using VirtualHost: {} for resource: {}", matchedVirtualHost.getName(), ldsResourceName);

                // 保存VirtualHost到map中，使用多种可能的key格式
                xdsVirtualHostMap.put(ldsResourceName, matchedVirtualHost);
                
                // 如果ldsResourceName包含端口号，也存储不带端口号的版本
                if (ldsResourceName.contains(":")) {
                    String nameWithoutPort = ldsResourceName.substring(0, ldsResourceName.indexOf(":"));
                    logger.info("[XDS] RDS: Also storing VirtualHost with key without port: {}", nameWithoutPort);
                    xdsVirtualHostMap.put(nameWithoutPort, matchedVirtualHost);
                }
                
                // 从VirtualHost的domains中提取可能的服务名并存储
                for (String domain : matchedVirtualHost.getDomains()) {
                    if (!domain.equals(ldsResourceName) && !xdsVirtualHostMap.containsKey(domain)) {
                        logger.info("[XDS] RDS: Also storing VirtualHost with domain key: {}", domain);
                        xdsVirtualHostMap.put(domain, matchedVirtualHost);
                    }
                }
                
                logger.info("[XDS] RDS: Current xdsVirtualHostMap keys: {}", xdsVirtualHostMap.keySet());
                List<Route> routes = matchedVirtualHost.getRoutes();
                logger.info("[XDS] Found {} routes in VirtualHost: {}", routes.size(), matchedVirtualHost.getName());

                // 收集所有可能的集群
                Set<String> clusters = new HashSet<>();
                Map<String, String> clusterNameMap = new HashMap<>();

                for (Route route : routes) {
                    RouteAction action = route.getRouteAction();
                    if (action != null) {
                        if (action.getCluster() != null) {
                            String clusterName = action.getCluster();
                            logger.info("[XDS] Found cluster in route: {}", clusterName);
                            clusters.add(clusterName);
                            clusterNameMap.put(clusterName, clusterName);
                        } else if (action.getWeightedClusters() != null) {
                            for (ClusterWeight weightedCluster : action.getWeightedClusters()) {
                                String clusterName = weightedCluster.getName();
                                logger.info("[XDS] Found weighted cluster in route: {} with weight: {}",
                                    clusterName, weightedCluster.getWeight());
                                clusters.add(clusterName);
                                clusterNameMap.put(clusterName, clusterName);
                            }
                        }
                    }
                }

                logger.info("[XDS] Total clusters found: {}", clusters.size());

                // 处理集群变化
                boolean shouldUpdateResult = existingClusters == null;
                Set<String> addedClusters = existingClusters == null ?
                    clusters : Sets.difference(clusters, existingClusters);
                Set<String> deletedClusters = existingClusters == null ?
                    Collections.emptySet() : Sets.difference(existingClusters, clusters);

                existingClusters = clusters;

                // 订阅新发现的集群
                for (String cluster : addedClusters) {
                    logger.info("[XDS] Subscribing to new cluster: {}", cluster);
                    CdsUpdateNodeDirectory cdsUpdateWatcher = new CdsUpdateNodeDirectory();
                    cdsWatchers.putIfAbsent(cluster, cdsUpdateWatcher);
                    pilotExchanger.subscribeXdsResource(cluster, XdsClusterResource.getInstance(), cdsUpdateWatcher);
                }

                // 如果有删除的集群，记录日志
                if (!deletedClusters.isEmpty()) {
                    logger.info("[XDS] Clusters removed: {}", deletedClusters);
                }
            }
        }

        public class CdsUpdateNodeDirectory implements XdsResourceListener<CdsUpdate> {

            @Override
            public void onResourceUpdate(CdsUpdate update) {
                logger.info("[XDS-DIAG] CdsUpdateWatcher received update: " + update);
                if (update == null) {
                    return;
                }
                processUpdate(update);
            }

            private void processUpdate(CdsUpdate update) {
                logger.info("[XDS] CDS update received for cluster: {}, type: {}", update.getClusterName(), update.getClusterType());
                // 根据 cluster 的类型进行相应的处理
                if (update.getClusterType() == ClusterType.EDS) {
                    // 保存Cluster信息到map中，在route时使用
                    xdsClusterMap.put(update.getClusterName(), update);
                    String edsResourceName =
                            update.getEdsServiceName() != null ? update.getEdsServiceName() : update.getClusterName();
                    logger.info("[XDS] EDS cluster detected, edsResourceName: {}", edsResourceName);

                    // 检查是否已经存在EDS监听器
                    if (edsWatchers.containsKey(edsResourceName)) {
                        logger.info("[XDS] EDS watcher already exists for: {}", edsResourceName);
                        return;
                    }

                    // 从集群名称中提取服务名（不带端口）
                    String serviceName = extractServiceNameFromCluster(update.getClusterName());
                    logger.info("[XDS] Extracted service name: {} from cluster: {}", serviceName, update.getClusterName());

                    // 创建EDS监听器
                    EdsUpdateLeafDirectory edsUpdateWatcher = new EdsUpdateLeafDirectory(update.getClusterName());
                    edsWatchers.putIfAbsent(edsResourceName, edsUpdateWatcher);

                    logger.info("[XDS] Subscribing to EDS resource: {} for cluster: {}", edsResourceName, update.getClusterName());
                    pilotExchanger.subscribeXdsResource(edsResourceName, XdsEndpointResource.getInstance(), edsUpdateWatcher);
                } else if (update.getClusterType() == ClusterType.AGGREGATE) {
                    // 非叶子节点，继续请求其他cluster信息
                    logger.info("[XDS] Aggregate cluster detected, subscribing to sub-clusters: {}", update.getPrioritizedClusterNames());
                    for (String cluster : update.getPrioritizedClusterNames()) {
                        if (cdsWatchers.containsKey(cluster)) {
                            logger.info("[XDS] CDS watcher already exists for: {}", cluster);
                            continue;
                        }
                        logger.info("[XDS] Creating new CDS watcher for: {}", cluster);
                        CdsUpdateNodeDirectory cdsUpdateWatcher = new CdsUpdateNodeDirectory();
                        cdsWatchers.putIfAbsent(cluster, cdsUpdateWatcher);
                        pilotExchanger.subscribeXdsResource(cluster, XdsClusterResource.getInstance(), cdsUpdateWatcher);
                    }
                } else if (update.getClusterType() == ClusterType.LOGICAL_DNS) {
                    logger.info("[XDS] LOGICAL_DNS cluster detected: {}", update.getClusterName());
                    // 处理LOGICAL_DNS类型的集群
                    String host = update.getDnsHostName();
                    if (host != null) {
                        logger.info("[XDS] LOGICAL_DNS cluster with host: {}", host);
                        
                        // 提取服务名和可能的端口信息
                        String serviceName = extractServiceNameFromCluster(update.getClusterName());
                        
                        // 尝试从集群名称中提取端口
                        int port = -1;
                        // 例如：outbound|50051||service.namespace.svc.cluster.local
                        if (update.getClusterName().contains("|")) {
                            String[] parts = update.getClusterName().split("\\|");
                            if (parts.length > 1) {
                                try {
                                    port = Integer.parseInt(parts[1]);
                                    logger.info("[XDS] Extracted port {} from cluster name: {}", port, update.getClusterName());
                                } catch (NumberFormatException e) {
                                    logger.warn("[XDS] Failed to parse port from cluster name part: {}", parts[1]);
                                }
                            }
                        }
                        
                        // 如果从集群名称中无法提取端口，尝试从主机名中提取
                        if (port <= 0 && host.contains(":")) {
                            String[] parts = host.split(":");
                            host = parts[0];
                            try {
                                port = Integer.parseInt(parts[1]);
                                logger.info("[XDS] Extracted port {} from host: {}", port, host);
                            } catch (NumberFormatException e) {
                                logger.warn("[XDS] Failed to parse port from host: {}", host);
                            }
                        }
                        
                        // 如果从集群名称和主机名都无法提取端口，尝试从服务名中提取
                        if (port <= 0 && serviceName.contains(":")) {
                            String[] parts = serviceName.split(":");
                            serviceName = parts[0];
                            try {
                                port = Integer.parseInt(parts[1]);
                                logger.info("[XDS] Extracted port {} from service name: {}", port, serviceName);
                            } catch (NumberFormatException e) {
                                logger.warn("[XDS] Failed to parse port from service name: {}", serviceName);
                            }
                        }
                        
                        // 如果仍然无法确定端口，使用Dubbo的默认端口
                        if (port <= 0) {
                            // 使用Dubbo默认的Triple协议端口
                            port = 50051;
                            logger.info("[XDS] Using default Dubbo Triple port: {}", port);
                        }
                        
                        // 创建URL并通知监听器
                        List<EdsListener> listeners = edsListeners.get(serviceName);
                        if (listeners != null && !listeners.isEmpty()) {
                            logger.info("[XDS] Found {} listeners for service: {}", listeners.size(), serviceName);
                            URLAddress address = new URLAddress(host, port);
                            List<URL> urls = Collections.singletonList(
                                URL.valueOf(address.toString())
                                    .setProtocol("tri")
                                    .addParameter("clusterID", update.getClusterName())
                            );
                            for (EdsListener listener : listeners) {
                                logger.info("[XDS] Notifying listener with URL: {} for LOGICAL_DNS cluster: {}", urls.get(0), update.getClusterName());
                                listener.onNotify(urls);
                            }
                        } else {
                            logger.warn("[XDS] No listeners found for service: {} from LOGICAL_DNS cluster: {}", serviceName, update.getClusterName());
                        }
                    }
                } else {
                    logger.info("[XDS] Unsupported cluster type: {} for cluster: {}", update.getClusterType(), update.getClusterName());
                }
            }

            // 从集群名称中提取服务名
            private String extractServiceNameFromCluster(String clusterName) {
                // 尝试从集群名称中提取服务名
                // 例如：outbound|50051||dubbo-demo-xds-provider-service.dubbo-proxyless.svc.cluster.local
                // 或者：outbound|50051|v1|dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local

                logger.info("[XDS] Extracting service name from cluster: {}", clusterName);

                if (clusterName.contains("|")) {
                    String[] parts = clusterName.split("\\|");
                    if (parts.length >= 4) {
                        String fullName = parts[3];
                        if (fullName.contains(".")) {
                            String serviceName = fullName.substring(0, fullName.indexOf("."));
                            logger.info("[XDS] Extracted service name: {} from cluster: {}", serviceName, clusterName);
                            return serviceName;
                        }
                        logger.info("[XDS] Using full name as service name: {}", fullName);
                        return fullName;
                    }
                } else if (clusterName.contains(":")) {
                    String serviceName = clusterName.substring(0, clusterName.indexOf(":"));
                    logger.info("[XDS] Extracted service name: {} from cluster: {}", serviceName, clusterName);
                    return serviceName;
                }

                // 默认返回原始集群名称
                logger.info("[XDS] Using original cluster name as service name: {}", clusterName);
                return clusterName;
            }
        }

        public class EdsUpdateLeafDirectory implements XdsResourceListener<EdsUpdate> {

            private final String clusterName;
            // 添加ldsResourceName字段
            private final String ldsResourceName;
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
                // 从clusterName中提取服务名（不带端口）
                this.ldsResourceName = extractServiceName(clusterName);
                logger.info("[XDS] Created EdsUpdateLeafDirectory for cluster: {}, using ldsResourceName: {}", clusterName, ldsResourceName);
            }

            // 从集群名称中提取服务名
            private String extractServiceName(String clusterName) {
                // 尝试从集群名称中提取服务名
                // 例如：outbound|50051||dubbo-demo-xds-provider-service.dubbo-proxyless.svc.cluster.local
                // 或者：dubbo-demo-xds-provider:50051
                // 或者：outbound|50051|v1|dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local

                logger.info("[XDS] Extracting service name from cluster: {}", clusterName);

                if (clusterName.contains("|")) {
                    String[] parts = clusterName.split("\\|");
                    if (parts.length >= 4) {
                        // 处理outbound|50051||dubbo-demo-xds-provider-service.dubbo-proxyless.svc.cluster.local格式
                        // 或者outbound|50051|v1|dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local格式
                        String fullName = parts[3];
                        if (fullName.contains(".")) {
                            String serviceName = fullName.substring(0, fullName.indexOf("."));
                            logger.info("[XDS] Extracted service name: {} from cluster: {}", serviceName, clusterName);
                            return serviceName;
                        }
                        logger.info("[XDS] Using full name as service name: {}", fullName);
                        return fullName;
                    }
                } else if (clusterName.contains(":")) {
                    // 处理dubbo-demo-xds-provider:50051格式
                    String serviceName = clusterName.substring(0, clusterName.indexOf(":"));
                    logger.info("[XDS] Extracted service name: {} from cluster: {}", serviceName, clusterName);
                    return serviceName;
                }

                // 默认返回原始集群名称
                logger.info("[XDS] Using original cluster name as service name: {}", clusterName);
                return clusterName;
            }

            @Override
            public void onResourceUpdate(EdsUpdate update) {
                logger.info("[XDS-DIAG] EdsUpdateLeafDirectory received update for cluster {}: {}", clusterName, update);
                if (update == null) {
                    logger.warn("[XDS] Received null EdsUpdate for cluster: {}", clusterName);
                    return;
                }

                logger.info("[XDS] Received EdsUpdate for cluster {}, localityCount={}, endpoints={}",
                           clusterName, update.getLocalityLbEndpointsMap().size(),
                           update.getLocalityLbEndpointsMap().values().stream()
                               .flatMap(e -> e.getEndpoints().stream())
                               .map(e -> e.getAddresses().get(0).getAddress() + ":" + e.getAddresses().get(0).getPort())
                               .collect(Collectors.toList()));

                xdsEdsMap.put(clusterName, update);
                Map<Locality, LocalityLbEndpoints> localityLbEndpoints = update.getLocalityLbEndpointsMap();
                List<DropOverload> dropOverloads = update.getDropPolicies();
                List<URLAddress> addresses = new ArrayList<>();
                Map<String, Map<Locality, Integer>> prioritizedLocalityWeights = new HashMap<>();
                List<String> sortedPriorityNames = generatePriorityNames(clusterName, localityLbEndpoints);

                // 提取所有健康的端点地址
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

                // 尝试多种可能的服务名称格式
                List<String> possibleServiceNames = new ArrayList<>();
                possibleServiceNames.add(ldsResourceName); // 主要服务名

                // 尝试从集群名提取的服务名
                String extractedName = extractServiceName(clusterName);
                if (!possibleServiceNames.contains(extractedName)) {
                    possibleServiceNames.add(extractedName);
                }

                // 如果服务名包含端口，也尝试不带端口的版本
                for (String serviceName : new ArrayList<>(possibleServiceNames)) {
                    if (serviceName.contains(":")) {
                        String nameWithoutPort = serviceName.substring(0, serviceName.indexOf(":"));
                        if (!possibleServiceNames.contains(nameWithoutPort)) {
                            possibleServiceNames.add(nameWithoutPort);
                        }
                    }
                }

                logger.info("[XDS] Trying to notify listeners with possible service names: {}", possibleServiceNames);

                boolean notifiedAny = false;

                // 对每个可能的服务名尝试查找并通知监听器
                for (String serviceName : possibleServiceNames) {
                    List<EdsListener> listeners = edsListeners.get(serviceName);
                    if (listeners != null && !listeners.isEmpty()) {
                        logger.info("[XDS] Found {} listeners for service name: {}", listeners.size(), serviceName);
                        for (EdsListener edsListener : listeners) {
                            // 为每个集群创建独立的URL，使用clusterName作为clusterID
                            List<URL> urls = addresses.stream()
                                .map(address -> URL.valueOf(address.toString())
                                    .setProtocol("tri")
                                    .addParameter("clusterID", clusterName))  // 使用完整的clusterName
                                .collect(Collectors.toList());
                            logger.info("[XDS] Notifying listener with {} URLs for cluster: {}, service: {}, URLs: {}",
                                urls.size(), clusterName, serviceName, urls.stream().map(URL::toString).collect(Collectors.toList()));
                            
                            // 不要覆盖现有的invoker，而是添加新的invoker
                            edsListener.onNotify(urls);
                            notifiedAny = true;
                        }
                    }
                }

                // 如果没有找到任何匹配的监听器，尝试通知所有监听器
                if (!notifiedAny) {
                    logger.warn("[XDS] No listeners found for any of the possible service names: {}. Checking all registered listeners.", possibleServiceNames);
                    for (Map.Entry<String, List<EdsListener>> entry : edsListeners.entrySet()) {
                        String key = entry.getKey();
                        List<EdsListener> listenerList = entry.getValue();
                        if (listenerList != null && !listenerList.isEmpty()) {
                            logger.info("[XDS] Found listeners for key: {}, trying to notify them", key);
                            for (EdsListener edsListener : listenerList) {
                                List<URL> urls = addresses.stream()
                                    .map(address -> URL.valueOf(address.toString())
                                        .setProtocol("tri")
                                        .addParameter("clusterID", clusterName))  // 使用完整的clusterName
                                    .collect(Collectors.toList());
                                logger.info("[XDS] Notifying listener with {} URLs for cluster: {}, URLs: {}", 
                                    urls.size(), clusterName, urls.stream().map(URL::toString).collect(Collectors.toList()));
                                edsListener.onNotify(urls);
                            }
                        }
                    }
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

    class CdsUpdateWatcher implements CdsListener {

        private final EdsListener parent;
        private final long httpMaxStreamDurationNano;
        private final List<NamedFilterConfig> filterConfigs;

        CdsUpdateWatcher(
            EdsListener parent, long httpMaxStreamDurationNano, List<NamedFilterConfig> filterConfigs) {
            this.parent = parent;
            this.httpMaxStreamDurationNano = httpMaxStreamDurationNano;
            this.filterConfigs = filterConfigs;
        }

        @Override
        public void onResourceUpdate(List<CdsUpdate> resources) {
            logger.info("[XDS-DIAG] CdsUpdateWatcher received updates: {}", resources);
            if (resources == null || resources.isEmpty()) {
                return;
            }

            // 处理每个CdsUpdate资源
            for (CdsUpdate cdsUpdate : resources) {
                onCdsUpdate(cdsUpdate.getClusterName(), cdsUpdate);
            }
        }

        // 保留原有方法以保持兼容性
        public void onCdsUpdate(String clusterName, CdsUpdate cdsUpdate) {
            logger.info("[XDS-DIAG] CdsUpdateWatcher received update: " + cdsUpdate);
            // 修复：使用正确的方法调用
            // 创建一个 EdsUpdateWatcher 来监听 EDS 更新
            List<EdsListener> listeners = new ArrayList<>();
            listeners.add(parent);
            EdsUpdateWatcher edsUpdateWatcher = new EdsUpdateWatcher(listeners);

            // 使用 subscribeXdsResource 方法订阅 EDS 资源
            String edsResourceName = cdsUpdate.getEdsServiceName() != null ?
                cdsUpdate.getEdsServiceName() : clusterName;
            pilotExchanger.subscribeXdsResource(edsResourceName, XdsEndpointResource.getInstance(), edsUpdateWatcher);
        }
    }

    class EdsUpdateWatcher implements XdsResourceListener<EdsUpdate> {

        private final List<EdsListener> listeners;

        // 添加localityPriorityNames变量定义
        private Map<Locality, String> localityPriorityNames = Collections.emptyMap();

        // 添加priorityNameGenId变量定义
        private int priorityNameGenId = 1;

        EdsUpdateWatcher(List<EdsListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onResourceUpdate(EdsUpdate update) {
            logger.info("[XDS-DIAG] EdsUpdateWatcher received update: " + update);
            if (update == null) {
                return;
            }
            logger.info("[XDS] Received EdsUpdate for cluster {}, localityCount={}, endpoints={}",
                       update.getClusterName(), update.getLocalityLbEndpointsMap().size(),
                       update.getLocalityLbEndpointsMap().values().stream()
                           .flatMap(e -> e.getEndpoints().stream())
                           .map(e -> e.getAddresses().get(0).getAddress() + ":" + e.getAddresses().get(0).getPort())
                           .collect(Collectors.toList()));
            xdsEdsMap.put(update.getClusterName(), update);
            Map<Locality, LocalityLbEndpoints> localityLbEndpoints = update.getLocalityLbEndpointsMap();
            List<URLAddress> addresses = new ArrayList<>();
            Map<String, Map<Locality, Integer>> prioritizedLocalityWeights = new HashMap<>();
            List<String> sortedPriorityNames = generatePriorityNames(update.getClusterName(), localityLbEndpoints);
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

            if (listeners != null && !listeners.isEmpty()) {
                logger.info("[XDS] Notifying {} listeners for cluster: {}", listeners.size(), update.getClusterName());
                for (EdsListener edsListener : listeners) {
                    List<URL> urls = addresses.stream()
                        .map(address -> URL.valueOf(address.toString())
                            .setProtocol("tri")
                            .addParameter("clusterID", update.getClusterName()))
                        .collect(Collectors.toList());
                    logger.info("[XDS] Notifying listener with {} URLs for cluster: {}", urls.size(), update.getClusterName());
                    edsListener.onNotify(urls);
                }
            } else {
                logger.warn("[XDS] No listeners available for cluster: {}", update.getClusterName());
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
    }
}
