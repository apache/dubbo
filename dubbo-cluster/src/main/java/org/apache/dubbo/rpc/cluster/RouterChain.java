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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotSwitcher;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import static org.apache.dubbo.rpc.cluster.Constants.ROUTER_KEY;

/**
 * Router chain
 */
public class RouterChain<T> {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RouterChain.class);

    private volatile SingleRouterChain<T> mainChain;
    private volatile SingleRouterChain<T> backupChain;
    private volatile SingleRouterChain<T> currentChain;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> RouterChain<T> buildChain(Class<T> interfaceClass, URL url) {
        SingleRouterChain<T> chain1 = buildSingleChain(interfaceClass, url);
        SingleRouterChain<T> chain2 = buildSingleChain(interfaceClass, url);
        return new RouterChain<>(new SingleRouterChain[]{chain1, chain2});
    }

    public static <T> SingleRouterChain<T> buildSingleChain(Class<T> interfaceClass, URL url) {
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

        return new SingleRouterChain<>(routers, stateRouters, shouldFailFast, routerSnapshotSwitcher);
    }

    public RouterChain(SingleRouterChain<T>[] chains) {
        if (chains.length != 2) {
            throw new IllegalArgumentException("chains' size should be 2.");
        }
        this.mainChain = chains[0];
        this.backupChain = chains[1];
        this.currentChain = this.mainChain;
    }

    private final AtomicReference<BitList<Invoker<T>>> notifyingInvokers = new AtomicReference<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ReadWriteLock getLock() {
        return lock;
    }

    public SingleRouterChain<T> getSingleChain(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        // If current is in:
        // 1. `setInvokers` is in progress
        // 2. Most of the invocation should use backup chain => currentChain == backupChain
        // 3. Main chain has been update success => notifyingInvokers.get() != null
        //     If `availableInvokers` is created from origin invokers => use backup chain
        //     If `availableInvokers` is created from newly invokers  => use main chain
        BitList<Invoker<T>> notifying = notifyingInvokers.get();
        if (notifying != null &&
            currentChain == backupChain &&
            availableInvokers.getOriginList() == notifying.getOriginList()) {
            return mainChain;
        }
        return currentChain;
    }

    /**
     * @deprecated use {@link RouterChain#getSingleChain(URL, BitList, Invocation)} and {@link SingleRouterChain#route(URL, BitList, Invocation)} instead
     */
    @Deprecated
    public List<Invoker<T>> route(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        return getSingleChain(url, availableInvokers, invocation).route(url, availableInvokers, invocation);
    }

    /**
     * Notify router chain of the initial addresses from registry at the first time.
     * Notify whenever addresses in registry change.
     */
    public synchronized void setInvokers(BitList<Invoker<T>> invokers, Runnable switchAction) {
        try {
            // Lock to prevent directory continue list
            lock.writeLock().lock();

            // Switch to back up chain. Will update main chain first.
            currentChain = backupChain;
        } finally {
            // Release lock to minimize the impact for each newly created invocations as much as possible.
            // Should not release lock until main chain update finished. Or this may cause long hang.
            lock.writeLock().unlock();
        }

        // Refresh main chain.
        // No one can request to use main chain. `currentChain` is backup chain. `route` method cannot access main chain.
        try {
            // Lock main chain to wait all invocation end
            // To wait until no one is using main chain.
            mainChain.getLock().writeLock().lock();

            // refresh
            mainChain.setInvokers(invokers);
        } catch (Throwable t) {
            logger.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Error occurred when refreshing router chain.", t);
            throw t;
        } finally {
            // Unlock main chain
            mainChain.getLock().writeLock().unlock();
        }

        // Set the reference of newly invokers to temp variable.
        // Reason: The next step will switch the invokers reference in directory, so we should check the `availableInvokers`
        //         argument when `route`. If the current invocation use newly invokers, we should use main chain to route, and
        //         this can prevent use newly invokers to route backup chain, which can only route origin invokers now.
        notifyingInvokers.set(invokers);

        // Switch the invokers reference in directory.
        // Cannot switch before update main chain or after backup chain update success. Or that will cause state inconsistent.
        switchAction.run();

        try {
            // Lock to prevent directory continue list
            // The invokers reference in directory now should be the newly one and should always use the newly one once lock released.
            lock.writeLock().lock();

            // Switch to main chain. Will update backup chain later.
            currentChain = mainChain;

            // Clean up temp variable.
            // `availableInvokers` check is useless now, because `route` method will no longer receive any `availableInvokers` related
            // with the origin invokers. The getter of invokers reference in directory is locked now, and will return newly invokers
            // once lock released.
            notifyingInvokers.set(null);
        } finally {
            // Release lock to minimize the impact for each newly created invocations as much as possible.
            // Will use newly invokers and main chain now.
            lock.writeLock().unlock();
        }

        // Refresh main chain.
        // No one can request to use main chain. `currentChain` is main chain. `route` method cannot access backup chain.
        try {
            // Lock main chain to wait all invocation end
            backupChain.getLock().writeLock().lock();

            // refresh
            backupChain.setInvokers(invokers);
        } catch (Throwable t) {
            logger.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Error occurred when refreshing router chain.", t);
            throw t;
        } finally {
            // Unlock backup chain
            backupChain.getLock().writeLock().unlock();
        }
    }

    public synchronized void destroy() {
        // 1. destroy another
        backupChain.destroy();

        // 2. switch
        lock.writeLock().lock();
        currentChain = backupChain;
        lock.writeLock().unlock();

        // 4. destroy
        mainChain.destroy();
    }

    public void addRouters(List<Router> routers) {
        mainChain.addRouters(routers);
        backupChain.addRouters(routers);
    }

    public SingleRouterChain<T> getCurrentChain() {
        return currentChain;
    }

    public List<Router> getRouters() {
        return currentChain.getRouters();
    }

    public StateRouter<T> getHeadStateRouter() {
        return currentChain.getHeadStateRouter();
    }

    @Deprecated
    public List<StateRouter<T>> getStateRouters() {
        return currentChain.getStateRouters();
    }
}
