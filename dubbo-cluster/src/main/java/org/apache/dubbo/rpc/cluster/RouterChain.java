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
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.RouterResult;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.cluster.Constants.ROUTER_KEY;

/**
 * Router chain
 */
public class RouterChain<T> {
    private static final Logger logger = LoggerFactory.getLogger(RouterChain.class);

    /**
     * full list of addresses from registry, classified by method name.
     */
    private volatile BitList<Invoker<T>> invokers = BitList.emptyList();

    /**
     * containing all routers, reconstruct every time 'route://' urls change.
     */
    private volatile List<Router> routers = Collections.emptyList();

    /**
     * Fixed router instances: ConfigConditionRouter, TagRouter, e.g.,
     * the rule for each instance may change but the instance will never delete or recreate.
     */
    private volatile List<Router> builtinRouters = Collections.emptyList();

    private volatile List<StateRouter<T>> builtinStateRouters = Collections.emptyList();
    private volatile List<StateRouter<T>> stateRouters = Collections.emptyList();

    public static <T> RouterChain<T> buildChain(Class<T> interfaceClass, URL url) {
        return new RouterChain<>(interfaceClass, url);
    }

    private RouterChain(Class<T> interfaceClass, URL url) {
        List<RouterFactory> extensionFactories = url.getOrDefaultApplicationModel().getExtensionLoader(RouterFactory.class)
            .getActivateExtension(url, ROUTER_KEY);

        List<Router> routers = extensionFactories.stream()
            .map(factory -> factory.getRouter(url))
            .sorted(Router::compareTo)
            .collect(Collectors.toList());

        initWithRouters(routers);

        List<StateRouterFactory> extensionStateRouterFactories = url.getOrDefaultApplicationModel()
            .getExtensionLoader(StateRouterFactory.class)
            .getActivateExtension(url, ROUTER_KEY);

        List<StateRouter<T>> stateRouters = extensionStateRouterFactories.stream()
            .map(factory -> factory.getRouter(interfaceClass, url))
            .sorted(StateRouter::compareTo)
            .collect(Collectors.toList());

        // init state routers
        initWithStateRouters(stateRouters);
    }

    /**
     * the resident routers must being initialized before address notification.
     * only for ut
     */
    public void initWithRouters(List<Router> builtinRouters) {
        this.builtinRouters = builtinRouters;
        this.routers = new ArrayList<>(builtinRouters);
    }

    /**
     * the resident routers must being initialized before address notification.
     * only for ut
     */
    public void initWithStateRouters(List<StateRouter<T>> builtinRouters) {
        this.builtinStateRouters = builtinRouters;
        setStateRouters(builtinStateRouters);
    }

    private void setStateRouters(List<StateRouter<T>> stateRouters) {
        this.stateRouters = new ArrayList<>(stateRouters);
    }

    /**
     * If we use route:// protocol in version before 2.7.0, each URL will generate a Router instance, so we should
     * keep the routers up to date, that is, each time router URLs changes, we should update the routers list, only
     * keep the builtinRouters which are available all the time and the latest notified routers which are generated
     * from URLs.
     *
     * @param routers routers from 'router://' rules in 2.6.x or before.
     */
    public void addRouters(List<Router> routers) {
        List<Router> newRouters = new ArrayList<>();
        newRouters.addAll(builtinRouters);
        newRouters.addAll(routers);
        CollectionUtils.sort(newRouters);
        this.routers = newRouters;
    }

    public void addStateRouters(List<StateRouter<T>> stateRouters) {
        List<StateRouter<T>> newStateRouters = new ArrayList<>();
        newStateRouters.addAll(builtinStateRouters);
        newStateRouters.addAll(stateRouters);
        CollectionUtils.sort(newStateRouters);
        setStateRouters(newStateRouters);
    }

    public List<Router> getRouters() {
        return routers;
    }

    public List<StateRouter<T>> getStateRouters() {
        return stateRouters;
    }

    /**
     * @param url
     * @param invocation
     * @return
     */
    public List<Invoker<T>> route(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {

        BitList<Invoker<T>> resultInvokers = availableInvokers.clone();

        // 1. route state router
        for (StateRouter<T> stateRouter : stateRouters) {
            StateRouterResult<Invoker<T>> routeResult = stateRouter.route(resultInvokers, url, invocation, false);
            resultInvokers = routeResult.getResult();
            if (resultInvokers.isEmpty()) {
                printRouterSnapshot(url, availableInvokers, invocation);
                return BitList.emptyList();
            }

            // stop continue routing
            if (!routeResult.isNeedContinueRoute()) {
                return routeResult.getResult();
            }
        }


        List<Invoker<T>> commonRouterResult = new ArrayList<>(resultInvokers);
        // 2. route common router
        for (Router router : routers) {
            // Copy resultInvokers to a arrayList. BitList not support
            RouterResult<Invoker<T>> routeResult = router.route(commonRouterResult, url, invocation, false);
            commonRouterResult = routeResult.getResult();
            if (CollectionUtils.isEmpty(commonRouterResult)) {
                printRouterSnapshot(url, availableInvokers, invocation);
                return BitList.emptyList();
            }

            // stop continue routing
            if (!routeResult.isNeedContinueRoute()) {
                return commonRouterResult;
            }
        }
        return commonRouterResult;
    }

    /**
     * store each router's input and output, log out if empty
     */
    private void printRouterSnapshot(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        logRouterSnapshot(url, invocation, buildRouterSnapshot(url, availableInvokers, invocation));
    }

    /**
     * Build each router's result
     */
    public RouterSnapshotNode<T> buildRouterSnapshot(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        BitList<Invoker<T>> resultInvokers = availableInvokers.clone();
        RouterSnapshotNode<T> snapshotNode = new RouterSnapshotNode<T>("Parent", resultInvokers.size());
        snapshotNode.setOutputInvokers(resultInvokers.clone());

        // 1. route state router
        for (StateRouter stateRouter : stateRouters) {
            BitList<Invoker<T>> inputInvokers = resultInvokers.clone();

            RouterSnapshotNode<T> currentNode = new RouterSnapshotNode<T>(stateRouter.getClass().getSimpleName(), inputInvokers.size());
            snapshotNode.appendNode(currentNode);

            StateRouterResult<Invoker<T>> routeResult = stateRouter.route(inputInvokers, url, invocation, true);
            resultInvokers = routeResult.getResult();
            String routerMessage = routeResult.getMessage();

            currentNode.setOutputInvokers(resultInvokers);
            currentNode.setRouterMessage(routerMessage);

            // result is empty, log out
            if (resultInvokers.isEmpty()) {
                return snapshotNode;
            }

            if (!routeResult.isNeedContinueRoute()) {
                return snapshotNode;
            }
        }

        List<Invoker<T>> commonRouterResult = resultInvokers;
        // 2. route common router
        for (Router router : routers) {
            // Copy resultInvokers to a arrayList. BitList not support
            List<Invoker<T>> inputInvokers = new ArrayList<>(commonRouterResult);

            RouterSnapshotNode<T> currentNode = new RouterSnapshotNode<T>(router.getClass().getSimpleName(), inputInvokers.size());
            snapshotNode.appendNode(currentNode);

            RouterResult<Invoker<T>> routeStateResult = router.route(inputInvokers, url, invocation, true);
            List<Invoker<T>> routeResult = routeStateResult.getResult();
            String routerMessage = routeStateResult.getMessage();

            currentNode.setOutputInvokers(routeResult);
            currentNode.setRouterMessage(routerMessage);

            // result is empty, log out
            if (CollectionUtils.isEmpty(routeResult)) {
                return snapshotNode;
            } else {
                commonRouterResult = routeResult;
            }

            if (!routeStateResult.isNeedContinueRoute()) {
                return snapshotNode;
            }
        }
        return snapshotNode;
    }

    private void logRouterSnapshot(URL url, Invocation invocation, RouterSnapshotNode<T> snapshotNode) {
        logger.warn("No provider available after route for the service " + url.getServiceKey()
            + " from registry " + url.getAddress()
            + " on the consumer " + NetUtils.getLocalHost()
            + " using the dubbo version " + Version.getVersion() + ". Router snapshot is below: \n" + snapshotNode.toString());
    }

    /**
     * Notify router chain of the initial addresses from registry at the first time.
     * Notify whenever addresses in registry change.
     */
    public void setInvokers(BitList<Invoker<T>> invokers) {
        this.invokers = (invokers == null ? BitList.emptyList() : invokers);
        routers.forEach(router -> router.notify(this.invokers));
        stateRouters.forEach(router -> router.notify(this.invokers));
    }

    public void destroy() {
        invokers = BitList.emptyList();
        for (Router router : routers) {
            try {
                router.stop();
            } catch (Exception e) {
                logger.error("Error trying to stop router " + router.getClass(), e);
            }
        }
        routers = Collections.emptyList();
        builtinRouters = Collections.emptyList();

        for (StateRouter router : stateRouters) {
            try {
                router.stop();
            } catch (Exception e) {
                logger.error("Error trying to stop stateRouter " + router.getClass(), e);
            }
        }
        setStateRouters(Collections.emptyList());
        builtinStateRouters = Collections.emptyList();
    }

}
