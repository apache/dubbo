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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.router.RouterResult;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotSwitcher;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.state.TailStateRouter;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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

    private volatile StateRouter<T> headStateRouter;

    private volatile List<StateRouter<T>> stateRouters;

    /**
     * Should continue route if current router's result is empty
     */
    private final boolean shouldFailFast;

    private final RouterSnapshotSwitcher routerSnapshotSwitcher;

    public static <T> RouterChain<T> buildChain(Class<T> interfaceClass, URL url) {
        ModuleModel moduleModel = url.getOrDefaultModuleModel();

        List<RouterFactory> extensionFactories = moduleModel.getExtensionLoader(RouterFactory.class)
            .getActivateExtension(url, ROUTER_KEY);

        List<Router> routers = extensionFactories.stream()
            .map(factory -> factory.getRouter(url))
            .sorted(Router::compareTo)
            .collect(Collectors.toList());

        List<StateRouter<T>> stateRouters = moduleModel
            .getExtensionLoader(StateRouterFactory.class)
            .getActivateExtension(url, ROUTER_KEY)
            .stream()
            .map(factory -> factory.getRouter(interfaceClass, url))
            .collect(Collectors.toList());


        boolean shouldFailFast = Boolean.parseBoolean(ConfigurationUtils.getProperty(moduleModel, Constants.SHOULD_FAIL_FAST_KEY, "true"));

        RouterSnapshotSwitcher routerSnapshotSwitcher = ScopeModelUtil.getFrameworkModel(moduleModel).getBeanFactory().getBean(RouterSnapshotSwitcher.class);

        return new RouterChain<>(routers, stateRouters, shouldFailFast, routerSnapshotSwitcher);
    }

    public RouterChain(List<Router> routers, List<StateRouter<T>> stateRouters, boolean shouldFailFast, RouterSnapshotSwitcher routerSnapshotSwitcher) {
        initWithRouters(routers);

        initWithStateRouters(stateRouters);

        this.shouldFailFast = shouldFailFast;
        this.routerSnapshotSwitcher = routerSnapshotSwitcher;
    }

    private void initWithStateRouters(List<StateRouter<T>> stateRouters) {
        StateRouter<T> stateRouter = TailStateRouter.getInstance();
        for (int i = stateRouters.size() - 1; i >= 0; i--) {
            StateRouter<T> nextStateRouter = stateRouters.get(i);
            nextStateRouter.setNextRouter(stateRouter);
            stateRouter = nextStateRouter;
        }
        this.headStateRouter = stateRouter;
        this.stateRouters = Collections.unmodifiableList(stateRouters);
    }

    /**
     * the resident routers must being initialized before address notification.
     * only for ut
     */
    public void initWithRouters(List<Router> builtinRouters) {
        this.builtinRouters = builtinRouters;
        this.routers = new LinkedList<>(builtinRouters);
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
        List<Router> newRouters = new LinkedList<>();
        newRouters.addAll(builtinRouters);
        newRouters.addAll(routers);
        CollectionUtils.sort(newRouters);
        this.routers = newRouters;
    }

    public List<Router> getRouters() {
        return routers;
    }

    public StateRouter<T> getHeadStateRouter() {
        return headStateRouter;
    }

    public List<Invoker<T>> route(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        if (RpcContext.getServiceContext().isNeedPrintRouterSnapshot()) {
            return routeAndPrint(url, availableInvokers, invocation);
        } else {
            return simpleRoute(url, availableInvokers, invocation);
        }
    }

    public List<Invoker<T>> routeAndPrint(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        RouterSnapshotNode<T> snapshot = buildRouterSnapshot(url, availableInvokers, invocation);
        logRouterSnapshot(url, invocation, snapshot);
        return snapshot.getChainOutputInvokers();
    }

    public List<Invoker<T>> simpleRoute(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        BitList<Invoker<T>> resultInvokers = availableInvokers.clone();

        // 1. route state router
        resultInvokers = headStateRouter.route(resultInvokers, url, invocation, false, null);
        if (resultInvokers.isEmpty() && (shouldFailFast || routers.isEmpty())) {
            printRouterSnapshot(url, availableInvokers, invocation);
            return BitList.emptyList();
        }

        if (routers.isEmpty()) {
            return resultInvokers;
        }
        List<Invoker<T>> commonRouterResult = resultInvokers.cloneToArrayList();
        // 2. route common router
        for (Router router : routers) {
            // Copy resultInvokers to a arrayList. BitList not support
            RouterResult<Invoker<T>> routeResult = router.route(commonRouterResult, url, invocation, false);
            commonRouterResult = routeResult.getResult();
            if (CollectionUtils.isEmpty(commonRouterResult) && shouldFailFast) {
                printRouterSnapshot(url, availableInvokers, invocation);
                return BitList.emptyList();
            }

            // stop continue routing
            if (!routeResult.isNeedContinueRoute()) {
                return commonRouterResult;
            }
        }

        if (commonRouterResult.isEmpty()) {
            printRouterSnapshot(url, availableInvokers, invocation);
            return BitList.emptyList();
        }

        return commonRouterResult;
    }

    /**
     * store each router's input and output, log out if empty
     */
    private void printRouterSnapshot(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        if (logger.isWarnEnabled()) {
            logRouterSnapshot(url, invocation, buildRouterSnapshot(url, availableInvokers, invocation));
        }
    }

    /**
     * Build each router's result
     */
    public RouterSnapshotNode<T> buildRouterSnapshot(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        BitList<Invoker<T>> resultInvokers = availableInvokers.clone();
        RouterSnapshotNode<T> parentNode = new RouterSnapshotNode<T>("Parent", resultInvokers.clone());
        parentNode.setNodeOutputInvokers(resultInvokers.clone());

        // 1. route state router
        Holder<RouterSnapshotNode<T>> nodeHolder = new Holder<>();
        nodeHolder.set(parentNode);

        resultInvokers = headStateRouter.route(resultInvokers, url, invocation, true, nodeHolder);

        // result is empty, log out
        if (routers.isEmpty() || (resultInvokers.isEmpty() && shouldFailFast)) {
            parentNode.setChainOutputInvokers(resultInvokers.clone());
            return parentNode;
        }

        RouterSnapshotNode<T> commonRouterNode = new RouterSnapshotNode<T>("CommonRouter", resultInvokers.clone());
        parentNode.appendNode(commonRouterNode);
        List<Invoker<T>> commonRouterResult = resultInvokers;
        // 2. route common router
        for (Router router : routers) {
            // Copy resultInvokers to a arrayList. BitList not support
            List<Invoker<T>> inputInvokers = new ArrayList<>(commonRouterResult);

            RouterSnapshotNode<T> currentNode = new RouterSnapshotNode<T>(router.getClass().getSimpleName(), inputInvokers);

            // append to router node chain
            commonRouterNode.appendNode(currentNode);
            commonRouterNode = currentNode;

            RouterResult<Invoker<T>> routeStateResult = router.route(inputInvokers, url, invocation, true);
            List<Invoker<T>> routeResult = routeStateResult.getResult();
            String routerMessage = routeStateResult.getMessage();

            currentNode.setNodeOutputInvokers(routeResult);
            currentNode.setRouterMessage(routerMessage);

            commonRouterResult = routeResult;

            // result is empty, log out
            if (CollectionUtils.isEmpty(routeResult) && shouldFailFast) {
                break;
            }

            if (!routeStateResult.isNeedContinueRoute()) {
                break;
            }
        }
        commonRouterNode.setChainOutputInvokers(commonRouterNode.getNodeOutputInvokers());

        // 3. set router chain output reverse
        RouterSnapshotNode<T> currentNode = commonRouterNode;
        while (currentNode != null){
            RouterSnapshotNode<T> parent = currentNode.getParentNode();
            if (parent != null) {
                // common router only has one child invoke
                parent.setChainOutputInvokers(currentNode.getChainOutputInvokers());
            }
            currentNode = parent;
        }
        return parentNode;
    }

    private void logRouterSnapshot(URL url, Invocation invocation, RouterSnapshotNode<T> snapshotNode) {
        if (snapshotNode.getChainOutputInvokers() == null ||
            snapshotNode.getChainOutputInvokers().isEmpty()) {
            if (logger.isWarnEnabled()) {
                String message = "No provider available after route for the service " + url.getServiceKey()
                    + " from registry " + url.getAddress()
                    + " on the consumer " + NetUtils.getLocalHost()
                    + " using the dubbo version " + Version.getVersion() + ". Router snapshot is below: \n" + snapshotNode.toString();
                if (routerSnapshotSwitcher.isEnable()) {
                    routerSnapshotSwitcher.setSnapshot(message);
                }
                logger.warn(message);
            }
        } else {
            if (logger.isInfoEnabled()) {
                String message = "Router snapshot service " + url.getServiceKey()
                    + " from registry " + url.getAddress()
                    + " on the consumer " + NetUtils.getLocalHost()
                    + " using the dubbo version " + Version.getVersion() + " is below: \n" + snapshotNode.toString();
                if (routerSnapshotSwitcher.isEnable()) {
                    routerSnapshotSwitcher.setSnapshot(message);
                }
                logger.info(message);
            }
        }
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

    /**
     * for uts only
     */
    @Deprecated
    public void setHeadStateRouter(StateRouter<T> headStateRouter) {
        this.headStateRouter = headStateRouter;
    }

    /**
     * for uts only
     */
    @Deprecated
    public List<StateRouter<T>> getStateRouters() {
        return stateRouters;
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

        for (StateRouter<T> router : stateRouters) {
            try {
                router.stop();
            } catch (Exception e) {
                logger.error("Error trying to stop stateRouter " + router.getClass(), e);
            }
        }
        stateRouters = Collections.emptyList();
        headStateRouter = TailStateRouter.getInstance();
    }
}
