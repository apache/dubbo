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
package org.apache.dubbo.rpc.cluster.router.affinity;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.affinity.config.AffinityServiceStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AffinityRouteTest {
    private static final Logger logger = LoggerFactory.getLogger(AffinityRouteTest.class);

    private static BitList<Invoker<String>> invokers;

    private static List<String> providerUrls;

    @BeforeAll
    public static void setUp() {

        providerUrls = Arrays.asList(
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

    public List<String> filtrate(List<String> invokers, String key) {

        return invokers.stream().filter(invoker -> invoker.contains(key)).collect(Collectors.toList());
    }

    @Test
    void testMetAffinityRoute() {
        String config = "configVersion: v3.1\n"
                + "scope: service\n"
                + "key: service.apache.com\n"
                + "enabled: true\n"
                + "runtime: true\n"
                + "affinityAware:\n"
                + "  key: region\n"
                + "  ratio: 20\n";

        AffinityServiceStateRouter<String> affinityRoute = new AffinityServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));

        affinityRoute.process(new ConfigChangedEvent("com.foo.BarService", "", config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> res = affinityRoute.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());
        List<String> filtered = filtrate(new ArrayList<String>(providerUrls), "region=beijing");

        assertEquals(filtered.size(), res.size());
        logger.info("The affinity routing condition is met and the result is routed");
    }

    @Test
    void testUnMetAffinityRoute() {
        String config = "configVersion: v3.1\n"
                + "scope: service\n"
                + "key: service.apache.com\n"
                + "enabled: true\n"
                + "runtime: true\n"
                + "affinityAware:\n"
                + "  key: region\n"
                + "  ratio: 80\n";

        AffinityServiceStateRouter<String> affinityRoute = new AffinityServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));

        affinityRoute.process(new ConfigChangedEvent("com.foo.BarService", "", config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> res = affinityRoute.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());
        List<String> filtered = filtrate(new ArrayList<String>(providerUrls), "region=beijing");

        assertEquals(invokers.size(), res.size());
        logger.info("The affinity routing condition was not met and the result was not routed");
    }

    @Test
    void testRatioEqualsAffinityRoute() {
        String config = "configVersion: v3.1\n"
                + "scope: service\n"
                + "key: service.apache.com\n"
                + "enabled: true\n"
                + "runtime: true\n"
                + "affinityAware:\n"
                + "  key: region\n"
                + "  ratio: 40\n";

        AffinityServiceStateRouter<String> affinityRoute = new AffinityServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));

        affinityRoute.process(new ConfigChangedEvent("com.foo.BarService", "", config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> res = affinityRoute.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());
        List<String> filtered = filtrate(new ArrayList<String>(providerUrls), "region=beijing");

        assertEquals(filtered.size(), res.size());
        logger.info("The affinity routing condition is met and the result is routed");
    }

    @Test
    void testRatioNotEqualsAffinityRoute() {
        String config = "configVersion: v3.1\n"
                + "scope: service\n"
                + "key: service.apache.com\n"
                + "enabled: true\n"
                + "runtime: true\n"
                + "affinityAware:\n"
                + "  key: region\n"
                + "  ratio: 40.1\n";

        AffinityServiceStateRouter<String> affinityRoute = new AffinityServiceStateRouter<>(
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"));

        affinityRoute.process(new ConfigChangedEvent("com.foo.BarService", "", config, ConfigChangeType.ADDED));

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getComment");

        BitList<Invoker<String>> res = affinityRoute.route(
                invokers.clone(),
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?env=gray&region=beijing"),
                invocation,
                false,
                new Holder<>());
        List<String> filtered = filtrate(new ArrayList<String>(providerUrls), "region=beijing");

        assertEquals(invokers.size(), res.size());
        logger.info("The affinity routing condition was not met and the result was not routed");
    }
}
