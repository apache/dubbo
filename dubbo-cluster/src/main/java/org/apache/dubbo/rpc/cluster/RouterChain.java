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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 *
 */
public class RouterChain<T> {

    // full list of addresses from registry, classified by method name.
    private List<Invoker<T>> fullInvokers;
    private URL url;

    // containing all routers, reconstruct every time 'route://' urls change.
    private List<Router> routers = new CopyOnWriteArrayList<>();
    // Fixed router instances: ConfigConditionRouter, TagRouter, e.g., the rule for each instance may change but the instance will never delete or recreate.
    private List<Router> residentRouters;

    public static <T> RouterChain<T> buildChain(URL url) {
        RouterChain<T> routerChain = new RouterChain<>(url);
        List<RouterFactory> extensionFactories = ExtensionLoader.getExtensionLoader(RouterFactory.class).getActivateExtension(url, (String[]) null);
        List<Router> routers = extensionFactories.stream()
                .map(factory -> {
                    Router router = factory.getRouter(url);
                    router.addRouterChain(routerChain);
                    return router;
                }).collect(Collectors.toList());
        routerChain.setResidentRouters(routers);
        return routerChain;
    }

    protected RouterChain(List<Router> routers) {
        this.routers.addAll(routers);
    }

    protected RouterChain(URL url) {
        this.url = url;
    }

    /**
     * the resident routers must being initialized before address notification.
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
     * @param generatedRouters routers from 'router://' rules in 2.6.x or before.
     */
    public void setGeneratedRouters(List<Router> generatedRouters) {
        List<Router> newRouters = new CopyOnWriteArrayList<>();
        newRouters.addAll(residentRouters);
        newRouters.addAll(generatedRouters);
        this.routers = newRouters;
        // FIXME will sort cause concurrent problem? since it's kind of a write operation.
        this.sort();
       /* if (fullInvokers != null) {
            this.preRoute(fullInvokers, url, null);
        }*/
    }

    public void sort() {
        Collections.sort(routers);
    }

    /**
     * TODO
     *
     * Building of router cache can be triggered from within different threads, for example, registry notification and governance notification.
     * So this operation should be synchronized.
     * @param invokers
     * @param url
     * @param invocation
     */
    public void preRoute(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        for (Router router : routers) {
            router.preRoute(invokers, url, invocation);
        }
    }

    /**
     *
     * @param url
     * @param invocation
     * @return
     */
    public List<Invoker<T>> route(URL url, Invocation invocation) {
        List<Invoker<T>> finalInvokers = fullInvokers;
        for (Router router : routers) {
//            if (router.isRuntime()) {
//                finalInvokers = router.route(finalInvokers, url, invocation);
//            }
            finalInvokers = router.route(finalInvokers, url, invocation);
        }
        return finalInvokers;
    }

    /**
     * When any of the router's rule changed, notify the router chain to rebuild cache from scratch.
     */
    public void notifyRuleChanged() {
        if (CollectionUtils.isEmpty(this.fullInvokers)) {
            return;
        }
        preRoute(this.fullInvokers, url, null);
    }

    /**
     * Notify router chain of the initial addresses from registry at the first time.
     * Notify whenever addresses in registry change.
     *
     * @param invokers
     * @param url
     */
    public void notifyFullInvokers(List<Invoker<T>> invokers, URL url) {
        setFullMethodInvokers(invokers);
        preRoute(invokers, url, null);
    }

    public void setFullMethodInvokers(List<Invoker<T>> fullInvokers) {
        this.fullInvokers = (fullInvokers == null ? Collections.emptyList() : fullInvokers);
    }
}
