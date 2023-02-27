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

<<<<<<< HEAD
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
=======
import org.apache.dubbo.common.ProtocolServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.AddressListener;
import org.apache.dubbo.registry.Constants;
import org.apache.dubbo.registry.ProviderFirstParams;
import org.apache.dubbo.registry.integration.AbstractConfiguratorListener;
import org.apache.dubbo.registry.integration.DynamicDirectory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DISABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_DESTROY_INVOKER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_REFER_INVOKER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNSUPPORTED;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_HASHMAP_LOAD_FACTOR;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;
import static org.apache.dubbo.rpc.model.ScopeModelUtil.getModuleModel;

public class ServiceDiscoveryRegistryDirectory<T> extends DynamicDirectory<T> {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ServiceDiscoveryRegistryDirectory.class);

    /**
     * instance address to invoker mapping.
     * The initial value is null and the midway may be assigned to null, please use the local variable reference
     */
    private volatile Map<ProtocolServiceKeyWithAddress, Invoker<T>> urlInvokerMap;
    private volatile ReferenceConfigurationListener referenceConfigurationListener;
    private volatile boolean enableConfigurationListen = true;
    private volatile List<URL> originalUrls = null;
    private volatile Map<String, String> overrideQueryMap;
    private final Set<String> providerFirstParams;
    private final ModuleModel moduleModel;
    private final ProtocolServiceKey consumerProtocolServiceKey;
    private final ConcurrentMap<ProtocolServiceKey, URL> customizedConsumerUrlMap = new ConcurrentHashMap<>();

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

        String protocol = consumerUrl.getParameter(PROTOCOL_KEY, consumerUrl.getProtocol());
        consumerProtocolServiceKey = new ProtocolServiceKey(consumerUrl.getServiceInterface(), consumerUrl.getVersion(), consumerUrl.getGroup(),
            !CommonConstants.CONSUMER.equals(protocol) ? protocol : null);
>>>>>>> origin/3.2
    }

    @Override
    public void subscribe(URL url) {
<<<<<<< HEAD
        super.subscribe(url);
        if (ApplicationModel.getEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            enableConfigurationListen = true;
            CONSUMER_CONFIGURATION_LISTENER.addNotifyListener(this);
            referenceConfigurationListener = new ReferenceConfigurationListener(this, url);
        } else {
            enableConfigurationListen = false;
        }
=======
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
>>>>>>> origin/3.2
    }

    @Override
    public void unSubscribe(URL url) {
        super.unSubscribe(url);
        this.originalUrls = null;
<<<<<<< HEAD
        if (ApplicationModel.getEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            CONSUMER_CONFIGURATION_LISTENER.removeNotifyListener(this);
=======
        if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            getConsumerConfigurationListener(moduleModel).removeNotifyListener(this);
>>>>>>> origin/3.2
            referenceConfigurationListener.stop();
        }
    }

    @Override
<<<<<<< HEAD
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
=======
    public void destroy() {
        super.destroy();
        if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
            getConsumerConfigurationListener(moduleModel).removeNotifyListener(this);
            referenceConfigurationListener.stop();
        }
    }

    @Override
    public void buildRouterChain(URL url) {
        this.setRouterChain(RouterChain.buildChain(getInterface(), url.addParameter(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE)));
>>>>>>> origin/3.2
    }

    @Override
    public synchronized void notify(List<URL> instanceUrls) {
<<<<<<< HEAD
        // Set the context of the address notification thread.
        RpcContext.setRpcContext(getConsumerUrl());

        /**
         * 3.x added for extend URL address
         */
        ExtensionLoader<AddressListener> addressListenerExtensionLoader = ExtensionLoader.getExtensionLoader(AddressListener.class);
=======
        if (isDestroyed()) {
            return;
        }
        // Set the context of the address notification thread.
        RpcServiceContext.getServiceContext().setConsumerUrl(getConsumerUrl());

        //  3.x added for extend URL address
        ExtensionLoader<AddressListener> addressListenerExtensionLoader = getUrl().getOrDefaultModuleModel().getExtensionLoader(AddressListener.class);
>>>>>>> origin/3.2
        List<AddressListener> supportedListeners = addressListenerExtensionLoader.getActivateExtension(getUrl(), (String[]) null);
        if (supportedListeners != null && !supportedListeners.isEmpty()) {
            for (AddressListener addressListener : supportedListeners) {
                instanceUrls = addressListener.notify(instanceUrls, getConsumerUrl(), this);
            }
        }

        refreshOverrideAndInvoker(instanceUrls);
    }

    // RefreshOverrideAndInvoker will be executed by registryCenter and configCenter, so it should be synchronized.
<<<<<<< HEAD
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
=======
    @Override
    protected synchronized void refreshOverrideAndInvoker(List<URL> instanceUrls) {
        // mock zookeeper://xxx?mock=return null
        this.directoryUrl = overrideDirectoryWithConfigurator(getOriginalConsumerUrl());
        refreshInvoker(instanceUrls);
    }


    protected URL overrideDirectoryWithConfigurator(URL url) {
        // override url with configurator from "app-name.configurators"
        url = overrideWithConfigurators(getConsumerConfigurationListener(moduleModel).getConfigurators(), url);

        // override url with configurator from configurators from "service-name.configurators"
        if (referenceConfigurationListener != null) {
            url = overrideWithConfigurators(referenceConfigurationListener.getConfigurators(), url);
        }

        return url;
    }

    private URL overrideWithConfigurators(List<Configurator> configurators, URL url) {
        if (CollectionUtils.isNotEmpty(configurators)) {
            if (url instanceof DubboServiceAddressURL) {
                DubboServiceAddressURL interfaceAddressURL = (DubboServiceAddressURL) url;
                URL overriddenURL = interfaceAddressURL.getOverrideURL();
                if (overriddenURL == null) {
                    String appName = interfaceAddressURL.getApplication();
                    String side = interfaceAddressURL.getSide();
                    overriddenURL = URLBuilder.from(interfaceAddressURL)
                        .clearParameters()
                        .addParameter(APPLICATION_KEY, appName)
                        .addParameter(SIDE_KEY, side).build();
                }
                for (Configurator configurator : configurators) {
                    overriddenURL = configurator.configure(overriddenURL);
                }
                url = new DubboServiceAddressURL(
                    interfaceAddressURL.getUrlAddress(),
                    interfaceAddressURL.getUrlParam(),
                    interfaceAddressURL.getConsumerURL(),
                    (ServiceConfigURL) overriddenURL);
            } else {
                for (Configurator configurator : configurators) {
                    url = configurator.configure(url);
                }
            }
        }
        return url;
    }

    protected InstanceAddressURL overrideWithConfigurator(InstanceAddressURL providerUrl) {
        // override url with configurator from "app-name.configurators"
        providerUrl = overrideWithConfigurators(getConsumerConfigurationListener(moduleModel).getConfigurators(), providerUrl);
>>>>>>> origin/3.2

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

<<<<<<< HEAD
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
=======
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
            logger.warn(PROTOCOL_UNSUPPORTED, "", "", "Received url with EMPTY protocol, will clear all available addresses.");
            refreshRouter(BitList.emptyList(), () ->
                this.forbidden = true // Forbid to access
            );
            destroyAllInvokers(); // Close all invokers
        } else {
            this.forbidden = false; // Allow accessing
            if (CollectionUtils.isEmpty(invokerUrls)) {
                logger.warn(PROTOCOL_UNSUPPORTED, "", "", "Received empty url list, will ignore for protection purpose.");
                return;
            }

            // use local reference to avoid NPE as this.urlInvokerMap will be set null concurrently at destroyAllInvokers().
            Map<ProtocolServiceKeyWithAddress, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
            // can't use local reference as oldUrlInvokerMap's mappings might be removed directly at toInvokers().
            Map<ProtocolServiceKeyWithAddress, Invoker<T>> oldUrlInvokerMap = null;
            if (localUrlInvokerMap != null) {
                // the initial capacity should be set greater than the maximum number of entries divided by the load factor to avoid resizing.
                oldUrlInvokerMap = new LinkedHashMap<>(Math.round(1 + localUrlInvokerMap.size() / DEFAULT_HASHMAP_LOAD_FACTOR));
                localUrlInvokerMap.forEach(oldUrlInvokerMap::put);
            }
            Map<ProtocolServiceKeyWithAddress, Invoker<T>> newUrlInvokerMap = toInvokers(oldUrlInvokerMap, invokerUrls);// Translate url list to Invoker map
            logger.info("Refreshed invoker size " + newUrlInvokerMap.size());

            if (CollectionUtils.isEmptyMap(newUrlInvokerMap)) {
                logger.error(PROTOCOL_UNSUPPORTED, "", "", "Unsupported protocol.", new IllegalStateException("Cannot create invokers from url address list (total " + invokerUrls.size() + ")"));
                return;
            }
            List<Invoker<T>> newInvokers = Collections.unmodifiableList(new ArrayList<>(newUrlInvokerMap.values()));
            BitList<Invoker<T>> finalInvokers = multiGroup ? new BitList<>(toMergeInvokerList(newInvokers)) : new BitList<>(newInvokers);
            // pre-route and build cache
            refreshRouter(finalInvokers.clone(), () -> this.setInvokers(finalInvokers));
            this.urlInvokerMap = newUrlInvokerMap;

            if (oldUrlInvokerMap != null) {
                try {
                    destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap); // Close the unused Invoker
                } catch (Exception e) {
                    logger.warn(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", "destroyUnusedInvokers error. ", e);
                }
>>>>>>> origin/3.2
            }
        }

        // notify invokers refreshed
        this.invokersChanged();
<<<<<<< HEAD
=======

        logger.info("Received invokers changed event from registry. " +
            "Registry type: instance. " +
            "Service Key: " + getConsumerUrl().getServiceKey() + ". " +
            "Urls Size : " + invokerUrls.size() + ". " +
            "Invokers Size : " + getInvokers().size() + ". " +
            "Available Size: " + getValidInvokers().size() + ". " +
            "Available Invokers : " + joinValidInvokerAddresses());
>>>>>>> origin/3.2
    }

    /**
     * Turn urls into invokers, and if url has been refer, will not re-reference.
<<<<<<< HEAD
     *
     * @param urls
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<>();
        if (CollectionUtils.isEmpty(urls)) {
            return newUrlInvokerMap;
        }
=======
     * the items that will be put into newUrlInvokeMap will be removed from oldUrlInvokerMap.
     *
     * @param oldUrlInvokerMap it might be modified during the process.
     * @param urls
     * @return invokers
     */
    private Map<ProtocolServiceKeyWithAddress, Invoker<T>> toInvokers(Map<ProtocolServiceKeyWithAddress, Invoker<T>> oldUrlInvokerMap, List<URL> urls) {
        Map<ProtocolServiceKeyWithAddress, Invoker<T>> newUrlInvokerMap = new ConcurrentHashMap<>(urls == null ? 1 : (int) (urls.size() / 0.75f + 1));
        if (urls == null || urls.isEmpty()) {
            return newUrlInvokerMap;
        }

>>>>>>> origin/3.2
        for (URL url : urls) {
            InstanceAddressURL instanceAddressURL = (InstanceAddressURL) url;
            if (EMPTY_PROTOCOL.equals(instanceAddressURL.getProtocol())) {
                continue;
            }
<<<<<<< HEAD
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(instanceAddressURL.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + instanceAddressURL.getProtocol() +
                        " in notified url: " + instanceAddressURL + " from registry " + getUrl().getAddress() +
                        " to consumer " + NetUtils.getLocalHost() + ", supported protocol: " +
                        ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }

            // FIXME, some keys may need to be removed.
            instanceAddressURL.addConsumerParams(getConsumerUrl().getProtocolServiceKey(), queryMap);
=======
            if (!getUrl().getOrDefaultFrameworkModel().getExtensionLoader(Protocol.class).hasExtension(instanceAddressURL.getProtocol())) {

                // 4-1 - Unsupported protocol

                logger.error(PROTOCOL_UNSUPPORTED, "protocol extension does not installed", "", "Unsupported protocol.",
                    new IllegalStateException("Unsupported protocol " + instanceAddressURL.getProtocol() +
                        " in notified url: " + instanceAddressURL + " from registry " + getUrl().getAddress() +
                        " to consumer " + NetUtils.getLocalHost() + ", supported protocol: " +
                        getUrl().getOrDefaultFrameworkModel().getExtensionLoader(Protocol.class).getSupportedExtensions()));

                continue;
            }

            instanceAddressURL.setProviderFirstParams(providerFirstParams);
>>>>>>> origin/3.2

            // Override provider urls if needed
            if (enableConfigurationListen) {
                instanceAddressURL = overrideWithConfigurator(instanceAddressURL);
            }

<<<<<<< HEAD
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
=======
            // filter all the service available (version wildcard, group wildcard, protocol wildcard)
            int port = instanceAddressURL.getPort();
            List<ProtocolServiceKey> matchedProtocolServiceKeys = instanceAddressURL.getMetadataInfo()
                .getMatchedServiceInfos(consumerProtocolServiceKey)
                .stream()
                .filter(serviceInfo -> serviceInfo.getPort() <= 0 || serviceInfo.getPort() == port)
                .map(MetadataInfo.ServiceInfo::getProtocolServiceKey)
                .collect(Collectors.toList());

            // see org.apache.dubbo.common.ProtocolServiceKey.isSameWith
            // check if needed to override the consumer url
            boolean shouldWrap = matchedProtocolServiceKeys.size() != 1 || !consumerProtocolServiceKey.isSameWith(matchedProtocolServiceKeys.get(0));

            for (ProtocolServiceKey matchedProtocolServiceKey : matchedProtocolServiceKeys) {
                ProtocolServiceKeyWithAddress protocolServiceKeyWithAddress = new ProtocolServiceKeyWithAddress(matchedProtocolServiceKey, instanceAddressURL.getAddress());
                Invoker<T> invoker = oldUrlInvokerMap == null ? null : oldUrlInvokerMap.get(protocolServiceKeyWithAddress);
                if (invoker == null || urlChanged(invoker, instanceAddressURL, matchedProtocolServiceKey)) { // Not in the cache, refer again
                    try {
                        boolean enabled;
                        if (instanceAddressURL.hasParameter(DISABLED_KEY)) {
                            enabled = !instanceAddressURL.getParameter(DISABLED_KEY, false);
                        } else {
                            enabled = instanceAddressURL.getParameter(ENABLED_KEY, true);
                        }
                        if (enabled) {
                            if (shouldWrap) {
                                URL newConsumerUrl = ConcurrentHashMapUtils.computeIfAbsent(customizedConsumerUrlMap, matchedProtocolServiceKey,
                                    k -> consumerUrl.setProtocol(k.getProtocol())
                                        .addParameter(CommonConstants.GROUP_KEY, k.getGroup())
                                        .addParameter(CommonConstants.VERSION_KEY, k.getVersion()));
                                RpcContext.getServiceContext().setConsumerUrl(newConsumerUrl);
                                invoker = new InstanceWrappedInvoker<>(protocol.refer(serviceType, instanceAddressURL), newConsumerUrl, matchedProtocolServiceKey);
                            } else {
                                invoker = protocol.refer(serviceType, instanceAddressURL);
                            }
                        }
                    } catch (Throwable t) {
                        logger.error(PROTOCOL_FAILED_REFER_INVOKER, "", "", "Failed to refer invoker for interface:" + serviceType + ",url:(" + instanceAddressURL + ")" + t.getMessage(), t);
                    }
                    if (invoker != null) { // Put new invoker in cache
                        newUrlInvokerMap.put(protocolServiceKeyWithAddress, invoker);
                    }
                } else {
                    newUrlInvokerMap.put(protocolServiceKeyWithAddress, invoker);
                    oldUrlInvokerMap.remove(protocolServiceKeyWithAddress, invoker);
                }
>>>>>>> origin/3.2
            }
        }
        return newUrlInvokerMap;
    }

<<<<<<< HEAD
    private boolean urlChanged(Invoker<T> invoker, InstanceAddressURL newURL) {
=======
    private boolean urlChanged(Invoker<T> invoker, InstanceAddressURL newURL, ProtocolServiceKey protocolServiceKey) {
>>>>>>> origin/3.2
        InstanceAddressURL oldURL = (InstanceAddressURL) invoker.getUrl();

        if (!newURL.getInstance().equals(oldURL.getInstance())) {
            return true;
        }

        if (oldURL instanceof OverrideInstanceAddressURL || newURL instanceof OverrideInstanceAddressURL) {
<<<<<<< HEAD
            if(!(oldURL instanceof OverrideInstanceAddressURL && newURL instanceof OverrideInstanceAddressURL)) {
=======
            if (!(oldURL instanceof OverrideInstanceAddressURL && newURL instanceof OverrideInstanceAddressURL)) {
>>>>>>> origin/3.2
                // sub-class changed
                return true;
            } else {
                if (!((OverrideInstanceAddressURL) oldURL).getOverrideParams().equals(((OverrideInstanceAddressURL) newURL).getOverrideParams())) {
                    return true;
                }
            }
        }

<<<<<<< HEAD
        return !oldURL.getMetadataInfo().getServiceInfo(getConsumerUrl().getProtocolServiceKey())
                .equals(newURL.getMetadataInfo().getServiceInfo(getConsumerUrl().getProtocolServiceKey()));
    }

    private List<Invoker<T>> toMergeInvokerList(List<Invoker<T>> invokers) {
        return invokers;
=======
        MetadataInfo.ServiceInfo oldServiceInfo = oldURL.getMetadataInfo().getValidServiceInfo(protocolServiceKey.toString());
        if (null == oldServiceInfo) {
            return false;
        }

        return !oldServiceInfo.equals(newURL.getMetadataInfo().getValidServiceInfo(protocolServiceKey.toString()));
    }

    private List<Invoker<T>> toMergeInvokerList(List<Invoker<T>> invokers) {
        List<Invoker<T>> mergedInvokers = new ArrayList<>();
        Map<String, List<Invoker<T>>> groupMap = new HashMap<>();
        for (Invoker<T> invoker : invokers) {
            String group = invoker.getUrl().getGroup("");
            groupMap.computeIfAbsent(group, k -> new ArrayList<>());
            groupMap.get(group).add(invoker);
        }

        if (groupMap.size() == 1) {
            mergedInvokers.addAll(groupMap.values().iterator().next());
        } else if (groupMap.size() > 1) {
            for (List<Invoker<T>> groupList : groupMap.values()) {
                StaticDirectory<T> staticDirectory = new StaticDirectory<>(groupList);
                staticDirectory.buildRouterChain();
                mergedInvokers.add(cluster.join(staticDirectory, false));
            }
        } else {
            mergedInvokers = invokers;
        }
        return mergedInvokers;
>>>>>>> origin/3.2
    }

    /**
     * Close all invokers
     */
    @Override
    protected void destroyAllInvokers() {
<<<<<<< HEAD
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
        if (localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroyAll();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
=======
        Map<ProtocolServiceKeyWithAddress, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
        if (localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", "Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
>>>>>>> origin/3.2
                }
            }
            localUrlInvokerMap.clear();
        }
<<<<<<< HEAD
        invokers = null;
=======

        this.urlInvokerMap = null;
        this.destroyInvokers();
>>>>>>> origin/3.2
    }

    /**
     * Check whether the invoker in the cache needs to be destroyed
     * If set attribute of url: refer.autodestroy=false, the invokers will only increase without decreasing,there may be a refer leak
     *
     * @param oldUrlInvokerMap
     * @param newUrlInvokerMap
     */
<<<<<<< HEAD
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
=======
    private void destroyUnusedInvokers(Map<ProtocolServiceKeyWithAddress, Invoker<T>> oldUrlInvokerMap, Map<ProtocolServiceKeyWithAddress, Invoker<T>> newUrlInvokerMap) {
>>>>>>> origin/3.2
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
<<<<<<< HEAD
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
=======

        if (oldUrlInvokerMap == null || oldUrlInvokerMap.size() == 0) {
            return;
        }

        for (Map.Entry<ProtocolServiceKeyWithAddress, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
            Invoker<T> invoker = entry.getValue();
            if (invoker != null) {
                try {
                    invoker.destroy();
                    if (logger.isDebugEnabled()) {
                        logger.debug("destroy invoker[" + invoker.getUrl() + "] success. ");
                    }
                } catch (Exception e) {
                    logger.warn(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", "destroy invoker[" + invoker.getUrl() + "]failed." + e.getMessage(), e);
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
>>>>>>> origin/3.2
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
<<<<<<< HEAD
                URL backup = RpcContext.getContext().getConsumerUrl();
                RpcContext.getContext().setConsumerUrl(directory.getConsumerUrl());
                directory.refreshOverrideAndInvoker(directory.originalUrls);
                RpcContext.getContext().setConsumerUrl(backup);
=======
                URL backup = RpcContext.getServiceContext().getConsumerUrl();
                RpcContext.getServiceContext().setConsumerUrl(directory.getConsumerUrl());
                directory.refreshOverrideAndInvoker(directory.originalUrls);
                RpcContext.getServiceContext().setConsumerUrl(backup);
>>>>>>> origin/3.2
            }
        }
    }

    private static class ConsumerConfigurationListener extends AbstractConfiguratorListener {
        private final List<ServiceDiscoveryRegistryDirectory<?>> listeners = new ArrayList<>();

<<<<<<< HEAD
        ConsumerConfigurationListener() {
=======
        ConsumerConfigurationListener(ModuleModel moduleModel) {
            super(moduleModel);
>>>>>>> origin/3.2
        }

        void addNotifyListener(ServiceDiscoveryRegistryDirectory<?> listener) {
            if (listeners.size() == 0) {
<<<<<<< HEAD
                this.initWith(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
=======
                this.initWith(moduleModel.getApplicationModel().getApplicationName() + CONFIGURATORS_SUFFIX);
>>>>>>> origin/3.2
            }
            this.listeners.add(listener);
        }

        void removeNotifyListener(ServiceDiscoveryRegistryDirectory<?> listener) {
            this.listeners.remove(listener);
            if (listeners.size() == 0) {
<<<<<<< HEAD
                this.stopListen(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
=======
                this.stopListen(moduleModel.getApplicationModel().getApplicationName() + CONFIGURATORS_SUFFIX);
>>>>>>> origin/3.2
            }
        }

        @Override
        protected void notifyOverrides() {
            listeners.forEach(listener -> {
                if (listener.originalUrls != null) {
<<<<<<< HEAD
                    URL backup = RpcContext.getContext().getConsumerUrl();
                    RpcContext.getContext().setConsumerUrl(listener.getConsumerUrl());
                    listener.refreshOverrideAndInvoker(listener.originalUrls);
                    RpcContext.getContext().setConsumerUrl(backup);
=======
                    URL backup = RpcContext.getServiceContext().getConsumerUrl();
                    RpcContext.getServiceContext().setConsumerUrl(listener.getConsumerUrl());
                    listener.refreshOverrideAndInvoker(listener.originalUrls);
                    RpcContext.getServiceContext().setConsumerUrl(backup);
>>>>>>> origin/3.2
                }
            });
        }
    }
<<<<<<< HEAD
=======

    public static final class ProtocolServiceKeyWithAddress extends ProtocolServiceKey {
        private final String address;

        public ProtocolServiceKeyWithAddress(ProtocolServiceKey protocolServiceKey, String address) {
            super(protocolServiceKey.getInterfaceName(), protocolServiceKey.getVersion(), protocolServiceKey.getGroup(), protocolServiceKey.getProtocol());
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            ProtocolServiceKeyWithAddress that = (ProtocolServiceKeyWithAddress) o;
            return Objects.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), address);
        }
    }

    public static final class InstanceWrappedInvoker<T> implements Invoker<T> {
        private final Invoker<T> originInvoker;
        private final URL newConsumerUrl;
        private final ProtocolServiceKey protocolServiceKey;

        public InstanceWrappedInvoker(Invoker<T> originInvoker, URL newConsumerUrl, ProtocolServiceKey protocolServiceKey) {
            this.originInvoker = originInvoker;
            this.newConsumerUrl = newConsumerUrl;
            this.protocolServiceKey = protocolServiceKey;
        }

        @Override
        public Class<T> getInterface() {
            return originInvoker.getInterface();
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            // override consumer url with real protocol service key
            RpcContext.getServiceContext().setConsumerUrl(newConsumerUrl);
            // recreate invocation due to the protocol service key changed
            RpcInvocation copiedInvocation = new RpcInvocation(invocation.getTargetServiceUniqueName(),
                invocation.getServiceModel(), invocation.getMethodName(), invocation.getServiceName(), protocolServiceKey.toString(),
                invocation.getParameterTypes(), invocation.getArguments(), invocation.getObjectAttachments(),
                invocation.getInvoker(), invocation.getAttributes(),
                invocation instanceof RpcInvocation ? ((RpcInvocation) invocation).getInvokeMode() : null);
            copiedInvocation.setObjectAttachment(CommonConstants.GROUP_KEY, protocolServiceKey.getGroup());
            copiedInvocation.setObjectAttachment(CommonConstants.VERSION_KEY, protocolServiceKey.getVersion());
            return originInvoker.invoke(copiedInvocation);
        }

        @Override
        public URL getUrl() {
            RpcContext.getServiceContext().setConsumerUrl(newConsumerUrl);
            return originInvoker.getUrl();
        }

        @Override
        public boolean isAvailable() {
            RpcContext.getServiceContext().setConsumerUrl(newConsumerUrl);
            return originInvoker.isAvailable();
        }

        @Override
        public void destroy() {
            RpcContext.getServiceContext().setConsumerUrl(newConsumerUrl);
            originInvoker.destroy();
        }
    }

>>>>>>> origin/3.2
}
