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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.toTreeSet;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

public abstract class AbstractServiceNameMapping implements ServiceNameMapping, ScopeModelAware {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ApplicationModel applicationModel;
    private final MappingCacheManager mappingCacheManager;
    private final Map<String, Set<MappingListener>> mappingListeners = new ConcurrentHashMap<>();
    // mapping lock is shared among registries of the same application.
    private final ConcurrentMap<String, ReentrantLock> mappingLocks = new ConcurrentHashMap<>();
    private volatile boolean initiated;

    public AbstractServiceNameMapping(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.mappingCacheManager = new MappingCacheManager("",
            applicationModel.getFrameworkModel().getBeanFactory()
            .getBean(FrameworkExecutorRepository.class).getCacheRefreshingScheduledExecutor());
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    /**
     * Get the service names from the specified Dubbo service interface, group, version and protocol
     *
     * @return
     */
    abstract public Set<String> get(URL url);

    /**
     * Get the service names from the specified Dubbo service interface, group, version and protocol
     *
     * @return
     */
    abstract public Set<String> getAndListen(URL url, MappingListener mappingListener);

    abstract protected void removeListener(URL url, MappingListener mappingListener);

    @Override
    public synchronized void initInterfaceAppMapping(URL subscribedURL) {
        if (initiated) {
            return;
        }
        initiated = true;
        Set<String> subscribedServices = new TreeSet<>();
        String key = ServiceNameMapping.buildMappingKey(subscribedURL);
        String serviceNames = subscribedURL.getParameter(PROVIDED_BY);

        if (StringUtils.isNotEmpty(serviceNames)) {
            logger.info(key + " mapping to " + serviceNames + " instructed by provided-by set by user.");
            subscribedServices.addAll(parseServices(serviceNames));
        }

        if (isEmpty(subscribedServices)) {
            Set<String> cachedServices = this.getCachedMapping(key);
            if (!isEmpty(cachedServices)) {
                logger.info(key + " mapping to " + serviceNames + " instructed by local cache.");
                subscribedServices.addAll(cachedServices);
            }
        } else {
            this.putCachedMappingIfAbsent(key, subscribedServices);
        }
    }

    @Override
    public Set<String> getAndListen(URL registryURL, URL subscribedURL, MappingListener listener) {
        String key = ServiceNameMapping.buildMappingKey(subscribedURL);
        // use previously cached services.
        Set<String> mappingServices = this.getCachedMapping(key);

        // Asynchronously register listener in case previous cache does not exist or cache expired.
        if (CollectionUtils.isEmpty(mappingServices)) {
            try {
                logger.info("Local cache mapping is empty");
                mappingServices = (new AsyncMappingTask(listener, subscribedURL, false)).call();
            } catch (Exception e) {
                // ignore
            }
            if (CollectionUtils.isEmpty(mappingServices)) {
                String registryServices = registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY);
                if (StringUtils.isNotEmpty(registryServices)) {
                    logger.info(subscribedURL.getServiceInterface() + " mapping to " + registryServices + " instructed by registry subscribed-services.");
                    mappingServices = parseServices(registryServices);
                }
            }
            if (CollectionUtils.isNotEmpty(mappingServices)) {
                this.putCachedMapping(key, mappingServices);
            }
        } else {
            ExecutorService executorService = applicationModel.getFrameworkModel().getBeanFactory()
                .getBean(FrameworkExecutorRepository.class).getMappingRefreshingExecutor();
            executorService.submit(new AsyncMappingTask(listener, subscribedURL, true));
        }

        return mappingServices;
    }

    @Override
    public MappingListener stopListen(URL subscribeURL, MappingListener listener) {
        synchronized (mappingListeners) {
            String mappingKey = ServiceNameMapping.buildMappingKey(subscribeURL);
            Set<MappingListener> listeners = mappingListeners.get(mappingKey);
            //todo, remove listener from remote metadata center
            if (CollectionUtils.isNotEmpty(listeners)) {
                listeners.remove(listener);
                listener.stop();
                removeListener(subscribeURL, listener);
            }
            if (CollectionUtils.isEmpty(listeners)) {
                mappingListeners.remove(mappingKey);
                removeCachedMapping(mappingKey);
                removeMappingLock(mappingKey);
            }
            return listener;
        }
    }

    static Set<String> parseServices(String literalServices) {
        return isBlank(literalServices) ? emptySet() :
            unmodifiableSet(new TreeSet<>(of(literalServices.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(toSet())));
    }

    @Override
    public void putCachedMapping(String serviceKey, Set<String> apps) {
        mappingCacheManager.put(serviceKey, toTreeSet(apps));
    }

    protected void putCachedMappingIfAbsent(String serviceKey, Set<String> apps) {
        Lock lock = getMappingLock(serviceKey);
        try {
            lock.lock();
            if (CollectionUtils.isEmpty(mappingCacheManager.get(serviceKey))) {
                mappingCacheManager.put(serviceKey, toTreeSet(apps));
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<String> getCachedMapping(String mappingKey) {
        return mappingCacheManager.get(mappingKey);
    }

    @Override
    public Set<String> getCachedMapping(URL consumerURL) {
        return getCachedMapping(ServiceNameMapping.buildMappingKey(consumerURL));
    }

    @Override
    public Set<String> removeCachedMapping(String serviceKey) {
        return mappingCacheManager.remove(serviceKey);
    }

    @Override
    public Map<String, Set<String>> getCachedMapping() {
        return Collections.unmodifiableMap(mappingCacheManager.getAll());
    }

    public Lock getMappingLock(String key) {
        return mappingLocks.computeIfAbsent(key, _k -> new ReentrantLock());
    }

    protected void removeMappingLock(String key) {
        Lock lock = mappingLocks.get(key);
        if (lock != null) {
            try {
                lock.lock();
                mappingLocks.remove(key);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void $destroy() {
        mappingCacheManager.destroy();
        mappingListeners.clear();
        mappingLocks.clear();
    }

    private class AsyncMappingTask implements Callable<Set<String>> {
        private final MappingListener listener;
        private final URL subscribedURL;
        private final boolean notifyAtFirstTime;

        public AsyncMappingTask(MappingListener listener, URL subscribedURL, boolean notifyAtFirstTime) {
            this.listener = listener;
            this.subscribedURL = subscribedURL;
            this.notifyAtFirstTime = notifyAtFirstTime;
        }

        @Override
        public Set<String> call() throws Exception {
            synchronized (mappingListeners) {
                Set<String> mappedServices = emptySet();
                try {
                    String mappingKey = ServiceNameMapping.buildMappingKey(subscribedURL);
                    if (listener != null) {
                        mappedServices = toTreeSet(getAndListen(subscribedURL, listener));
                        Set<MappingListener> listeners = mappingListeners.computeIfAbsent(mappingKey, _k -> new HashSet<>());
                        listeners.add(listener);
                        if (CollectionUtils.isNotEmpty(mappedServices)) {
                            if (notifyAtFirstTime) {
                                // guarantee at-least-once notification no matter what kind of underlying meta server is used.
                                // listener notification will also cause updating of mapping cache.
                                listener.onEvent(new MappingChangedEvent(mappingKey, mappedServices));
                            }
                        }
                    } else {
                        mappedServices = get(subscribedURL);
                        if (CollectionUtils.isNotEmpty(mappedServices)) {
                            AbstractServiceNameMapping.this.putCachedMapping(mappingKey, mappedServices);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed getting mapping info from remote center. ", e);
                }
                return mappedServices;
            }
        }
    }
}
