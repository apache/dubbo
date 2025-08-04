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
package org.apache.dubbo.xds.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.xds.XdsResourceFactory;
import org.apache.dubbo.xds.util.HashUtils;
import org.apache.dubbo.xds.resource.filter.FilterConfig;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.xds.resource.matcher.FractionMatcher;
import org.apache.dubbo.xds.resource.matcher.HeaderMatcher;
import org.apache.dubbo.xds.resource.route.ClusterWeight;
import org.apache.dubbo.xds.resource.route.HashPolicy;
import org.apache.dubbo.xds.resource.route.Route;
import org.apache.dubbo.xds.resource.route.RouteAction;
import org.apache.dubbo.xds.resource.route.RouteMatch;
import org.apache.dubbo.xds.resource.route.VirtualHost;
import org.apache.dubbo.xds.resource.update.CdsUpdate;
import org.apache.dubbo.xds.resource.update.CdsUpdate.ClusterType;
import org.apache.dubbo.xds.resource.update.EdsUpdate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.apache.dubbo.config.Constants.MESH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.LoadbalanceRules.CONSISTENT_HASH;

public class XdsRouter<T> extends AbstractStateRouter<T> {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsRouter.class);
    private final XdsResourceFactory xdsResourceFactory = XdsResourceFactory.getInstance();
    private static final String XDS_HASH_ATTACHMENT_KEY = "xds.hash";

    public XdsRouter(URL url) {
        super(url);
    }

    @Override
    protected BitList<Invoker<T>> doRoute(
            BitList<Invoker<T>> invokers,
            URL url,
            Invocation invocation,
            boolean needToPrintMessage,
            Holder<RouterSnapshotNode<T>> routerSnapshotNodeHolder,
            Holder<String> messageHolder)
            throws RpcException {

        if (invokers.isEmpty()) {
            return invokers;
        }

        String serviceName = invocation.getInvoker().getUrl().getParameter("provided-by");
        VirtualHost virtualHost = xdsResourceFactory.getXdsVirtualHostMap().get(serviceName);
        if (virtualHost == null) {
            logger.error("[XDS] No VirtualHost found for serviceName: {}", serviceName);
            return invokers;
        }

        RouteMatchResult result = selectRouteForRequest(virtualHost, invocation);
        if (result == null || StringUtils.isEmpty(result.getSelectedCluster())) {
            logger.error("[XDS] No cluster selected for request, returning all invokers");
            return invokers;
        }

        applyRouteConfigForInvocation(result.getRoute(), invocation);

        BitList<Invoker<T>> matchedInvokers = matchInvoker(result.getSelectedCluster(), invokers);
        if (matchedInvokers.isEmpty()) {
            logger.warn("[XDS] No invokers match cluster: {}, returning all invokers", result.getSelectedCluster());
            return invokers;
        }
        return matchedInvokers;
    }

    private void applyRouteConfigForInvocation(Route route, Invocation invocation) {
        RouteAction action = route.getRouteAction();
        if (action == null) {
            return;
        }

        invocation.put("xds.route.action", action);

        if (action.getTimeoutNano() != null) {
            long timeoutMs = action.getTimeoutNano() / 1_000_000L;
            invocation.setAttachment(TIMEOUT_KEY, String.valueOf(timeoutMs));
            logger.info("[XDS] Applied timeout: {}ms from RouteAction", timeoutMs);
        }

        if (route.getFilterConfigOverrides() != null) {
            for (Map.Entry<String, FilterConfig> entry : route.getFilterConfigOverrides().entrySet()) {
                String filterName = entry.getKey();
                FilterConfig filterConfig = entry.getValue();
                invocation.setAttachment("xds.filter." + filterName, filterConfig);
            }
            logger.debug("[XDS] Applied filter config overrides: {}", route.getFilterConfigOverrides().keySet());
        }

        if (action.isAutoHostRewrite()) {
            invocation.setAttachment("xds.auto.host.rewrite", "true");
            logger.debug("[XDS] Applied auto host rewrite");
        }

        if (action.getRetryPolicy() != null) {
            int retries = Math.max(0, action.getRetryPolicy().getMaxAttempts() - 1);
            invocation.setAttachment(RETRIES_KEY, String.valueOf(retries));
            logger.info("[XDS] Applied basic retry policy: {} retries (xDS retry logic will be handled by ClusterInvoker)", retries);
        }
    }

    private RouteMatchResult selectRouteForRequest(VirtualHost virtualHost, Invocation invocation) {
        String path = buildRequestPath(invocation);

        for (Route route : virtualHost.getRoutes()) {
            RouteMatch routeMatch = route.getRouteMatch();

            if (matchRoute(routeMatch, path, invocation)) {
                logger.debug("[XDS] Route matched: {}", route);
                RouteAction action = route.getRouteAction();
                if (action == null) {
                    logger.warn("[XDS] Route has no action, skipping: {}", route);
                    continue;
                }

                String cluster = selectClusterFromAction(action);
                if (cluster != null) {
                    return new RouteMatchResult(cluster, route);
                }
            }
        }

        logger.warn("[XDS] No route matched for path: {}", path);
        return null;
    }

    private String buildRequestPath(Invocation invocation) {
        String serviceName = invocation.getInvoker().getUrl().getPath();
        String methodName = RpcUtils.getMethodName(invocation);
        return "/" + serviceName + "/" + methodName;
    }

    private boolean matchRoute(RouteMatch routeMatch, String path, Invocation invocation) {
        // 1. path match
        if (!routeMatch.isPathMatch(path)) {
            return false;
        }

        // 2. header match
        List<HeaderMatcher> headerMatchers = routeMatch.getHeaderMatchers();
        if (CollectionUtils.isNotEmpty(headerMatchers)) {
            for (HeaderMatcher headerMatcher : headerMatchers) {
                String headerName = headerMatcher.name();
                String headerValue = getHeaderValue(invocation, headerName);

                if (!headerMatcher.matches(headerValue)) {
                    return false;
                }
            }
        }

        // 3. fraction match
        if (routeMatch.getFractionMatcher() != null) {
            return matchFraction(routeMatch.getFractionMatcher());
        }

        return true;
    }

    private boolean matchFraction(FractionMatcher fractionMatcher) {
        int numerator = fractionMatcher.getNumerator();
        int denominator = fractionMatcher.getDenominator();

        if (numerator <= 0) {
            return false;
        }

        if (numerator >= denominator) {
            return true;
        }
        long seed = System.nanoTime();
        int randomValue = new java.util.Random(seed).nextInt(denominator);

        return randomValue < numerator;
    }

    private String getHeaderValue(Invocation invocation, String headerName) {
        String attachment = invocation.getAttachment(headerName);
        if (attachment != null) {
            return attachment;
        }

        URL invokerUrl = invocation.getInvoker().getUrl();
        return invokerUrl.getParameter(headerName);
    }

    //需要确认集群名称的逻辑
    private String selectClusterFromAction(RouteAction action) {
        if (action.getCluster() != null) {
            String cluster = action.getCluster();
            return resolveClusterName(cluster);
        } else if (action.getWeightedClusters() != null) {
            String cluster = selectWeightedCluster(action.getWeightedClusters());
            return resolveClusterName(cluster);
        } else if (action.getNamedClusterSpecifierPluginConfig() != null) {
            String cluster = action.getNamedClusterSpecifierPluginConfig().name();
            return resolveClusterName(cluster);
        } else {
            throw new IllegalArgumentException("[XDS] RouteAction has no cluster, header, weighted clusters, or named cluster specifier plugin config");
        }
    }

    private String resolveClusterName(String clusterName) {
        if (clusterName == null) {
            return null;
        }

        CdsUpdate xdsCluster = xdsResourceFactory.getXdsClusterMap().get(clusterName);
        if (xdsCluster != null) {
            return findCluster(xdsCluster);
        } else {
            logger.warn("[XDS] No CdsUpdate found for cluster: {}", clusterName);
            return clusterName;
        }
    }

    private String findCluster(CdsUpdate xdsCluster) {
        if (ClusterType.EDS.equals(xdsCluster.getClusterType())) {
            return xdsCluster.getEdsServiceName();
        } else if (ClusterType.AGGREGATE.equals(xdsCluster.getClusterType())) {
            String cluster = xdsCluster.getPrioritizedClusterNames().get(0);
            CdsUpdate cdsUpdate = xdsResourceFactory.getXdsClusterMap().get(cluster);
            if (cdsUpdate != null) {
                return findCluster(cdsUpdate);
            }
        }
        return xdsCluster.getClusterName();
    }

    private String selectWeightedCluster(List<ClusterWeight> weightedClusters) {
        int totalWeight = Math.max(weightedClusters.stream().mapToInt(ClusterWeight::getWeight).sum(), 1);

        long seed = System.nanoTime();
        int target = new java.util.Random(seed).nextInt(totalWeight) + 1;

        int cumulativeWeight = 0;
        for (ClusterWeight weightedCluster : weightedClusters) {
            int weight = weightedCluster.getWeight();
            cumulativeWeight += weight;

            if (target <= cumulativeWeight) {
                return weightedCluster.getName();
            }
        }

        return null;
    }

    private BitList<Invoker<T>> matchInvoker(String clusterName, BitList<Invoker<T>> invokers) {
        BitList<Invoker<T>> result = invokers.clone();
        result.removeIf(inv -> {
            String clusterID = inv.getUrl().getParameter("clusterID");
            boolean matches = clusterID != null && clusterID.equals(clusterName);
            return !matches;
        });

        return result;
    }

    private static class RouteMatchResult {

        private final String selectedCluster;

        private final Route route;

        public RouteMatchResult(String selectedCluster, Route route) {
            this.selectedCluster = selectedCluster;
            this.route = route;
        }

        public String getSelectedCluster() {
            return selectedCluster;
        }

        public RouteAction getRouteAction() {
            return route.getRouteAction();
        }

        public Route getRoute() {
            return route;
        }
    }
}
