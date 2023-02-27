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
package org.apache.dubbo.rpc.cluster;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.filter.DemoService;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotSwitcher;
import org.apache.dubbo.rpc.cluster.router.condition.config.AppStateRouter;
import org.apache.dubbo.rpc.cluster.router.condition.config.ListenableStateRouter;
import org.apache.dubbo.rpc.cluster.router.condition.config.ServiceStateRouter;
import org.apache.dubbo.rpc.cluster.router.mesh.route.MeshAppRuleListener;
import org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleManager;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.MESH_RULE_DATA_ID_SUFFIX;
import static org.mockito.Mockito.when;

class RouterChainTest {

    /**
     * verify the router and state router loaded by default
     */
    @Test
    void testBuildRouterChain() {
        RouterChain<DemoService> routerChain = createRouterChanin();
        Assertions.assertEquals(0, routerChain.getRouters().size());
        Assertions.assertEquals(7, routerChain.getStateRouters().size());
    }

    private RouterChain<DemoService> createRouterChanin() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        URL url = new ServiceConfigURL("dubbo",
            "127.0.0.1",
            20881,
            DemoService.class.getName(),
            parameters);

        RouterChain<DemoService> routerChain = RouterChain.buildChain(DemoService.class, url);
        return routerChain;
    }

    @Test
    void testRoute() {
        RouterChain<DemoService> routerChain = createRouterChanin();

        // mockInvoker will be filtered out by MockInvokersSelector
        Invoker<DemoService> mockInvoker = createMockInvoker();

        // invoker1 will be filtered out by MeshStateRouter
        Map<String, String> map1 = new HashMap<>();
        map1.put("env-sign", "yyyyyyy");
        Invoker<DemoService> invoker1 = createNormalInvoker(map1);

        // invoker2 will be filtered out by TagStateRouter
        Map<String, String> map2 = new HashMap<>();
        map2.put("env-sign", "xxx");
        map2.put("tag1", "hello");
        Invoker<DemoService> invoker2 = createNormalInvoker(map2);

        // invoker3 will be filtered out by AppStateRouter
        Map<String, String> map3 = new HashMap<>();
        map3.put("env-sign", "xxx");
        map3.put("tag1", "hello");
        map3.put(TAG_KEY, "TAG_");
        Invoker<DemoService> invoker3 = createNormalInvoker(map3);

        // invoker4 will be filtered out by ServiceStateRouter
        Map<String, String> map4 = new HashMap<>();
        map4.put("env-sign", "xxx");
        map4.put("tag1", "hello");
        map4.put(TAG_KEY, "TAG_");
        map4.put("timeout", "5000");
        Invoker<DemoService> invoker4 = createNormalInvoker(map4);

        // invoker5 is the only one returned at the end that is not filtered out
        Map<String, String> map5 = new HashMap<>();
        map5.put("env-sign", "xxx");
        map5.put("tag1", "hello");
        map5.put(TAG_KEY, "TAG_");
        map5.put("timeout", "5000");
        map5.put("serialization", "hessian2");
        Invoker<DemoService> invoker5 = createNormalInvoker(map5);

        BitList<Invoker<DemoService>> invokers = new BitList<>(Arrays.asList(mockInvoker, invoker1, invoker2, invoker3, invoker4, invoker5));
        routerChain.setInvokers(invokers, () -> {});

        // mesh rule for MeshStateRouter
        MeshRuleManager meshRuleManager = mockInvoker.getUrl().getOrDefaultModuleModel().getBeanFactory().getBean(MeshRuleManager.class);
        ConcurrentHashMap<String, MeshAppRuleListener> appRuleListeners = meshRuleManager.getAppRuleListeners();
        MeshAppRuleListener meshAppRuleListener = appRuleListeners.get(invoker1.getUrl().getRemoteApplication());
        ConfigChangedEvent configChangedEvent = new ConfigChangedEvent("demo-route" + MESH_RULE_DATA_ID_SUFFIX, DynamicConfiguration.DEFAULT_GROUP,
            MESH_RULE1 + "---\n" + MESH_RULE2, ConfigChangeType.ADDED);
        meshAppRuleListener.process(configChangedEvent);


        // condition rule for AppStateRouter&ServiceStateRouter
        ListenableStateRouter serviceRouter = routerChain.getStateRouters().stream().filter(s -> s instanceof ServiceStateRouter).map(s -> (ListenableStateRouter) s).findAny().orElse(null);
        ConfigChangedEvent serviceConditionEvent = new ConfigChangedEvent(DynamicConfiguration.getRuleKey(mockInvoker.getUrl()) + ".condition-router", DynamicConfiguration.DEFAULT_GROUP,
            SERVICE_CONDITION_RULE, ConfigChangeType.ADDED);
        serviceRouter.process(serviceConditionEvent);

        ListenableStateRouter appRouter = routerChain.getStateRouters().stream().filter(s -> s instanceof AppStateRouter).map(s -> (ListenableStateRouter) s).findAny().orElse(null);
        ConfigChangedEvent appConditionEvent = new ConfigChangedEvent("app.condition-router", DynamicConfiguration.DEFAULT_GROUP,
            APP_CONDITION_RULE, ConfigChangeType.ADDED);
        appRouter.process(appConditionEvent);

        // prepare consumerUrl and RpcInvocation
        URL consumerUrl = URL.valueOf("consumer://localhost/DemoInterface?remote.application=app1");
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setServiceName("DemoService");
        rpcInvocation.setObjectAttachment("trafficLabel", "xxx");
        rpcInvocation.setObjectAttachment(TAG_KEY, "TAG_");

        RpcContext.getServiceContext().setNeedPrintRouterSnapshot(true);
        RouterSnapshotSwitcher routerSnapshotSwitcher = FrameworkModel.defaultModel().getBeanFactory().getBean(RouterSnapshotSwitcher.class);
        routerSnapshotSwitcher.addEnabledService("org.apache.dubbo.demo.DemoService");
        // route
        List<Invoker<DemoService>> result = routerChain.getSingleChain(consumerUrl, invokers, rpcInvocation)
            .route(consumerUrl, invokers, rpcInvocation);
        Assertions.assertEquals(result.size(), 1);
        Assertions.assertTrue(result.contains(invoker5));

        String snapshotLog =
            "[ Parent (Input: 6) (Current Node Output: 6) (Chain Node Output: 1) ] Input: localhost:9103,localhost:9103,localhost:9103,localhost:9103,localhost:9103 -> Chain Node Output: localhost:9103...\n" +
                "  [ MockInvokersSelector (Input: 6) (Current Node Output: 5) (Chain Node Output: 1) Router message: invocation.need.mock not set. Return normal Invokers. ] Current Node Output: localhost:9103,localhost:9103,localhost:9103,localhost:9103,localhost:9103\n" +
                "    [ StandardMeshRuleRouter (Input: 5) (Current Node Output: 4) (Chain Node Output: 1) Router message: Match App: app Subset: isolation  ] Current Node Output: localhost:9103,localhost:9103,localhost:9103,localhost:9103\n" +
                "      [ TagStateRouter (Input: 4) (Current Node Output: 3) (Chain Node Output: 1) Router message: Disable Tag Router. Reason: tagRouterRule is invalid or disabled ] Current Node Output: localhost:9103,localhost:9103,localhost:9103\n" +
                "        [ ServiceStateRouter (Input: 3) (Current Node Output: 3) (Chain Node Output: 1) Router message: null ] Current Node Output: localhost:9103,localhost:9103,localhost:9103\n" +
                "          [ ConditionStateRouter (Input: 3) (Current Node Output: 2) (Chain Node Output: 2) Router message: Match return. ] Current Node Output: localhost:9103,localhost:9103\n" +
                "          [ ProviderAppStateRouter (Input: 2) (Current Node Output: 2) (Chain Node Output: 1) Router message: Directly return. Reason: Invokers from previous router is empty or conditionRouters is empty. ] Current Node Output: localhost:9103,localhost:9103\n" +
                "            [ AppStateRouter (Input: 2) (Current Node Output: 2) (Chain Node Output: 1) Router message: null ] Current Node Output: localhost:9103,localhost:9103\n" +
                "              [ ConditionStateRouter (Input: 2) (Current Node Output: 1) (Chain Node Output: 1) Router message: Match return. ] Current Node Output: localhost:9103\n" +
                "              [ AppScriptStateRouter (Input: 1) (Current Node Output: 1) (Chain Node Output: 1) Router message: Directly return from script router. Reason: Invokers from previous router is empty or script is not enabled. Script rule is: null ] Current Node Output: localhost:9103";
        String[] snapshot = routerSnapshotSwitcher.cloneSnapshot();
        Assertions.assertTrue(snapshot[0].contains(snapshotLog));

        RpcContext.getServiceContext().setNeedPrintRouterSnapshot(false);
        result = routerChain.getSingleChain(consumerUrl, invokers, rpcInvocation)
            .route(consumerUrl, invokers, rpcInvocation);
        Assertions.assertEquals(result.size(), 1);
        Assertions.assertTrue(result.contains(invoker5));

        routerChain.destroy();
        Assertions.assertEquals(routerChain.getRouters().size(), 0);
        Assertions.assertEquals(routerChain.getStateRouters().size(), 0);

    }

    private Invoker<DemoService> createMockInvoker() {
        URL url = URL.valueOf("mock://localhost:9103/DemoInterface?remote.application=app");
        Invoker<DemoService> invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    private Invoker<DemoService> createNormalInvoker(Map<String, String> parameters) {
        URL url = URL.valueOf("dubbo://localhost:9103/DemoInterface?remote.application=app");
        if (CollectionUtils.isNotEmptyMap(parameters)) {
            url = url.addParameters(parameters);
        }
        Invoker<DemoService> invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }


    private final static String MESH_RULE1 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: DestinationRule\n" +
        "metadata: { name: demo-route }\n" +
        "spec:\n" +
        "  host: demo\n" +
        "  subsets:\n" +
        "    - labels: { env-sign: xxx, tag1: hello }\n" +
        "      name: isolation\n" +
        "    - labels: { env-sign: yyy }\n" +
        "      name: testing-trunk\n" +
        "    - labels: { env-sign: zzz }\n" +
        "      name: testing\n" +
        "  trafficPolicy:\n" +
        "    loadBalancer: { simple: ROUND_ROBIN }\n" +
        "\n";

    private final static String MESH_RULE2 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
        "kind: VirtualService\n" +
        "metadata: { name: demo-route }\n" +
        "spec:\n" +
        "  dubbo:\n" +
        "    - routedetail:\n" +
        "        - match:\n" +
        "            - attachments: \n" +
        "                dubboContext: {trafficLabel: {regex: xxx}}\n" +
        "          name: xxx-project\n" +
        "          route:\n" +
        "            - destination: {host: demo, subset: isolation}\n" +
        "      services:\n" +
        "        - {regex: DemoService}\n" +
        "  hosts: [demo]\n";

    private static final String APP_CONDITION_RULE = "scope: application\n" +
        "force: true\n" +
        "runtime: false\n" +
        "enabled: true\n" +
        "priority: 1\n" +
        "key: demo-consumer\n" +
        "conditions:\n" +
        "- => serialization=hessian2";

    private static final String SERVICE_CONDITION_RULE = "scope: service\n" +
        "force: true\n" +
        "runtime: false\n" +
        "enabled: true\n" +
        "priority: 1\n" +
        "key: org.apache.dubbo.demo.DemoService\n" +
        "conditions:\n" +
        "- => timeout=5000";
}
