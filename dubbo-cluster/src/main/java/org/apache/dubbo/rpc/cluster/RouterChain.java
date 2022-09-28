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
import org.apache.dubbo.common.config.ConfigurationUtils;
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

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_INTERRUPTED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ROUTER_WAIT_LONG;
import static org.apache.dubbo.rpc.cluster.Constants.ROUTER_KEY;

/**
 * Router chain
 */
public class RouterChain<T> {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RouterChain.class);

    private final SingleRouterChain<T> mainChain;
    private final SingleRouterChain<T> backupChain;
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

    public List<Invoker<T>> route(URL url, BitList<Invoker<T>> availableInvokers, Invocation invocation) {
        return currentChain.route(url, availableInvokers, invocation);
    }

    /**
     * Notify router chain of the initial addresses from registry at the first time.
     * Notify whenever addresses in registry change.
     */
    public synchronized void setInvokers(BitList<Invoker<T>> invokers) {
        // 1. switch
        currentChain = backupChain;

        // 2. wait
        waitChain(mainChain);

        // 3. notify
        mainChain.setInvokers(invokers);

        // 4. switch back
        currentChain = mainChain;

        // 5. wait
        waitChain(backupChain);

        // 6. notify
        backupChain.setInvokers(invokers);
    }

    private void waitChain(SingleRouterChain<T> oldChain) {
        try {
            Thread.sleep(1);
            int waitTime = 0;
            while (oldChain.getCurrentConcurrency() != 0) {
                if (waitTime++ == 1000) {
                    logger.warn(REGISTRY_ROUTER_WAIT_LONG, "Wait router to long", "", "Wait router invoke end exceed 1000ms, router may stuck in.");
                }
                // long time wait
                Thread.sleep(1);
            }
        } catch (Throwable t) {
            logger.error(INTERNAL_INTERRUPTED, "Wait router to interrupted", "", "Wait router to interrupted.");
        }
    }

    public synchronized void destroy() {
        // 1. destroy another
        backupChain.destroy();

        // 2. switch
        currentChain = backupChain;

        // 3. wait
        waitChain(mainChain);

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
