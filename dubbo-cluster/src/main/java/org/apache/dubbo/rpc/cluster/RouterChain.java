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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.threadpool.support.fixed.FixedThreadPool;
import org.apache.dubbo.common.utils.BitList;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.router.state.AddrCache;
import org.apache.dubbo.rpc.cluster.router.state.RouterCache;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Router chain
 */
public class RouterChain<T> {

    // full list of addresses from registry, classified by method name.
    private List<Invoker<T>> invokers = Collections.emptyList();

    // containing all routers, reconstruct every time 'route://' urls change.
    private volatile List<Router> routers = Collections.emptyList();

    // Fixed router instances: ConfigConditionRouter, TagRouter, e.g., the rule for each instance may change but the
    // instance will never delete or recreate.
    private List<Router> builtinRouters = Collections.emptyList();
    private List<StateRouter> builtinStateRouters = Collections.emptyList();
    private List<StateRouter> stateRouters = Collections.emptyList();

    protected URL url;

    protected AtomicReference<AddrCache> cache = new AtomicReference<>();
    private Semaphore loopPermit = new Semaphore(1);

    private final static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
        new NamedInternalThreadFactory("dubbo-state-router-scheduled-", true));

    private final static ExecutorService loopThreadPool = new ThreadPoolExecutor(1, 1,
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(1024), new NamedInternalThreadFactory("dubbo-state-router-loop-",true), new ThreadPoolExecutor.AbortPolicy());

    private final static ExecutorService poolRouterThreadPool = new ThreadPoolExecutor(1, 10,
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(1024), new NamedInternalThreadFactory("dubbo-state-router-pool-",true), new ThreadPoolExecutor.AbortPolicy());

    private static final Logger logger = LoggerFactory.getLogger(StaticDirectory.class);

    public static <T> RouterChain<T> buildChain(URL url) {
        return new RouterChain<>(url);
    }

    public RouterChain(List<Invoker<T>> invokers) {
        this.invokers = invokers;
    }

    private RouterChain(URL url) {
        List<RouterFactory> extensionFactories = ExtensionLoader.getExtensionLoader(RouterFactory.class)
                .getActivateExtension(url, "router");

        List<Router> routers = extensionFactories.stream()
                .map(factory -> factory.getRouter(url))
                .collect(Collectors.toList());

        initWithRouters(routers);

        List<StateRouterFactory> extensionStateRouterFactories = ExtensionLoader.getExtensionLoader(StateRouterFactory.class)
            .getActivateExtension(url, "stateRouter");

        List<StateRouter> stateRouters = extensionStateRouterFactories.stream()
            .map(factory -> factory.getRouter(url))
            .sorted(StateRouter::compareTo)
            .collect(Collectors.toList());

        // init state routers
        initWithStateRouters(stateRouters);
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                loop(false);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * the resident routers must being initialized before address notification.
     * FIXME: this method should not be public
     */
    public void initWithRouters(List<Router> builtinRouters) {
        this.builtinRouters = builtinRouters;
        this.routers = new ArrayList<>(builtinRouters);
        this.sort();
    }

    public void initWithStateRouters(List<StateRouter> builtinRouters) {
        this.builtinStateRouters = builtinRouters;
        this.stateRouters = new ArrayList<>(builtinRouters);
    }

    private void sortStateRouters() {
        Collections.sort(stateRouters);
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


    public List<Router> getRouters() {
        return routers;
    }

    public void addStateRouters(List<StateRouter> stateRouters) {
        List<StateRouter> newStateRouters = new ArrayList<>();
        newStateRouters.addAll(builtinStateRouters);
        newStateRouters.addAll(stateRouters);
        CollectionUtils.sort(newStateRouters);
        this.stateRouters = newStateRouters;
    }

    private void sort() {
        Collections.sort(routers);
    }

    /**
     *
     * @param url
     * @param invocation
     * @return
     */
    public List<Invoker<T>> route(URL url, Invocation invocation) {

        AddrCache cache = this.cache.get();
        if (cache == null) {
            throw new RpcException(RpcException.ROUTER_CACHE_NOT_BUILD, "Failed to invoke the method "
                + invocation.getMethodName() + " in the service " + url.getServiceInterface()
                + ". address cache not build "
                + " on the consumer " + NetUtils.getLocalHost()
                + " using the dubbo version " + Version.getVersion()
                + ".");
        }
        BitList<Invoker<T>> finalBitListInvokers = new BitList<Invoker<T>>(invokers, false);
        for (StateRouter stateRouter : stateRouters) {
            if (stateRouter.isEnable()) {
                finalBitListInvokers = stateRouter.route(finalBitListInvokers, cache.getCache().get(stateRouter.getName()), url, invocation);
            }
        }

        // FIXME 看一下高版本的RoaringBitMap，是否有内置函数支持更高效的遍历
        List<Invoker<T>> finalInvokers = new ArrayList<>(finalBitListInvokers.size());
        Iterator<Invoker<T>> iter = finalBitListInvokers.iterator();
        while (iter.hasNext()) {
            finalInvokers.add(iter.next());
        }

        for (Router router : routers) {
            finalInvokers = router.route(finalInvokers, url, invocation);
        }
        return finalInvokers;
    }

    /**
     * Notify router chain of the initial addresses from registry at the first time.
     * Notify whenever addresses in registry change.
     */
    public void setInvokers(List<Invoker<T>> invokers) {
        this.invokers = (invokers == null ? Collections.emptyList() : invokers);
        stateRouters.forEach(router -> router.notify(this.invokers));
        routers.forEach(router -> router.notify(this.invokers));
        loop(true);
    }

    private void buildCache(boolean notify) {
        if (invokers == null || invokers.size() <= 0) {
            return;
        }
        AddrCache origin = cache.get();
        List copyInvokers = new ArrayList<>(this.invokers);
        CountDownLatch cdl = new CountDownLatch(stateRouters.size());
        AddrCache newCache = new AddrCache();
        newCache.setInvokers((List)invokers);
        final AtomicBoolean poolSuccess = new AtomicBoolean(true);
        for (StateRouter stateRouter : (List<StateRouter>)stateRouters) {
            if (stateRouter.isEnable()) {
                poolRouterThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        RouterCache routerCache = null;
                        try {
                            routerCache = poolRouter(stateRouter, origin, copyInvokers, notify);
                            //file cache
                            newCache.getCache().put(stateRouter.getName(), routerCache);
                        } catch (Throwable t) {
                            poolSuccess.set(false);
                            logger.error("Failed to pool router: " + stateRouter.getUrl() + ", cause: " + t.getMessage(), t);
                        } finally {
                            cdl.countDown();
                        }
                    }
                });
            }
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (poolSuccess.get()) {
            this.cache.set(newCache);
        }
    }

    private RouterCache poolRouter(StateRouter router, AddrCache orign, List<Invoker<T>> invokers, boolean notify) {
        String routerName = router.getName();
        RouterCache routerCache = null;
        if (isCacheMiss(orign, routerName) || router.shouldRePool() || notify) {
            return router.pool(invokers);
        } else {
            routerCache = orign.getCache().get(routerName);
        }
        if (routerCache == null) {
            return new RouterCache();
        }
        return routerCache;
    }

    private boolean isCacheMiss(AddrCache cache, String routerName) {
        if (cache == null || cache.getCache() == null || cache.getInvokers() == null || cache.getCache().get(routerName) == null) {
            return true;
        }
        return false;
    }

    private void loop(boolean notify) {
        if (notify) {
            try {
                loopPermit.acquire();
                loopThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        loopPermit.release();
                        buildCache(true);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            if (loopPermit.tryAcquire()) {
                loopThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        loopPermit.release();
                        buildCache(false);
                    }
                });
            }
        }
    }

}
