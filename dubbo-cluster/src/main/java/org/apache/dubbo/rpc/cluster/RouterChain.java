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
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.InvokerTreeCache;
import org.apache.dubbo.rpc.cluster.router.TreeNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 *
 */
public class RouterChain<T> {

    // full list of addresses from registry, classified by method name.
    private Map<String, List<Invoker<T>>> fullMethodInvokers;
    private URL url;

    // a tree-structured cache generated from the full address list after being filtered by all routers.
    // it's aimed to improve performance, only routers explicitly specifies 'runtime=true' will be executed when an RPC comes.
    private InvokerTreeCache<T> treeCache;
    // containing all routers, reconstruct every time 'route://' urls change.
    private List<Router> routers = new CopyOnWriteArrayList<>();
    // Fixed router instances: ConfigConditionRouter, TagRouter, e.g., the rule for each instance may change but the instance will never delete or recreate.
    private List<Router> residentRouters;

    public static <T> RouterChain<T> buildChain(DynamicConfiguration dynamicConfiguration, URL url) {
        RouterChain<T> routerChain = new RouterChain<>(url);
        List<RouterFactory> extensionFactories = ExtensionLoader.getExtensionLoader(RouterFactory.class).getActivateExtension(dynamicConfiguration.getUrl(), (String[]) null);
        List<Router> routers = extensionFactories.stream()
                .map(factory -> {
                    Router router = factory.getRouter(dynamicConfiguration, url);
                    router.setRouterChain(routerChain);
                    return router;
                }).collect(Collectors.toList());
        routerChain.setResidentRouters(routers);
        return routerChain;
    }

    protected RouterChain(List<Router> routers) {
        this.routers.addAll(routers);
        treeCache = new InvokerTreeCache<>();
    }

    protected RouterChain(URL url) {
        treeCache = new InvokerTreeCache<>();
        this.url = url;
    }

    /**
     * the resident routers must have already been generated before notification of provider addresses.
     *
     * @param residentRouters
     */
    public void setResidentRouters(List<Router> residentRouters) {
        this.residentRouters = residentRouters;
        this.routers.addAll(residentRouters);
        this.sort();
    }

    /**
     * If we use route:// protocol in version before 2.7.0, each URL will generate a Router instance,
     * so we should keep the routers up to date, that is, each time router URLs changes, we should update the routers list,
     * only keep the residentRouters which are available all the time and the latest notified routers which are generated from URLs.
     *
     * @param generatedRouters
     */
    public void setGeneratedRouters(List<Router> generatedRouters) {
        List<Router> newRouters = new CopyOnWriteArrayList<>();
        newRouters.addAll(residentRouters);
        newRouters.addAll(generatedRouters);
        this.routers = newRouters;
        // FIXME will sort cause concurrent problem? since it's kind of a write operation.
        this.sort();
        if (fullMethodInvokers != null) {
            this.preRoute(fullMethodInvokers, url, null);
        }
    }

    public void addRouter(Router router) {
        this.routers.add(router);
        this.sort();
    }

    public void sort() {
        Collections.sort(routers);
    }

    /**
     * Route cache building can be triggered in different threads, for example, registry notification and governance notification.
     * So this operation should be synchronized.
     * @param methodInvokers
     * @param url
     * @param invocation     TODO has not been used yet
     */
    public void preRoute(Map<String, List<Invoker<T>>> methodInvokers, URL url, Invocation invocation) {
        if (CollectionUtils.isEmpty(routers) || methodInvokers == null) {
            treeCache.refreshTree(null);
            return;
        }
        TreeNode<T> root = treeCache.buildRootNode();
        Router router = routers.get(0);
        methodInvokers.forEach((method, invokers) -> {
            TreeNode<T> node = new TreeNode<>("METHOD_ROUTER", "method", method, invokers, true);
            root.addChild(node);
            Invocation invocation1 = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            doRoute(router, 1, node, router.preRoute(invokers, url, invocation1), url, invocation1);
        });
        treeCache.refreshTree(root);
    }

    private void doRoute(Router router, int i, TreeNode parentNode, Map<String, List<Invoker<T>>> invokers, URL url, Invocation invocation) {
        invokers.forEach((routerValue, list) -> {
            TreeNode<T> node = new TreeNode<>(router.getName(), router.getKey(), routerValue, list, router.isForce());
            parentNode.addChild(node);
            // Only when we have more routers and the sub-lis is not empty.
            if (i < routers.size() && CollectionUtils.isNotEmpty(list)) {
                Router nextRouter = routers.get(i);
                doRoute(nextRouter, i + 1, node, nextRouter.preRoute(list, url, invocation), url, invocation);
            }
        });
    }

    /**
     *
     * @param url
     * @param invocation
     * @return
     */
    public List<Invoker<T>> route(URL url, Invocation invocation) {
        List<Invoker<T>> finalInvokers = treeCache.getInvokers(treeCache.getTree(), url, invocation);
        for (Router router : routers) {
            if (router.isRuntime()) {
                finalInvokers = router.route(finalInvokers, url, invocation);
            }
        }
        return finalInvokers;
    }

    public List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        List<Invoker<T>> finalInvokers = invokers;
        for (Router router : routers) {
            if (router.isRuntime()) {
                finalInvokers = router.route(invokers, url, invocation);
            }
        }
        return finalInvokers;
    }

    public void notifyRuleChanged() {
        preRoute(this.fullMethodInvokers, url, null);
    }

    public void notifyFullInvokers(Map<String, List<Invoker<T>>> invokers, URL url) {
        setFullMethodInvokers(invokers);
        preRoute(invokers, url, null);
    }

    public void setFullMethodInvokers(Map<String, List<Invoker<T>>> fullMethodInvokers) {
        this.fullMethodInvokers = fullMethodInvokers;
    }
}
