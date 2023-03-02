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

package org.apache.dubbo.rpc.cluster.router.circuitBreaker;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.circuitBreaker.model.CircuitBreakerRule;
import org.apache.dubbo.rpc.cluster.router.circuitBreaker.model.CircuitBreakerRuleParser;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.CIRCUIT_BREAKER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.mockito.Mockito.when;

class CircuitBreakerRouterTest {
    private URL url;
    private ModuleModel originModel;
    private ModuleModel moduleModel;
    private Set<TracingContextProvider> tracingContextProviders;

    @BeforeEach
    public void setup() {
        originModel = ApplicationModel.defaultModel().getDefaultModule();
        moduleModel = Mockito.spy(originModel);

        ScopeBeanFactory originBeanFactory = originModel.getBeanFactory();
        ScopeBeanFactory beanFactory = Mockito.spy(originBeanFactory);
        when(moduleModel.getBeanFactory()).thenReturn(beanFactory);


        ExtensionLoader<TracingContextProvider> extensionLoader = Mockito.mock(ExtensionLoader.class);
        tracingContextProviders = new HashSet<>();
        when(extensionLoader.getSupportedExtensionInstances()).thenReturn(tracingContextProviders);
        when(moduleModel.getExtensionLoader(TracingContextProvider.class)).thenReturn(extensionLoader);

        url = URL.valueOf("test://localhost/DemoInterface").setScopeModel(moduleModel);
    }

    @Test
    void testTagRoutePickInvokers() {
        StateRouter router = new CircuitBreakerRouterFactory().getRouter(CircuitBreakerRule.class, url);

        List<Invoker<String>> originInvokers = new ArrayList<>();

        URL url1 = URL.valueOf("test://127.0.0.1:7777/DemoInterface?dubbo.tag=tag2").setScopeModel(moduleModel);
        URL url2 = URL.valueOf("test://127.0.0.1:7778/DemoInterface").setScopeModel(moduleModel);
        URL url3 = URL.valueOf("test://127.0.0.1:7779/DemoInterface").setScopeModel(moduleModel);
        Invoker<String> invoker1 = new MockInvoker<>(url1, true);
        Invoker<String> invoker2 = new MockInvoker<>(url2, true);
        Invoker<String> invoker3 = new MockInvoker<>(url3, true);
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setAttachment(TAG_KEY, "tag2");
        List<Invoker<String>> filteredInvokers = router.route(invokers.clone(), invokers.get(0).getUrl(), invocation, false, new Holder<>());
        Assertions.assertEquals(1, filteredInvokers.size());
        Assertions.assertEquals(invoker1, filteredInvokers.get(0));
    }

    @Test
    void testTagRouteWithDynamicRuleV3() {
        CircuitBreakerRouter router = (CircuitBreakerRouter) new CircuitBreakerRouterFactory().getRouter(CircuitBreakerRule.class, url);
        router = Mockito.spy(router);

        List<Invoker<String>> originInvokers = new ArrayList<>();

        URL url1 = URL.valueOf("test://127.0.0.1:7777/DemoInterface?application=foo&dubbo.tag=tag2&match_key=value").setScopeModel(moduleModel);
        URL url2 = URL.valueOf("test://127.0.0.1:7778/DemoInterface?application=foo&match_key=value").setScopeModel(moduleModel);
        URL url3 = URL.valueOf("test://127.0.0.1:7779/DemoInterface?application=foo").setScopeModel(moduleModel);
        Invoker<String> invoker1 = new MockInvoker<>(url1, true);
        Invoker<String> invoker2 = new MockInvoker<>(url2, true);
        Invoker<String> invoker3 = new MockInvoker<>(url3, true);
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        RpcInvocation invocation = new RpcInvocation();
//        invocation.setAttachment(TAG_KEY, "tag2");
        invocation.setAttachment(CIRCUIT_BREAKER_KEY, "tag2");
        CircuitBreakerRule rule = getCircuitBreakerRule();
        Mockito.when(router.getInvokers()).thenReturn(invokers);
        rule.init(router);
        router.setCircuitBreakerRule(rule);
        List<Invoker<String>> filteredInvokers = router.route(invokers, invokers.get(0).getUrl(), invocation, false, new Holder<>());
        Assertions.assertEquals(2, filteredInvokers.size());
//        Assertions.(invoker1, filteredInvokers.get(0));
    }

    /**
     * CircuitBreakerRule parse test when the tags addresses is null
     *
     * <pre>
     *     ~ -> null
     *     null -> null
     * </pre>
     */
    /**
     * configVersion: v3.0
     * force: true
     * enabled: true
     * key: shop-detail
     * circuitBreaker:
     *   - name: gray
     *     match:
     *       - key: env
     *         value:
     *           exact: gray
     */
    @Test
    void CircuitBreakerRuleParseTest() {
        String circuitBreakerRuleConfig = "---\n" +
            "force: false\n" +
            "runtime: true\n" +
            "enabled: false\n" +
            "priority: 1\n" +
            "key: demo-provider\n" +
            "circuitBreakers:\n" +
            "  - name: tag1\n" +
            "    addresses: null\n" +
            "  - name: tag2\n" +
            "    addresses: [\"30.5.120.37:20880\"]\n" +
            "  - name: tag3\n" +
            "    addresses: []\n" +
            "  - name: tag4\n" +
            "    addresses: ~\n" +
            "...";

        CircuitBreakerRule circuitBreakerRule = CircuitBreakerRuleParser.parse(circuitBreakerRuleConfig);
        CircuitBreakerRouter<?> router = Mockito.mock(CircuitBreakerRouter.class);
        Mockito.when(router.getInvokers()).thenReturn(BitList.emptyList());
        circuitBreakerRule.init(router);

        // assert tags
        assert circuitBreakerRule.getKey().equals("demo-provider");
        assert circuitBreakerRule.getPriority() == 1;
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag1");
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag2");
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag3");
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag4");
        // assert addresses
        assert circuitBreakerRule.getAddresses().contains("30.5.120.37:20880");
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag1") == null;
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag2").size() == 1;
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag3") == null;
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag4") == null;
        assert circuitBreakerRule.getAddresses().size() == 1;
    }


    @Test
    void CircuitBreakerRuleParseTestV3() {
        String circuitBreakerRuleConfig = "---\n" +
            "configVersion: v3.0\n" +
            "force: false\n" +
            "runtime: true\n" +
            "enabled: true\n" +
            "priority: 1\n" +
            "key: demo-provider\n" +
            "circuitBreakers:\n" +
            "  - name: tag1\n" +
            "    match:\n" +
            "    - key: match_key1\n" +
            "      value:\n" +
            "       exact: value1\n" +
            "  - name: tag2\n" +
            "    addresses:\n" +
            "     - \"10.20.3.3:20880\"\n" +
            "     - \"10.20.3.4:20880\"\n" +
            "    match:\n" +
            "    - key: match_key2\n" +
            "      value:\n" +
            "       exact: value2\n" +
            "  - name: tag3\n" +
            "    match:\n" +
            "    - key: match_key2\n" +
            "      value:\n" +
            "       exact: value2\n" +
            "  - name: tag4\n" +
            "    match:\n" +
            "    - key: not_exist\n" +
            "      value:\n" +
            "       exact: not_exist\n" +
            "  - name: tag5\n" +
            "    match:\n" +
            "    - key: match_key2\n" +
            "      value:\n" +
            "       wildcard: \"*\"\n" +
            "...";

        CircuitBreakerRule circuitBreakerRule = CircuitBreakerRuleParser.parse(circuitBreakerRuleConfig);
        CircuitBreakerRouter<String> router = Mockito.mock(CircuitBreakerRouter.class);
        Mockito.when(router.getInvokers()).thenReturn(getInvokers());
        circuitBreakerRule.init(router);

        // assert tags
        assert circuitBreakerRule.getKey().equals("demo-provider");
        assert circuitBreakerRule.getPriority() == 1;
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag1");
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag2");
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag3");
        assert circuitBreakerRule.getCircuitBreakerNames().contains("tag4");
        // assert addresses
        assert circuitBreakerRule.getAddresses().size() == 2;
        assert circuitBreakerRule.getAddresses().contains("10.20.3.3:20880");
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag1").size() == 2;
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag2").size() == 2;
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag3").size() == 1;
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag5").size() == 1;
        assert circuitBreakerRule.getCircuitBreakernameToAddresses().get("tag4") == null;

    }

    public BitList<Invoker<String>> getInvokers() {
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf(
            "dubbo://10.20.3.3:20880/com.foo.BarService?match_key1=value1&match_key2=value2"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService?match_key1=value1"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);
        return invokers;
    }

    private CircuitBreakerRule getCircuitBreakerRule() {
        String circuitBreakerRuleConfig = "---\n" +
            "configVersion: v3.0\n" +
            "force: false\n" +
            "runtime: true\n" +
            "enabled: true\n" +
            "priority: 1\n" +
            "key: demo-provider\n" +
            "circuitBreakers:\n" +
            "  - name: tag2\n" +
            "    match:\n" +
            "    - key: match_key\n" +
            "      value:\n" +
            "       exact: value\n" +
            "...";

        CircuitBreakerRule circuitBreakerRule = CircuitBreakerRuleParser.parse(circuitBreakerRuleConfig);
        return circuitBreakerRule;
    }
}
