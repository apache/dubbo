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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.registry.client.migration.MigrationClusterInvoker;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.EXTRA_KEYS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_PROTOCOL_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.RegistryConstants.ALL_CATEGORIES;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.OVERRIDE_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.utils.UrlUtils.classifyUrls;
import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;
import static org.apache.dubbo.registry.Constants.DEFAULT_REGISTRY_RETRY_PERIOD;
import static org.apache.dubbo.registry.Constants.ENABLE_CONFIGURATION_LISTEN;
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
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.rpc.Constants.DEPRECATED_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.INTERFACES;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CONSUMER_URL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WARMUP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;
import static org.apache.dubbo.rpc.model.ScopeModelUtil.getApplicationModel;

/**
 * TODO, replace RegistryProtocol completely in the future.
 */
public class RegistryProtocol implements Protocol, ScopeModelAware {
    public static final String[] DEFAULT_REGISTER_PROVIDER_KEYS = {
        APPLICATION_KEY, CODEC_KEY, EXCHANGER_KEY, SERIALIZATION_KEY, CLUSTER_KEY, CONNECTIONS_KEY, DEPRECATED_KEY,
        GROUP_KEY, LOADBALANCE_KEY, MOCK_KEY, PATH_KEY, TIMEOUT_KEY, TOKEN_KEY, VERSION_KEY, WARMUP_KEY,
        WEIGHT_KEY, DUBBO_VERSION_KEY, RELEASE_KEY
    };

    public static final String[] DEFAULT_REGISTER_CONSUMER_KEYS = {
        APPLICATION_KEY, VERSION_KEY, GROUP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY
    };

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);
    private final Map<String, ServiceConfigurationListener> serviceConfigurationListeners = new ConcurrentHashMap<>();
    //To solve the problem of RMI repeated exposure port conflicts, the services that have been exposed are no longer exposed.
    //provider url <--> exporter
    private final ConcurrentMap<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<>();
    protected Protocol protocol;
    protected ProxyFactory proxyFactory;
    //protected RegistryFactory registryFactory;

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
        } else {
            return new String[0];
        }
    }

    public RegistryProtocol() {
    }

    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    // Cannot inject registryFactory (application scope) into protocol (framework scope)
//    public void setRegistryFactory(RegistryFactory registryFactory) {
//        this.registryFactory = registryFactory;
//    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public int getDefaultPort() {
        return 9090;
    }

    public Map<URL, NotifyListener> getOverrideListeners() {
        Map<URL, NotifyListener> map = new HashMap<>();
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
        registry.register(registeredProviderUrl);
    }

    private void registerStatedUrl(URL registryUrl, URL registeredProviderUrl, boolean registered) {
        ProviderModel model = (ProviderModel) registeredProviderUrl.getServiceModel();
        model.addStatedUrl(new ProviderModel.RegisterStatedURL(
            registeredProviderUrl,
            registryUrl,
            registered));
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
        Map<URL, NotifyListener> overrideListeners = getProviderConfigurationListener(providerUrl).getOverrideListeners();
        overrideListeners.put(registryUrl, overrideSubscribeListener);

        providerUrl = overrideUrlWithConfig(providerUrl, overrideSubscribeListener);
        //export invoker
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker, providerUrl);

        // url to registry
        final Registry registry = getRegistry(registryUrl);
        final URL registeredProviderUrl = getUrlToRegistry(providerUrl, registryUrl);

        // decide if we need to delay publish
        boolean register = providerUrl.getParameter(REGISTER_KEY, true);
        if (register) {
            register(registry, registeredProviderUrl);
        }

        // register stated url on provider model
        registerStatedUrl(registryUrl, registeredProviderUrl, register);


        exporter.setRegisterUrl(registeredProviderUrl);
        exporter.setSubscribeUrl(overrideSubscribeUrl);

        if (!registry.isServiceDiscovery()) {
            // Deprecated! Subscribe to override rules in 2.6.x or before.
            registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);
        }

        notifyExport(exporter);
        //Ensure that a new exporter instance is returned every time export
        return new DestroyableExporter<>(exporter);
    }

    private <T> void notifyExport(ExporterChangeableWrapper<T> exporter) {
        ScopeModel scopeModel = exporter.getRegisterUrl().getScopeModel();
        List<RegistryProtocolListener> listeners = ScopeModelUtil.getExtensionLoader(RegistryProtocolListener.class, scopeModel)
            .getActivateExtension(exporter.getOriginInvoker().getUrl(), REGISTRY_PROTOCOL_LISTENER_KEY);
        if (CollectionUtils.isNotEmpty(listeners)) {
            for (RegistryProtocolListener listener : listeners) {
                listener.onExport(this, exporter);
            }
        }
    }

    private URL overrideUrlWithConfig(URL providerUrl, OverrideListener listener) {
        ProviderConfigurationListener providerConfigurationListener = getProviderConfigurationListener(providerUrl);
        providerUrl = providerConfigurationListener.overrideUrl(providerUrl);

        ServiceConfigurationListener serviceConfigurationListener = new ServiceConfigurationListener(providerUrl.getOrDefaultModuleModel(), providerUrl, listener);
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
                    () -> doReExport(originInvoker, exporter, registryUrl, registeredUrl, newProviderUrl),
                    registeredUrl,
                    null
                );
                oldTask = reExportFailedTasks.putIfAbsent(registeredUrl, task);
                if (oldTask == null) {
                    // never has a retry task. then start a new task for retry.
                    retryTimer.newTimeout(task, registryUrl.getParameter(REGISTRY_RETRY_PERIOD_KEY, DEFAULT_REGISTRY_RETRY_PERIOD), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private <T> void doReExport(final Invoker<T> originInvoker, ExporterChangeableWrapper<T> exporter,
                                URL registryUrl, URL oldProviderUrl, URL newProviderUrl) {
        if (getProviderUrl(originInvoker).getParameter(REGISTER_KEY, true)) {
            Registry registry;
            try {
                registry = getRegistry(getRegistryUrl(originInvoker));
            } catch (Exception e) {
                throw new SkipFailbackWrapperException(e);
            }

            logger.info("Try to unregister old url: " + oldProviderUrl);
            registry.reExportUnregister(oldProviderUrl);

            logger.info("Try to register new url: " + newProviderUrl);
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
        ProviderModel providerModel = frameworkModel.getServiceRepository()
            .lookupExportedService(providerUrl.getServiceKey());

        List<ProviderModel.RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
        return statedUrls.stream()
            .filter(u -> u.getRegistryUrl().equals(registryUrl)
                && u.getProviderUrl().getProtocol().equals(providerUrl.getProtocol()))
            .findFirst().orElseThrow(() -> new IllegalStateException("There should have at least one registered url."));
    }

    /**
     * Get an instance of registry based on the address of invoker
     *
     * @param registryUrl
     * @return
     */
    protected Registry getRegistry(final URL registryUrl) {
        RegistryFactory registryFactory = ScopeModelUtil.getExtensionLoader(RegistryFactory.class, registryUrl.getScopeModel()).getAdaptiveExtension();
        return registryFactory.getRegistry(registryUrl);
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
        //The address you see at the registry
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return providerUrl.removeParameters(getFilteredKeys(providerUrl)).removeParameters(
                MONITOR_KEY, BIND_IP_KEY, BIND_PORT_KEY, QOS_ENABLE, QOS_HOST, QOS_PORT, ACCEPT_FOREIGN_IP, VALIDATION_KEY,
                INTERFACES);
        } else {
            String extraKeys = registryUrl.getParameter(EXTRA_KEYS_KEY, "");
            // if path is not the same as interface name then we should keep INTERFACE_KEY,
            // otherwise, the registry structure of zookeeper would be '/dubbo/path/providers',
            // but what we expect is '/dubbo/interface/providers'
            if (!providerUrl.getPath().equals(providerUrl.getParameter(INTERFACE_KEY))) {
                if (StringUtils.isNotEmpty(extraKeys)) {
                    extraKeys += ",";
                }
                extraKeys += INTERFACE_KEY;
            }
            String[] paramsToRegistry = getParamsToRegistry(DEFAULT_REGISTER_PROVIDER_KEYS
                , COMMA_SPLIT_PATTERN.split(extraKeys));
            return URL.valueOf(providerUrl, paramsToRegistry, providerUrl.getParameter(METHODS_KEY, (String[]) null));
        }

    }

    private URL getSubscribedOverrideUrl(URL registeredProviderUrl) {
        return registeredProviderUrl.setProtocol(PROVIDER_PROTOCOL)
            .addParameters(CATEGORY_KEY, CONFIGURATORS_CATEGORY, CHECK_KEY, String.valueOf(false));
    }

    /**
     * Get the address of the providerUrl through the url of the invoker
     *
     * @param originInvoker
     * @return
     */
    private URL getProviderUrl(final Invoker<?> originInvoker) {
        Object providerURL = originInvoker.getUrl().getAttribute(EXPORT_KEY);
        if (!(providerURL instanceof URL)) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + originInvoker.getUrl().getAddress());
        }
        return (URL) providerURL;
    }

    /**
     * Get the key cached in bounds by invoker
     *
     * @param originInvoker
     * @return
     */
    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters(DYNAMIC_KEY, ENABLED_KEY).toFullString();
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
        Map<String, String> qs = (Map<String, String>) url.getAttribute(REFER_KEY);
        String group = qs.get(GROUP_KEY);
        if (StringUtils.isNotEmpty(group)) {
            if ((COMMA_SPLIT_PATTERN.split(group)).length > 1 || "*".equals(group)) {
                return doRefer(Cluster.getCluster(url.getScopeModel(), MergeableCluster.NAME), registry, type, url, qs);
            }
        }

        Cluster cluster = Cluster.getCluster(url.getScopeModel(), qs.get(CLUSTER_KEY));
        return doRefer(cluster, registry, type, url, qs);
    }

    protected <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url, Map<String, String> parameters) {
        Map<String, Object> consumerAttribute = new HashMap<>(url.getAttributes());
        consumerAttribute.remove(REFER_KEY);
        URL consumerUrl = new ServiceConfigURL(parameters.get(PROTOCOL_KEY) == null ? DUBBO : parameters.get(PROTOCOL_KEY),
            null,
            null,
            parameters.get(REGISTER_IP_KEY),
            0, getPath(parameters, type),
            parameters,
            consumerAttribute);
        url = url.putAttribute(CONSUMER_URL_KEY, consumerUrl);
        ClusterInvoker<T> migrationInvoker = getMigrationInvoker(this, cluster, registry, type, url, consumerUrl);
        return interceptInvoker(migrationInvoker, url, consumerUrl, url);
    }

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
     * @param registryURL The actual registry url, zookeeper:// for example
     * @param <T>         The service definition
     * @return The @param MigrationInvoker passed in
     */
    protected <T> Invoker<T> interceptInvoker(ClusterInvoker<T> invoker, URL url, URL consumerUrl, URL registryURL) {
        List<RegistryProtocolListener> listeners = findRegistryProtocolListeners(url);
        if (CollectionUtils.isEmpty(listeners)) {
            return invoker;
        }

        for (RegistryProtocolListener listener : listeners) {
            listener.onRefer(this, invoker, consumerUrl, registryURL);
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
        Map<String, String> parameters = new HashMap<>(directory.getConsumerUrl().getParameters());
        URL urlToRegistry = new ServiceConfigURL(
            parameters.get(PROTOCOL_KEY) == null ? DUBBO : parameters.get(PROTOCOL_KEY),
            parameters.remove(REGISTER_IP_KEY), 0, getPath(parameters, type), parameters);
        urlToRegistry = urlToRegistry.setScopeModel(directory.getConsumerUrl().getScopeModel());
        urlToRegistry = urlToRegistry.setServiceModel(directory.getConsumerUrl().getServiceModel());
        if (directory.isShouldRegister()) {
            directory.setRegisteredConsumerUrl(urlToRegistry);
            registry.register(directory.getRegisteredConsumerUrl());
        }
        directory.buildRouterChain(urlToRegistry);
        directory.subscribe(toSubscribeUrl(urlToRegistry));

        return (ClusterInvoker<T>) cluster.join(directory, true);
    }

    public <T> void reRefer(ClusterInvoker<?> invoker, URL newSubscribeUrl) {
        if (!(invoker instanceof MigrationClusterInvoker)) {
            logger.error("Only invoker type of MigrationClusterInvoker supports reRefer, current invoker is " + invoker.getClass());
            return;
        }

        MigrationClusterInvoker<?> migrationClusterInvoker = (MigrationClusterInvoker<?>) invoker;
        migrationClusterInvoker.reRefer(newSubscribeUrl);
    }

    public static URL toSubscribeUrl(URL url) {
        return url.addParameter(CATEGORY_KEY, ALL_CATEGORIES);
    }

    protected List<RegistryProtocolListener> findRegistryProtocolListeners(URL url) {
        return ScopeModelUtil.getExtensionLoader(RegistryProtocolListener.class, url.getScopeModel())
            .getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY);
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
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }

    //Merge the urls of configurators
    private static URL getConfiguredInvokerUrl(List<Configurator> configurators, URL url) {
        if (CollectionUtils.isNotEmpty(configurators)) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
        }
        return url;
    }

    public static class InvokerDelegate<T> extends InvokerWrapper<T> {

        /**
         * @param invoker
         * @param url     invoker.getUrl return this value
         */
        public InvokerDelegate(Invoker<T> invoker, URL url) {
            super(invoker, url);
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
     * 1.Ensure that the exporter returned by registry protocol can be normal destroyed
     * 2.No need to re-register to the registry after notify
     * 3.The invoker passed by the export method , would better to be the invoker of exporter
     */
    private class OverrideListener implements NotifyListener {
        private final URL subscribeUrl;
        private final Invoker originInvoker;

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
            if (logger.isDebugEnabled()) {
                logger.debug("original override urls: " + urls);
            }

            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl.addParameter(CATEGORY_KEY,
                CONFIGURATORS_CATEGORY));
            if (logger.isDebugEnabled()) {
                logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
            }

            // No matching results
            if (matchedUrls.isEmpty()) {
                return;
            }

            this.configurators = Configurator.toConfigurators(classifyUrls(matchedUrls, UrlUtils::isConfigurator))
                .orElse(configurators);

            doOverrideIfNecessary();
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
                logger.warn(new IllegalStateException("error state, exporter should not be null"));
                return;
            }
            //The current, may have been merged many times
            Invoker<?> exporterInvoker = exporter.getInvoker();
            URL currentUrl = exporterInvoker == null ? null : exporterInvoker.getUrl();
            //Merged with this configuration
            URL newUrl = getConfiguredInvokerUrl(configurators, originUrl);
            newUrl = getConfiguredInvokerUrl(getProviderConfigurationListener(originUrl).getConfigurators(), newUrl);
            newUrl = getConfiguredInvokerUrl(serviceConfigurationListeners.get(originUrl.getServiceKey())
                .getConfigurators(), newUrl);
            if (!newUrl.equals(currentUrl)) {
                if (newUrl.getParameter(Constants.NEED_REEXPORT, true)) {
                    RegistryProtocol.this.reExport(originInvoker, newUrl);
                }
                logger.info("exported provider url changed, origin url: " + originUrl +
                    ", old export url: " + currentUrl + ", new export url: " + newUrl);
            }
        }

        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
            List<URL> result = new ArrayList<>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                // Compatible with the old version
                if (url.getCategory() == null && OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
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

    private ProviderConfigurationListener getProviderConfigurationListener(URL url) {
        return getProviderConfigurationListener(url.getOrDefaultModuleModel());
    }

    private ProviderConfigurationListener getProviderConfigurationListener(ModuleModel moduleModel) {
        return moduleModel.getBeanFactory().getOrRegisterBean(ProviderConfigurationListener.class,
            type -> new ProviderConfigurationListener(moduleModel));
    }

    private class ServiceConfigurationListener extends AbstractConfiguratorListener {
        private URL providerUrl;
        private OverrideListener notifyListener;

        public ServiceConfigurationListener(ModuleModel moduleModel, URL providerUrl, OverrideListener notifyListener) {
            super(moduleModel);
            this.providerUrl = providerUrl;
            this.notifyListener = notifyListener;
            if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true)) {
                this.initWith(DynamicConfiguration.getRuleKey(providerUrl) + CONFIGURATORS_SUFFIX);
            }
        }

        private <T> URL overrideUrl(URL providerUrl) {
            return RegistryProtocol.getConfiguredInvokerUrl(configurators, providerUrl);
        }

        @Override
        protected void notifyOverrides() {
            notifyListener.doOverrideIfNecessary();
        }
    }

    private class ProviderConfigurationListener extends AbstractConfiguratorListener {

        private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<>();

        public ProviderConfigurationListener(ModuleModel moduleModel) {
            super(moduleModel);
            if (moduleModel.getModelEnvironment().getConfiguration().convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true)) {
                this.initWith(moduleModel.getApplicationModel().getApplicationName() + CONFIGURATORS_SUFFIX);
            }
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
            overrideListeners.values().forEach(listener -> ((OverrideListener) listener).doOverrideIfNecessary());
        }

        public Map<URL, NotifyListener> getOverrideListeners() {
            return overrideListeners;
        }
    }

    /**
     * exporter proxy, establish the corresponding relationship between the returned exporter and the exporter
     * exported by the protocol, and can modify the relationship at the time of override.
     *
     * @param <T>
     */
    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        private final ScheduledExecutorService executor;

        private final Invoker<T> originInvoker;
        private Exporter<T> exporter;
        private URL subscribeUrl;
        private URL registerUrl;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
            ExecutorRepository executorRepository = originInvoker.getUrl().getOrDefaultApplicationModel()
                .getDefaultExtension(ExecutorRepository.class);
            this.executor = executorRepository.getSharedScheduledExecutor();
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
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);

            Registry registry = RegistryProtocol.this.getRegistry(getRegistryUrl(originInvoker));
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
            try {
                if (subscribeUrl != null) {
                    Map<URL, NotifyListener> overrideListeners = getProviderConfigurationListener(subscribeUrl).getOverrideListeners();
                    NotifyListener listener = overrideListeners.remove(registerUrl);
                    if (listener != null) {
                        if (!registry.isServiceDiscovery()) {
                            registry.unsubscribe(subscribeUrl, listener);
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
                }
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }

            //TODO wait for shutdown timeout is a bit strange
            int timeout = ConfigurationUtils.getServerShutdownTimeout(subscribeUrl.getScopeModel());
            executor.schedule(() -> {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }, timeout, TimeUnit.MILLISECONDS);
        }

        public void setSubscribeUrl(URL subscribeUrl) {
            this.subscribeUrl = subscribeUrl;
        }

        public void setRegisterUrl(URL registerUrl) {
            this.registerUrl = registerUrl;
        }

        public URL getRegisterUrl() {
            return registerUrl;
        }
    }

}
