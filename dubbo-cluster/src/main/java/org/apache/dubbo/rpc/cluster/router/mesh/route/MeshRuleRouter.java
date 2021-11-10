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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Router;
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
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;
import org.apache.dubbo.rpc.cluster.router.mesh.util.VsDestinationGroupRuleListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public abstract class MeshRuleRouter implements Router, VsDestinationGroupRuleListener {

    public static final Logger logger = LoggerFactory.getLogger(MeshRuleRouter.class);

    private final URL url;


    private Map<String, String> sourcesLabels = new HashMap<>();
    private volatile List<Invoker<?>> invokerList = new ArrayList<>();
    private volatile String remoteAppName;

    private static final String INVALID_APP_NAME = "unknown";
    public static final String DESTINATION_RULE_KEY = "DestinationRule";
    public static final String VIRTUAL_SERVICE_KEY = "VirtualService";
    public static final String KIND_KEY = "kind";

    protected MeshRuleManager meshRuleManager;
    protected Set<TracingContextProvider> tracingContextProviders;

    protected volatile MeshRuleCache meshRuleCache = MeshRuleCache.emptyCache();

    public MeshRuleRouter(URL url) {
        this.url = url;
        sourcesLabels.putAll(url.getParameters());
        this.meshRuleManager = url.getScopeModel().getBeanFactory().getBean(MeshRuleManager.class);
        this.tracingContextProviders = url.getOrDefaultApplicationModel().getExtensionLoader(TracingContextProvider.class).getSupportedExtensionInstances();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        MeshRuleCache ruleCache = this.meshRuleCache;
        if (!ruleCache.containsRule()) {
            return invokers;
        }

        for (String appName : meshRuleCache.getAppList()) {
            List<DubboRouteDestination> routeDestination = getDubboRouteDestination(meshRuleCache.getVsDestinationGroup(appName), invocation);
            if (routeDestination == null) {
                return invokers;
            } else {
                // TODO aggregation
                return randomSelectDestination(invokers, meshRuleCache, appName, routeDestination);
            }
        }
        return invokers;
    }

    protected <T> List<Invoker<T>> randomSelectDestination(List<Invoker<T>> invokers, MeshRuleCache meshRuleCache, String appName, List<DubboRouteDestination> routeDestination) throws RpcException {
        int totalWeight = 0;
        for (DubboRouteDestination dubboRouteDestination : routeDestination) {
            totalWeight += Math.max(dubboRouteDestination.getWeight(), 1);
        }
        int target = ThreadLocalRandom.current().nextInt(totalWeight);
        for (DubboRouteDestination destination : routeDestination) {
            target -= Math.max(destination.getWeight(), 1);
            if (target <= 0) {
                List<Invoker<T>> result = computeDestination(invokers, meshRuleCache, appName, destination.getDestination());
                if (CollectionUtils.isNotEmpty(result)) {
                    return result;
                }
            }
        }
        for (DubboRouteDestination destination : routeDestination) {
            List<Invoker<T>> result = computeDestination(invokers, meshRuleCache, appName, destination.getDestination());
            if (CollectionUtils.isNotEmpty(result)) {
                return result;
            }
        }
        return null;
    }

    protected <T> List<Invoker<T>> computeDestination(List<Invoker<T>> invokers, MeshRuleCache meshRuleCache, String appName, DubboDestination dubboDestination) throws RpcException {
        String subset = dubboDestination.getSubset();

        List<Invoker<?>> result;

        // TODO make intersection with invokers
        do {
            result = meshRuleCache.getSubsetInvokers(appName, subset);

            if (CollectionUtils.isNotEmpty(result)) {
                return (List) result;
            }

            DubboRouteDestination dubboRouteDestination = dubboDestination.getFallback();
            if (dubboRouteDestination == null) {
                break;
            }
            dubboDestination = dubboRouteDestination.getDestination();

            subset = dubboDestination.getSubset();
        } while (true);

        return null;
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        List invokerList = invokers == null ? Collections.emptyList() : invokers;
        this.invokerList = invokerList;
        registerAppRule(invokerList);
        computeSubset(this.meshRuleCache != null ? this.meshRuleCache.getAppToVDGroup() : null);
    }

    private void registerAppRule(List<Invoker<?>> invokers) {
        if (StringUtils.isEmpty(remoteAppName)) {
            synchronized (this) {
                if (StringUtils.isEmpty(remoteAppName) && CollectionUtils.isNotEmpty(invokers)) {
                    for (Invoker invoker : invokers) {
                        String applicationName = invoker.getUrl().getRemoteApplication();
                        if (StringUtils.isNotEmpty(applicationName) && !INVALID_APP_NAME.equals(applicationName)) {
                            remoteAppName = applicationName;
                            meshRuleManager.register(remoteAppName, this);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRuleChange(String appName, List<Map<String, Object>> rules) {
        Map<String, VsDestinationGroup> appToVDGroup = new HashMap<>();
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
            appToVDGroup.put(appName, vsDestinationGroup);
        } catch (Throwable t) {
            logger.error("");
            appToVDGroup = this.meshRuleCache != null ? this.meshRuleCache.getAppToVDGroup() : null;
        }

        computeSubset(appToVDGroup);
    }

    @Override
    public void clearRule(String appName) {
        computeSubset(null);
    }

    protected void computeSubset(Map<String, VsDestinationGroup> vsDestinationGroupMap) {
        this.meshRuleCache = MeshRuleCache.build(url.getProtocolServiceKey(), this.invokerList, vsDestinationGroupMap);
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    protected List<DubboRouteDestination> getDubboRouteDestination(VsDestinationGroup vsDestinationGroup, Invocation invocation) {
        if (vsDestinationGroup != null) {
            List<VirtualServiceRule> virtualServiceRuleList = vsDestinationGroup.getVirtualServiceRuleList();
            if (virtualServiceRuleList.size() > 0) {
                for (VirtualServiceRule virtualServiceRule : virtualServiceRuleList) {
                    DubboRoute dubboRoute = getDubboRoute(virtualServiceRule, invocation);
                    if (dubboRoute != null) {
                        return getDubboRouteDestination(dubboRoute, invocation);
                    }
                }
            }
        }
        return null;
    }

    protected DubboRoute getDubboRoute(VirtualServiceRule virtualServiceRule, Invocation invocation) {
        String serviceName = invocation.getServiceName();

        VirtualServiceSpec spec = virtualServiceRule.getSpec();
        List<DubboRoute> dubboRouteList = spec.getDubbo();
        if (dubboRouteList.size() > 0) {
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


    protected List<DubboRouteDestination> getDubboRouteDestination(DubboRoute dubboRoute, Invocation invocation) {
        List<DubboRouteDetail> dubboRouteDetailList = dubboRoute.getRoutedetail();
        if (dubboRouteDetailList.size() > 0) {
            DubboRouteDetail dubboRouteDetail = findMatchDubboRouteDetail(dubboRouteDetailList, invocation);
            if (dubboRouteDetail != null) {
                return dubboRouteDetail.getRoute();
            }
        }

        return null;
    }

    protected DubboRouteDetail findMatchDubboRouteDetail(List<DubboRouteDetail> dubboRouteDetailList, Invocation invocation) {
        for (DubboRouteDetail dubboRouteDetail : dubboRouteDetailList) {
            List<DubboMatchRequest> matchRequestList = dubboRouteDetail.getMatch();
            if (CollectionUtils.isEmpty(matchRequestList)) {
                return dubboRouteDetail;
            }

            if (matchRequestList.stream().allMatch(
                request -> request.isMatch(invocation, sourcesLabels, tracingContextProviders))) {
                return dubboRouteDetail;
            }
        }
        return null;
    }

    @Override
    public void stop() {
        meshRuleManager.unregister(this);
    }
}
