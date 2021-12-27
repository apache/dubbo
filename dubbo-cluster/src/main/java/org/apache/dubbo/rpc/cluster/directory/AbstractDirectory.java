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
package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CONSUMER_URL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * Abstract implementation of Directory: Invoker list returned from this Directory's list method have been filtered by Routers
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

    // logger
    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    private final URL url;

    private volatile boolean destroyed = false;

    protected volatile URL consumerUrl;

    protected RouterChain<T> routerChain;

    protected final Map<String, String> queryMap;

    /**
     * Invokers initialized flag.
     */
    private volatile boolean invokersInitialized = false;

    /**
     * All invokers from registry
     */
    private volatile BitList<Invoker<T>> invokers = BitList.emptyList();

    /**
     * Valid Invoker. All invokers from registry exclude unavailable and disabled invokers.
     */
    private volatile BitList<Invoker<T>> validInvokers = BitList.emptyList();

    /**
     * Waiting to reconnect invokers.
     */
    protected volatile List<Invoker<T>> invokersToReconnect = new CopyOnWriteArrayList<>();

    /**
     * Disabled Invokers. Will not be recovered in reconnect task, but be recovered if registry remove it.
     */
    protected final Set<Invoker<T>> disabledInvokers = new ConcurrentHashSet<>();

    private final Semaphore checkConnectivityPermit = new Semaphore(1);

    private final ScheduledExecutorService connectivityExecutor;

    private volatile ScheduledFuture<?> connectivityCheckFuture;

    /**
     * The max count of invokers for each reconnect task select to try to reconnect.
     */
    private final int reconnectTaskTryCount;

    /**
     * The period of reconnect task if needed. (in ms)
     */
    private final int reconnectTaskPeriod;

    public AbstractDirectory(URL url) {
        this(url, null, false);
    }

    public AbstractDirectory(URL url, boolean isUrlFromRegistry) {
        this(url, null, isUrlFromRegistry);
    }

    public AbstractDirectory(URL url, RouterChain<T> routerChain, boolean isUrlFromRegistry) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }

        this.url = url.removeAttribute(REFER_KEY).removeAttribute(MONITOR_KEY);

        Map<String, String> queryMap;
        Object referParams = url.getAttribute(REFER_KEY);
        if (referParams instanceof Map) {
            queryMap = (Map<String, String>) referParams;
            this.consumerUrl = (URL) url.getAttribute(CONSUMER_URL_KEY);
        } else {
            queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        }

        // remove some local only parameters
        ApplicationModel applicationModel = url.getOrDefaultApplicationModel();
        this.queryMap = applicationModel.getBeanFactory().getBean(ClusterUtils.class).mergeLocalParams(queryMap);

        if (consumerUrl == null) {
            String host = StringUtils.isNotEmpty(queryMap.get("register.ip")) ? queryMap.get("register.ip") : this.url.getHost();
            String path = queryMap.get(PATH_KEY);
            String consumedProtocol = queryMap.get(PROTOCOL_KEY) == null ? DUBBO : queryMap.get(PROTOCOL_KEY);

            URL consumerUrlFrom = this.url
                .setHost(host)
                .setPort(0)
                .setProtocol(consumedProtocol)
                .setPath(path == null ? queryMap.get(INTERFACE_KEY) : path);
            if (isUrlFromRegistry) {
                // reserve parameters if url is already a consumer url
                consumerUrlFrom = consumerUrlFrom.clearParameters().setServiceModel(url.getServiceModel()).setScopeModel(url.getScopeModel());
            }
            this.consumerUrl = consumerUrlFrom.addParameters(queryMap).removeAttribute(MONITOR_KEY);
        }

        this.connectivityExecutor = applicationModel.getExtensionLoader(ExecutorRepository.class).getDefaultExtension().getConnectivityScheduledExecutor();
        Configuration configuration = ConfigurationUtils.getGlobalConfiguration(url.getOrDefaultModuleModel());
        this.reconnectTaskTryCount = configuration.getInt(CommonConstants.RECONNECT_TASK_TRY_COUNT, CommonConstants.DEFAULT_RECONNECT_TASK_TRY_COUNT);
        this.reconnectTaskPeriod = configuration.getInt(CommonConstants.RECONNECT_TASK_PERIOD, CommonConstants.DEFAULT_RECONNECT_TASK_PERIOD);
        setRouterChain(routerChain);
    }

    @Override
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory already destroyed .url: " + getUrl());
        }

        BitList<Invoker<T>> availableInvokers;
        // use clone to avoid being modified at doList().
        if (invokersInitialized) {
            availableInvokers = validInvokers.clone();
        } else {
            availableInvokers = invokers.clone();
        }

        List<Invoker<T>> routedResult = doList(availableInvokers, invocation);
        if (routedResult.isEmpty()) {
            logger.warn("No provider available after connectivity filter for the service " + getConsumerUrl().getServiceKey()
                + " All validInvokers' size: " + validInvokers.size()
                + " All routed invokers' size: " + routedResult.size()
                + " All invokers' size: " + invokers.size()
                + " from registry " + getUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost()
                + " using the dubbo version " + Version.getVersion() + ".");
        }
        return Collections.unmodifiableList(routedResult);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public RouterChain<T> getRouterChain() {
        return routerChain;
    }

    public void setRouterChain(RouterChain<T> routerChain) {
        this.routerChain = routerChain;
    }

    protected void addRouters(List<Router> routers) {
        routers = routers == null ? Collections.emptyList() : routers;
        routerChain.addRouters(routers);
    }

    public URL getConsumerUrl() {
        return consumerUrl;
    }

    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        destroyed = true;
        destroyInvokers();
        invokersToReconnect.clear();
        disabledInvokers.clear();
    }

    @Override
    public void discordAddresses() {
        // do nothing by default
    }

    @Override
    public void addInvalidateInvoker(Invoker<T> invoker) {
        // 1. remove this invoker from validInvokers list, this invoker will not be listed in the next time
        if (removeValidInvoker(invoker)) {
            // 2. add this invoker to reconnect list
            invokersToReconnect.add(invoker);
            // 3. try start check connectivity task
            checkConnectivity();
        }
    }

    public void checkConnectivity() {
        // try to submit task, to ensure there is only one task at most for each directory
        if (checkConnectivityPermit.tryAcquire()) {
            this.connectivityCheckFuture = connectivityExecutor.schedule(() -> {
                try {
                    if (isDestroyed()) {
                        return;
                    }
                    RpcContext.getServiceContext().setConsumerUrl(getConsumerUrl());
                    List<Invoker<T>> needDeleteList = new ArrayList<>();
                    List<Invoker<T>> invokersToTry = new ArrayList<>();

                    // 1. pick invokers from invokersToReconnect
                    // limit max reconnectTaskTryCount, prevent this task hang up all the connectivityExecutor for long time
                    if (invokersToReconnect.size() < reconnectTaskTryCount) {
                        invokersToTry.addAll(invokersToReconnect);
                    } else {
                        for (int i = 0; i < reconnectTaskTryCount; i++) {
                            Invoker<T> tInvoker = invokersToReconnect.get(ThreadLocalRandom.current().nextInt(invokersToReconnect.size()));
                            if (!invokersToTry.contains(tInvoker)) {
                                // ignore if is selected, invokersToTry's size is always smaller than reconnectTaskTryCount + 1
                                invokersToTry.add(tInvoker);
                            }
                        }
                    }

                    // 2. try to check the invoker's status
                    for (Invoker<T> invoker : invokersToTry) {
                        if (invokers.contains(invoker)) {
                            if (invoker.isAvailable()) {
                                needDeleteList.add(invoker);
                            }
                        } else {
                            needDeleteList.add(invoker);
                        }
                    }

                    // 3. recover valid invoker
                    for (Invoker<T> tInvoker : needDeleteList) {
                        if (invokers.contains(tInvoker)) {
                            addValidInvoker(tInvoker);
                            logger.info("Recover service address: " + tInvoker.getUrl() + "  from invalid list.");
                        }
                        invokersToReconnect.remove(tInvoker);
                    }
                } finally {
                    checkConnectivityPermit.release();
                }

                // 4. submit new task if it has more to recover
                if (!invokersToReconnect.isEmpty()) {
                    checkConnectivity();
                }
            }, reconnectTaskPeriod, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Refresh invokers from total invokers
     * 1. all the invokers in need to reconnect list should be removed in the valid invokers list
     * 2. all the invokers in disabled invokers list should be removed in the valid invokers list
     * 3. all the invokers disappeared from total invokers should be removed in the need to reconnect list
     * 4. all the invokers disappeared from total invokers should be removed in the disabled invokers list
     */
    public void refreshInvoker() {
        if (invokersInitialized) {
            refreshInvokerInternal();
        }
    }

    private synchronized void refreshInvokerInternal() {
        BitList<Invoker<T>> copiedInvokers = invokers.clone();
        refreshInvokers(copiedInvokers, invokersToReconnect);
        refreshInvokers(copiedInvokers, disabledInvokers);
        validInvokers = copiedInvokers;
    }

    private void refreshInvokers(BitList<Invoker<T>> targetInvokers, Collection<Invoker<T>> invokersToRemove) {
        List<Invoker<T>> needToRemove = new LinkedList<>();
        for (Invoker<T> tInvoker : invokersToRemove) {
            if (targetInvokers.contains(tInvoker)) {
                targetInvokers.remove(tInvoker);
            } else {
                needToRemove.add(tInvoker);
            }
        }
        invokersToRemove.removeAll(needToRemove);
    }

    @Override
    public void addDisabledInvoker(Invoker<T> invoker) {
        if (invokers.contains(invoker)) {
            disabledInvokers.add(invoker);
            removeValidInvoker(invoker);
            logger.info("Disable service address: " + invoker.getUrl() + ".");
        }
    }

    @Override
    public void recoverDisabledInvoker(Invoker<T> invoker) {
        if (disabledInvokers.remove(invoker)) {
            try {
                addValidInvoker(invoker);
                logger.info("Recover service address: " + invoker.getUrl() + "  from disabled list.");
            } catch (Throwable ignore) {

            }
        }
    }

    /**
     * for ut only
     */
    @Deprecated
    public Semaphore getCheckConnectivityPermit() {
        return checkConnectivityPermit;
    }

    /**
     * for ut only
     */
    @Deprecated
    public ScheduledFuture<?> getConnectivityCheckFuture() {
        return connectivityCheckFuture;
    }

    public BitList<Invoker<T>> getInvokers() {
        // return clone to avoid being modified.
        return invokers.clone();
    }

    public BitList<Invoker<T>> getValidInvokers() {
        // return clone to avoid being modified.
        return validInvokers.clone();
    }

    public List<Invoker<T>> getInvokersToReconnect() {
        return invokersToReconnect;
    }

    public Set<Invoker<T>> getDisabledInvokers() {
        return disabledInvokers;
    }

    protected void setInvokers(BitList<Invoker<T>> invokers) {
        this.invokers = invokers;
        refreshInvokerInternal();
        this.invokersInitialized = true;
    }

    protected void destroyInvokers() {
        // set empty instead of clearing to support concurrent access.
        this.invokers = BitList.emptyList();
        this.validInvokers = BitList.emptyList();
        this.invokersInitialized = false;
    }

    private boolean addValidInvoker(Invoker<T> invoker) {
        synchronized (this.validInvokers) {
            return this.validInvokers.add(invoker);
        }
    }

    private boolean removeValidInvoker(Invoker<T> invoker) {
        synchronized (this.validInvokers) {
            return this.validInvokers.remove(invoker);
        }
    }

    protected abstract List<Invoker<T>> doList(BitList<Invoker<T>> invokers, Invocation invocation) throws RpcException;

}
