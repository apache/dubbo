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
package com.alibaba.dubbo.registry.integration;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.registry.support.ProviderConsumerRegTable;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.alibaba.dubbo.common.Constants.ACCEPT_FOREIGN_IP;
import static com.alibaba.dubbo.common.Constants.QOS_ENABLE;
import static com.alibaba.dubbo.common.Constants.QOS_PORT;

/**
 * RegistryProtocol
 *
 * 注册中心协议实现类。
 */
public class RegistryProtocol implements Protocol {

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);

    /**
     * 单例。在 Dubbo SPI 中，被初始化，有且仅有一次。
     */
    private static RegistryProtocol INSTANCE;
    private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<URL, NotifyListener>();

    /**
     * 绑定关系集合。
     *
     * key：服务 Dubbo URL
     */
    // To solve the problem of RMI repeated exposure port conflicts, the services that have been exposed are no longer exposed.
    // 用于解决rmi重复暴露端口冲突的问题，已经暴露过的服务不再重新暴露
    // providerurl <--> exporter
    private final Map<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<String, ExporterChangeableWrapper<?>>();
    /**
     * Cluster 自适应拓展实现类对象
     */
    private Cluster cluster;
    /**
     * Protocol 自适应拓展实现类，通过 Dubbo SPI 自动注入。
     */
    private Protocol protocol;
    /**
     * RegistryFactory 自适应拓展实现类，通过 Dubbo SPI 自动注入。
     */
    private RegistryFactory registryFactory;
    private ProxyFactory proxyFactory;

    public RegistryProtocol() {
        INSTANCE = this;
    }

    public static RegistryProtocol getRegistryProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(Constants.REGISTRY_PROTOCOL); // load
        }
        return INSTANCE;
    }

    // Filter the parameters that do not need to be output in url(Starting with .)
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

    public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {
        // 暴露服务
        // export invoker
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker);

        // 获得注册中心 URL
        URL registryUrl = getRegistryUrl(originInvoker);

        // 获得注册中心对象
        // registry provider
        final Registry registry = getRegistry(originInvoker);

        // 获得服务提供者 URL
        final URL registedProviderUrl = getRegistedProviderUrl(originInvoker);

        //to judge to delay publish whether or not
        boolean register = registedProviderUrl.getParameter("register", true);

        // 向注册中心订阅服务消费者
        ProviderConsumerRegTable.registerProvider(originInvoker, registryUrl, registedProviderUrl);

        // 向注册中心注册服务提供者（自己）
        if (register) {
            register(registryUrl, registedProviderUrl);
            ProviderConsumerRegTable.getProviderWrapper(originInvoker).setReg(true); // // 标记向本地注册表的注册服务提供者，已经注册
        }

        // 【TODO 8015】配置规则
        // Subscribe the override data
        // FIXME When the provider subscribes, it will affect the scene : a certain JVM exposes the service and call the same service. Because the subscribed is cached key with the name of the service, it causes the subscription information to cover.
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(registedProviderUrl);
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
        overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);
        //Ensure that a new exporter instance is returned every time export
        return new DestroyableExporter<T>(exporter, originInvoker, overrideSubscribeUrl, registedProviderUrl);
    }

    /**
     * 暴露服务。
     *
     * 此处的 Local 指的是，本地启动服务，但是不包括向注册中心注册服务的意思。
     *
     * @param originInvoker 原始 Invoker
     * @param <T> 泛型
     * @return Exporter 对象
     */
    @SuppressWarnings("unchecked")
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker) {
        // 获得在 `bounds` 中的缓存 Key
        String key = getCacheKey(originInvoker);
        // 从 `bounds` 获得，是不是已经暴露过服务
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            synchronized (bounds) {
                exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
                // 未暴露过，进行暴露服务
                if (exporter == null) {
                    // 创建 Invoker Delegate 对象
                    final Invoker<?> invokerDelegete = new InvokerDelegete<T>(originInvoker, getProviderUrl(originInvoker));
                    // 暴露服务，创建 Exporter 对象
                    // 使用 创建的Exporter对象 + originInvoker ，创建 ExporterChangeableWrapper 对象
                    exporter = new ExporterChangeableWrapper<T>((Exporter<T>) protocol.export(invokerDelegete), originInvoker);
                    // 添加到 `bounds`
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

    /**
     * 获得注册中心 URL
     *
     * @param originInvoker 原始 Invoker
     * @return URL
     */
    private URL getRegistryUrl(Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) { // protocol
            String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY);
        }
        return registryUrl;
    }


    /**
     * Return the url that is registered to the registry and filter the url parameter once
     *
     * @param originInvoker
     * @return
     */
    private URL getRegistedProviderUrl(final Invoker<?> originInvoker) {
        // 从注册中心的 export 参数中，获得服务提供者的 URL
        URL providerUrl = getProviderUrl(originInvoker);
        //The address you see at the registry
        return providerUrl.removeParameters(getFilteredKeys(providerUrl)) // 移除 .hide 为前缀的参数
                .removeParameter(Constants.MONITOR_KEY) // monitor
                .removeParameter(Constants.BIND_IP_KEY) // bind.ip
                .removeParameter(Constants.BIND_PORT_KEY) // bind.port
                .removeParameter(QOS_ENABLE) // qos.enable
                .removeParameter(QOS_PORT) // qos.port
                .removeParameter(ACCEPT_FOREIGN_IP); // qos.accept.foreign.ip
    }

    private URL getSubscribedOverrideUrl(URL registedProviderUrl) {
        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL)
                .addParameters(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY,
                        Constants.CHECK_KEY, String.valueOf(false));
    }

    /**
     * Get the address of the providerUrl through the url of the invoker
     *
     * 通过 invoker 的 url 获取 providerUrl 的地址
     *
     * @param origininvoker
     * @return
     */
    private URL getProviderUrl(final Invoker<?> origininvoker) {
        String export = origininvoker.getUrl().getParameterAndDecoded(Constants.EXPORT_KEY); // export
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + origininvoker.getUrl());
        }
        return URL.valueOf(export);
    }

    /**
     * Get the key cached in bounds by invoker
     *
     * 获 取invoker 在 bounds中 缓存的key
     *
     * @param originInvoker 原始 Invoker
     * @return url 字符串
     */
    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        return providerUrl.removeParameters("dynamic", "enabled").toFullString();
    }

    @SuppressWarnings("unchecked")
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // 获得真实的注册中心的 URL
        url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);
        // 获得注册中心
        Registry registry = registryFactory.getRegistry(url);
        // TODO 芋艿
        if (RegistryService.class.equals(type)) {
            return proxyFactory.getInvoker((T) registry, type, url);
        }

        // 获得服务引用配置参数集合
        // group="a,b" or group="*"
        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        String group = qs.get(Constants.GROUP_KEY);
        // 分组聚合，参见文档 http://dubbo.io/books/dubbo-user-book/demos/group-merger.html
        if (group != null && group.length() > 0) {
            if ((Constants.COMMA_SPLIT_PATTERN.split(group)).length > 1
                    || "*".equals(group)) {
                // 执行服务引用
                return doRefer(getMergeableCluster(), registry, type, url);
            }
        }
        // 执行服务引用
        return doRefer(cluster, registry, type, url);
    }

    private Cluster getMergeableCluster() {
        return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
    }

    /**
     * 执行服务引用，返回 Invoker 对象
     *
     * @param cluster Cluster 对象
     * @param registry 注册中心对象
     * @param type 服务接口类型
     * @param url 注册中心 URL
     * @param <T> 泛型
     * @return Invoker 对象
     */
    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        // 创建 RegistryDirectory 对象，并设置注册中心
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        // 创建订阅 URL
        // all attributes of REFER_KEY
        Map<String, String> parameters = new HashMap<String, String>(directory.getUrl().getParameters()); // 服务引用配置集合
        URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, parameters.remove(Constants.REGISTER_IP_KEY), 0, type.getName(), parameters);
        // 向注册中心注册自己（服务消费者）
        if (!Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                    Constants.CHECK_KEY, String.valueOf(false)));
        }
        // 向注册中心订阅服务提供者
        directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY,
                Constants.PROVIDERS_CATEGORY
                        + "," + Constants.CONFIGURATORS_CATEGORY
                        + "," + Constants.ROUTERS_CATEGORY));

        // 创建 Invoker 对象，【TODO 8015】集群容错
        Invoker invoker = cluster.join(directory);
        // 向本地注册表，注册消费者
        ProviderConsumerRegTable.registerConsumer(invoker, url, subscribeUrl, directory);
        return invoker;
    }

    public void destroy() {
        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();
    }

    public static class InvokerDelegete<T> extends InvokerWrapper<T> {

        /**
         * Invoker 对象
         *
         * 因为父类未提供 invoker 属性的获取方法，因此这里增加了和父类 invoker 一样的这个属性。
         */
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

    /**
     * Reexport: the exporter destroy problem in protocol
     * 1.Ensure that the exporter returned by registryprotocol can be normal destroyed
     * 2.No need to re-register to the registry after notify
     * 3.The invoker passed by the export method , would better to be the invoker of exporter
     */
    private class OverrideListener implements NotifyListener {

        private final URL subscribeUrl;
        private final Invoker originInvoker;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }

        /**
         * @param urls The list of registered information , is always not empty, The meaning is the same as the return value of {@link com.alibaba.dubbo.registry.RegistryService#lookup(URL)}.
         */
        public synchronized void notify(List<URL> urls) {
            logger.debug("original override urls: " + urls);
            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl);
            logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
            // No matching results
            if (matchedUrls.isEmpty()) {
                return;
            }

            List<Configurator> configurators = RegistryDirectory.toConfigurators(matchedUrls);

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

        //Merge the urls of configurators
        private URL getConfigedInvokerUrl(List<Configurator> configurators, URL url) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
            return url;
        }
    }

    /**
     * exporter proxy, establish the corresponding relationship between the returned exporter and the exporter exported by the protocol, and can modify the relationship at the time of override.
     *
     * exporter 代理, 建立返回的 exporter 与 protocol export 出的 exporter 的对应关系，在 override 时可以进行关系修改.
     *
     * @param <T>
     */
    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        /**
         * 原 Invoker 对象
         */
        private final Invoker<T> originInvoker;
        /**
         * 暴露的 Exporter 对象
         */
        private Exporter<T> exporter;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
        }

        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }

        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        public void setExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        public void unexport() {
            String key = getCacheKey(this.originInvoker);
            // 移除出 `bounds`
            bounds.remove(key);
            // 取消暴露
            exporter.unexport();
        }
    }

    /**
     * 可销毁的 Exporter
     *
     * @param <T> 泛型
     */
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

        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        public void unexport() {
            //
            Registry registry = RegistryProtocol.INSTANCE.getRegistry(originInvoker);
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }

            //
            try {
                NotifyListener listener = RegistryProtocol.INSTANCE.overrideListeners.remove(subscribeUrl);
                registry.unsubscribe(subscribeUrl, listener);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }

            //
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
}