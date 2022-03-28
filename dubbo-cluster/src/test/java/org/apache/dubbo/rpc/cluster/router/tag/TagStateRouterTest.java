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

package org.apache.dubbo.rpc.cluster.router.tag;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRouterRule;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRuleParser;
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

import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.mockito.Mockito.when;

public class TagStateRouterTest {
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
    public void testTagRoutePickInvokers() {
        StateRouter router = new TagStateRouterFactory().getRouter(TagRouterRule.class, url);

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
        List<Invoker<String>> filteredInvokers = router.route(invokers, invokers.get(0).getUrl(), invocation, false, new Holder<>());
        Assertions.assertEquals(1, filteredInvokers.size());
        Assertions.assertEquals(invoker1, filteredInvokers.get(0));
    }

    /**
     * TagRouterRule parse test when the tags addresses is null
     *
     * <pre>
     *     ~ -> null
     *     null -> null
     * </pre>
     */
    @Test
    public void tagRouterRuleParseTest() {
        String tagRouterRuleConfig = "---\n" +
            "force: false\n" +
            "runtime: true\n" +
            "enabled: false\n" +
            "priority: 1\n" +
            "key: demo-provider\n" +
            "tags:\n" +
            "  - name: tag1\n" +
            "    addresses: null\n" +
            "  - name: tag2\n" +
            "    addresses: [\"30.5.120.37:20880\"]\n" +
            "  - name: tag3\n" +
            "    addresses: []\n" +
            "  - name: tag4\n" +
            "    addresses: ~\n" +
            "...";

        TagRouterRule tagRouterRule = TagRuleParser.parse(tagRouterRuleConfig);

        // assert tags
        assert tagRouterRule.getKey().equals("demo-provider");
        assert tagRouterRule.getPriority() == 1;
        assert tagRouterRule.getTagNames().contains("tag1");
        assert tagRouterRule.getTagNames().contains("tag2");
        assert tagRouterRule.getTagNames().contains("tag3");
        assert tagRouterRule.getTagNames().contains("tag4");
        // assert addresses
        assert tagRouterRule.getAddresses().contains("30.5.120.37:20880");
        assert tagRouterRule.getTagnameToAddresses().get("tag1") == null;
        assert tagRouterRule.getTagnameToAddresses().get("tag2").size() == 1;
        assert tagRouterRule.getTagnameToAddresses().get("tag3") == null;
        assert tagRouterRule.getTagnameToAddresses().get("tag4") == null;
        assert tagRouterRule.getAddresses().size() == 1;
    }
}
