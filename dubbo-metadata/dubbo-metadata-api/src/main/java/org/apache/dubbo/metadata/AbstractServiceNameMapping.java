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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

public abstract class AbstractServiceNameMapping implements ServiceNameMapping, ScopeModelAware {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ApplicationModel applicationModel;
    private final Map<String, Set<String>> serviceToAppsMapping = new ConcurrentHashMap<>();
    private final Map<String, MappingListener> mappingListeners = new ConcurrentHashMap<>();

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
    public Set<String> getServices(URL subscribedURL) {
        Set<String> subscribedServices = new TreeSet<>();

        String serviceNames = subscribedURL.getParameter(PROVIDED_BY);
        if (StringUtils.isNotEmpty(serviceNames)) {
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + serviceNames + " instructed by provided-by set by user.");
            subscribedServices.addAll(parseServices(serviceNames));
        }

        String key = ServiceNameMapping.buildMappingKey(subscribedURL);

        if (isEmpty(subscribedServices)) {
            Set<String> cachedServices = this.getCachedMapping(key);
            if(!isEmpty(cachedServices)) {
                subscribedServices.addAll(cachedServices);
            }
        }

        if (isEmpty(subscribedServices)) {
            Set<String> mappedServices = get(subscribedURL);
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + mappedServices + " instructed by remote metadata center.");
            subscribedServices.addAll(mappedServices);
        }

        this.putCachedMapping(key, subscribedServices);

        return subscribedServices;
    }

    /**
     * Register callback to listen to mapping changes.
     *
     * @return cached or remote mapping data
     */
    @Override
    public Set<String> getAndListen(URL registryURL, URL subscribedURL, MappingListener listener) {
        String key = ServiceNameMapping.buildMappingKey(subscribedURL);
        // use previously cached services.
        Set<String> cachedServices = this.getCachedMapping(key);

       Runnable runnable = () -> {
            synchronized (mappingListeners) {
                if (listener != null) {
                    Set<String> mappedServices = getAndListen(subscribedURL, listener);
                    this.putCachedMapping(key, mappedServices);
                    mappingListeners.put(subscribedURL.getProtocolServiceKey(), listener);
                } else {
                    Set<String> mappedServices = get(subscribedURL);
                    this.putCachedMapping(key, mappedServices);
                }
            }
        };

        // Asynchronously register listener in case previous cache does not exist or cache updating in the future.
        if (CollectionUtils.isEmpty(cachedServices)) {
            runnable.run();
            cachedServices = this.getCachedMapping(key);
            if (CollectionUtils.isEmpty(cachedServices)) {
                String registryServices = registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY);
                if (StringUtils.isNotEmpty(registryServices)) {
                    logger.info(subscribedURL.getServiceInterface() + " mapping to " + registryServices + " instructed by registry subscribed-services.");
                    cachedServices = parseServices(registryServices);
                }
            }
        } else {
            ExecutorService executorService = applicationModel.getApplicationExecutorRepository().nextExecutorExecutor();
            executorService.submit(runnable);
        }

        return cachedServices;
    }

    @Override
    public MappingListener stopListen(URL subscribeURL) {
        synchronized (mappingListeners) {
            MappingListener listener = mappingListeners.remove(subscribeURL.getProtocolServiceKey());
            //todo, remove listener from remote metadata center
            listener.stop();
            removeListener(subscribeURL, listener);
            if (mappingListeners.size() == 0) {
                removeCachedMapping(ServiceNameMapping.buildMappingKey(subscribeURL));
            }
            return listener;
        }
    }

    static Set<String> parseServices(String literalServices) {
        return isBlank(literalServices) ? emptySet() :
            unmodifiableSet(of(literalServices.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(toSet()));
    }

    @Override
    public void putCachedMapping(String serviceKey, Set<String> apps) {
        serviceToAppsMapping.put(serviceKey, new TreeSet<>(apps));
    }

    @Override
    public Set<String> getCachedMapping(String mappingKey) {
        return serviceToAppsMapping.get(mappingKey);
    }

    @Override
    public Set<String> getCachedMapping(URL consumerURL) {
        return serviceToAppsMapping.get(ServiceNameMapping.buildMappingKey(consumerURL));
    }

    @Override
    public Set<String> removeCachedMapping(String serviceKey) {
        return serviceToAppsMapping.remove(serviceKey);
    }

    @Override
    public Map<String, Set<String>> getCachedMapping() {
        return Collections.unmodifiableMap(serviceToAppsMapping);
    }
}
