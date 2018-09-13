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

import org.apache.commons.lang.ArrayUtils;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.dynamic.ConfigChangeEvent;
import org.apache.dubbo.config.dynamic.ConfigChangeType;
import org.apache.dubbo.config.dynamic.ConfigurationListener;
import org.apache.dubbo.config.dynamic.DynamicConfiguration;
import org.apache.dubbo.config.dynamic.DynamicConfigurationFactory;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.integration.parser.ConfigParser;
import org.apache.dubbo.registry.support.ProviderConsumerRegTable;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.Constants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.Constants.ADD_PARAM_KEYS_KEY;
import static org.apache.dubbo.common.Constants.APPLICATION_KEY;
import static org.apache.dubbo.common.Constants.CLUSTER_KEY;
import static org.apache.dubbo.common.Constants.CODEC_KEY;
import static org.apache.dubbo.common.Constants.CONFIGURATORS_SUFFIX;
import static org.apache.dubbo.common.Constants.CONFIG_PROTOCOL;
import static org.apache.dubbo.common.Constants.CONNECTIONS_KEY;
import static org.apache.dubbo.common.Constants.DEPRECATED_KEY;
import static org.apache.dubbo.common.Constants.EXCHANGER_KEY;
import static org.apache.dubbo.common.Constants.EXPORT_KEY;
import static org.apache.dubbo.common.Constants.GROUP_KEY;
import static org.apache.dubbo.common.Constants.INTERFACES;
import static org.apache.dubbo.common.Constants.INTERFACE_KEY;
import static org.apache.dubbo.common.Constants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.Constants.METHODS_KEY;
import static org.apache.dubbo.common.Constants.MOCK_KEY;
import static org.apache.dubbo.common.Constants.PATH_KEY;
import static org.apache.dubbo.common.Constants.QOS_ENABLE;
import static org.apache.dubbo.common.Constants.QOS_PORT;
import static org.apache.dubbo.common.Constants.REFER_KEY;
import static org.apache.dubbo.common.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.common.Constants.TIMEOUT_KEY;
import static org.apache.dubbo.common.Constants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.Constants.TOKEN_KEY;
import static org.apache.dubbo.common.Constants.VALIDATION_KEY;
import static org.apache.dubbo.common.Constants.VERSION_KEY;
import static org.apache.dubbo.common.Constants.WARMUP_KEY;
import static org.apache.dubbo.common.Constants.WEIGHT_KEY;

/**
 * RegistryProtocol
 */
public class RegistryProtocol implements Protocol {

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);
    private static RegistryProtocol INSTANCE;
    private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<URL, NotifyListener>();
    //To solve the problem of RMI repeated exposure port conflicts, the services that have been exposed are no longer exposed.
    //providerurl <--> exporter
    private final Map<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<String, ExporterChangeableWrapper<?>>();
    private Cluster cluster;
    private Protocol protocol;
    private RegistryFactory registryFactory;
    private ProxyFactory proxyFactory;
    private DynamicConfiguration dynamicConfiguration;

    public RegistryProtocol() {
        INSTANCE = this;
    }

    public static RegistryProtocol getRegistryProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(Constants.REGISTRY_PROTOCOL); // load
        }
        return INSTANCE;
    }

    //Filter the parameters that do not need to be output in url(Starting with .)
    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (params != null && !params.isEmpty()) {
            List<String> filteredKeys = new ArrayList<String>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getKey().startsWith(Constants.HIDE_KEY_PREFIX)) {
                    filteredKeys.add(entry.getKey());
                }
            }
            return filteredKeys.toArray(new String[filteredKeys.size()]);
        } else {
            return new String[]{};
        }
    }

    public void initDynamicConfiguration(URL url) {
        if (dynamicConfiguration == null) {
            dynamicConfiguration = ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class).getAdaptiveExtension().getDynamicConfiguration(getConfigUrl(url));
        }
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public int getDefaultPort() {
        return 9090;
    }

    public Map<URL, NotifyListener> getOverrideListeners() {
        return overrideListeners;
    }

    public void register(URL registryUrl, URL registedProviderUrl) {
        Registry registry = registryFactory.getRegistry(registryUrl);
        registry.register(registedProviderUrl);
    }

    @Override
    public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {
        URL registryUrl = getRegistryUrl(originInvoker);
        initDynamicConfiguration(registryUrl);

        // url to export locally
        URL providerUrl = getProviderUrl(originInvoker);
        providerUrl = overrideUrlWithConfig(providerUrl);

        //export invoker
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker, providerUrl);

        // url to registry
        final Registry registry = getRegistry(originInvoker);
        final URL registeredProviderUrl = getRegistedProviderUrl(providerUrl, registryUrl);

        ProviderConsumerRegTable.registerProvider(originInvoker, registryUrl, registeredProviderUrl);

        //to judge if we need to delay publish
        boolean register = registeredProviderUrl.getParameter("register", true);
        if (register) {
            register(registryUrl, registeredProviderUrl);
            ProviderConsumerRegTable.getProviderWrapper(originInvoker).setReg(true);
        }

        // Subscribe the override data
        // FIXME When the provider subscribes, it will affect the scene : a certain JVM exposes the service and call the same service. Because the subscribed is cached key with the name of the service, it causes the subscription information to cover.
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(registeredProviderUrl);
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
        overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);

        dynamicConfiguration.addListener(overrideSubscribeUrl.getServiceKey() + Constants.CONFIGURATORS_SUFFIX, overrideSubscribeListener);
        //Ensure that a new exporter instance is returned every time export
        return new DestroyableExporter<T>(exporter, originInvoker, overrideSubscribeUrl, registeredProviderUrl);
    }

    private <T> URL overrideUrlWithConfig(URL providerUrl) {
        List<Configurator> dynamicConfigurators = new LinkedList<>();
        String appRawConfig = dynamicConfiguration.getConfig(providerUrl.getParameter(Constants.APPLICATION_KEY) + Constants.CONFIGURATORS_SUFFIX, "dubbo");
        if (!StringUtils.isEmpty(appRawConfig)) {
            dynamicConfigurators.addAll(RegistryDirectory.configToConfiguratiors(appRawConfig));
        }
        String rawConfig = dynamicConfiguration.getConfig(providerUrl.getServiceKey() + Constants.CONFIGURATORS_SUFFIX, "dubbo");
        if (!StringUtils.isEmpty(rawConfig)) {
            dynamicConfigurators.addAll(RegistryDirectory.configToConfiguratiors(rawConfig));
        }
        providerUrl = getConfigedInvokerUrl(dynamicConfigurators, providerUrl);
        return providerUrl;
    }

    private URL getConfigUrl(URL registryUrl) {
        Map<String, String> qs = StringUtils.parseQueryString(registryUrl.getParameterAndDecoded(REFER_KEY));
        URL url = registryUrl
                .removeParameters(EXPORT_KEY, REFER_KEY)
                .setProtocol(CONFIG_PROTOCOL)
                .setPath(qs.get(INTERFACE_KEY));
        String configType = registryUrl.getParameter(Constants.CONFIG_TYPE_KEY);
        if (StringUtils.isEmpty(configType)) {
            url = url.addParameter(Constants.CONFIG_TYPE_KEY, registryUrl.getProtocol());
        }

        String configAddress = registryUrl.getParameter(Constants.CONFIG_ADDRESS_KEY);
        if (StringUtils.isNotEmpty(configAddress)) {
            url = url.setAddress(configAddress);
        }
        return url;
    }

    @SuppressWarnings("unchecked")
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker, URL providerUrl) {
        String key = getCacheKey(originInvoker);
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            synchronized (bounds) {
                exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
                if (exporter == null) {

                    final Invoker<?> invokerDelegete = new InvokerDelegete<T>(originInvoker, providerUrl);
                    exporter = new ExporterChangeableWrapper<T>((Exporter<T>) protocol.export(invokerDelegete), originInvoker);
                    bounds.put(key, exporter);
                }
            }
        }
        return exporter;
    }

    /**
     * Reexport the invoker of the modified url
     *
     * @param originInvoker
     * @param newInvokerUrl
     */
    @SuppressWarnings("unchecked")
    private <T> void doChangeLocalExport(final Invoker<T> originInvoker, URL newInvokerUrl) {
        String key = getCacheKey(originInvoker);
        final ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            logger.warn(new IllegalStateException("error state, exporter should not be null"));
        } else {
            final Invoker<T> invokerDelegete = new InvokerDelegete<T>(originInvoker, newInvokerUrl);
            exporter.setExporter(protocol.export(invokerDelegete));
        }
    }

    /**
     * Get an instance of registry based on the address of invoker
     *
     * @param originInvoker
     * @return
     */
    private Registry getRegistry(final Invoker<?> originInvoker) {
        URL registryUrl = getRegistryUrl(originInvoker);
        return registryFactory.getRegistry(registryUrl);
    }

    private URL getRegistryUrl(Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY);
        }
        return registryUrl;
    }


    /**
     * Return the url that is registered to the registry and filter the url parameter once
     *
     * @param providerUrl
     * @return url to registry.
     */
    private URL getRegistedProviderUrl(final URL providerUrl, final URL registryUrl) {
        //The address you see at the registry
        if(!registryUrl.getParameter(Constants.SIMPLE_KEY,false)){
            final URL registedProviderUrl = providerUrl.removeParameters(getFilteredKeys(providerUrl))
                    .removeParameter(Constants.MONITOR_KEY)
                    .removeParameter(Constants.BIND_IP_KEY)
                    .removeParameter(Constants.BIND_PORT_KEY)
                    .removeParameter(QOS_ENABLE)
                    .removeParameter(QOS_PORT)
                    .removeParameter(ACCEPT_FOREIGN_IP)
                    .removeParameter(VALIDATION_KEY)
                    .removeParameter(INTERFACES);
            return registedProviderUrl;
        }else{
            String[] addionalParameterKeys = registryUrl.getParameter(ADD_PARAM_KEYS_KEY, new String[0]);
            String[] registryParams = {APPLICATION_KEY, CODEC_KEY, EXCHANGER_KEY, SERIALIZATION_KEY, CLUSTER_KEY, CONNECTIONS_KEY, DEPRECATED_KEY,
                    GROUP_KEY, LOADBALANCE_KEY, MOCK_KEY, PATH_KEY, TIMEOUT_KEY, TOKEN_KEY, VERSION_KEY, WARMUP_KEY, WEIGHT_KEY, TIMESTAMP_KEY};
            ArrayUtils.addAll(registryParams, addionalParameterKeys);
            String[] methods = providerUrl.getParameter(METHODS_KEY, (String[]) null);
            return URL.valueOf(providerUrl, registryParams, methods);
        }

    }

    private URL getSubscribedOverrideUrl(URL registedProviderUrl) {
        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL)
                .addParameters(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY,
                        Constants.CHECK_KEY, String.valueOf(false));
    }

    /**
     * Get the address of the providerUrl through the url of the invoker
     *
     * @param origininvoker
     * @return
     */
    private URL getProviderUrl(final Invoker<?> origininvoker) {
        String export = origininvoker.getUrl().getParameterAndDecoded(EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + origininvoker.getUrl());
        }
        return URL.valueOf(export);
    }

    /**
     * Get the key cached in bounds by invoker
     *
     * @param originInvoker
     * @return
     */
    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString();
        return key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);
        initDynamicConfiguration(url);
        Registry registry = registryFactory.getRegistry(url);
        if (RegistryService.class.equals(type)) {
            return proxyFactory.getInvoker((T) registry, type, url);
        }

        // group="a,b" or group="*"
        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        String group = qs.get(Constants.GROUP_KEY);
        if (group != null && group.length() > 0) {
            if ((Constants.COMMA_SPLIT_PATTERN.split(group)).length > 1
                    || "*".equals(group)) {
                return doRefer(getMergeableCluster(), registry, type, url);
            }
        }
        return doRefer(cluster, registry, type, url);
    }

    private Cluster getMergeableCluster() {
        return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
    }

    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        directory.setDynamicConfiguration(dynamicConfiguration);
        directory.buildRouterChain(dynamicConfiguration);
        // all attributes of REFER_KEY
        Map<String, String> parameters = new HashMap<String, String>(directory.getUrl().getParameters());
        URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, parameters.remove(Constants.REGISTER_IP_KEY), 0, type.getName(), parameters);
        if (!Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            registry.register(getRegistedConsumerUrl(subscribeUrl, directory.getUrl()));
        }
        directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY,
                Constants.PROVIDERS_CATEGORY
                        + "," + Constants.CONFIGURATORS_CATEGORY
                        + "," + Constants.ROUTERS_CATEGORY));

        Invoker invoker = cluster.join(directory);
        ProviderConsumerRegTable.registerConsumer(invoker, url, subscribeUrl, directory);
        return invoker;
    }

    private URL getRegistedConsumerUrl(final URL consumerUrl, URL registryUrl) {
        if(!registryUrl.getParameter(Constants.SIMPLE_KEY,false)){
            return consumerUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                    Constants.CHECK_KEY, String.valueOf(false));
        }else{
            String[] addionalParameterKeys = registryUrl.getParameter(ADD_PARAM_KEYS_KEY, new String[0]);
            String[] registryParams = {APPLICATION_KEY, CODEC_KEY, EXCHANGER_KEY, SERIALIZATION_KEY, CLUSTER_KEY, CONNECTIONS_KEY, DEPRECATED_KEY,
                    GROUP_KEY, LOADBALANCE_KEY, MOCK_KEY, PATH_KEY, TIMEOUT_KEY, TOKEN_KEY, VERSION_KEY, WARMUP_KEY, WEIGHT_KEY, TIMESTAMP_KEY};
            ArrayUtils.addAll(registryParams, addionalParameterKeys);
            String[] methods = consumerUrl.getParameter(METHODS_KEY, (String[]) null);
            return URL.valueOf(consumerUrl, registryParams, methods).addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                    Constants.CHECK_KEY, String.valueOf(false));
        }
    }

    @Override
    public void destroy() {
        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();
    }

    //Merge the urls of configurators
    private URL getConfigedInvokerUrl(List<Configurator> configurators, URL url) {
        if (configurators != null && configurators.size() > 0) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
        }
        return url;
    }

    public static class InvokerDelegete<T> extends InvokerWrapper<T> {
        private final Invoker<T> invoker;

        /**
         * @param invoker
         * @param url     invoker.getUrl return this value
         */
        public InvokerDelegete(Invoker<T> invoker, URL url) {
            super(invoker, url);
            this.invoker = invoker;
        }

        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegete) {
                return ((InvokerDelegete<T>) invoker).getInvoker();
            } else {
                return invoker;
            }
        }
    }

    static private class DestroyableExporter<T> implements Exporter<T> {

        public static final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("Exporter-Unexport", true));

        private Exporter<T> exporter;
        private Invoker<T> originInvoker;
        private URL subscribeUrl;
        private URL registerUrl;

        public DestroyableExporter(Exporter<T> exporter, Invoker<T> originInvoker, URL subscribeUrl, URL registerUrl) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
            this.subscribeUrl = subscribeUrl;
            this.registerUrl = registerUrl;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        @Override
        public void unexport() {
            Registry registry = RegistryProtocol.INSTANCE.getRegistry(originInvoker);
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
            try {
                NotifyListener listener = RegistryProtocol.INSTANCE.overrideListeners.remove(subscribeUrl);
                registry.unsubscribe(subscribeUrl, listener);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        int timeout = ConfigUtils.getServerShutdownTimeout();
                        if (timeout > 0) {
                            logger.info("Waiting " + timeout + "ms for registry to notify all consumers before unexport. Usually, this is called when you use dubbo API");
                            Thread.sleep(timeout);
                        }
                        exporter.unexport();
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
            });
        }
    }

    /**
     * Reexport: the exporter destroy problem in protocol
     * 1.Ensure that the exporter returned by registryprotocol can be normal destroyed
     * 2.No need to re-register to the registry after notify
     * 3.The invoker passed by the export method , would better to be the invoker of exporter
     */
    private class OverrideListener implements NotifyListener, ConfigurationListener {

        private final URL subscribeUrl;
        private final Invoker originInvoker;
        private List<Configurator> configurators;
        private List<Configurator> dynamicConfigurators;
        private List<Configurator> appDynamicConfigurators;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }

        /**
         * @param urls The list of registered information , is always not empty, The meaning is the same as the return value of {@link org.apache.dubbo.registry.RegistryService#lookup(URL)}.
         */
        @Override
        public synchronized void notify(List<URL> urls) {
            logger.debug("original override urls: " + urls);
            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl.addParameter(Constants.CATEGORY_KEY,
                    Constants.CONFIGURATORS_CATEGORY
                            + "," + Constants.DYNAMIC_CONFIGURATORS_CATEGORY
                            + "," + Constants.APP_DYNAMIC_CONFIGURATORS_CATEGORY));
            logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
            // No matching results
            if (matchedUrls.isEmpty()) {
                return;
            }
            List<URL> configuratorUrls = matchedUrls.stream().filter(u -> u.getParameter(Constants.CATEGORY_KEY).equals(Constants.CONFIGURATORS_CATEGORY)).collect(Collectors.toList());
            List<URL> dynamicConfiguratorUrls = matchedUrls.stream().filter(u -> u.getParameter(Constants.CATEGORY_KEY).equals(Constants.DYNAMIC_CONFIGURATORS_CATEGORY)).collect(Collectors.toList());
            List<URL> appDynamicConfiguratorUrls = matchedUrls.stream().filter(u -> u.getParameter(Constants.CATEGORY_KEY).equals(Constants.APP_DYNAMIC_CONFIGURATORS_CATEGORY)).collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(configuratorUrls)) {
                configurators = RegistryDirectory.toConfigurators(configuratorUrls);
            }
            if (CollectionUtils.isNotEmpty(dynamicConfiguratorUrls)) {
                dynamicConfigurators = RegistryDirectory.toConfigurators(dynamicConfiguratorUrls);
            }
            if (CollectionUtils.isNotEmpty(appDynamicConfiguratorUrls)) {
                appDynamicConfigurators = RegistryDirectory.toConfigurators(appDynamicConfiguratorUrls);
            }

            final Invoker<?> invoker;
            if (originInvoker instanceof InvokerDelegete) {
                invoker = ((InvokerDelegete<?>) originInvoker).getInvoker();
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
            URL currentUrl = exporter.getInvoker().getUrl();
            //Merged with this configuration
            URL newUrl = getConfigedInvokerUrl(configurators, originUrl);
            newUrl = getConfigedInvokerUrl(appDynamicConfigurators, newUrl);
            newUrl = getConfigedInvokerUrl(dynamicConfigurators, newUrl);
            if (!currentUrl.equals(newUrl)) {
                RegistryProtocol.this.doChangeLocalExport(originInvoker, newUrl);
                logger.info("exported provider url changed, origin url: " + originUrl + ", old export url: " + currentUrl + ", new export url: " + newUrl);
            }
        }

        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
            List<URL> result = new ArrayList<URL>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                // Compatible with the old version
                if (url.getParameter(Constants.CATEGORY_KEY) == null
                        && Constants.OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
                    overrideUrl = url.addParameter(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY);
                }

                // Check whether url is to be applied to the current service
                if (UrlUtils.isMatch(currentSubscribe, overrideUrl)) {
                    result.add(url);
                }
            }
            return result;
        }

        @Override
        public void process(ConfigChangeEvent event) {
            List<URL> urls;
            if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
                URL originUrl = RegistryProtocol.this.getProviderUrl(originInvoker);
                originUrl = originUrl.clearParameters().setProtocol(Constants.EMPTY_PROTOCOL);
                // determine it's a app level or service level change.
                if (event.getKey().endsWith(originUrl.getParameter(APPLICATION_KEY) + CONFIGURATORS_SUFFIX)) {
                    originUrl = originUrl.addParameter(Constants.CATEGORY_KEY, Constants.APP_DYNAMIC_CONFIGURATORS_CATEGORY);
                } else {
                    originUrl = originUrl.addParameter(Constants.CATEGORY_KEY, Constants.DYNAMIC_CONFIGURATORS_CATEGORY);
                }
                urls = new ArrayList<>();
                urls.add(originUrl);
            } else {
                // parseConfigurators will recognize app/service config automatically.
                urls = ConfigParser.parseConfigurators(event.getNewValue());
            }
            notify(urls);
        }

        @Override
        public URL getUrl() {
            return subscribeUrl;
        }
    }

    /**
     * exporter proxy, establish the corresponding relationship between the returned exporter and the exporter exported by the protocol, and can modify the relationship at the time of override.
     *
     * @param <T>
     */
    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        private final Invoker<T> originInvoker;
        private Exporter<T> exporter;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
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
            exporter.unexport();
        }
    }
}
