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
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that building another service's route chain does not change the first service's routing result.
 *
 * @date 2026-10-26
 */
class AppStateRouterFactoryTest {

    @Test
    void orderChainKeepsItsRouteAfterUserChainIsBuilt() {
        assertFirstServiceChainKeepsItsRoute("OrderService", "UserService");
    }

    @Test
    void userChainKeepsItsRouteAfterOrderChainIsBuilt() {
        assertFirstServiceChainKeepsItsRoute("UserService", "OrderService");
    }

    private void assertFirstServiceChainKeepsItsRoute(String firstService, String secondService) {
        ModuleModel moduleModel = ApplicationModel.defaultModel().getDefaultModule();
        URL firstUrl = consumerUrl(firstService, moduleModel);
        URL secondUrl = consumerUrl(secondService, moduleModel);
        AppStateRouterFactory factory = new AppStateRouterFactory();

        // The first service has a downstream filtering router, as it would for a script rule.
        SingleRouterChain<String> firstChain = chain(
                factory.getRouter(String.class, firstUrl), new PortFilterRouter(firstUrl, 23982));
        Invoker<String> firstV1 = invoker(firstService, 23981, moduleModel);
        Invoker<String> firstV2 = invoker(firstService, 23982, moduleModel);
        BitList<Invoker<String>> firstInvokers = new BitList<>(Arrays.asList(firstV1, firstV2));
        firstChain.setInvokers(firstInvokers);
        assertEquals(
                Collections.singletonList(firstV2),
                firstChain.simpleRoute(firstUrl, firstInvokers, new RpcInvocation()));

        // Building a second service chain must not replace the first chain's successor.
        SingleRouterChain<String> secondChain =
                chain(factory.getRouter(String.class, secondUrl), new PassThroughRouter(secondUrl));
        Invoker<String> secondInvoker = invoker(secondService, 23983, moduleModel);
        BitList<Invoker<String>> secondInvokers = new BitList<>(Collections.singletonList(secondInvoker));
        secondChain.setInvokers(secondInvokers);
        assertEquals(
                Collections.singletonList(secondInvoker),
                secondChain.simpleRoute(secondUrl, secondInvokers, new RpcInvocation()));
        assertEquals(
                Collections.singletonList(firstV2),
                firstChain.simpleRoute(firstUrl, firstInvokers, new RpcInvocation()));
    }

    private URL consumerUrl(String service, ModuleModel moduleModel) {
        return URL.valueOf("dubbo://127.0.0.1:20880/" + service + "?application=router-chain-consumer")
                .setScopeModel(moduleModel);
    }

    private Invoker<String> invoker(String service, int port, ModuleModel moduleModel) {
        return new MockInvoker<>(
                URL.valueOf("dubbo://127.0.0.1:" + port + "/" + service).setScopeModel(moduleModel), true);
    }

    private SingleRouterChain<String> chain(StateRouter<String> appRouter, StateRouter<String> nextRouter) {
        return new SingleRouterChain<>(
                Collections.emptyList(), Arrays.asList(appRouter, nextRouter), true, null);
    }

    private static class PassThroughRouter extends AbstractStateRouter<String> {
        PassThroughRouter(URL url) {
            super(url);
        }

        @Override
        protected BitList<Invoker<String>> doRoute(
                BitList<Invoker<String>> invokers,
                URL url,
                Invocation invocation,
                boolean needToPrintMessage,
                Holder<RouterSnapshotNode<String>> nodeHolder,
                Holder<String> messageHolder) {
            return invokers;
        }
    }

    private static class PortFilterRouter extends AbstractStateRouter<String> {
        private final int port;

        PortFilterRouter(URL url, int port) {
            super(url);
            this.port = port;
        }

        @Override
        protected BitList<Invoker<String>> doRoute(
                BitList<Invoker<String>> invokers,
                URL url,
                Invocation invocation,
                boolean needToPrintMessage,
                Holder<RouterSnapshotNode<String>> nodeHolder,
                Holder<String> messageHolder) {
            List<Invoker<String>> selected = new ArrayList<>();
            for (Invoker<String> invoker : invokers) {
                if (invoker.getUrl().getPort() == port) {
                    selected.add(invoker);
                }
            }
            return new BitList<>(selected);
        }
    }
}
