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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.xds.rule.ClusterWeight;
import org.apache.dubbo.rpc.cluster.router.xds.rule.DestinationSubset;
import org.apache.dubbo.rpc.cluster.router.xds.rule.HTTPRouteDestination;
import org.apache.dubbo.rpc.cluster.router.xds.rule.HeaderMatcher;
import org.apache.dubbo.rpc.cluster.router.xds.rule.HttpRequestMatch;
import org.apache.dubbo.rpc.cluster.router.xds.rule.PathMatcher;
import org.apache.dubbo.rpc.cluster.router.xds.rule.XdsRouteRule;

public class XdsRouter<T> extends AbstractStateRouter<T> implements XdsRouteRuleListener, EdsEndpointListener {

    private Set<String> subscribeApplications;

    private final ConcurrentHashMap<String, List<XdsRouteRule>> xdsRouteRuleMap;

    private final ConcurrentHashMap<String, DestinationSubset<T>> destinationSubsetMap;

    private final RdsRouteRuleManager rdsRouteRuleManager;

    private final EdsEndpointManager edsEndpointManager;

    private volatile BitList<Invoker<T>> currentInvokeList;

    private static final String BINARY_HEADER_SUFFIX = "-bin";

    private final boolean isEnable;

    public XdsRouter(URL url) {
        super(url);
        isEnable = PilotExchanger.isEnabled();
        rdsRouteRuleManager = url.getOrDefaultApplicationModel().getBeanFactory().getBean(RdsRouteRuleManager.class);
        edsEndpointManager = url.getOrDefaultApplicationModel().getBeanFactory().getBean(EdsEndpointManager.class);
        subscribeApplications = new ConcurrentHashSet<>();
        destinationSubsetMap = new ConcurrentHashMap<>();
        xdsRouteRuleMap = new ConcurrentHashMap<>();
        currentInvokeList = new BitList<>(new ArrayList<>());
    }

    /**
     * @deprecated only for uts
     */
    protected XdsRouter(URL url, RdsRouteRuleManager rdsRouteRuleManager, EdsEndpointManager edsEndpointManager, boolean isEnable) {
        super(url);
        this.isEnable = isEnable;
        this.rdsRouteRuleManager = rdsRouteRuleManager;
        this.edsEndpointManager = edsEndpointManager;
        subscribeApplications = new ConcurrentHashSet<>();
        destinationSubsetMap = new ConcurrentHashMap<>();
        xdsRouteRuleMap = new ConcurrentHashMap<>();
        currentInvokeList = new BitList<>(new ArrayList<>());
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation,
                                          boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder,
                                          Holder<String> messageHolder) throws RpcException {
        if (!isEnable) {
            if (needToPrintMessage) {
                messageHolder.set("Directly Return. Reason: Pilot exchanger has not been initialized, may not in mesh mode.");
            }
            return invokers;
        }

        if (CollectionUtils.isEmpty(invokers)) {
            if (needToPrintMessage) {
                messageHolder.set("Directly Return. Reason: Invokers from previous router is empty.");
            }
            return invokers;
        }

        if (CollectionUtils.isEmptyMap(xdsRouteRuleMap)) {
            if (needToPrintMessage) {
                messageHolder.set("Directly Return. Reason: xds route rule is empty.");
            }
            return invokers;
        }

        StringBuilder stringBuilder = needToPrintMessage ? new StringBuilder() : null;

        // find match cluster
        String matchCluster = null;
        Set<String> appNames = subscribeApplications;
        for (String subscribeApplication : appNames) {
            List<XdsRouteRule> rules = xdsRouteRuleMap.get(subscribeApplication);
            if (CollectionUtils.isEmpty(rules)) {
                continue;
            }
            for (XdsRouteRule rule : rules) {
                String cluster = computeMatchCluster(invocation, rule);
                if (cluster != null) {
                    matchCluster = cluster;
                    break;
                }
            }
            if (matchCluster != null) {
                if (stringBuilder != null) {
                    stringBuilder.append("Match App: ").append(subscribeApplication).append(" Cluster: ").append(matchCluster).append(' ');
                }
                break;
            }
        }
        // not match request just return
        if (matchCluster == null) {
            if (needToPrintMessage) {
                messageHolder.set("Directly Return. Reason: xds rule not match.");
            }
            return invokers;
        }
        DestinationSubset<T> destinationSubset = destinationSubsetMap.get(matchCluster);
        // cluster no target provider
        if (destinationSubset == null) {
            if (needToPrintMessage) {
                messageHolder.set(stringBuilder.append("no target subset").toString());
            }
            return BitList.emptyList();
        }
        if (needToPrintMessage) {
            messageHolder.set(stringBuilder.toString());
        }
        if (destinationSubset.getInvokers() == null) {
            return BitList.emptyList();
        }

        return destinationSubset.getInvokers().and(invokers);
    }

    private String computeMatchCluster(Invocation invocation, XdsRouteRule rule) {
        // compute request match cluster
        HttpRequestMatch requestMatch = rule.getMatch();
        if (requestMatch.getPathMatcher() == null && CollectionUtils.isEmpty(requestMatch.getHeaderMatcherList())) {
            return null;
        }
        PathMatcher pathMatcher = requestMatch.getPathMatcher();
        if (pathMatcher != null) {
            String path = "/" + invocation.getInvoker().getUrl().getPath() + "/" + invocation.getMethodName();
            if (!pathMatcher.isMatch(path)) {
                return null;
            }
        }
        List<HeaderMatcher> headerMatchers = requestMatch.getHeaderMatcherList();
        for (HeaderMatcher headerMatcher : headerMatchers) {
            String headerName = headerMatcher.getName();
            // not support byte
            if (headerName.endsWith(BINARY_HEADER_SUFFIX)) {
                return null;
            }
            String headValue = invocation.getAttachment(headerName);
            if (!headerMatcher.match(headValue)) {
                return null;
            }
        }
        HTTPRouteDestination route = rule.getRoute();
        if (route.getCluster() != null) {
            return route.getCluster();
        }
        return computeWeightCluster(route.getWeightedClusters());
    }

    private String computeWeightCluster(List<ClusterWeight> weightedClusters) {
        int totalWeight = Math.max(weightedClusters.stream().mapToInt(ClusterWeight::getWeight).sum(), 1);
        // target must greater than 0
        // if weight is 0, the destination will not receive any traffic.
        int target = ThreadLocalRandom.current().nextInt(1, totalWeight + 1);
        for (ClusterWeight weightedCluster : weightedClusters) {
            int weight = weightedCluster.getWeight();
            target -= weight;
            if (target <= 0) {
                return weightedCluster.getName();
            }
        }
        return null;
    }

    public void notify(BitList<Invoker<T>> invokers) {
        BitList<Invoker<T>> invokerList = invokers == null ? BitList.emptyList() : invokers;
        currentInvokeList = invokerList.clone();

        // compute need subscribe/unsubscribe rds application
        Set<String> currentApplications = new HashSet<>();
        for (Invoker<T> invoker : invokerList) {
            String applicationName = invoker.getUrl().getRemoteApplication();
            if (StringUtils.isNotEmpty(applicationName)) {
                currentApplications.add(applicationName);
            }
        }

        if (!subscribeApplications.equals(currentApplications)) {
            synchronized (this) {
                for (String currentApplication : currentApplications) {
                    if (!subscribeApplications.contains(currentApplication)) {
                        rdsRouteRuleManager.subscribeRds(currentApplication, this);
                    }
                }
                for (String preApplication : subscribeApplications) {
                    if (!currentApplications.contains(preApplication)) {
                        rdsRouteRuleManager.unSubscribeRds(preApplication, this);
                    }
                }
                subscribeApplications = currentApplications;
            }
        }

        // update subset
        synchronized (this) {
            BitList<Invoker<T>> allInvokers = currentInvokeList.clone();
            for (DestinationSubset<T> subset : destinationSubsetMap.values()) {
                computeSubset(subset, allInvokers);
            }
        }

    }

    private void computeSubset(DestinationSubset<T> subset, BitList<Invoker<T>> invokers) {
        Set<Endpoint> endpoints = subset.getEndpoints();
        List<Invoker<T>> filterInvokers = invokers.stream().filter(inv -> {
            String host = inv.getUrl().getHost();
            int port = inv.getUrl().getPort();
            Optional<Endpoint> any = endpoints.stream()
                .filter(end -> host.equals(end.getAddress()) && port == end.getPortValue())
                .findAny();
            return any.isPresent();
        }).collect(Collectors.toList());
        subset.setInvokers(new BitList<>(filterInvokers));
    }

    @Override
    public synchronized void onRuleChange(String appName, List<XdsRouteRule> xdsRouteRules) {
        if (CollectionUtils.isEmpty(xdsRouteRules)) {
            clearRule(appName);
            return;
        }
        Set<String> oldCluster = getAllCluster();
        xdsRouteRuleMap.put(appName, xdsRouteRules);
        Set<String> newCluster = getAllCluster();
        changeClusterSubscribe(oldCluster, newCluster);
    }

    private Set<String> getAllCluster() {
        if (CollectionUtils.isEmptyMap(xdsRouteRuleMap)) {
            return new HashSet<>();
        }
        Set<String> clusters = new HashSet<>();
        xdsRouteRuleMap.forEach((appName, rules) -> {
            for (XdsRouteRule rule : rules) {
                HTTPRouteDestination action = rule.getRoute();
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
            edsEndpointManager.unSubscribeEds(cluster, this);
            destinationSubsetMap.remove(cluster);
        }
        // add subscribe cluster
        for (String cluster : addSubscribe) {
            destinationSubsetMap.put(cluster, new DestinationSubset<>(cluster));
            edsEndpointManager.subscribeEds(cluster, this);
        }
    }

    @Override
    public synchronized void clearRule(String appName) {
        Set<String> oldCluster = getAllCluster();
        List<XdsRouteRule> oldRules = xdsRouteRuleMap.remove(appName);
        if (CollectionUtils.isEmpty(oldRules)) {
            return;
        }
        Set<String> newCluster = getAllCluster();
        changeClusterSubscribe(oldCluster, newCluster);
    }

    @Override
    public synchronized void onEndPointChange(String cluster, Set<Endpoint> endpoints) {
        // find and update subset
        DestinationSubset<T> subset = destinationSubsetMap.get(cluster);
        if (subset == null) {
            return;
        }
        subset.setEndpoints(endpoints);
        computeSubset(subset, currentInvokeList.clone());
    }

    @Override
    public void stop() {
        for (String app : subscribeApplications) {
            rdsRouteRuleManager.unSubscribeRds(app, this);
        }
        for (String cluster : getAllCluster()) {
            edsEndpointManager.unSubscribeEds(cluster, this);
        }
    }


    @Deprecated
    Set<String> getSubscribeApplications() {
        return subscribeApplications;
    }

    /**
     * for ut only
     */
    @Deprecated
    BitList<Invoker<T>> getInvokerList() {
        return currentInvokeList;
    }

    /**
     * for ut only
     */
    @Deprecated
    ConcurrentHashMap<String, List<XdsRouteRule>> getXdsRouteRuleMap() {
        return xdsRouteRuleMap;
    }


    /**
     * for ut only
     */
    @Deprecated
    ConcurrentHashMap<String, DestinationSubset<T>> getDestinationSubsetMap() {
        return destinationSubsetMap;
    }

}
