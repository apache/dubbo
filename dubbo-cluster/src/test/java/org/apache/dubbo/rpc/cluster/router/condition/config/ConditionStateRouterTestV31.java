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
package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConditionStateRouterTestV31 {

    private static BitList<Invoker<String>> invokers;
    @BeforeAll
    public static void setUp() {

        List<String> providerUrls = Arrays.asList(
                "dubbo://127.0.0.1/com.foo.BarService",
                "dubbo://127.0.0.1/com.foo.BarService",
                "dubbo://127.0.0.1/com.foo.BarService?env=normal",
                "dubbo://127.0.0.1/com.foo.BarService?env=normal",
                "dubbo://127.0.0.1/com.foo.BarService?env=normal",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing&env=gray",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing&env=gray",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing&env=gray",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing&env=gray",
                "dubbo://127.0.0.1/com.foo.BarService?region=beijing&env=normal",
                "dubbo://127.0.0.1/com.foo.BarService?region=hangzhou",
                "dubbo://127.0.0.1/com.foo.BarService?region=hangzhou",
                "dubbo://127.0.0.1/com.foo.BarService?region=hangzhou&env=gray",
                "dubbo://127.0.0.1/com.foo.BarService?region=hangzhou&env=gray",
                "dubbo://127.0.0.1/com.foo.BarService?region=hangzhou&env=normal",
                "dubbo://127.0.0.1/com.foo.BarService?region=hangzhou&env=normal",
                "dubbo://127.0.0.1/com.foo.BarService?region=hangzhou&env=normal",
                "dubbo://dubbo.apache.org/com.foo.BarService",
                "dubbo://dubbo.apache.org/com.foo.BarService",
                "dubbo://dubbo.apache.org/com.foo.BarService?env=normal",
                "dubbo://dubbo.apache.org/com.foo.BarService?env=normal",
                "dubbo://dubbo.apache.org/com.foo.BarService?env=normal",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing&env=gray",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing&env=gray",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing&env=gray",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing&env=gray",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=beijing&env=normal",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou&env=gray",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou&env=gray",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou&env=normal",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou&env=normal",
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou&env=normal"
        );

        List<Invoker<String>> invokerList = providerUrls.stream()
                .map(url -> new MockInvoker<String>(URL.valueOf(url)))
                .collect(Collectors.toList());

        invokers = new BitList<>(invokerList);

    }

    @Test
    public void testConditionRoutePriority() throws Exception {
//        String config = "configVersion: v3.1\n" +
//                "scope: service\n" +
//                "force: false\n" +
//                "runtime: true\n" +
//                "enabled: true\n" +
//                "key: shop\n" +
//                "conditions:\n" +
//                "  - from:\n" +
//                "      match:\n" +
//                "    to:\n" +
//                "      - match: region=beijing & version=v1\n" +
//                "      - match: region=beijing & version=v2\n" +
//                "        weight: 200\n" +
//                "      - match: region=beijing & version=v3\n" +
//                "        weight: 300\n" +
//                "    force: false\n" +
//                "    ratio: 20\n" +
//                "    priority: 20\n" +
//                "  - from:\n" +
//                "      match: region=beijing & version=v1\n" +
//                "    to:\n" +
//                "      - match: env=gray & region=beijing\n" +
//                "    force: false\n" +
//                "    priority: 100\n";
        String config = "configVersion: v3.1\n" +
                "scope: service\n" +
                "force: false\n" +
                "runtime: true\n" +
                "enabled: true\n" +
                "key: shop\n" +
                "conditions:\n" +
                "  - from:\n" +
                "      match:\n" +
                "    to:\n" +
                "      - match: region=$region & version=v1\n" +
                "      - match: region=$region & version=v2\n" +
                "        weight: 200\n" +
                "      - match: region=$region & version=v3\n" +
                "        weight: 300\n" +
                "    force: false\n" +
                "    ratio: 20\n" +
                "    priority: 20\n" +
                "  - from:\n" +
                "      match: region=beijing & version=v1\n" +
                "    to:\n" +
                "      - match: env=$env & region=beijing\n" +
                "    force: false\n" +
                "    priority: 100\n";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing&version=v1"));
        router.process(new ConfigChangedEvent("com.foo.BarService","",config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing&version=v1"), invocation, false, new Holder<>());

        int expectedLen = 0;
        for (Invoker<?> invoker : invokers) {
            if ("beijing".equals(invoker.getUrl().getParameter("region")) && "gray".equals(invoker.getUrl().getParameter("env"))) {
                expectedLen++;
            }
        }

        if (invokers.size() * 100 / expectedLen <= 20) {
            expectedLen = 0;
        }

        System.out.println("expectedLen = " + expectedLen); // expectedLen = 8

        Assertions.assertEquals(expectedLen, result.size());
    }

    @Test
    public void testConditionRouteTrafficDisable() throws Exception {
        String config = "configVersion: v3.1\n" +
                "scope: service\n" +
                "force: true\n" +
                "runtime: true\n" +
                "enabled: true\n" +
                "key: shop\n" +
                "conditions:\n" +
                "  - from:\n" +
                "      match:\n" +
                "    to:\n" +
                "      - match: region=$region & version=v1\n" +
                "      - match: region=$region & version=v2\n" +
                "        weight: 200\n" +
                "      - match: region=$region & version=v3\n" +
                "        weight: 300\n" +
                "    force: false\n" +
                "    ratio: 20\n" +
                "    priority: 20\n" +
                "  - from:\n" +
                "      match: region=beijing & version=v1\n" +
                "    to:\n" +
                "    force: true\n" +
                "    ratio: 20\n" +
                "    priority: 100\n";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing&version=v1"));
        router.process(new ConfigChangedEvent("com.foo.BarService","",config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("echo");

        BitList<Invoker<String>> result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing&version=v1"), invocation, false, new Holder<>());

        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void testConditionRouteRegionPriority() throws Exception {
        String config = "configVersion: v3.1\n" +
                "scope: service\n" +
                "force: true\n" +
                "runtime: true\n" +
                "enabled: true\n" +
                "key: shop\n" +
                "conditions:\n" +
                "  - from:\n" +
                "      match:\n" +
                "    to:\n" +
                "      - match: region=$region & env=$env\n";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing&version=v1"));
        router.process(new ConfigChangedEvent("com.foo.BarService","",config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing&version=v1"), invocation, false, new Holder<>());

        int expectedLen = 0;
        for (Invoker<?> invoker : invokers) {
            if ("gray".equals(invoker.getUrl().getParameter("env")) && "beijing".equals(invoker.getUrl().getParameter("region"))) {
                expectedLen++;
            }
        }

        Assertions.assertEquals(expectedLen, result.size());

        result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=hangzhou"), invocation, false, new Holder<>());
        expectedLen = 0;
        for (Invoker<?> invoker : invokers) {
            if ("gray".equals(invoker.getUrl().getParameter("env")) && "hangzhou".equals(invoker.getUrl().getParameter("region"))) {
                expectedLen++;
            }
        }

        Assertions.assertEquals(expectedLen, result.size());

        result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=normal&region=shanghai"), invocation, false, new Holder<>());
        expectedLen = 0;
        for (Invoker<?> invoker : invokers) {
            if ("normal".equals(invoker.getUrl().getParameter("env")) && "shanghai".equals(invoker.getUrl().getParameter("region"))) {
                expectedLen++;
            }
        }

        Assertions.assertEquals(expectedLen, result.size());
    }


    @Test
    public void testConditionRouteRegionPriorityFail() throws Exception {
        String config = "configVersion: v3.1\n" +
                "scope: service\n" +
                "force: true\n" +
                "runtime: true\n" +
                "enabled: true\n" +
                "key: shop\n" +
                "conditions:\n" +
                "  - from:\n" +
                "      match:\n" +
                "    to:\n" +
                "      - match: region=$region & env=$env\n" +
                "    ratio: 100\n";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService","",config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"), invocation, false, new Holder<>());

        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void testConditionRouteMatchFail() throws Exception {
        String config = "configVersion: v3.1\n" +
                "scope: service\n" +
                "force: false\n" +
                "runtime: true\n" +
                "enabled: true\n" +
                "key: shop\n" +
                "conditions:\n" +
                "  - from:\n" +
                "      match:\n" +
                "    to:\n" +
                "      - match: region=$region & env=$env & err-tag=Err-tag\n" +
                "  - from:\n" +
                "      match:\n" +
                "    trafficDisable: true\n" +
                "    to:\n" +
                "      - match:\n";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService","",config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("errMethod");

        BitList<Invoker<String>> result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"), invocation, false, new Holder<>());

        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void testConditionRouteBanSpecialTraffic() throws Exception {
        String config = "configVersion: v3.1\n" +
                "scope: service\n" +
                "force: true\n" +
                "runtime: true\n" +
                "enabled: true\n" +
                "key: shop\n" +
                "conditions:\n" +
                "  - from:\n" +
                "      match: env=gray\n" +
                "    to:\n" +
                "      - match:\n" +
                "    force: true\n" +
                "    priority: 100\n" +
                "  - from:\n" +
                "      match:\n" +
                "    to:\n" +
                "      - match:\n" +
                "    force: true\n" +
                "    priority: 100\n";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService","",config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("errMethod");

        BitList<Invoker<String>> result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"), invocation, false, new Holder<>());

        Assertions.assertEquals(invokers.size(), result.size());
    }

    @Test
    public void testApplicationConditionRouteBanSpecialTraffic() throws Exception {
        String config = "configVersion: v3.1\n" +
                "scope: application\n" +
                "force: true\n" +
                "runtime: true\n" +
                "enabled: true\n" +
                "key: shop\n" +
                "conditions:\n" +
                "  - from:\n" +
                "      match: env=gray\n" +
                "    to:\n" +
                "      - match:\n" +
                "    force: true\n" +
                "    priority: 100\n" +
                "  - from:\n" +
                "      match:\n" +
                "    to:\n" +
                "      - match:\n" +
                "    force: true\n" +
                "    priority: 100\n";

        AppStateRouter<String> router = new AppStateRouter<>(URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService","",config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("errMethod");

        BitList<Invoker<String>> result = router.route(invokers.clone(), URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"), invocation, false, new Holder<>());

        Assertions.assertEquals(invokers.size(), result.size());
    }
}
