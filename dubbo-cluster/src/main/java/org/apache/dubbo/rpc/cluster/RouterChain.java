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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.dynamic.DynamicConfiguration;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.InvokerTreeCache;
import org.apache.dubbo.rpc.cluster.router.TreeNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class RouterChain<T> {

    List<Invoker<T>> fullInvokers;

    private InvokerTreeCache<T> treeCache;

    List<Router> routers;

    public static <T> RouterChain<T> buildChain(DynamicConfiguration dynamicConfiguration) {
        List<RouterFactory> extensionFactories = ExtensionLoader.getExtensionLoader(RouterFactory.class).getActivateExtension(dynamicConfiguration.getUrl(), (String[]) null);
        List<Router> routers = extensionFactories.stream().map(factory -> factory.getRouter(dynamicConfiguration)).collect(Collectors.toList());
        return new RouterChain<>(routers);
    }

    public RouterChain(List<Router> routers) {
        this.routers = routers;
    }


    public void addRouter(Router router) {
        this.routers.add(router);
    }

    public void sort() {

    }

    /**
     * @param methodInvokers
     * @param url
     * @param invocation     TODO has no been used yet
     */
    public void preRoute(Map<String, List<Invoker<T>>> methodInvokers, URL url, Invocation invocation) {
        if (CollectionUtils.isEmpty(routers)) {
            return;
        }
        TreeNode root = treeCache.buildTree();
        Router router = routers.get(0);
        methodInvokers.forEach((method, invokers) -> {
            TreeNode<T> node = new TreeNode<>("method", method, invokers, true);
            root.addChild(node);
            Invocation invocation1 = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            routeeee(router, 1, root, router.preRoute(invokers, url, invocation1), url, invocation1);
        });
    }

    private void routeeee(Router router, int i, TreeNode parentNode, Map<String, List<Invoker<T>>> invokers, URL url, Invocation invocation) {
        invokers.forEach((routerValue, list) -> {
            TreeNode<T> node = new TreeNode<>(router.getKey(), routerValue, list, router.isForce());
            parentNode.addChild(node);
            Router nextRouter = routers.get(i);
            if (CollectionUtils.isNotEmpty(list)) {
                routeeee(nextRouter, i + 1, node, nextRouter.preRoute(list, url, invocation), url, invocation);
            }
        });
    }

    public List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // TODO will calculate every time
        List<Invoker<T>> finalInvokers = treeCache.getInvokers(treeCache.getTree(), url, invocation);
        for (Router router : routers) {
            if (router.isRuntime()) {
                finalInvokers = router.route(finalInvokers, url, invocation);
            }
        }
        return finalInvokers;
    }

    public void notifyFullInvokers(Map<String, List<Invoker<T>>> invokers, URL url) {
        preRoute(invokers, url, null);
    }
}
