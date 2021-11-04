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
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.RouterResult;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AddrCache;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.RouterCache;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.cluster.Constants.ROUTER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.STATE_ROUTER_KEY;

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
    private List<Router> builtinRouters = Collections.emptyList();

    private List<StateRouter> builtinStateRouters = Collections.emptyList();
    private List<StateRouter> stateRouters = Collections.emptyList();
    private final ExecutorRepository executorRepository;

    protected URL url;

    private AtomicReference<AddrCache<T>> cache = new AtomicReference<>();

    private final Semaphore loopPermit = new Semaphore(1);
    private final Semaphore loopPermitNotify = new Semaphore(1);

    private final ExecutorService loopPool;

    private AtomicBoolean firstBuildCache = new AtomicBoolean(true);

    public static <T> RouterChain<T> buildChain(URL url) {
        return new RouterChain<>(url);
    }

    private RouterChain(URL url) {
        executorRepository = url.getOrDefaultApplicationModel().getExtensionLoader(ExecutorRepository.class)
            .getDefaultExtension();
        loopPool = executorRepository.nextExecutorExecutor();
        List<RouterFactory> extensionFactories = url.getOrDefaultApplicationModel().getExtensionLoader(RouterFactory.class)
            .getActivateExtension(url, ROUTER_KEY);

        List<Router> routers = extensionFactories.stream()
            .map(factory -> factory.getRouter(url))
            .sorted(Router::compareTo)
            .collect(Collectors.toList());

        initWithRouters(routers);

        List<StateRouterFactory> extensionStateRouterFactories = url.getOrDefaultApplicationModel()
            .getExtensionLoader(StateRouterFactory.class)
            .getActivateExtension(url, STATE_ROUTER_KEY);

        List<StateRouter> stateRouters = extensionStateRouterFactories.stream()
            .map(factory -> factory.getRouter(url, this))
            .sorted(StateRouter::compareTo)
            .collect(Collectors.toList());

        // init state routers
        initWithStateRouters(stateRouters);
    }

    /**
     * the resident routers must being initialized before address notification.
     * FIXME: this method should not be public
     */
    public void initWithRouters(List<Router> builtinRouters) {
        this.builtinRouters = builtinRouters;
        this.routers = new ArrayList<>(builtinRouters);
    }

    private void initWithStateRouters(List<StateRouter> builtinRouters) {
        this.builtinStateRouters = builtinRouters;
        this.stateRouters = new ArrayList<>(builtinRouters);
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

    public void addStateRouters(List<StateRouter> stateRouters) {
        List<StateRouter> newStateRouters = new ArrayList<>();
        newStateRouters.addAll(builtinStateRouters);
        newStateRouters.addAll(stateRouters);
        CollectionUtils.sort(newStateRouters);
        this.stateRouters = newStateRouters;
    }

    public List<Router> getRouters() {
        return routers;
    }

    public List<StateRouter> getStateRouters() {
        return stateRouters;
    }

    /**
     * @param url
     * @param invocation
     * @return
     */
    public List<Invoker<T>> route(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {

        AddrCache<T> cache = this.cache.get();
        BitList<Invoker<T>> resultInvokers = availableInvokers.clone();

        // 1. route state router
        if (cache != null) {
            for (StateRouter stateRouter : stateRouters) {
                if (stateRouter.isEnable()) {
                    RouterCache<T> routerCache = cache.getCache().get(stateRouter.getName());
                    StateRouterResult<Invoker<T>> routeResult = stateRouter.route(resultInvokers, routerCache, url, invocation, false);
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
        AddrCache<T> cache = this.cache.get();
        BitList<Invoker<T>> resultInvokers = availableInvokers.clone();
        RouterSnapshotNode<T> snapshotNode = new RouterSnapshotNode<T>("Parent", resultInvokers.size());
        snapshotNode.setOutputInvokers(resultInvokers.clone());

        // 1. route state router
        if (cache != null) {
            for (StateRouter stateRouter : stateRouters) {
                if (stateRouter.isEnable()) {
                    BitList<Invoker<T>> inputInvokers = resultInvokers.clone();

                    RouterSnapshotNode<T> currentNode = new RouterSnapshotNode<T>(stateRouter.getName(), inputInvokers.size());
                    snapshotNode.appendNode(currentNode);

                    RouterCache<T> routerCache = cache.getCache().get(stateRouter.getName());
                    StateRouterResult<Invoker<T>> routeResult = stateRouter.route(inputInvokers, routerCache, url, invocation, true);
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
        stateRouters.forEach(router -> router.notify(this.invokers));
        routers.forEach(router -> router.notify(this.invokers));
        loop(true);
    }

    /**
     * Build the asynchronous address cache for stateRouter.
     *
     * @param notify Whether the addresses in registry have changed.
     */
    private void buildCache(boolean notify) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }
        AddrCache<T> origin = cache.get();
        List<Invoker<T>> copyInvokers = new ArrayList<>(this.invokers);
        AddrCache<T> newCache = new AddrCache<T>();
        Map<String, RouterCache<T>> routerCacheMap = new HashMap<>((int) (stateRouters.size() / 0.75f) + 1);
        newCache.setInvokers(invokers);
        for (StateRouter stateRouter : stateRouters) {
            try {
                RouterCache routerCache = poolRouter(stateRouter, origin, copyInvokers, notify);
                //file cache
                routerCacheMap.put(stateRouter.getName(), routerCache);
            } catch (Throwable t) {
                logger.error("Failed to pool router: " + stateRouter.getUrl() + ", cause: " + t.getMessage(), t);
                return;
            }
        }

        newCache.setCache(routerCacheMap);
        this.cache.set(newCache);
    }

    /**
     * Cache the address list for each StateRouter.
     *
     * @param router   router
     * @param origin   The original address cache
     * @param invokers The full address list
     * @param notify   Whether the addresses in registry has changed.
     * @return
     */
    private RouterCache poolRouter(StateRouter router, AddrCache<T> origin, List<Invoker<T>> invokers, boolean notify) {
        String routerName = router.getName();
        RouterCache routerCache;
        if (isCacheMiss(origin, routerName) || router.shouldRePool() || notify) {
            return router.pool(invokers);
        } else {
            routerCache = origin.getCache().get(routerName);
        }
        if (routerCache == null) {
            return new RouterCache();
        }
        return routerCache;
    }

    private boolean isCacheMiss(AddrCache<T> cache, String routerName) {
        return cache == null || cache.getCache() == null || cache.getInvokers() == null || cache.getCache().get(
            routerName)
            == null;
    }

    /***
     * Build the asynchronous address cache for stateRouter.
     * @param notify Whether the addresses in registry has changed.
     */
    public void loop(boolean notify) {
        if (firstBuildCache.compareAndSet(true, false)) {
            buildCache(notify);
        }

        try {
            if (notify) {
                if (loopPermitNotify.tryAcquire()) {
                    loopPool.submit(new NotifyLoopRunnable(true, loopPermitNotify));
                }
            } else {
                if (loopPermit.tryAcquire()) {
                    loopPool.submit(new NotifyLoopRunnable(false, loopPermit));
                }
            }
        } catch (RejectedExecutionException e) {
            if (loopPool.isShutdown()) {
                logger.warn("loopPool executor service is shutdown, ignoring notify loop");
                return;
            }
            throw e;
        }
    }

    class NotifyLoopRunnable implements Runnable {

        private final boolean notify;
        private final Semaphore loopPermit;

        public NotifyLoopRunnable(boolean notify, Semaphore loopPermit) {
            this.notify = notify;
            this.loopPermit = loopPermit;
        }

        @Override
        public void run() {
            buildCache(notify);
            loopPermit.release();
        }
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
        stateRouters = Collections.emptyList();
        builtinStateRouters = Collections.emptyList();
    }

}
