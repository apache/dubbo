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
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MeshRuleRouterTest {
    private final static String rule1 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
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
    private final static String rule2 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
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
        "        - match:\n" +
        "            - attachments: \n" +
        "                dubboContext: {trafficLabel: {regex: testing-trunk}}\n" +
        "          name: testing-trunk\n" +
        "          route:\n" +
        "            - destination:\n" +
        "                host: demo\n" +
        "                subset: testing-trunk\n" +
        "                fallback:\n" +
        "                  host: demo\n" +
        "                  subset: testing\n" +
        "        - name: testing\n" +
        "          route:\n" +
        "            - destination: {host: demo, subset: testing}\n" +
        "      services:\n" +
        "        - {regex: ccc}\n" +
        "  hosts: [demo]\n";
    private final static String rule3 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
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
        "        - match:\n" +
        "            - attachments: \n" +
        "                dubboContext: {trafficLabel: {regex: testing-trunk}}\n" +
        "          name: testing-trunk\n" +
        "          route:\n" +
        "            - destination:\n" +
        "                host: demo\n" +
        "                subset: testing-trunk\n" +
        "                fallback:\n" +
        "                  host: demo\n" +
        "                  subset: testing\n" +
        "        - match:\n" +
        "            - attachments: \n" +
        "                dubboContext: {trafficLabel: {regex: testing}}\n" +
        "          name: testing\n" +
        "          route:\n" +
        "            - destination: {host: demo, subset: testing}\n" +
        "  hosts: [demo]\n";
    private final static String rule4 = "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
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
        "        - match:\n" +
        "            - attachments: \n" +
        "                dubboContext: {trafficLabel: {regex: testing-trunk}}\n" +
        "          name: testing-trunk\n" +
        "          route:\n" +
        "            - destination:\n" +
        "                host: demo\n" +
        "                subset: testing-trunk\n" +
        "                fallback:\n" +
        "                  destination:\n" +
        "                    host: demo\n" +
        "                    subset: testing\n" +
        "            - weight: 10\n" +
        "              destination:\n" +
        "                host: demo\n" +
        "                subset: isolation\n" +
        "        - match:\n" +
        "            - attachments: \n" +
        "                dubboContext: {trafficLabel: {regex: testing}}\n" +
        "          name: testing\n" +
        "          route:\n" +
        "            - destination: {host: demo, subset: testing}\n" +
        "  hosts: [demo]\n";

    private ModuleModel originModel;
    private ModuleModel moduleModel;
    private MeshRuleManager meshRuleManager;
    private Set<TracingContextProvider> tracingContextProviders;
    private URL url;

    @BeforeEach
    public void setup() {
        originModel = ApplicationModel.defaultModel().getDefaultModule();
        moduleModel = Mockito.spy(originModel);

        ScopeBeanFactory originBeanFactory = originModel.getBeanFactory();
        ScopeBeanFactory beanFactory = Mockito.spy(originBeanFactory);
        when(moduleModel.getBeanFactory()).thenReturn(beanFactory);

        meshRuleManager = Mockito.mock(MeshRuleManager.class);
        when(beanFactory.getBean(MeshRuleManager.class)).thenReturn(meshRuleManager);

        ExtensionLoader<TracingContextProvider> extensionLoader = Mockito.mock(ExtensionLoader.class);
        tracingContextProviders = new HashSet<>();
        when(extensionLoader.getSupportedExtensionInstances()).thenReturn(tracingContextProviders);
        when(moduleModel.getExtensionLoader(TracingContextProvider.class)).thenReturn(extensionLoader);

        url = URL.valueOf("test://localhost/DemoInterface").setScopeModel(moduleModel);
    }

    @AfterEach
    public void teardown() {
        originModel.destroy();
    }

    private Invoker<Object> createInvoker(String app) {
        URL url = URL.valueOf("dubbo://localhost/DemoInterface?" + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    private Invoker<Object> createInvoker(Map<String, String> parameters) {
        URL url = URL.valueOf("dubbo://localhost/DemoInterface?remote.application=app1").addParameters(parameters);
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }


    @Test
    public void testNotify() {
        StandardMeshRuleRouter<Object> meshRuleRouter = new StandardMeshRuleRouter<>(url);
        meshRuleRouter.notify(null);
        assertEquals(0, meshRuleRouter.getRemoteAppName().size());

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(createInvoker(""), createInvoker("unknown"), createInvoker("app1")));

        meshRuleRouter.notify(invokers);

        assertEquals(1, meshRuleRouter.getRemoteAppName().size());
        assertTrue(meshRuleRouter.getRemoteAppName().contains("app1"));
        assertEquals(invokers, meshRuleRouter.getInvokerList());

        verify(meshRuleManager, times(1)).register("app1", meshRuleRouter);

        invokers = new BitList<>(Arrays.asList(createInvoker("unknown"), createInvoker("app2")));
        meshRuleRouter.notify(invokers);
        verify(meshRuleManager, times(1)).register("app2", meshRuleRouter);
        verify(meshRuleManager, times(1)).unregister("app1", meshRuleRouter);
        assertEquals(invokers, meshRuleRouter.getInvokerList());

        meshRuleRouter.stop();
        verify(meshRuleManager, times(1)).unregister("app2", meshRuleRouter);
    }

    @Test
    public void testRuleChange() {
        StandardMeshRuleRouter<Object> meshRuleRouter = new StandardMeshRuleRouter<>(url);

        Yaml yaml = new Yaml(new SafeConstructor());
        List<Map<String, Object>> rules = new LinkedList<>();
        rules.add(yaml.load(rule1));

        meshRuleRouter.onRuleChange("app1", rules);
        assertEquals(0, meshRuleRouter.getMeshRuleCache().getAppToVDGroup().size());

        rules.add(yaml.load(rule2));
        meshRuleRouter.onRuleChange("app1", rules);
        assertEquals(1, meshRuleRouter.getMeshRuleCache().getAppToVDGroup().size());
        assertTrue(meshRuleRouter.getMeshRuleCache().getAppToVDGroup().containsKey("app1"));

        meshRuleRouter.onRuleChange("app2", rules);
        assertEquals(2, meshRuleRouter.getMeshRuleCache().getAppToVDGroup().size());
        assertTrue(meshRuleRouter.getMeshRuleCache().getAppToVDGroup().containsKey("app1"));
        assertTrue(meshRuleRouter.getMeshRuleCache().getAppToVDGroup().containsKey("app2"));

        meshRuleRouter.clearRule("app1");
        assertEquals(1, meshRuleRouter.getMeshRuleCache().getAppToVDGroup().size());
        assertTrue(meshRuleRouter.getMeshRuleCache().getAppToVDGroup().containsKey("app2"));
    }

    @Test
    public void testRoute1() {
        StandardMeshRuleRouter<Object> meshRuleRouter = new StandardMeshRuleRouter<>(url);
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(createInvoker(""), createInvoker("unknown"), createInvoker("app1")));
        assertEquals(invokers, meshRuleRouter.route(invokers.clone(), null, null, false, null));
        Holder<String> message = new Holder<>();
        meshRuleRouter.doRoute(invokers.clone(), null, null, true, null, message);
        assertEquals("MeshRuleCache has not been built. Skip route.", message.get());
    }

    @Test
    public void testRoute2() {
        StandardMeshRuleRouter<Object> meshRuleRouter = new StandardMeshRuleRouter<>(url);

        Yaml yaml = new Yaml(new SafeConstructor());
        List<Map<String, Object>> rules = new LinkedList<>();
        rules.add(yaml.load(rule1));
        rules.add(yaml.load(rule2));
        meshRuleRouter.onRuleChange("app1", rules);

        Invoker<Object> isolation = createInvoker(new HashMap<String, String>() {{
            put("env-sign", "xxx");
            put("tag1", "hello");
        }});
        Invoker<Object> testingTrunk = createInvoker(Collections.singletonMap("env-sign", "yyy"));
        Invoker<Object> testing = createInvoker(Collections.singletonMap("env-sign", "zzz"));

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(isolation, testingTrunk, testing));
        meshRuleRouter.notify(invokers);

        RpcInvocation rpcInvocation = new RpcInvocation();

        rpcInvocation.setServiceName("ccc");
        rpcInvocation.setAttachment("trafficLabel", "xxx");
        assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
        assertEquals(isolation, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));
        Holder<String> message = new Holder<>();
        meshRuleRouter.doRoute(invokers.clone(), null, rpcInvocation, true, null, message);
        assertEquals("Match App: app1 Subset: isolation ", message.get());

        rpcInvocation.setAttachment("trafficLabel", "testing-trunk");
        assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
        assertEquals(testingTrunk, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));

        rpcInvocation.setAttachment("trafficLabel", null);
        assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
        assertEquals(testing, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));

        rpcInvocation.setServiceName("aaa");
        assertEquals(invokers, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null));
        message = new Holder<>();
        meshRuleRouter.doRoute(invokers.clone(), null, rpcInvocation, true, null, message);
        assertEquals("Empty protection after routed.", message.get());

        rules = new LinkedList<>();
        rules.add(yaml.load(rule1));
        rules.add(yaml.load(rule3));
        meshRuleRouter.onRuleChange("app1", rules);

        rpcInvocation.setServiceName("ccc");
        rpcInvocation.setAttachment("trafficLabel", "xxx");
        assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
        assertEquals(isolation, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));

        rpcInvocation.setAttachment("trafficLabel", "testing-trunk");
        assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
        assertEquals(testingTrunk, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));

        rpcInvocation.setAttachment("trafficLabel", "testing");
        assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
        assertEquals(testing, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));

        rpcInvocation.setServiceName("aaa");
        assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
        assertEquals(testing, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));

        rpcInvocation.setAttachment("trafficLabel",null);
        assertEquals(invokers, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null));

        rules = new LinkedList<>();
        rules.add(yaml.load(rule1));
        rules.add(yaml.load(rule4));
        meshRuleRouter.onRuleChange("app1", rules);

        rpcInvocation.setAttachment("trafficLabel", "testing-trunk");

        int testingCount = 0;
        int isolationCount = 0;
        for (int i = 0; i < 1000; i++) {
            BitList<Invoker<Object>> result = meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null);
            assertEquals(1, result.size());
            if (result.contains(testing)) {
                testingCount++;
            } else {
                isolationCount++;
            }
        }
        assertTrue(isolationCount > testingCount * 10);

        invokers.removeAll(Arrays.asList(isolation, testingTrunk));
        for (int i = 0; i < 1000; i++) {
            assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
            assertEquals(testing, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));
        }

        meshRuleRouter.notify(invokers);

        for (int i = 0; i < 1000; i++) {
            assertEquals(1, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).size());
            assertEquals(testing, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null).get(0));
        }

        Invoker<Object> mock = createInvoker(Collections.singletonMap("env-sign", "mock"));
        invokers = new BitList<>(Arrays.asList(isolation, testingTrunk, testing, mock));

        meshRuleRouter.notify(invokers);
        invokers.removeAll(Arrays.asList(isolation, testingTrunk, testing));
        assertEquals(invokers, meshRuleRouter.route(invokers.clone(), null, rpcInvocation, false, null));

    }
}
