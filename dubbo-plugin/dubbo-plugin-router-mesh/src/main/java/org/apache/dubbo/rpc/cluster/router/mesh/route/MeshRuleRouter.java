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
package org.apache.dubbo.rpc.cluster.router.mesh.route;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.VsDestinationGroup;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboMatchRequest;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboRoute;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboRouteDetail;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.VirtualServiceRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.VirtualServiceSpec;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.destination.DubboDestination;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.destination.DubboRouteDestination;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.StringMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.util.MeshRuleListener;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_RECEIVE_RULE;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.DESTINATION_RULE_KEY;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.INVALID_APP_NAME;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.KIND_KEY;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.VIRTUAL_SERVICE_KEY;

public abstract class MeshRuleRouter<T> extends AbstractStateRouter<T> implements MeshRuleListener {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MeshRuleRouter.class);

    private final Map<String, String> sourcesLabels;
    private volatile BitList<Invoker<T>> invokerList = BitList.emptyList();
    private volatile Set<String> remoteAppName = Collections.emptySet();

    protected MeshRuleManager meshRuleManager;
    protected Set<TracingContextProvider> tracingContextProviders;

    protected volatile MeshRuleCache<T> meshRuleCache = MeshRuleCache.emptyCache();

    public MeshRuleRouter(URL url) {
        super(url);
        sourcesLabels = Collections.unmodifiableMap(new HashMap<>(url.getParameters()));
        this.meshRuleManager = url.getOrDefaultModuleModel().getBeanFactory().getBean(MeshRuleManager.class);
        this.tracingContextProviders = url.getOrDefaultModuleModel().getExtensionLoader(TracingContextProvider.class).getSupportedExtensionInstances();
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation,
                                          boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder,
                                          Holder<String> messageHolder) throws RpcException {
        MeshRuleCache<T> ruleCache = this.meshRuleCache;
        if (!ruleCache.containsRule()) {
            if (needToPrintMessage) {
                messageHolder.set("MeshRuleCache has not been built. Skip route.");
            }
            return invokers;
        }

        BitList<Invoker<T>> result = new BitList<>(invokers.getOriginList(), true, invokers.getTailList());

        StringBuilder stringBuilder = needToPrintMessage ? new StringBuilder() : null;

        // loop each application
        for (String appName : ruleCache.getAppList()) {
            // find destination by invocation
            List<DubboRouteDestination> routeDestination = getDubboRouteDestination(ruleCache.getVsDestinationGroup(appName), invocation);
            if (routeDestination != null) {
                // aggregate target invokers
                String subset = randomSelectDestination(ruleCache, appName, routeDestination, invokers);
                if (subset != null) {
                    BitList<Invoker<T>> destination = meshRuleCache.getSubsetInvokers(appName, subset);
                    result = result.or(destination);
                    if (stringBuilder != null) {
                        stringBuilder.append("Match App: ").append(appName).append(" Subset: ").append(subset).append(' ');
                    }
                }
            }
        }

        // result = result.or(ruleCache.getUnmatchedInvokers());

        // empty protection
        if (result.isEmpty()) {
            if (needToPrintMessage) {
                messageHolder.set("Empty protection after routed.");
            }
            return invokers;
        }

        if (needToPrintMessage) {
            messageHolder.set(stringBuilder.toString());
        }
        return invokers.and(result);
    }

    /**
     * Select RouteDestination by Invocation
     */
    protected List<DubboRouteDestination> getDubboRouteDestination(VsDestinationGroup vsDestinationGroup, Invocation invocation) {
        if (vsDestinationGroup != null) {
            List<VirtualServiceRule> virtualServiceRuleList = vsDestinationGroup.getVirtualServiceRuleList();
            if (CollectionUtils.isNotEmpty(virtualServiceRuleList)) {
                for (VirtualServiceRule virtualServiceRule : virtualServiceRuleList) {
                    // match virtual service (by serviceName)
                    DubboRoute dubboRoute = getDubboRoute(virtualServiceRule, invocation);
                    if (dubboRoute != null) {
                        // match route detail (by params)
                        return getDubboRouteDestination(dubboRoute, invocation);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Match virtual service (by serviceName)
     */
    protected DubboRoute getDubboRoute(VirtualServiceRule virtualServiceRule, Invocation invocation) {
        String serviceName = invocation.getServiceName();

        VirtualServiceSpec spec = virtualServiceRule.getSpec();
        List<DubboRoute> dubboRouteList = spec.getDubbo();
        if (CollectionUtils.isNotEmpty(dubboRouteList)) {
            for (DubboRoute dubboRoute : dubboRouteList) {
                List<StringMatch> stringMatchList = dubboRoute.getServices();
                if (CollectionUtils.isEmpty(stringMatchList)) {
                    return dubboRoute;
                }
                for (StringMatch stringMatch : stringMatchList) {
                    if (stringMatch.isMatch(serviceName)) {
                        return dubboRoute;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Match route detail (by params)
     */
    protected List<DubboRouteDestination> getDubboRouteDestination(DubboRoute dubboRoute, Invocation invocation) {
        List<DubboRouteDetail> dubboRouteDetailList = dubboRoute.getRoutedetail();
        if (CollectionUtils.isNotEmpty(dubboRouteDetailList)) {
            for (DubboRouteDetail dubboRouteDetail : dubboRouteDetailList) {
                List<DubboMatchRequest> matchRequestList = dubboRouteDetail.getMatch();
                if (CollectionUtils.isEmpty(matchRequestList)) {
                    return dubboRouteDetail.getRoute();
                }

                if (matchRequestList.stream().allMatch(
                    request -> request.isMatch(invocation, sourcesLabels, tracingContextProviders))) {
                    return dubboRouteDetail.getRoute();
                }
            }
        }

        return null;
    }

    /**
     * Find out target invokers from RouteDestination
     */
    protected String randomSelectDestination(MeshRuleCache<T> meshRuleCache, String appName, List<DubboRouteDestination> routeDestination, BitList<Invoker<T>> availableInvokers) throws RpcException {
        // randomly select one DubboRouteDestination from list by weight
        int totalWeight = 0;
        for (DubboRouteDestination dubboRouteDestination : routeDestination) {
            totalWeight += Math.max(dubboRouteDestination.getWeight(), 1);
        }
        int target = ThreadLocalRandom.current().nextInt(totalWeight);
        for (DubboRouteDestination destination : routeDestination) {
            target -= Math.max(destination.getWeight(), 1);
            if (target <= 0) {
                // match weight
                String result = computeDestination(meshRuleCache, appName, destination.getDestination(), availableInvokers);
                if (result != null) {
                    return result;
                }
            }
        }

        // fall back
        for (DubboRouteDestination destination : routeDestination) {
            String result = computeDestination(meshRuleCache, appName, destination.getDestination(), availableInvokers);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Compute Destination Subset
     */
    protected String computeDestination(MeshRuleCache<T> meshRuleCache, String appName, DubboDestination dubboDestination, BitList<Invoker<T>> availableInvokers) throws RpcException {
        String subset = dubboDestination.getSubset();

        do {
            BitList<Invoker<T>> result = meshRuleCache.getSubsetInvokers(appName, subset);

            if (CollectionUtils.isNotEmpty(result) && !availableInvokers.clone().and(result).isEmpty()) {
                return subset;
            }

            // fall back
            DubboRouteDestination dubboRouteDestination = dubboDestination.getFallback();
            if (dubboRouteDestination == null) {
                break;
            }
            dubboDestination = dubboRouteDestination.getDestination();

            if (dubboDestination == null) {
                break;
            }
            subset = dubboDestination.getSubset();
        } while (true);

        return null;
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {
        BitList<Invoker<T>> invokerList = invokers == null ? BitList.emptyList() : invokers;
        this.invokerList = invokerList.clone();
        registerAppRule(invokerList);
        computeSubset(this.meshRuleCache.getAppToVDGroup());
    }

    private void registerAppRule(BitList<Invoker<T>> invokers) {
        Set<String> currentApplication = new HashSet<>();
        if (CollectionUtils.isNotEmpty(invokers)) {
            for (Invoker<T> invoker : invokers) {
                String applicationName = invoker.getUrl().getRemoteApplication();
                if (StringUtils.isNotEmpty(applicationName) && !INVALID_APP_NAME.equals(applicationName)) {
                    currentApplication.add(applicationName);
                }
            }
        }

        if (!remoteAppName.equals(currentApplication)) {
            synchronized (this) {
                Set<String> current = new HashSet<>(currentApplication);
                Set<String> previous = new HashSet<>(remoteAppName);
                previous.removeAll(currentApplication);
                current.removeAll(remoteAppName);
                for (String app : current) {
                    meshRuleManager.register(app, this);
                }
                for (String app : previous) {
                    meshRuleManager.unregister(app, this);
                }
                remoteAppName = currentApplication;
            }
        }
    }

    @Override
    public synchronized void onRuleChange(String appName, List<Map<String, Object>> rules) {
        // only update specified app's rule
        Map<String, VsDestinationGroup> appToVDGroup = new ConcurrentHashMap<>(this.meshRuleCache.getAppToVDGroup());
        try {
            VsDestinationGroup vsDestinationGroup = new VsDestinationGroup();
            vsDestinationGroup.setAppName(appName);

            for (Map<String, Object> rule : rules) {
                if (DESTINATION_RULE_KEY.equals(rule.get(KIND_KEY))) {
                    DestinationRule destinationRule = PojoUtils.mapToPojo(rule, DestinationRule.class);
                    vsDestinationGroup.getDestinationRuleList().add(destinationRule);
                } else if (VIRTUAL_SERVICE_KEY.equals(rule.get(KIND_KEY))) {
                    VirtualServiceRule virtualServiceRule = PojoUtils.mapToPojo(rule, VirtualServiceRule.class);
                    vsDestinationGroup.getVirtualServiceRuleList().add(virtualServiceRule);
                }
            }
            if (vsDestinationGroup.isValid()) {
                appToVDGroup.put(appName, vsDestinationGroup);
            }
        } catch (Throwable t) {
            logger.error(CLUSTER_FAILED_RECEIVE_RULE,"failed to parse mesh route rule","","Error occurred when parsing rule component.",t);
        }

        computeSubset(appToVDGroup);
    }

    @Override
    public synchronized void clearRule(String appName) {
        Map<String, VsDestinationGroup> appToVDGroup = new ConcurrentHashMap<>(this.meshRuleCache.getAppToVDGroup());
        appToVDGroup.remove(appName);
        computeSubset(appToVDGroup);
    }

    protected void computeSubset(Map<String, VsDestinationGroup> vsDestinationGroupMap) {
        this.meshRuleCache = MeshRuleCache.build(getUrl().getProtocolServiceKey(), this.invokerList, vsDestinationGroupMap);
    }

    @Override
    public void stop() {
        for (String app : remoteAppName) {
            meshRuleManager.unregister(app, this);
        }
    }

    /**
     * for ut only
     */
    @Deprecated
    public Set<String> getRemoteAppName() {
        return remoteAppName;
    }

    /**
     * for ut only
     */
    @Deprecated
    public BitList<Invoker<T>> getInvokerList() {
        return invokerList;
    }

    /**
     * for ut only
     */
    @Deprecated
    public MeshRuleCache<T> getMeshRuleCache() {
        return meshRuleCache;
    }
}
