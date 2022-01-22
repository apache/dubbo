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
import org.apache.dubbo.registry.ProviderFirstParams;
import org.apache.dubbo.registry.integration.AbstractConfiguratorListener;
import org.apache.dubbo.registry.integration.DynamicDirectory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.DISABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_HASHMAP_LOAD_FACTOR;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;
import static org.apache.dubbo.rpc.model.ScopeModelUtil.getModuleModel;

public class ServiceDiscoveryRegistryDirectory<T> extends DynamicDirectory<T> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryRegistryDirectory.class);

    /**
     * instance address to invoker mapping.
     * The initial value is null and the midway may be assigned to null, please use the local variable reference
     */
    private volatile Map<String, Invoker<T>> urlInvokerMap;
    private volatile ReferenceConfigurationListener referenceConfigurationListener;
    private volatile boolean enableConfigurationListen = true;
    private volatile List<URL> originalUrls = null;
    private volatile Map<String, String> overrideQueryMap;
    private final Set<String> providerFirstParams;
    private final ModuleModel moduleModel;

    public ServiceDiscoveryRegistryDirectory(Class<T> serviceType, URL url) {
        super(serviceType, url);
        moduleModel = getModuleModel(url.getScopeModel());

        Set<ProviderFirstParams> providerFirstParams = url.getOrDefaultApplicationModel().getExtensionLoader(ProviderFirstParams.class).getSupportedExtensionInstances();
        if (CollectionUtils.isEmpty(providerFirstParams)) {
            this.providerFirstParams = null;
        } else {
            if (providerFirstParams.size() == 1) {
                this.providerFirstParams = Collections.unmodifiableSet(providerFirstParams.iterator().next().params());
            } else {
                Set<String> params = new HashSet<>();
                for (ProviderFirstParams paramsFilter : providerFirstParams) {
                    if (paramsFilter.params() == null) {
                        break;
                    }
                    params.addAll(paramsFilter.params());
                }
                this.providerFirstParams = Collections.unmodifiableSet(params);
            }
        }

    }

    @Override
    public void subscribe(URL url) {
        if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            enableConfigurationListen = true;
            getConsumerConfigurationListener(moduleModel).addNotifyListener(this);
            referenceConfigurationListener = new ReferenceConfigurationListener(this.moduleModel, this, url);
        } else {
            enableConfigurationListen = false;
        }
        super.subscribe(url);
    }

    private ConsumerConfigurationListener getConsumerConfigurationListener(ModuleModel moduleModel) {
        return moduleModel.getBeanFactory().getOrRegisterBean(ConsumerConfigurationListener.class,
            type -> new ConsumerConfigurationListener(moduleModel));
    }

    @Override
    public void unSubscribe(URL url) {
        super.unSubscribe(url);
        this.originalUrls = null;
        if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            getConsumerConfigurationListener(moduleModel).removeNotifyListener(this);
            referenceConfigurationListener.stop();
        }
    }

    @Override
    public void buildRouterChain(URL url) {
        this.setRouterChain(RouterChain.buildChain(getInterface(), url.addParameter(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE)));
    }

    @Override
    public synchronized void notify(List<URL> instanceUrls) {
        if (isDestroyed()) {
            return;
        }
        // Set the context of the address notification thread.
        RpcServiceContext.setRpcContext(getConsumerUrl());

        //  3.x added for extend URL address
        ExtensionLoader<AddressListener> addressListenerExtensionLoader = getUrl().getOrDefaultModuleModel().getExtensionLoader(AddressListener.class);
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
        refreshInvoker(instanceUrls);
    }

    private InstanceAddressURL overrideWithConfigurator(InstanceAddressURL providerUrl) {
        // override url with configurator from "app-name.configurators"
        providerUrl = overrideWithConfigurators(getConsumerConfigurationListener(moduleModel).getConfigurators(), providerUrl);

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

    @Override
    public boolean isServiceDiscovery() {
        return true;
    }

    /**
     * This implementation makes sure all application names related to serviceListener received address notification.
     * <p>
     * FIXME, make sure deprecated "interface-application" mapping item be cleared in time.
     */
    @Override
    public boolean isNotificationReceived() {
        return serviceListener == null || serviceListener.isDestroyed()
            || serviceListener.getAllInstances().size() == serviceListener.getServiceNames().size();
    }

    private void refreshInvoker(List<URL> invokerUrls) {
        Assert.notNull(invokerUrls, "invokerUrls should not be null, use EMPTY url to clear current addresses.");
        this.originalUrls = invokerUrls;

        if (invokerUrls.size() == 1 && EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            logger.warn("Received url with EMPTY protocol, will clear all available addresses.");
            this.forbidden = true; // Forbid to access
            routerChain.setInvokers(BitList.emptyList());
            destroyAllInvokers(); // Close all invokers
        } else {
            this.forbidden = false; // Allow accessing
            if (CollectionUtils.isEmpty(invokerUrls)) {
                logger.warn("Received empty url list, will ignore for protection purpose.");
                return;
            }

            // use local reference to avoid NPE as this.urlInvokerMap will be set null concurrently at destroyAllInvokers().
            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
            // can't use local reference as oldUrlInvokerMap's mappings might be removed directly at toInvokers().
            Map<String, Invoker<T>> oldUrlInvokerMap = null;
            if (localUrlInvokerMap != null) {
                // the initial capacity should be set greater than the maximum number of entries divided by the load factor to avoid resizing.
                oldUrlInvokerMap = new LinkedHashMap<>(Math.round(1 + localUrlInvokerMap.size() / DEFAULT_HASHMAP_LOAD_FACTOR));
                localUrlInvokerMap.forEach(oldUrlInvokerMap::put);
            }
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(oldUrlInvokerMap, invokerUrls);// Translate url list to Invoker map
            logger.info("Refreshed invoker size " + newUrlInvokerMap.size());

            if (CollectionUtils.isEmptyMap(newUrlInvokerMap)) {
                logger.error(new IllegalStateException("Cannot create invokers from url address list (total " + invokerUrls.size() + ")"));
                return;
            }
            List<Invoker<T>> newInvokers = Collections.unmodifiableList(new ArrayList<>(newUrlInvokerMap.values()));
            this.setInvokers(multiGroup ? new BitList<>(toMergeInvokerList(newInvokers)) : new BitList<>(newInvokers));
            // pre-route and build cache
            routerChain.setInvokers(this.getInvokers());
            this.urlInvokerMap = newUrlInvokerMap;

            if (oldUrlInvokerMap != null) {
                try {
                    destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap); // Close the unused Invoker
                } catch (Exception e) {
                    logger.warn("destroyUnusedInvokers error. ", e);
                }
            }
        }

        // notify invokers refreshed
        this.invokersChanged();
    }

    /**
     * Turn urls into invokers, and if url has been refer, will not re-reference.
     * the items that will be put into newUrlInvokeMap will be removed from oldUrlInvokerMap.
     *
     * @param oldUrlInvokerMap it might be modified during the process.
     * @param urls
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new ConcurrentHashMap<>(urls == null ? 1 : (int) (urls.size() / 0.75f + 1));
        if (urls == null || urls.isEmpty()) {
            return newUrlInvokerMap;
        }
        for (URL url : urls) {
            InstanceAddressURL instanceAddressURL = (InstanceAddressURL) url;
            if (EMPTY_PROTOCOL.equals(instanceAddressURL.getProtocol())) {
                continue;
            }
            if (!getUrl().getOrDefaultFrameworkModel().getExtensionLoader(Protocol.class).hasExtension(instanceAddressURL.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + instanceAddressURL.getProtocol() +
                    " in notified url: " + instanceAddressURL + " from registry " + getUrl().getAddress() +
                    " to consumer " + NetUtils.getLocalHost() + ", supported protocol: " +
                    getUrl().getOrDefaultFrameworkModel().getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }

            instanceAddressURL.setProviderFirstParams(providerFirstParams);

            // Override provider urls if needed
            if (enableConfigurationListen) {
                instanceAddressURL = overrideWithConfigurator(instanceAddressURL);
            }

            Invoker<T> invoker = oldUrlInvokerMap == null ? null : oldUrlInvokerMap.get(instanceAddressURL.getAddress());
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
                oldUrlInvokerMap.remove(instanceAddressURL.getAddress(), invoker);
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

        this.urlInvokerMap = null;
        this.destroyInvokers();
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

        if (oldUrlInvokerMap == null || oldUrlInvokerMap.size() == 0) {
            return;
        }

        for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
            Invoker<T> invoker = entry.getValue();
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
        logger.info(oldUrlInvokerMap.size() + " deprecated invokers deleted.");
    }

    private class ReferenceConfigurationListener extends AbstractConfiguratorListener {
        private final ServiceDiscoveryRegistryDirectory<?> directory;
        private final URL url;

        ReferenceConfigurationListener(ModuleModel moduleModel, ServiceDiscoveryRegistryDirectory<?> directory, URL url) {
            super(moduleModel);
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
                URL backup = RpcContext.getServiceContext().getConsumerUrl();
                RpcContext.getServiceContext().setConsumerUrl(directory.getConsumerUrl());
                directory.refreshOverrideAndInvoker(directory.originalUrls);
                RpcContext.getServiceContext().setConsumerUrl(backup);
            }
        }
    }

    private static class ConsumerConfigurationListener extends AbstractConfiguratorListener {
        private final List<ServiceDiscoveryRegistryDirectory<?>> listeners = new ArrayList<>();

        ConsumerConfigurationListener(ModuleModel moduleModel) {
            super(moduleModel);
        }

        void addNotifyListener(ServiceDiscoveryRegistryDirectory<?> listener) {
            if (listeners.size() == 0) {
                this.initWith(moduleModel.getApplicationModel().getApplicationName() + CONFIGURATORS_SUFFIX);
            }
            this.listeners.add(listener);
        }

        void removeNotifyListener(ServiceDiscoveryRegistryDirectory<?> listener) {
            this.listeners.remove(listener);
            if (listeners.size() == 0) {
                this.stopListen(moduleModel.getApplicationModel().getApplicationName() + CONFIGURATORS_SUFFIX);
            }
        }

        @Override
        protected void notifyOverrides() {
            listeners.forEach(listener -> {
                if (listener.originalUrls != null) {
                    URL backup = RpcContext.getServiceContext().getConsumerUrl();
                    RpcContext.getServiceContext().setConsumerUrl(listener.getConsumerUrl());
                    listener.refreshOverrideAndInvoker(listener.originalUrls);
                    RpcContext.getServiceContext().setConsumerUrl(backup);
                }
            });
        }
    }
}
