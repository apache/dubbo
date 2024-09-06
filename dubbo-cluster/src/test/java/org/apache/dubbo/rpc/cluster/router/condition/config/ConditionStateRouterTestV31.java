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
import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.ConditionRuleParser;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.MultiDestConditionRouterRule;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                "dubbo://dubbo.apache.org/com.foo.BarService?region=hangzhou&env=normal");

        List<Invoker<String>> invokerList = providerUrls.stream()
                .map(url -> new MockInvoker<String>(URL.valueOf(url)))
                .collect(Collectors.toList());

        invokers = new BitList<>(invokerList);
    }

    @Test
    public void testParseRawRule() {
        String config =
                "configVersion: v3.1\n" + "scope: service\n" + "force: false\n" + "runtime: true\n" + "enabled: true\n"
                        + "key: shop\n" + "conditions:\n" + "  - from:\n" + "      match:\n" + "    to:\n"
                        + "      - match: region=$region & version=v1\n"
                        + "      - match: region=$region & version=v2\n" + "        weight: 200\n"
                        + "      - match: region=$region & version=v3\n" + "        weight: 300\n" + "  - from:\n"
                        + "      match: region=beijing & version=v1\n" + "    to:\n"
                        + "      - match: env=$env & region=beijing\n";

        AbstractRouterRule routerRule = ConditionRuleParser.parse(config);
        Assertions.assertInstanceOf(MultiDestConditionRouterRule.class, routerRule);
        MultiDestConditionRouterRule rule = (MultiDestConditionRouterRule) routerRule;
        Assertions.assertEquals(rule.getConditions().size(), 2);
        Assertions.assertEquals(rule.getConditions().get(0).getTo().size(), 3);
        Assertions.assertEquals(rule.getConditions().get(1).getTo().size(), 1);
        System.out.println("rule.getConditions() = " + rule.getConditions());
    }

    @Test
    public void testMultiplyConditionRoute() {

        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: com.foo.BarService\n"
                + "force: false\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env!=gray\n"
                + "        weight: 100";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing&version=v1"),
                invocation,
                false,
                new Holder<>());

        int count = 0;
        for (Invoker<String> invoker : invokers) {
            String url = invoker.getUrl().toString();
            if (url.contains("env") && !url.contains("gray")) {
                count++;
            }
        }
        Assertions.assertEquals(count, result.size());
    }

    @Test
    public void testRemoveDuplicatesCondition() {
        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: org.apache.dubbo.samples.CommentService\n"
                + "force: false\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env!=gray\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env!=gray\n"
                + "        weight: 100";
        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());

        int count = 0;
        for (Invoker<String> invoker : invokers) {
            String url = invoker.getUrl().toString();
            if (url.contains("env") && !url.contains("gray")) {
                count++;
            }
        }
        Assertions.assertEquals(count, result.size());
    }

    @Test
    public void testConsequentCondition() {
        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: org.apache.dubbo.samples.CommentService\n"
                + "force: false\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env!=gray\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "      match: region=beijing\n"
                + "    to:\n"
                + "      - match: region=beijing\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "    to:\n"
                + "      - match: host!=127.0.0.1";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());

        int count = 0;
        for (Invoker<String> invoker : invokers) {
            String url = invoker.getUrl().toString();
            if ((url.contains("env") && !url.contains("gray"))
                    && url.contains("region=beijing")
                    && !url.contains("127.0.0.1")) {
                count++;
            }
        }
        Assertions.assertEquals(count, result.size());
    }

    @Test
    public void testUnMatchCondition() {
        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: org.apache.dubbo.samples.CommentService\n"
                + "force: false\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: env!=gray\n"
                + "    to:\n"
                + "      - match: env=gray\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "      match: region!=beijing\n"
                + "    to:\n"
                + "      - match: region=beijing\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "    to:\n"
                + "      - match: host!=127.0.0.1";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());

        int count = 0;
        for (Invoker<String> invoker : invokers) {
            String url = invoker.getUrl().toString();
            if (!url.contains("127.0.0.1")) {
                count++;
            }
        }
        Assertions.assertEquals(count, result.size());
    }

    @Test
    public void testMatchAndRouteZero() {
        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: org.apache.dubbo.samples.CommentService\n"
                + "force: true\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env=ErrTag\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "      match: region!=beijing\n"
                + "    to:\n"
                + "      - match: region=beijing\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "    to:\n"
                + "      - match: host!=127.0.0.1";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());
        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void testMatchRouteZeroAndIgnore() {
        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: org.apache.dubbo.samples.CommentService\n"
                + "force: false\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: region=beijing\n"
                + "    to:\n"
                + "      - match: region!=beijing\n"
                + "        weight: 100\n"
                + "  - from:\n"
                + "    to:\n"
                + "      - match: host!=127.0.0.1\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env=ErrTag\n"
                + "        weight: 100";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());

        int count = 0;
        for (Invoker<String> invoker : invokers) {
            String url = invoker.getUrl().toString();
            if ((url.contains("region") && !url.contains("beijing") && !url.contains("127.0.0.1"))) {
                count++;
            }
        }
        Assertions.assertEquals(count, result.size());
    }

    @Test
    public void testTrafficDisabledAndIgnoreConditionRouteForce() {
        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: org.apache.dubbo.samples.CommentService\n"
                + "force: false\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: host=127.0.0.1\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env!=gray\n"
                + "        weight: 100\n"
                + "  - to:\n"
                + "      - match: region!=beijing";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> result = router.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());

        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void testMultiplyDestination() {
        String rawRule = "configVersion: v3.1\n" + "scope: service\n"
                + "key: org.apache.dubbo.samples.CommentService\n"
                + "force: false\n"
                + "runtime: true\n"
                + "enabled: true\n"
                + "conditions:\n"
                + "  - from:\n"
                + "      match: env=gray\n"
                + "    to:\n"
                + "      - match: env!=gray\n"
                + "        weight: 100\n"
                + "      - match: env=gray\n"
                + "        weight: 900\n"
                + "  - from:\n"
                + "      match: region=beijing\n"
                + "    to:\n"
                + "      - match: region!=beijing\n"
                + "        weight: 100\n"
                + "      - match: region=beijing\n"
                + "        weight: 200";

        ServiceStateRouter<String> router = new ServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));
        router.process(new ConfigChangedEvent("com.foo.BarService", "", rawRule, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        Map<Integer, Integer> actualDistribution = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            BitList<Invoker<String>> result = router.route(
                    invokers.clone(),
                    URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                    invocation,
                    false,
                    new Holder<>());

            actualDistribution.put(result.size(), actualDistribution.getOrDefault(result.size(), 0) + 1);
        }
        System.out.println("actualDistribution = " + actualDistribution);
        int sum = 0;
        for (Map.Entry<Integer, Integer> entry : actualDistribution.entrySet()) {
            sum += entry.getValue();
        }
        assertEquals(actualDistribution.size(), 4); // 8 6 4 2
        Assertions.assertNotNull(actualDistribution.get(8));
        Assertions.assertNotNull(actualDistribution.get(6));
        Assertions.assertNotNull(actualDistribution.get(4));
        Assertions.assertNotNull(actualDistribution.get(2));
        assertEquals(sum, 1000);
    }
}
