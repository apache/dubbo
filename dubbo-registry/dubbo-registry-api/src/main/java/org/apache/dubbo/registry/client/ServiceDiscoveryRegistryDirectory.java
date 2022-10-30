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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.AddressListener;
import org.apache.dubbo.registry.Constants;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.integration.AbstractConfiguratorListener;
import org.apache.dubbo.registry.integration.DynamicDirectory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DISABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;

public class ServiceDiscoveryRegistryDirectory<T> extends DynamicDirectory<T> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryRegistryDirectory.class);

    // instance address to invoker mapping.
    private volatile Map<String, Invoker<T>> urlInvokerMap; // The initial value is null and the midway may be assigned to null, please use the local variable reference
    private final static ConsumerConfigurationListener CONSUMER_CONFIGURATION_LISTENER = new ConsumerConfigurationListener();
    private volatile ReferenceConfigurationListener referenceConfigurationListener;
    private volatile boolean enableConfigurationListen = true;
    private volatile List<URL> originalUrls = null; // initial for null
    private volatile Map<String, String> overrideQueryMap;

    private ServiceInstancesChangedListener listener;

    public ServiceDiscoveryRegistryDirectory(Class<T> serviceType, URL url) {
        super(serviceType, url);
    }

    @Override
    public void subscribe(URL url) {
        super.subscribe(url);
        if (ApplicationModel.getEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            enableConfigurationListen = true;
            CONSUMER_CONFIGURATION_LISTENER.addNotifyListener(this);
            referenceConfigurationListener = new ReferenceConfigurationListener(this, url);
        } else {
            enableConfigurationListen = false;
        }
    }

    @Override
    public void unSubscribe(URL url) {
        super.unSubscribe(url);
        this.originalUrls = null;
        if (ApplicationModel.getEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            CONSUMER_CONFIGURATION_LISTENER.removeNotifyListener(this);
            referenceConfigurationListener.stop();
        }
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (localUrlInvokerMap != null && localUrlInvokerMap.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                if (invoker.isAvailable()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized void notify(List<URL> instanceUrls) {
        // Set the context of the address notification thread.
        RpcContext.setRpcContext(getConsumerUrl());

        /**
         * 3.x added for extend URL address
         */
        ExtensionLoader<AddressListener> addressListenerExtensionLoader = ExtensionLoader.getExtensionLoader(AddressListener.class);
        List<AddressListener> supportedListeners = addressListenerExtensionLoader.getActivateExtension(getUrl(), (String[]) null);
        if (supportedListeners != null && !supportedListeners.isEmpty()) {
            for (AddressListener addressListener : supportedListeners) {
                instanceUrls = addressListener.notify(instanceUrls, getConsumerUrl(), this);
            }
        }

        refreshOverrideAndInvoker(instanceUrls);
    }

    // RefreshOverrideAndInvoker will be executed by registryCenter and configCenter, so it should be synchronized.
    private synchronized void refreshOverrideAndInvoker(List<URL> instanceUrls) {
        // mock zookeeper://xxx?mock=return null
        if (enableConfigurationListen) {
            overrideDirectoryUrl();
        }
        refreshInvoker(instanceUrls);
    }

    // TODO: exact
    private void overrideDirectoryUrl() {
        // merge override parameters
        this.overrideDirectoryUrl = directoryUrl;
        List<Configurator> localAppDynamicConfigurators = CONSUMER_CONFIGURATION_LISTENER.getConfigurators(); // local reference
        doOverrideUrl(localAppDynamicConfigurators);
        if (referenceConfigurationListener != null) {
            List<Configurator> localDynamicConfigurators = referenceConfigurationListener.getConfigurators(); // local reference
            doOverrideUrl(localDynamicConfigurators);
        }
    }

    private void doOverrideUrl(List<Configurator> configurators) {
        if (CollectionUtils.isNotEmpty(configurators)) {
            for (Configurator configurator : configurators) {
                this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl);
                Map<String, String> newParams = new HashMap<>(this.overrideDirectoryUrl.getParameters());
                directoryUrl.getParameters().forEach(newParams::remove);
                this.overrideQueryMap = newParams;
            }
        }
    }

    private InstanceAddressURL overrideWithConfigurator(InstanceAddressURL providerUrl) {
        // override url with configurator from configurator from "app-name.configurators"
        providerUrl = overrideWithConfigurators(CONSUMER_CONFIGURATION_LISTENER.getConfigurators(), providerUrl);

        // override url with configurator from configurators from "service-name.configurators"
        if (referenceConfigurationListener != null) {
            providerUrl = overrideWithConfigurators(referenceConfigurationListener.getConfigurators(), providerUrl);
        }

        return providerUrl;
    }

    private InstanceAddressURL overrideWithConfigurators(List<Configurator> configurators, InstanceAddressURL url) {
        if (CollectionUtils.isNotEmpty(configurators)) {
            // wrap url
            OverrideInstanceAddressURL overrideInstanceAddressURL = new OverrideInstanceAddressURL(url);
            if (overrideQueryMap != null) {
                // override app-level configs
                overrideInstanceAddressURL = (OverrideInstanceAddressURL) overrideInstanceAddressURL.addParameters(overrideQueryMap);
            }
            for (Configurator configurator : configurators) {
                overrideInstanceAddressURL = (OverrideInstanceAddressURL) configurator.configure(overrideInstanceAddressURL);
            }
            return overrideInstanceAddressURL;
        }
        return url;
    }

    private void refreshInvoker(List<URL> invokerUrls) {
        Assert.notNull(invokerUrls, "invokerUrls should not be null, use empty url list to clear address.");
        this.originalUrls = invokerUrls;

        if (invokerUrls.size() == 0) {
            this.forbidden = true; // Forbid to access
            this.invokers = Collections.emptyList();
            routerChain.setInvokers(this.invokers);
            destroyAllInvokers(); // Close all invokers
            return;
        }

        this.forbidden = false; // Allow to access
        Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap; // local reference
        if (CollectionUtils.isEmpty(invokerUrls)) {
            return;
        }

        Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);// Translate url list to Invoker map

        if (CollectionUtils.isEmptyMap(newUrlInvokerMap)) {
            logger.error(new IllegalStateException("Cannot create invokers from url address list (total " + invokerUrls.size() + ")"));
            return;
        }

        List<Invoker<T>> newInvokers = Collections.unmodifiableList(new ArrayList<>(newUrlInvokerMap.values()));
        // pre-route and build cache, notice that route cache should build on original Invoker list.
        // toMergeMethodInvokerMap() will wrap some invokers having different groups, those wrapped invokers not should be routed.
        routerChain.setInvokers(newInvokers);
        this.invokers = multiGroup ? toMergeInvokerList(newInvokers) : newInvokers;
        this.urlInvokerMap = newUrlInvokerMap;

        if (oldUrlInvokerMap != null) {
            try {
                destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap); // Close the unused Invoker
            } catch (Exception e) {
                logger.warn("destroyUnusedInvokers error. ", e);
            }
        }

        // notify invokers refreshed
        this.invokersChanged();
    }

    /**
     * Turn urls into invokers, and if url has been refer, will not re-reference.
     *
     * @param urls
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<>();
        if (CollectionUtils.isEmpty(urls)) {
            return newUrlInvokerMap;
        }
        for (URL url : urls) {
            InstanceAddressURL instanceAddressURL = (InstanceAddressURL) url;
            if (EMPTY_PROTOCOL.equals(instanceAddressURL.getProtocol())) {
                continue;
            }
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(instanceAddressURL.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + instanceAddressURL.getProtocol() +
                        " in notified url: " + instanceAddressURL + " from registry " + getUrl().getAddress() +
                        " to consumer " + NetUtils.getLocalHost() + ", supported protocol: " +
                        ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }

            // FIXME, some keys may need to be removed.
            instanceAddressURL.addConsumerParams(getConsumerUrl().getProtocolServiceKey(), queryMap);

            // Override provider urls if needed
            if (enableConfigurationListen) {
                instanceAddressURL = overrideWithConfigurator(instanceAddressURL);
            }

            Invoker<T> invoker = urlInvokerMap == null ? null : urlInvokerMap.get(instanceAddressURL.getAddress());
            if (invoker == null || urlChanged(invoker, instanceAddressURL)) { // Not in the cache, refer again
                try {
                    boolean enabled = true;
                    if (instanceAddressURL.hasParameter(DISABLED_KEY)) {
                        enabled = !instanceAddressURL.getParameter(DISABLED_KEY, false);
                    } else {
                        enabled = instanceAddressURL.getParameter(ENABLED_KEY, true);
                    }
                    if (enabled) {
                        invoker = protocol.refer(serviceType, instanceAddressURL);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to refer invoker for interface:" + serviceType + ",url:(" + instanceAddressURL + ")" + t.getMessage(), t);
                }
                if (invoker != null) { // Put new invoker in cache
                    newUrlInvokerMap.put(instanceAddressURL.getAddress(), invoker);
                }
            } else {
                newUrlInvokerMap.put(instanceAddressURL.getAddress(), invoker);
            }
        }
        return newUrlInvokerMap;
    }

    private boolean urlChanged(Invoker<T> invoker, InstanceAddressURL newURL) {
        InstanceAddressURL oldURL = (InstanceAddressURL) invoker.getUrl();

        if (!newURL.getInstance().equals(oldURL.getInstance())) {
            return true;
        }

        if (oldURL instanceof OverrideInstanceAddressURL || newURL instanceof OverrideInstanceAddressURL) {
            if(!(oldURL instanceof OverrideInstanceAddressURL && newURL instanceof OverrideInstanceAddressURL)) {
                // sub-class changed
                return true;
            } else {
                if (!((OverrideInstanceAddressURL) oldURL).getOverrideParams().equals(((OverrideInstanceAddressURL) newURL).getOverrideParams())) {
                    return true;
                }
            }
        }

        return !oldURL.getMetadataInfo().getServiceInfo(getConsumerUrl().getProtocolServiceKey())
                .equals(newURL.getMetadataInfo().getServiceInfo(getConsumerUrl().getProtocolServiceKey()));
    }

    private List<Invoker<T>> toMergeInvokerList(List<Invoker<T>> invokers) {
        return invokers;
    }

    /**
     * Close all invokers
     */
    @Override
    protected void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
        if (localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroyAll();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            localUrlInvokerMap.clear();
        }
        invokers = null;
    }

    /**
     * Check whether the invoker in the cache needs to be destroyed
     * If set attribute of url: refer.autodestroy=false, the invokers will only increase without decreasing,there may be a refer leak
     *
     * @param oldUrlInvokerMap
     * @param newUrlInvokerMap
     */
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
        // check deleted invoker
        List<String> deleted = null;
        if (oldUrlInvokerMap != null) {
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
                if (!newInvokers.contains(entry.getValue())) {
                    if (deleted == null) {
                        deleted = new ArrayList<>();
                    }
                    deleted.add(entry.getKey());
                }
            }
        }

        if (deleted != null) {
            for (String addressKey : deleted) {
                if (addressKey != null) {
                    Invoker<T> invoker = oldUrlInvokerMap.remove(addressKey);
                    if (invoker != null) {
                        try {
                            invoker.destroyAll();
                            if (logger.isDebugEnabled()) {
                                logger.debug("destroy invoker[" + invoker.getUrl() + "] success. ");
                            }
                        } catch (Exception e) {
                            logger.warn("destroy invoker[" + invoker.getUrl() + "] failed. " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private static class ReferenceConfigurationListener extends AbstractConfiguratorListener {
        private final ServiceDiscoveryRegistryDirectory<?> directory;
        private final URL url;

        ReferenceConfigurationListener(ServiceDiscoveryRegistryDirectory<?> directory, URL url) {
            this.directory = directory;
            this.url = url;
            this.initWith(DynamicConfiguration.getRuleKey(url) + CONFIGURATORS_SUFFIX);
        }

        void stop() {
            this.stopListen(DynamicConfiguration.getRuleKey(url) + CONFIGURATORS_SUFFIX);
        }

        @Override
        protected void notifyOverrides() {
            // to notify configurator/router changes
            if (directory.originalUrls != null) {
                URL backup = RpcContext.getContext().getConsumerUrl();
                RpcContext.getContext().setConsumerUrl(directory.getConsumerUrl());
                directory.refreshOverrideAndInvoker(directory.originalUrls);
                RpcContext.getContext().setConsumerUrl(backup);
            }
        }
    }

    private static class ConsumerConfigurationListener extends AbstractConfiguratorListener {
        private final List<ServiceDiscoveryRegistryDirectory<?>> listeners = new ArrayList<>();

        ConsumerConfigurationListener() {
        }

        void addNotifyListener(ServiceDiscoveryRegistryDirectory<?> listener) {
            if (listeners.size() == 0) {
                this.initWith(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
            }
            this.listeners.add(listener);
        }

        void removeNotifyListener(ServiceDiscoveryRegistryDirectory<?> listener) {
            this.listeners.remove(listener);
            if (listeners.size() == 0) {
                this.stopListen(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
            }
        }

        @Override
        protected void notifyOverrides() {
            listeners.forEach(listener -> {
                if (listener.originalUrls != null) {
                    URL backup = RpcContext.getContext().getConsumerUrl();
                    RpcContext.getContext().setConsumerUrl(listener.getConsumerUrl());
                    listener.refreshOverrideAndInvoker(listener.originalUrls);
                    RpcContext.getContext().setConsumerUrl(backup);
                }
            });
        }
    }
}
