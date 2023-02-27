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
package org.apache.dubbo.registry.integration;

<<<<<<< HEAD
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.Constants;
=======
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
>>>>>>> origin/3.2
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
<<<<<<< HEAD
=======
import org.apache.dubbo.registry.client.migration.MigrationClusterInvoker;
>>>>>>> origin/3.2
import org.apache.dubbo.registry.client.migration.ServiceDiscoveryMigrationInvoker;
import org.apache.dubbo.registry.retry.ReExportTask;
import org.apache.dubbo.registry.support.SkipFailbackWrapperException;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Configurator;
<<<<<<< HEAD
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.cluster.support.MergeableCluster;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.EXTRA_KEYS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HIDDEN_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ON_CONNECT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ON_DISCONNECT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
=======
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.cluster.support.MergeableCluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.EXTRA_KEYS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.IPV6_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_PROTOCOL_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_UNSUPPORTED_CATEGORY;
>>>>>>> origin/3.2
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
<<<<<<< HEAD
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.OVERRIDE_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.utils.UrlUtils.classifyUrls;
import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.DEFAULT_REGISTRY_RETRY_PERIOD;
=======
import static org.apache.dubbo.common.constants.RegistryConstants.ALL_CATEGORIES;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.OVERRIDE_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.UrlUtils.classifyUrls;
import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;
import static org.apache.dubbo.registry.Constants.DEFAULT_REGISTRY_RETRY_PERIOD;
import static org.apache.dubbo.registry.Constants.ENABLE_CONFIGURATION_LISTEN;
>>>>>>> origin/3.2
import static org.apache.dubbo.registry.Constants.PROVIDER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.registry.Constants.REGISTER_KEY;
import static org.apache.dubbo.registry.Constants.REGISTRY_RETRY_PERIOD_KEY;
import static org.apache.dubbo.registry.Constants.SIMPLIFIED_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.remoting.Constants.CHECK_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.CONNECTIONS_KEY;
import static org.apache.dubbo.remoting.Constants.EXCHANGER_KEY;
<<<<<<< HEAD
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.rpc.Constants.DEPRECATED_KEY;
import static org.apache.dubbo.rpc.Constants.INTERFACES;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
=======
import static org.apache.dubbo.remoting.Constants.PREFER_SERIALIZATION_KEY;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.rpc.Constants.DEPRECATED_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.INTERFACES;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CONSUMER_URL_KEY;
>>>>>>> origin/3.2
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WARMUP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;
<<<<<<< HEAD
=======
import static org.apache.dubbo.rpc.model.ScopeModelUtil.getApplicationModel;
>>>>>>> origin/3.2

/**
 * TODO, replace RegistryProtocol completely in the future.
 */
<<<<<<< HEAD
public class RegistryProtocol implements Protocol {
    public static final String[] DEFAULT_REGISTER_PROVIDER_KEYS = {
            APPLICATION_KEY, CODEC_KEY, EXCHANGER_KEY, SERIALIZATION_KEY, CLUSTER_KEY, CONNECTIONS_KEY, DEPRECATED_KEY,
            GROUP_KEY, LOADBALANCE_KEY, MOCK_KEY, PATH_KEY, TIMEOUT_KEY, TOKEN_KEY, VERSION_KEY, WARMUP_KEY,
            WEIGHT_KEY, TIMESTAMP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY, TAG_KEY
    };

    public static final String[] DEFAULT_REGISTER_CONSUMER_KEYS = {
            APPLICATION_KEY, VERSION_KEY, GROUP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY
    };

    private static final String REGISTRY_PROTOCOL_LISTENER_KEY = "registry.protocol.listener";
    private static final int DEFAULT_PORT = 9090;

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryProtocol.class);
    private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<>();
    private final Map<String, ServiceConfigurationListener> serviceConfigurationListeners = new ConcurrentHashMap<>();
    private final ProviderConfigurationListener providerConfigurationListener = new ProviderConfigurationListener();
    // To solve the problem of RMI repeated exposure port conflicts, the services that have been exposed are no longer exposed.
    // providerurl <--> exporter
    private final ConcurrentMap<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<>();
    protected Protocol protocol;
    protected RegistryFactory registryFactory;
    protected ProxyFactory proxyFactory;

    private ConcurrentMap<URL, ReExportTask> reExportFailedTasks = new ConcurrentHashMap<>();
    private HashedWheelTimer retryTimer =
            new HashedWheelTimer(new NamedThreadFactory("DubboReexportTimer", true), DEFAULT_REGISTRY_RETRY_PERIOD, TimeUnit.MILLISECONDS,
                    128);

    // get the parameters which shouldn't been displayed in url string(Starting with .)
    private static String[] getHiddenKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (CollectionUtils.isNotEmptyMap(params)) {
            return params.keySet().stream()
                    .filter(k -> k.startsWith(HIDDEN_KEY_PREFIX))
                    .toArray(String[]::new);
=======
public class RegistryProtocol implements Protocol, ScopeModelAware {
    public static final String[] DEFAULT_REGISTER_PROVIDER_KEYS = {
        APPLICATION_KEY, CODEC_KEY, EXCHANGER_KEY, SERIALIZATION_KEY, PREFER_SERIALIZATION_KEY, CLUSTER_KEY, CONNECTIONS_KEY, DEPRECATED_KEY,
        GROUP_KEY, LOADBALANCE_KEY, MOCK_KEY, PATH_KEY, TIMEOUT_KEY, TOKEN_KEY, VERSION_KEY, WARMUP_KEY,
        WEIGHT_KEY, DUBBO_VERSION_KEY, RELEASE_KEY, SIDE_KEY, IPV6_KEY
    };

    public static final String[] DEFAULT_REGISTER_CONSUMER_KEYS = {
        APPLICATION_KEY, VERSION_KEY, GROUP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY
    };

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RegistryProtocol.class);

    private final Map<String, ServiceConfigurationListener> serviceConfigurationListeners = new ConcurrentHashMap<>();
    //To solve the problem of RMI repeated exposure port conflicts, the services that have been exposed are no longer exposed.
    //provider url <--> exporter
    private final ConcurrentMap<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<>();
    protected Protocol protocol;
    protected ProxyFactory proxyFactory;

    private ConcurrentMap<URL, ReExportTask> reExportFailedTasks = new ConcurrentHashMap<>();
    private HashedWheelTimer retryTimer = new HashedWheelTimer(new NamedThreadFactory("DubboReexportTimer", true), DEFAULT_REGISTRY_RETRY_PERIOD, TimeUnit.MILLISECONDS, 128);
    private FrameworkModel frameworkModel;

    //Filter the parameters that do not need to be output in url(Starting with .)
    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (CollectionUtils.isNotEmptyMap(params)) {
            return params.keySet().stream()
                .filter(k -> k.startsWith(HIDE_KEY_PREFIX))
                .toArray(String[]::new);
>>>>>>> origin/3.2
        } else {
            return new String[0];
        }
    }

<<<<<<< HEAD
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
=======
    public RegistryProtocol() {
    }

    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
>>>>>>> origin/3.2
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public int getDefaultPort() {
<<<<<<< HEAD
        return DEFAULT_PORT;
    }

    public Map<URL, NotifyListener> getOverrideListeners() {
        return overrideListeners;
    }

    private void registerStatedUrl(URL registryUrl, URL registeredProviderUrl, boolean registered) {
        ProviderModel model = ApplicationModel.getProviderModel(registeredProviderUrl.getServiceKey());
        model.addStatedUrl(new ProviderModel.RegisterStatedURL(
                registeredProviderUrl,
                registryUrl,
                registered));
=======
        return 9090;
    }

    public Map<URL, Set<NotifyListener>> getOverrideListeners() {
        Map<URL, Set<NotifyListener>> map = new HashMap<>();
        List<ApplicationModel> applicationModels = frameworkModel.getApplicationModels();
        if (applicationModels.size() == 1) {
            return applicationModels.get(0).getBeanFactory().getBean(ProviderConfigurationListener.class).getOverrideListeners();
        } else {
            for (ApplicationModel applicationModel : applicationModels) {
                map.putAll(applicationModel.getBeanFactory().getBean(ProviderConfigurationListener.class).getOverrideListeners());
            }
        }
        return map;
    }

    private void register(Registry registry, URL registeredProviderUrl) {
        ApplicationDeployer deployer = registeredProviderUrl.getOrDefaultApplicationModel().getDeployer();
        try {
            deployer.increaseServiceRefreshCount();
            registry.register(registeredProviderUrl);
        } finally {
            deployer.decreaseServiceRefreshCount();
        }
    }

    private void registerStatedUrl(URL registryUrl, URL registeredProviderUrl, boolean registered) {
        ProviderModel model = (ProviderModel) registeredProviderUrl.getServiceModel();
        model.addStatedUrl(new ProviderModel.RegisterStatedURL(
            registeredProviderUrl,
            registryUrl,
            registered));
>>>>>>> origin/3.2
    }

    @Override
    public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {
        URL registryUrl = getRegistryUrl(originInvoker);
        // url to export locally
        URL providerUrl = getProviderUrl(originInvoker);

        // Subscribe the override data
        // FIXME When the provider subscribes, it will affect the scene : a certain JVM exposes the service and call
        //  the same service. Because the subscribed is cached key with the name of the service, it causes the
        //  subscription information to cover.
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(providerUrl);
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
<<<<<<< HEAD
        overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);

        providerUrl = overrideUrlWithConfig(providerUrl, overrideSubscribeListener);
        // export invoker
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker, providerUrl);

        // url to registry
        final Registry registry = getRegistry(originInvoker);
        final URL registeredProviderUrl = getUrlToRegistry(providerUrl, registryUrl);

        // decide if we need to delay publish
        boolean register = providerUrl.getParameter(REGISTER_KEY, true);
        if (register) {
            registry.register(registeredProviderUrl);
=======
        Map<URL, Set<NotifyListener>> overrideListeners = getProviderConfigurationListener(overrideSubscribeUrl).getOverrideListeners();
        overrideListeners.computeIfAbsent(overrideSubscribeUrl, k -> new ConcurrentHashSet<>())
            .add(overrideSubscribeListener);

        providerUrl = overrideUrlWithConfig(providerUrl, overrideSubscribeListener);
        //export invoker
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker, providerUrl);

        // url to registry
        final Registry registry = getRegistry(registryUrl);
        final URL registeredProviderUrl = getUrlToRegistry(providerUrl, registryUrl);

        // decide if we need to delay publish (provider itself and registry should both need to register)
        boolean register = providerUrl.getParameter(REGISTER_KEY, true) && registryUrl.getParameter(REGISTER_KEY, true);
        if (register) {
            register(registry, registeredProviderUrl);
>>>>>>> origin/3.2
        }

        // register stated url on provider model
        registerStatedUrl(registryUrl, registeredProviderUrl, register);


        exporter.setRegisterUrl(registeredProviderUrl);
        exporter.setSubscribeUrl(overrideSubscribeUrl);
<<<<<<< HEAD

        // Deprecated! Subscribe to override rules in 2.6.x or before.
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);
=======
        exporter.setNotifyListener(overrideSubscribeListener);

        if (!registry.isServiceDiscovery()) {
            // Deprecated! Subscribe to override rules in 2.6.x or before.
            registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);
        }
>>>>>>> origin/3.2

        notifyExport(exporter);
        //Ensure that a new exporter instance is returned every time export
        return new DestroyableExporter<>(exporter);
    }

    private <T> void notifyExport(ExporterChangeableWrapper<T> exporter) {
<<<<<<< HEAD
        List<RegistryProtocolListener> listeners = ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class)
                .getActivateExtension(exporter.getOriginInvoker().getUrl(), REGISTRY_PROTOCOL_LISTENER_KEY);
=======
        ScopeModel scopeModel = exporter.getRegisterUrl().getScopeModel();
        List<RegistryProtocolListener> listeners = ScopeModelUtil.getExtensionLoader(RegistryProtocolListener.class, scopeModel)
            .getActivateExtension(exporter.getOriginInvoker().getUrl(), REGISTRY_PROTOCOL_LISTENER_KEY);
>>>>>>> origin/3.2
        if (CollectionUtils.isNotEmpty(listeners)) {
            for (RegistryProtocolListener listener : listeners) {
                listener.onExport(this, exporter);
            }
        }
    }

    private URL overrideUrlWithConfig(URL providerUrl, OverrideListener listener) {
<<<<<<< HEAD
        providerUrl = providerConfigurationListener.overrideUrl(providerUrl);
        ServiceConfigurationListener serviceConfigurationListener = new ServiceConfigurationListener(providerUrl, listener);
=======
        ProviderConfigurationListener providerConfigurationListener = getProviderConfigurationListener(providerUrl);
        providerUrl = providerConfigurationListener.overrideUrl(providerUrl);

        ServiceConfigurationListener serviceConfigurationListener = new ServiceConfigurationListener(providerUrl.getOrDefaultModuleModel(), providerUrl, listener);
>>>>>>> origin/3.2
        serviceConfigurationListeners.put(providerUrl.getServiceKey(), serviceConfigurationListener);
        return serviceConfigurationListener.overrideUrl(providerUrl);
    }

    @SuppressWarnings("unchecked")
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker, URL providerUrl) {
        String key = getCacheKey(originInvoker);

        return (ExporterChangeableWrapper<T>) bounds.computeIfAbsent(key, s -> {
            Invoker<?> invokerDelegate = new InvokerDelegate<>(originInvoker, providerUrl);
            return new ExporterChangeableWrapper<>((Exporter<T>) protocol.export(invokerDelegate), originInvoker);
        });
    }

    public <T> void reExport(Exporter<T> exporter, URL newInvokerUrl) {
        if (exporter instanceof ExporterChangeableWrapper) {
            ExporterChangeableWrapper<T> exporterWrapper = (ExporterChangeableWrapper<T>) exporter;
            Invoker<T> originInvoker = exporterWrapper.getOriginInvoker();
            reExport(originInvoker, newInvokerUrl);
        }
    }

    /**
     * Reexport the invoker of the modified url
     *
     * @param originInvoker
     * @param newInvokerUrl
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> void reExport(final Invoker<T> originInvoker, URL newInvokerUrl) {
        String key = getCacheKey(originInvoker);
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        URL registeredUrl = exporter.getRegisterUrl();

        URL registryUrl = getRegistryUrl(originInvoker);
        URL newProviderUrl = getUrlToRegistry(newInvokerUrl, registryUrl);

        // update local exporter
        Invoker<T> invokerDelegate = new InvokerDelegate<T>(originInvoker, newInvokerUrl);
        exporter.setExporter(protocol.export(invokerDelegate));

        // update registry
        if (!newProviderUrl.equals(registeredUrl)) {
            try {
                doReExport(originInvoker, exporter, registryUrl, registeredUrl, newProviderUrl);
            } catch (Exception e) {
                ReExportTask oldTask = reExportFailedTasks.get(registeredUrl);
                if (oldTask != null) {
                    return;
                }
                ReExportTask task = new ReExportTask(
<<<<<<< HEAD
                        () -> doReExport(originInvoker, exporter, registryUrl, registeredUrl, newProviderUrl),
                        registeredUrl,
                        null
=======
                    () -> doReExport(originInvoker, exporter, registryUrl, registeredUrl, newProviderUrl),
                    registeredUrl,
                    null
>>>>>>> origin/3.2
                );
                oldTask = reExportFailedTasks.putIfAbsent(registeredUrl, task);
                if (oldTask == null) {
                    // never has a retry task. then start a new task for retry.
<<<<<<< HEAD
                    retryTimer.newTimeout(task, registryUrl.getParameter(REGISTRY_RETRY_PERIOD_KEY, DEFAULT_REGISTRY_RETRY_PERIOD),
                            TimeUnit.MILLISECONDS);
=======
                    retryTimer.newTimeout(task, registryUrl.getParameter(REGISTRY_RETRY_PERIOD_KEY, DEFAULT_REGISTRY_RETRY_PERIOD), TimeUnit.MILLISECONDS);
>>>>>>> origin/3.2
                }
            }
        }
    }

    private <T> void doReExport(final Invoker<T> originInvoker, ExporterChangeableWrapper<T> exporter,
                                URL registryUrl, URL oldProviderUrl, URL newProviderUrl) {
        if (getProviderUrl(originInvoker).getParameter(REGISTER_KEY, true)) {
<<<<<<< HEAD
            Registry registry = null;
            try {
                registry = getRegistry(originInvoker);
=======
            Registry registry;
            try {
                registry = getRegistry(getRegistryUrl(originInvoker));
>>>>>>> origin/3.2
            } catch (Exception e) {
                throw new SkipFailbackWrapperException(e);
            }

<<<<<<< HEAD
            LOGGER.info("Try to unregister old url: " + oldProviderUrl);
            registry.reExportUnregister(oldProviderUrl);

            LOGGER.info("Try to register new url: " + newProviderUrl);
=======
            logger.info("Try to unregister old url: " + oldProviderUrl);
            registry.reExportUnregister(oldProviderUrl);

            logger.info("Try to register new url: " + newProviderUrl);
>>>>>>> origin/3.2
            registry.reExportRegister(newProviderUrl);
        }
        try {
            ProviderModel.RegisterStatedURL statedUrl = getStatedUrl(registryUrl, newProviderUrl);
            statedUrl.setProviderUrl(newProviderUrl);
            exporter.setRegisterUrl(newProviderUrl);
        } catch (Exception e) {
            throw new SkipFailbackWrapperException(e);
        }
    }

    private ProviderModel.RegisterStatedURL getStatedUrl(URL registryUrl, URL providerUrl) {
<<<<<<< HEAD
        ProviderModel providerModel = ApplicationModel.getServiceRepository()
                .lookupExportedService(providerUrl.getServiceKey());

        List<ProviderModel.RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
        return statedUrls.stream()
                .filter(u -> u.getRegistryUrl().equals(registryUrl)
                        && u.getProviderUrl().getProtocol().equals(providerUrl.getProtocol()))
                .findFirst().orElseThrow(() -> new IllegalStateException("There should have at least one registered url."));
=======
        ProviderModel providerModel = frameworkModel.getServiceRepository()
            .lookupExportedService(providerUrl.getServiceKey());

        List<ProviderModel.RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
        return statedUrls.stream()
            .filter(u -> u.getRegistryUrl().equals(registryUrl)
                && u.getProviderUrl().getProtocol().equals(providerUrl.getProtocol()))
            .findFirst().orElseThrow(() -> new IllegalStateException("There should have at least one registered url."));
>>>>>>> origin/3.2
    }

    /**
     * Get an instance of registry based on the address of invoker
     *
<<<<<<< HEAD
     * @param originInvoker
     * @return
     */
    protected Registry getRegistry(final Invoker<?> originInvoker) {
        URL registryUrl = getRegistryUrl(originInvoker);
        return getRegistry(registryUrl);
    }

    protected Registry getRegistry(URL url) {
        try {
            return registryFactory.getRegistry(url);
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
            throw t;
        }
=======
     * @param registryUrl
     * @return
     */
    protected Registry getRegistry(final URL registryUrl) {
        RegistryFactory registryFactory = ScopeModelUtil.getExtensionLoader(RegistryFactory.class, registryUrl.getScopeModel()).getAdaptiveExtension();
        return registryFactory.getRegistry(registryUrl);
>>>>>>> origin/3.2
    }

    protected URL getRegistryUrl(Invoker<?> originInvoker) {
        return originInvoker.getUrl();
    }

    protected URL getRegistryUrl(URL url) {
        if (SERVICE_REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return url;
        }
        return url.addParameter(REGISTRY_KEY, url.getProtocol()).setProtocol(SERVICE_REGISTRY_PROTOCOL);
    }

    /**
     * Return the url that is registered to the registry and filter the url parameter once
     *
     * @param providerUrl
     * @return url to registry.
     */
    private URL getUrlToRegistry(final URL providerUrl, final URL registryUrl) {
<<<<<<< HEAD

        URL registeredProviderUrl = removeUselessParameters(providerUrl);

        //The address you see at the registry
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return registeredProviderUrl.removeParameters(getHiddenKeys(registeredProviderUrl)).removeParameters(
                    MONITOR_KEY, BIND_IP_KEY, BIND_PORT_KEY, QOS_ENABLE, QOS_HOST, QOS_PORT, ACCEPT_FOREIGN_IP, VALIDATION_KEY,
                    INTERFACES);
=======
        //The address you see at the registry
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return providerUrl.removeParameters(getFilteredKeys(providerUrl)).removeParameters(
                MONITOR_KEY, BIND_IP_KEY, BIND_PORT_KEY, QOS_ENABLE, QOS_HOST, QOS_PORT, ACCEPT_FOREIGN_IP, VALIDATION_KEY,
                INTERFACES);
>>>>>>> origin/3.2
        } else {
            String extraKeys = registryUrl.getParameter(EXTRA_KEYS_KEY, "");
            // if path is not the same as interface name then we should keep INTERFACE_KEY,
            // otherwise, the registry structure of zookeeper would be '/dubbo/path/providers',
            // but what we expect is '/dubbo/interface/providers'
<<<<<<< HEAD
            if (!registeredProviderUrl.getPath().equals(registeredProviderUrl.getParameter(INTERFACE_KEY))) {
=======
            if (!providerUrl.getPath().equals(providerUrl.getParameter(INTERFACE_KEY))) {
>>>>>>> origin/3.2
                if (StringUtils.isNotEmpty(extraKeys)) {
                    extraKeys += ",";
                }
                extraKeys += INTERFACE_KEY;
            }
            String[] paramsToRegistry = getParamsToRegistry(DEFAULT_REGISTER_PROVIDER_KEYS
<<<<<<< HEAD
                    , COMMA_SPLIT_PATTERN.split(extraKeys));
            return URL.valueOf(registeredProviderUrl, paramsToRegistry, registeredProviderUrl.getParameter(METHODS_KEY, (String[]) null));
=======
                , COMMA_SPLIT_PATTERN.split(extraKeys));
            return URL.valueOf(providerUrl, paramsToRegistry, providerUrl.getParameter(METHODS_KEY, (String[]) null));
>>>>>>> origin/3.2
        }

    }

<<<<<<< HEAD
    /**
     * Remove information that does not require registration
     *
     * @param providerUrl
     * @return
     */
    private URL removeUselessParameters(URL providerUrl) {
        return providerUrl.removeParameters(ON_CONNECT_KEY, ON_DISCONNECT_KEY);
    }

    private URL getSubscribedOverrideUrl(URL registeredProviderUrl) {
        return registeredProviderUrl.setProtocol(PROVIDER_PROTOCOL)
                .addParameters(CATEGORY_KEY, CONFIGURATORS_CATEGORY, CHECK_KEY, String.valueOf(false));
=======
    private URL getSubscribedOverrideUrl(URL registeredProviderUrl) {
        return registeredProviderUrl.setProtocol(PROVIDER_PROTOCOL)
            .addParameters(CATEGORY_KEY, CONFIGURATORS_CATEGORY, CHECK_KEY, String.valueOf(false));
>>>>>>> origin/3.2
    }

    /**
     * Get the address of the providerUrl through the url of the invoker
     *
     * @param originInvoker
     * @return
     */
    private URL getProviderUrl(final Invoker<?> originInvoker) {
<<<<<<< HEAD
        String export = originInvoker.getUrl().getParameterAndDecoded(EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + originInvoker.getUrl());
        }
        return URL.valueOf(export);
=======
        Object providerURL = originInvoker.getUrl().getAttribute(EXPORT_KEY);
        if (!(providerURL instanceof URL)) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + originInvoker.getUrl().getAddress());
        }
        return (URL) providerURL;
>>>>>>> origin/3.2
    }

    /**
     * Get the key cached in bounds by invoker
     *
     * @param originInvoker
     * @return
     */
    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
<<<<<<< HEAD
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString();
=======
        String key = providerUrl.removeParameters(DYNAMIC_KEY, ENABLED_KEY).toFullString();
>>>>>>> origin/3.2
        return key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        url = getRegistryUrl(url);
        Registry registry = getRegistry(url);
        if (RegistryService.class.equals(type)) {
            return proxyFactory.getInvoker((T) registry, type, url);
        }

        // group="a,b" or group="*"
<<<<<<< HEAD
        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        String group = qs.get(GROUP_KEY);
        if (group != null && group.length() > 0) {
            if ((COMMA_SPLIT_PATTERN.split(group)).length > 1 || "*".equals(group)) {
                return doRefer(Cluster.getCluster(MergeableCluster.NAME), registry, type, url, qs);
            }
        }

        Cluster cluster = Cluster.getCluster(qs.get(CLUSTER_KEY));
=======
        Map<String, String> qs = (Map<String, String>) url.getAttribute(REFER_KEY);
        String group = qs.get(GROUP_KEY);
        if (StringUtils.isNotEmpty(group)) {
            if ((COMMA_SPLIT_PATTERN.split(group)).length > 1 || "*".equals(group)) {
                return doRefer(Cluster.getCluster(url.getScopeModel(), MergeableCluster.NAME), registry, type, url, qs);
            }
        }

        Cluster cluster = Cluster.getCluster(url.getScopeModel(), qs.get(CLUSTER_KEY));
>>>>>>> origin/3.2
        return doRefer(cluster, registry, type, url, qs);
    }

    protected <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url, Map<String, String> parameters) {
<<<<<<< HEAD
        URL consumerUrl = new URL(CONSUMER_PROTOCOL, parameters.remove(REGISTER_IP_KEY), 0, type.getName(), parameters);
=======
        Map<String, Object> consumerAttribute = new HashMap<>(url.getAttributes());
        consumerAttribute.remove(REFER_KEY);
        String p = isEmpty(parameters.get(PROTOCOL_KEY)) ? CONSUMER : parameters.get(PROTOCOL_KEY);
        URL consumerUrl = new ServiceConfigURL(
            p,
            null,
            null,
            parameters.get(REGISTER_IP_KEY),
            0, getPath(parameters, type),
            parameters,
            consumerAttribute
        );
        url = url.putAttribute(CONSUMER_URL_KEY, consumerUrl);
>>>>>>> origin/3.2
        ClusterInvoker<T> migrationInvoker = getMigrationInvoker(this, cluster, registry, type, url, consumerUrl);
        return interceptInvoker(migrationInvoker, url, consumerUrl);
    }

<<<<<<< HEAD
    protected <T> ClusterInvoker<T> getMigrationInvoker(RegistryProtocol registryProtocol, Cluster cluster, Registry registry,
                                                        Class<T> type, URL url, URL consumerUrl) {
        return new ServiceDiscoveryMigrationInvoker<T>(registryProtocol, cluster, registry, type, url, consumerUrl);
    }

=======
    private String getPath(Map<String, String> parameters, Class<?> type) {
        return !ProtocolUtils.isGeneric(parameters.get(GENERIC_KEY)) ? type.getName() : parameters.get(INTERFACE_KEY);
    }

    protected <T> ClusterInvoker<T> getMigrationInvoker(RegistryProtocol registryProtocol, Cluster cluster, Registry registry, Class<T> type, URL url, URL consumerUrl) {
        return new ServiceDiscoveryMigrationInvoker<T>(registryProtocol, cluster, registry, type, url, consumerUrl);
    }

    /**
     * This method tries to load all RegistryProtocolListener definitions, which are used to control the behaviour of invoker by interacting with defined, then uses those listeners to
     * change the status and behaviour of the MigrationInvoker.
     * <p>
     * Currently available Listener is MigrationRuleListener, one used to control the Migration behaviour with dynamically changing rules.
     *
     * @param invoker     MigrationInvoker that determines which type of invoker list to use
     * @param url         The original url generated during refer, more like a registry:// style url
     * @param consumerUrl Consumer url representing current interface and its config
     * @param <T>         The service definition
     * @return The @param MigrationInvoker passed in
     */
>>>>>>> origin/3.2
    protected <T> Invoker<T> interceptInvoker(ClusterInvoker<T> invoker, URL url, URL consumerUrl) {
        List<RegistryProtocolListener> listeners = findRegistryProtocolListeners(url);
        if (CollectionUtils.isEmpty(listeners)) {
            return invoker;
        }

        for (RegistryProtocolListener listener : listeners) {
<<<<<<< HEAD
            listener.onRefer(this, invoker, consumerUrl);
=======
            listener.onRefer(this, invoker, consumerUrl, url);
>>>>>>> origin/3.2
        }
        return invoker;
    }

    public <T> ClusterInvoker<T> getServiceDiscoveryInvoker(Cluster cluster, Registry registry, Class<T> type, URL url) {
        DynamicDirectory<T> directory = new ServiceDiscoveryRegistryDirectory<>(type, url);
        return doCreateInvoker(directory, cluster, registry, type);
    }

    public <T> ClusterInvoker<T> getInvoker(Cluster cluster, Registry registry, Class<T> type, URL url) {
        // FIXME, this method is currently not used, create the right registry before enable.
        DynamicDirectory<T> directory = new RegistryDirectory<>(type, url);
        return doCreateInvoker(directory, cluster, registry, type);
    }

    protected <T> ClusterInvoker<T> doCreateInvoker(DynamicDirectory<T> directory, Cluster cluster, Registry registry, Class<T> type) {
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        // all attributes of REFER_KEY
<<<<<<< HEAD
        Map<String, String> parameters = new HashMap<String, String>(directory.getConsumerUrl().getParameters());
        URL urlToRegistry = new URL(CONSUMER_PROTOCOL, parameters.remove(REGISTER_IP_KEY), 0, type.getName(), parameters);
=======
        Map<String, String> parameters = new HashMap<>(directory.getConsumerUrl().getParameters());
        URL urlToRegistry = new ServiceConfigURL(
            parameters.get(PROTOCOL_KEY) == null ? CONSUMER : parameters.get(PROTOCOL_KEY),
            parameters.remove(REGISTER_IP_KEY),
            0,
            getPath(parameters, type),
            parameters
        );
        urlToRegistry = urlToRegistry.setScopeModel(directory.getConsumerUrl().getScopeModel());
        urlToRegistry = urlToRegistry.setServiceModel(directory.getConsumerUrl().getServiceModel());
>>>>>>> origin/3.2
        if (directory.isShouldRegister()) {
            directory.setRegisteredConsumerUrl(urlToRegistry);
            registry.register(directory.getRegisteredConsumerUrl());
        }
        directory.buildRouterChain(urlToRegistry);
        directory.subscribe(toSubscribeUrl(urlToRegistry));

<<<<<<< HEAD
        return (ClusterInvoker<T>) cluster.join(directory);
=======
        return (ClusterInvoker<T>) cluster.join(directory, true);
>>>>>>> origin/3.2
    }

    public <T> void reRefer(ClusterInvoker<?> invoker, URL newSubscribeUrl) {
        if (!(invoker instanceof MigrationClusterInvoker)) {
<<<<<<< HEAD
            LOGGER.error("Only invoker type of MigrationClusterInvoker supports reRefer, current invoker is " + invoker.getClass());
=======
            logger.error(REGISTRY_UNSUPPORTED_CATEGORY, "", "", "Only invoker type of MigrationClusterInvoker supports reRefer, current invoker is " + invoker.getClass());
>>>>>>> origin/3.2
            return;
        }

        MigrationClusterInvoker<?> migrationClusterInvoker = (MigrationClusterInvoker<?>) invoker;
        migrationClusterInvoker.reRefer(newSubscribeUrl);
    }

    public static URL toSubscribeUrl(URL url) {
<<<<<<< HEAD
        return url.addParameter(CATEGORY_KEY, PROVIDERS_CATEGORY + "," + CONFIGURATORS_CATEGORY + "," + ROUTERS_CATEGORY);
    }

    protected List<RegistryProtocolListener> findRegistryProtocolListeners(URL url) {
        return ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class)
                .getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY);
=======
        return url.addParameter(CATEGORY_KEY, ALL_CATEGORIES);
    }

    protected List<RegistryProtocolListener> findRegistryProtocolListeners(URL url) {
        return ScopeModelUtil.getExtensionLoader(RegistryProtocolListener.class, url.getScopeModel())
            .getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY);
>>>>>>> origin/3.2
    }

    // available to test
    public String[] getParamsToRegistry(String[] defaultKeys, String[] additionalParameterKeys) {
        int additionalLen = additionalParameterKeys.length;
        String[] registryParams = new String[defaultKeys.length + additionalLen];
        System.arraycopy(defaultKeys, 0, registryParams, 0, defaultKeys.length);
        System.arraycopy(additionalParameterKeys, 0, registryParams, defaultKeys.length, additionalLen);
        return registryParams;
    }

    @Override
    public void destroy() {
<<<<<<< HEAD
        List<RegistryProtocolListener> listeners = ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class)
                .getLoadedExtensionInstances();
        if (CollectionUtils.isNotEmpty(listeners)) {
            for (RegistryProtocolListener listener : listeners) {
                listener.onDestroy();
            }
        }

        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
=======
        // FIXME all application models in framework are removed at this moment
        for (ApplicationModel applicationModel : frameworkModel.getApplicationModels()) {
            for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
                List<RegistryProtocolListener> listeners = moduleModel.getExtensionLoader(RegistryProtocolListener.class)
                    .getLoadedExtensionInstances();
                if (CollectionUtils.isNotEmpty(listeners)) {
                    for (RegistryProtocolListener listener : listeners) {
                        listener.onDestroy();
                    }
                }
            }
        }

        for (ApplicationModel applicationModel : frameworkModel.getApplicationModels()) {
            if (applicationModel.getModelEnvironment().getConfiguration().convert(Boolean.class, org.apache.dubbo.registry.Constants.ENABLE_CONFIGURATION_LISTEN, true)) {
                for (ModuleModel moduleModel : applicationModel.getPubModuleModels()) {
                    String applicationName = applicationModel.tryGetApplicationName();
                    if (applicationName == null) {
                        // already removed
                        continue;
                    }
                    if (moduleModel.getServiceRepository().getExportedServices().size() > 0) {
                        moduleModel.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension()
                            .removeListener(applicationName + CONFIGURATORS_SUFFIX,
                                getProviderConfigurationListener(moduleModel));
                    }
                }
            }
        }

        List<Exporter<?>> exporters = new ArrayList<>(bounds.values());
>>>>>>> origin/3.2
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();
<<<<<<< HEAD

        String application = ApplicationModel.tryGetApplication();
        if (application == null) {
            // already removed
            return;
        }
        ExtensionLoader.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension()
            .removeListener(application + CONFIGURATORS_SUFFIX, providerConfigurationListener);
=======
>>>>>>> origin/3.2
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }

<<<<<<< HEAD
    // merge the urls of configurators
    private static URL getConfiguredInvokerUrl(List<Configurator> configurators, URL url) {
        if (configurators != null && configurators.size() > 0) {
=======
    //Merge the urls of configurators
    private static URL getConfiguredInvokerUrl(List<Configurator> configurators, URL url) {
        if (CollectionUtils.isNotEmpty(configurators)) {
>>>>>>> origin/3.2
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
        }
        return url;
    }

    public static class InvokerDelegate<T> extends InvokerWrapper<T> {
<<<<<<< HEAD
        private final Invoker<T> invoker;
=======
>>>>>>> origin/3.2

        /**
         * @param invoker
         * @param url     invoker.getUrl return this value
         */
        public InvokerDelegate(Invoker<T> invoker, URL url) {
            super(invoker, url);
<<<<<<< HEAD
            this.invoker = invoker;
=======
>>>>>>> origin/3.2
        }

        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegate) {
                return ((InvokerDelegate<T>) invoker).getInvoker();
            } else {
                return invoker;
            }
        }
    }

    private static class DestroyableExporter<T> implements Exporter<T> {

        private Exporter<T> exporter;

        public DestroyableExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        @Override
        public void unexport() {
            exporter.unexport();
        }
    }

    /**
     * Reexport: the exporter destroy problem in protocol
<<<<<<< HEAD
     * 1.Ensure that the exporter returned by registryprotocol can be normal destroyed
=======
     * 1.Ensure that the exporter returned by registry protocol can be normal destroyed
>>>>>>> origin/3.2
     * 2.No need to re-register to the registry after notify
     * 3.The invoker passed by the export method , would better to be the invoker of exporter
     */
    private class OverrideListener implements NotifyListener {
        private final URL subscribeUrl;
        private final Invoker originInvoker;

<<<<<<< HEAD

=======
>>>>>>> origin/3.2
        private List<Configurator> configurators;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }

        /**
         * @param urls The list of registered information, is always not empty, The meaning is the same as the
         *             return value of {@link org.apache.dubbo.registry.RegistryService#lookup(URL)}.
         */
        @Override
        public synchronized void notify(List<URL> urls) {
<<<<<<< HEAD
            LOGGER.debug("original override urls: " + urls);

            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl.addParameter(CATEGORY_KEY,
                    CONFIGURATORS_CATEGORY));
            LOGGER.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
=======
            if (logger.isDebugEnabled()) {
                logger.debug("original override urls: " + urls);
            }

            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl);
            if (logger.isDebugEnabled()) {
                logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
            }
>>>>>>> origin/3.2

            // No matching results
            if (matchedUrls.isEmpty()) {
                return;
            }

            this.configurators = Configurator.toConfigurators(classifyUrls(matchedUrls, UrlUtils::isConfigurator))
<<<<<<< HEAD
                    .orElse(configurators);

            doOverrideIfNecessary();
=======
                .orElse(configurators);

            ApplicationDeployer deployer = subscribeUrl.getOrDefaultApplicationModel().getDeployer();

            try {
                deployer.increaseServiceRefreshCount();
                doOverrideIfNecessary();
            } finally {
                deployer.decreaseServiceRefreshCount();
            }
>>>>>>> origin/3.2
        }

        public synchronized void doOverrideIfNecessary() {
            final Invoker<?> invoker;
            if (originInvoker instanceof InvokerDelegate) {
                invoker = ((InvokerDelegate<?>) originInvoker).getInvoker();
            } else {
                invoker = originInvoker;
            }
            //The origin invoker
            URL originUrl = RegistryProtocol.this.getProviderUrl(invoker);
            String key = getCacheKey(originInvoker);
            ExporterChangeableWrapper<?> exporter = bounds.get(key);
            if (exporter == null) {
<<<<<<< HEAD
                LOGGER.warn(new IllegalStateException("error state, exporter should not be null"));
=======
                logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", "error state, exporter should not be null", new IllegalStateException("error state, exporter should not be null"));
>>>>>>> origin/3.2
                return;
            }
            //The current, may have been merged many times
            Invoker<?> exporterInvoker = exporter.getInvoker();
            URL currentUrl = exporterInvoker == null ? null : exporterInvoker.getUrl();
            //Merged with this configuration
            URL newUrl = getConfiguredInvokerUrl(configurators, originUrl);
<<<<<<< HEAD
            newUrl = getConfiguredInvokerUrl(providerConfigurationListener.getConfigurators(), newUrl);
            newUrl = getConfiguredInvokerUrl(serviceConfigurationListeners.get(originUrl.getServiceKey())
                    .getConfigurators(), newUrl);
            if (!newUrl.equals(currentUrl)) {
                if(newUrl.getParameter(Constants.NEED_REEXPORT, true)) {
                    RegistryProtocol.this.reExport(originInvoker, newUrl);
                }
                LOGGER.info("exported provider url changed, origin url: " + originUrl +
                        ", old export url: " + currentUrl + ", new export url: " + newUrl);
=======
            newUrl = getConfiguredInvokerUrl(getProviderConfigurationListener(originUrl).getConfigurators(), newUrl);
            newUrl = getConfiguredInvokerUrl(serviceConfigurationListeners.get(originUrl.getServiceKey())
                .getConfigurators(), newUrl);
            if (!newUrl.equals(currentUrl)) {
                if (newUrl.getParameter(Constants.NEED_REEXPORT, true)) {
                    RegistryProtocol.this.reExport(originInvoker, newUrl);
                }
                logger.info("exported provider url changed, origin url: " + originUrl +
                    ", old export url: " + currentUrl + ", new export url: " + newUrl);
>>>>>>> origin/3.2
            }
        }

        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
<<<<<<< HEAD
            List<URL> result = new ArrayList<URL>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                // Compatible with the old version
                if (url.getParameter(CATEGORY_KEY) == null && OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
=======
            List<URL> result = new ArrayList<>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                // Compatible with the old version
                if (url.getCategory() == null && OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
>>>>>>> origin/3.2
                    overrideUrl = url.addParameter(CATEGORY_KEY, CONFIGURATORS_CATEGORY);
                }

                // Check whether url is to be applied to the current service
                if (UrlUtils.isMatch(currentSubscribe, overrideUrl)) {
                    result.add(url);
                }
            }
            return result;
        }
    }

<<<<<<< HEAD
=======
    private ProviderConfigurationListener getProviderConfigurationListener(URL url) {
        return getProviderConfigurationListener(url.getOrDefaultModuleModel());
    }

    private ProviderConfigurationListener getProviderConfigurationListener(ModuleModel moduleModel) {
        return moduleModel.getBeanFactory().getOrRegisterBean(ProviderConfigurationListener.class,
            type -> new ProviderConfigurationListener(moduleModel));
    }

>>>>>>> origin/3.2
    private class ServiceConfigurationListener extends AbstractConfiguratorListener {
        private URL providerUrl;
        private OverrideListener notifyListener;

<<<<<<< HEAD
        public ServiceConfigurationListener(URL providerUrl, OverrideListener notifyListener) {
            this.providerUrl = providerUrl;
            this.notifyListener = notifyListener;
            this.initWith(DynamicConfiguration.getRuleKey(providerUrl) + CONFIGURATORS_SUFFIX);
=======
        private final ModuleModel moduleModel;

        public ServiceConfigurationListener(ModuleModel moduleModel, URL providerUrl, OverrideListener notifyListener) {
            super(moduleModel);
            this.providerUrl = providerUrl;
            this.notifyListener = notifyListener;
            this.moduleModel = moduleModel;
            if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true)) {
                this.initWith(DynamicConfiguration.getRuleKey(providerUrl) + CONFIGURATORS_SUFFIX);
            }
>>>>>>> origin/3.2
        }

        private <T> URL overrideUrl(URL providerUrl) {
            return RegistryProtocol.getConfiguredInvokerUrl(configurators, providerUrl);
        }

        @Override
        protected void notifyOverrides() {
<<<<<<< HEAD
            notifyListener.doOverrideIfNecessary();
=======
            ApplicationDeployer deployer = this.moduleModel.getApplicationModel().getDeployer();
            try {
                deployer.increaseServiceRefreshCount();
                notifyListener.doOverrideIfNecessary();
            } finally {
                deployer.decreaseServiceRefreshCount();
            }
>>>>>>> origin/3.2
        }
    }

    private class ProviderConfigurationListener extends AbstractConfiguratorListener {

<<<<<<< HEAD
        public ProviderConfigurationListener() {
            this.initWith(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
=======
        private final Map<URL, Set<NotifyListener>> overrideListeners = new ConcurrentHashMap<>();

        private final ModuleModel moduleModel;

        public ProviderConfigurationListener(ModuleModel moduleModel) {
            super(moduleModel);
            this.moduleModel = moduleModel;
            if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true)) {
                this.initWith(moduleModel.getApplicationModel().getApplicationName() + CONFIGURATORS_SUFFIX);
            }
>>>>>>> origin/3.2
        }

        /**
         * Get existing configuration rule and override provider url before exporting.
         *
         * @param providerUrl
         * @param <T>
         * @return
         */
        private <T> URL overrideUrl(URL providerUrl) {
            return RegistryProtocol.getConfiguredInvokerUrl(configurators, providerUrl);
        }

        @Override
        protected void notifyOverrides() {
<<<<<<< HEAD
            overrideListeners.values().forEach(listener -> ((OverrideListener) listener).doOverrideIfNecessary());
=======
            ApplicationDeployer deployer = this.moduleModel.getApplicationModel().getDeployer();
            try {
                deployer.increaseServiceRefreshCount();
                overrideListeners.values().forEach(listeners -> {
                    for (NotifyListener listener : listeners) {
                        ((OverrideListener) listener).doOverrideIfNecessary();
                    }
                });
            } finally {
                deployer.decreaseServiceRefreshCount();
            }
        }

        public Map<URL, Set<NotifyListener>> getOverrideListeners() {
            return overrideListeners;
>>>>>>> origin/3.2
        }
    }

    /**
     * exporter proxy, establish the corresponding relationship between the returned exporter and the exporter
     * exported by the protocol, and can modify the relationship at the time of override.
     *
     * @param <T>
     */
    private class ExporterChangeableWrapper<T> implements Exporter<T> {

<<<<<<< HEAD
        private final ExecutorService executor = newSingleThreadExecutor(new NamedThreadFactory("Exporter-Unexport", true));
=======
        private final ScheduledExecutorService executor;
>>>>>>> origin/3.2

        private final Invoker<T> originInvoker;
        private Exporter<T> exporter;
        private URL subscribeUrl;
        private URL registerUrl;

<<<<<<< HEAD
        private AtomicBoolean unexported = new AtomicBoolean(false);
=======
        private NotifyListener notifyListener;
>>>>>>> origin/3.2

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
<<<<<<< HEAD
=======
            FrameworkExecutorRepository frameworkExecutorRepository = originInvoker.getUrl().getOrDefaultFrameworkModel().getBeanFactory()
                .getBean(FrameworkExecutorRepository.class);
            this.executor = frameworkExecutorRepository.getSharedScheduledExecutor();
>>>>>>> origin/3.2
        }

        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        public void setExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        @Override
        public void unexport() {
<<<<<<< HEAD
            if (!unexported.compareAndSet(false,true)) {
                return;
            }

            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);

            Registry registry = RegistryProtocol.this.getRegistry(originInvoker);
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                LOGGER.warn(t.getMessage(), t);
            }
            try {
                NotifyListener listener = RegistryProtocol.this.overrideListeners.remove(subscribeUrl);
                registry.unsubscribe(subscribeUrl, listener);
                ExtensionLoader.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension()
                        .removeListener(subscribeUrl.getServiceKey() + CONFIGURATORS_SUFFIX,
                                serviceConfigurationListeners.get(subscribeUrl.getServiceKey()));
            } catch (Throwable t) {
                LOGGER.warn(t.getMessage(), t);
            }

            executor.submit(() -> {
                try {
                    int timeout = ConfigurationUtils.getServerShutdownTimeout();
                    if (timeout > 0) {
                        LOGGER.info("Waiting " + timeout + "ms for registry to notify all consumers before unexport. " +
                                "Usually, this is called when you use dubbo API");
                        Thread.sleep(timeout);
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    LOGGER.warn(t.getMessage(), t);
                }
            });
=======
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);

            Registry registry = RegistryProtocol.this.getRegistry(getRegistryUrl(originInvoker));
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", t.getMessage(), t);
            }
            try {
                if (subscribeUrl != null) {
                    Map<URL, Set<NotifyListener>> overrideListeners = getProviderConfigurationListener(subscribeUrl).getOverrideListeners();
                    Set<NotifyListener> listeners = overrideListeners.get(subscribeUrl);
                    if(listeners != null){
                        if (listeners.remove(notifyListener)) {
                            if (!registry.isServiceDiscovery()) {
                                registry.unsubscribe(subscribeUrl, notifyListener);
                            }
                            ApplicationModel applicationModel = getApplicationModel(registerUrl.getScopeModel());
                            if (applicationModel.getModelEnvironment().getConfiguration().convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true)) {
                                for (ModuleModel moduleModel : applicationModel.getPubModuleModels()) {
                                    if (moduleModel.getServiceRepository().getExportedServices().size() > 0) {
                                        moduleModel.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension()
                                            .removeListener(subscribeUrl.getServiceKey() + CONFIGURATORS_SUFFIX,
                                                serviceConfigurationListeners.remove(subscribeUrl.getServiceKey()));
                                    }
                                }
                            }
                        }
                        if (listeners.isEmpty()) {
                            overrideListeners.remove(subscribeUrl);
                        }
                    }
                }
            } catch (Throwable t) {
                logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", t.getMessage(), t);
            }

            //TODO wait for shutdown timeout is a bit strange
            int timeout = DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
            if (subscribeUrl != null) {
                timeout = ConfigurationUtils.getServerShutdownTimeout(subscribeUrl.getScopeModel());
            }
            executor.schedule(() -> {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", t.getMessage(), t);
                }
            }, timeout, TimeUnit.MILLISECONDS);
>>>>>>> origin/3.2
        }

        public void setSubscribeUrl(URL subscribeUrl) {
            this.subscribeUrl = subscribeUrl;
        }

        public void setRegisterUrl(URL registerUrl) {
            this.registerUrl = registerUrl;
        }

<<<<<<< HEAD
=======
        public void setNotifyListener(NotifyListener notifyListener) {
            this.notifyListener = notifyListener;
        }

>>>>>>> origin/3.2
        public URL getRegisterUrl() {
            return registerUrl;
        }
    }

<<<<<<< HEAD
    // for unit test
    private static RegistryProtocol INSTANCE;

    // for unit test
    public RegistryProtocol() {
        INSTANCE = this;
    }

    // for unit test
    public static RegistryProtocol getRegistryProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(REGISTRY_PROTOCOL); // load
        }
        return INSTANCE;
    }
=======
>>>>>>> origin/3.2
}
