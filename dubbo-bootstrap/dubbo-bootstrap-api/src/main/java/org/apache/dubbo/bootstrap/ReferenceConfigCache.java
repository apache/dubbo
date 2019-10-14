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
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.event.ReferenceConfigDestroyedEvent;
import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.cluster.support.registry.ZoneAwareCluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROXY_CLASS_REF;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SEMICOLON_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * A simple util class for cache {@link ReferenceConfig}.
 * <p>
 * {@link ReferenceConfig} is a heavy Object, it's necessary to cache these object
 * for the framework which create {@link ReferenceConfig} frequently.
 * <p>
 * You can implement and use your own {@link ReferenceConfig} cache if you need use complicate strategy.
 */
public class ReferenceConfigCache {
    public static final String DEFAULT_NAME = "_DEFAULT_";
    /**
     * Create the key with the <b>Group</b>, <b>Interface</b> and <b>version</b> attribute of {@link ReferenceConfig}.
     * <p>
     * key example: <code>group1/org.apache.dubbo.foo.FooService:1.0.0</code>.
     */
    public static final KeyGenerator DEFAULT_KEY_GENERATOR = referenceConfig -> {
        String iName = referenceConfig.getInterface();
        if (StringUtils.isBlank(iName)) {
            Class<?> clazz = referenceConfig.getInterfaceClass();
            iName = clazz.getName();
        }
        if (StringUtils.isBlank(iName)) {
            throw new IllegalArgumentException("No interface info in ReferenceConfig" + referenceConfig);
        }

        StringBuilder ret = new StringBuilder();
        if (!StringUtils.isBlank(referenceConfig.getGroup())) {
            ret.append(referenceConfig.getGroup()).append("/");
        }
        ret.append(iName);
        if (!StringUtils.isBlank(referenceConfig.getVersion())) {
            ret.append(":").append(referenceConfig.getVersion());
        }
        return ret.toString();
    };

    static final ConcurrentMap<String, ReferenceConfigCache> CACHE_HOLDER = new ConcurrentHashMap<String, ReferenceConfigCache>();
    private final String name;
    private final KeyGenerator generator;

    private final ConcurrentMap<String, ReferenceConfig<?>> referredReferences = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, ConcurrentMap<String, Object>> proxies = new ConcurrentHashMap<>();

    private ReferenceConfigCache(String name, KeyGenerator generator) {
        this.name = name;
        this.generator = generator;
    }

    /**
     * Get the cache use default name and {@link #DEFAULT_KEY_GENERATOR} to generate cache key.
     * Create cache if not existed yet.
     */
    public static ReferenceConfigCache getCache() {
        return getCache(DEFAULT_NAME);
    }

    /**
     * Get the cache use specified name and {@link KeyGenerator}.
     * Create cache if not existed yet.
     */
    public static ReferenceConfigCache getCache(String name) {
        return getCache(name, DEFAULT_KEY_GENERATOR);
    }

    /**
     * Get the cache use specified {@link KeyGenerator}.
     * Create cache if not existed yet.
     */
    public static ReferenceConfigCache getCache(String name, KeyGenerator keyGenerator) {
        ReferenceConfigCache cache = CACHE_HOLDER.get(name);
        if (cache != null) {
            return cache;
        }
        CACHE_HOLDER.putIfAbsent(name, new ReferenceConfigCache(name, keyGenerator));
        return CACHE_HOLDER.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ReferenceConfig<T> referenceConfig) {
        String key = generator.generateKey(referenceConfig);
        Class<?> type = referenceConfig.getInterfaceClass();

        proxies.computeIfAbsent(type, _t -> new ConcurrentHashMap());

        ConcurrentMap<String, Object> proxiesOfType = proxies.get(type);
        proxiesOfType.computeIfAbsent(key, _k -> {
            Object proxy = ReferHelper.refer(referenceConfig);
            referredReferences.put(key, referenceConfig);
            return proxy;
        });

        return (T) proxiesOfType.get(key);
    }

    /**
     * Fetch cache with the specified key. The key is decided by KeyGenerator passed-in. If the default KeyGenerator is
     * used, then the key is in the format of <code>group/interfaceClass:version</code>
     *
     * @param key  cache key
     * @param type object class
     * @param <T>  object type
     * @return object from the cached ReferenceConfig
     * @see KeyGenerator#generateKey(ReferenceConfig)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Map<String, Object> proxiesOfType = proxies.get(type);
        if (CollectionUtils.isEmptyMap(proxiesOfType)) {
            return null;
        }
        return (T) proxiesOfType.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        ReferenceConfig<?> rc = referredReferences.get(key);
        if (rc == null) {
            return null;
        }

        return (T) get(key, rc.getInterfaceClass());
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> get(Class<T> type) {
        Map<String, Object> proxiesOfType = proxies.get(type);
        if (CollectionUtils.isEmptyMap(proxiesOfType)) {
            return Collections.emptyList();
        }

        List<T> proxySet = new ArrayList<>();
        proxiesOfType.values().forEach(obj -> proxySet.add((T) obj));
        return proxySet;
    }

    public void destroy(String key, Class<?> type) {
        ReferenceConfig<?> rc = referredReferences.remove(key);
        if (rc == null) {
            return;
        }

        ConfigManager.getInstance().removeConfig(rc);

        Map<String, Object> proxiesOfType = proxies.remove(type);
        proxiesOfType.forEach((_k, _v) -> {
            if (key == null || _k.equals(key)) {
                Destroyable proxy = (Destroyable) _v;
                proxy.$destroy();
            }
        });

    }

    public void destroy(Class<?> type) {
        destroy(null, type);
    }

    /**
     * clear and destroy one {@link ReferenceConfig} in the cache.
     *
     * @param referenceConfig use for create key.
     */
    public <T> void destroy(ReferenceConfig<T> referenceConfig) {
        String key = generator.generateKey(referenceConfig);
        Class<?> type = referenceConfig.getInterfaceClass();

        destroy(key, type);
    }

    /**
     * clear and destroy all {@link ReferenceConfig} in the cache.
     */
    public void destroyAll() {
        if (CollectionUtils.isEmptyMap(referredReferences)) {
            return;
        }

        referredReferences.forEach((_k, referenceConfig) -> {
            ConfigManager.getInstance().removeConfig(referenceConfig);
        });

        proxies.forEach((_type, proxiesOfType) -> {
            proxiesOfType.forEach((_k, v) -> {
                Destroyable proxy = (Destroyable) v;
                proxy.$destroy();
            });
        });

        referredReferences.clear();
        proxies.clear();
    }

    @Override
    public String toString() {
        return "ReferenceConfigCache(name: " + name
                + ")";
    }

    public interface KeyGenerator {
        String generateKey(ReferenceConfig<?> referenceConfig);
    }

    public static class ReferHelper {
        public static final Logger logger = LoggerFactory.getLogger(ReferHelper.class);

        /**
         * The {@link Protocol} implementation with adaptive functionality,it will be different in different scenarios.
         * A particular {@link Protocol} implementation is determined by the protocol attribute in the {@link URL}.
         * For example:
         *
         * <li>when the url is registry://224.5.6.7:1234/org.apache.dubbo.registry.RegistryService?application=dubbo-sample,
         * then the protocol is <b>RegistryProtocol</b></li>
         *
         * <li>when the url is dubbo://224.5.6.7:1234/org.apache.dubbo.config.api.DemoService?application=dubbo-sample, then
         * the protocol is <b>DubboProtocol</b></li>
         * <p>
         * Actually，when the {@link ExtensionLoader} init the {@link Protocol} instants,it will automatically wraps two
         * layers, and eventually will get a <b>ProtocolFilterWrapper</b> or <b>ProtocolListenerWrapper</b>
         */
        private static final Protocol REF_PROTOCOL = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        /**
         * The {@link Cluster}'s implementation with adaptive functionality, and actually it will get a {@link Cluster}'s
         * specific implementation who is wrapped with <b>MockClusterInvoker</b>
         */
        private static final Cluster CLUSTER = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

        /**
         * A {@link ProxyFactory} implementation that will generate a reference service's proxy,the JavassistProxyFactory is
         * its default implementation
         */
        private static final ProxyFactory PROXY_FACTORY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

        public static <T> T refer(ReferenceConfig<T> rc) {
            checkAndUpdateSubConfigs(rc);

            Class<?> interfaceClass = rc.getInterfaceClass();
            String interfaceName = rc.getInterface();

            rc.checkStubAndLocal(interfaceClass);
            BootstrapUtils.checkMock(interfaceClass, rc);
            Map<String, String> map = new HashMap<String, String>();

            map.put(SIDE_KEY, CONSUMER_SIDE);

            ReferenceConfig.appendRuntimeParameters(map);
            if (!ProtocolUtils.isGeneric(rc.getGeneric())) {
                String revision = Version.getVersion(interfaceClass, rc.getVersion());
                if (revision != null && revision.length() > 0) {
                    map.put(REVISION_KEY, revision);
                }

                String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
                if (methods.length == 0) {
                    logger.warn("No method found in service interface " + interfaceClass.getName());
                    map.put(METHODS_KEY, ANY_VALUE);
                } else {
                    map.put(METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), COMMA_SEPARATOR));
                }
            }
            map.put(INTERFACE_KEY, interfaceName);
            AbstractConfig.appendParameters(map, rc.getMetrics());
            AbstractConfig.appendParameters(map, rc.getApplication());
            AbstractConfig.appendParameters(map, rc.getModule());
            // remove 'default.' prefix for configs from ConsumerConfig
            // appendParameters(map, consumer, Constants.DEFAULT_KEY);
            AbstractConfig.appendParameters(map, rc.getConsumer());
            AbstractConfig.appendParameters(map, rc);
            Map<String, Object> attributes = null;
            if (CollectionUtils.isNotEmpty(rc.getMethods())) {
                attributes = new HashMap<>();
                for (MethodConfig methodConfig : rc.getMethods()) {
                    AbstractConfig.appendParameters(map, methodConfig, methodConfig.getName());
                    String retryKey = methodConfig.getName() + ".retry";
                    if (map.containsKey(retryKey)) {
                        String retryValue = map.remove(retryKey);
                        if ("false".equals(retryValue)) {
                            map.put(methodConfig.getName() + ".retries", "0");
                        }
                    }
                    ConsumerModel.AsyncMethodInfo asyncMethodInfo = AbstractConfig.convertMethodConfig2AsyncInfo(methodConfig);
                    if (asyncMethodInfo != null) {
//                    consumerModel.getMethodModel(methodConfig.getName()).addAttribute(ASYNC_KEY, asyncMethodInfo);
                        attributes.put(methodConfig.getName(), asyncMethodInfo);
                    }
                }
            }

            String hostToRegistry = ConfigUtils.getSystemProperty(DUBBO_IP_TO_REGISTRY);
            if (StringUtils.isEmpty(hostToRegistry)) {
                hostToRegistry = NetUtils.getLocalHost();
            } else if (isInvalidLocalHost(hostToRegistry)) {
                throw new IllegalArgumentException("Specified invalid registry ip from property:" + DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
            }
            map.put(REGISTER_IP_KEY, hostToRegistry);

            ServiceMetadata serviceMetadata = rc.getServiceMetadata();
            serviceMetadata.getAttachments().putAll(map);

            T proxy = createProxy(map, rc);

            serviceMetadata.setTarget(proxy);
            serviceMetadata.addAttribute(PROXY_CLASS_REF, proxy);

            ServiceModel serviceModel = ApplicationModel.registerServiceModel(interfaceClass);
            ApplicationModel.initConsumerModel(serviceMetadata.getServiceKey(),
                    rc.buildConsumerModel(attributes, serviceModel, rc, proxy));

            // dispatch a ReferenceConfigDestroyedEvent since 2.7.4
            dispatch(new ReferenceConfigDestroyedEvent(rc));
            return proxy;
        }

        @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
        private static <T> T createProxy(Map<String, String> map, ReferenceConfig<?> rc) {
            Class<?> interfaceClass = rc.getInterfaceClass();
            String interfaceName = rc.getInterface();

            Invoker<?> invoker;
            if (shouldJvmRefer(map, rc)) {
                URL url = new URL(LOCAL_PROTOCOL, LOCALHOST_VALUE, 0, interfaceClass.getName()).addParameters(map);
                invoker = REF_PROTOCOL.refer(interfaceClass, url);
                if (logger.isInfoEnabled()) {
                    logger.info("Using injvm service " + interfaceClass.getName());
                }
            } else {
                List<URL> urls = new ArrayList<URL>();
                if (rc.getUrl() != null && rc.getUrl().length() > 0) { // user specified URL, could be peer-to-peer address, or register center's address.
                    String[] us = SEMICOLON_SPLIT_PATTERN.split(rc.getUrl());
                    if (us != null && us.length > 0) {
                        for (String u : us) {
                            URL url = URL.valueOf(u);
                            if (StringUtils.isEmpty(url.getPath())) {
                                url = url.setPath(interfaceName);
                            }
                            if (UrlUtils.isRegistry(url)) {
                                urls.add(url.addParameterAndEncoded(REFER_KEY, StringUtils.toQueryString(map)));
                            } else {
                                urls.add(ClusterUtils.mergeUrl(url, map));
                            }
                        }
                    }
                } else { // assemble URL from register center's configuration
                    // if protocols not injvm checkRegistry
                    if (!LOCAL_PROTOCOL.equalsIgnoreCase(rc.getProtocol())) {
                        rc.checkRegistry();
                        List<URL> us = BootstrapUtils.loadRegistries(rc, false);
                        if (CollectionUtils.isNotEmpty(us)) {
                            for (URL u : us) {
                                URL monitorUrl = BootstrapUtils.loadMonitor(rc, u);
                                if (monitorUrl != null) {
                                    map.put(MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                                }
                                urls.add(u.addParameterAndEncoded(REFER_KEY, StringUtils.toQueryString(map)));
                            }
                        }
                        if (urls.isEmpty()) {
                            throw new IllegalStateException("No such any registry to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                        }
                    }
                }

                if (urls.size() == 1) {
                    invoker = REF_PROTOCOL.refer(interfaceClass, urls.get(0));
                } else {
                    List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                    URL registryURL = null;
                    for (URL url : urls) {
                        invokers.add(REF_PROTOCOL.refer(interfaceClass, url));
                        if (UrlUtils.isRegistry(url)) {
                            registryURL = url; // use last registry url
                        }
                    }
                    if (registryURL != null) { // registry url is available
                        // for multi-subscription scenario, use 'zone-aware' policy by default
                        URL u = registryURL.addParameterIfAbsent(CLUSTER_KEY, ZoneAwareCluster.NAME);
                        // The invoker wrap relation would be like: ZoneAwareClusterInvoker(StaticDirectory) -> FailoverClusterInvoker(RegistryDirectory, routing happens here) -> Invoker
                        invoker = CLUSTER.join(new StaticDirectory(u, invokers));
                    } else { // not a registry url, must be direct invoke.
                        invoker = CLUSTER.join(new StaticDirectory(invokers));
                    }
                }
                rc.updateUrls(urls);
            }

            if (rc.shouldCheck() && !invoker.isAvailable()) {
                throw new IllegalStateException("Failed to check the status of the service "
                        + interfaceName
                        + ". No provider available for the service "
                        + (rc.getGroup() == null ? "" : rc.getGroup() + "/")
                        + interfaceName +
                        (rc.getVersion() == null ? "" : ":" + rc.getVersion())
                        + " from the url "
                        + invoker.getUrl()
                        + " to the consumer "
                        + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion());
            }
            if (logger.isInfoEnabled()) {
                logger.info("Refer dubbo service " + interfaceClass.getName() + " from url " + invoker.getUrl());
            }
            /**
             * @since 2.7.0
             * ServiceData Store
             */
            String metadata = map.get(METADATA_KEY);
            WritableMetadataService metadataService = WritableMetadataService.getExtension(metadata == null ? DEFAULT_METADATA_STORAGE_TYPE : metadata);
            if (metadataService != null) {
                URL consumerURL = new URL(CONSUMER_PROTOCOL, map.remove(REGISTER_IP_KEY), 0, map.get(INTERFACE_KEY), map);
                metadataService.publishServiceDefinition(consumerURL);
            }
            // create service proxy
            return (T) PROXY_FACTORY.getProxy(invoker);
        }

        /**
         * This method should be called right after the creation of this class's instance, before any property in other config modules is used.
         * Check each config modules are created properly and override their properties if necessary.
         */
        public static void checkAndUpdateSubConfigs(ReferenceConfig<?> rc) {
            if (StringUtils.isEmpty(rc.getInterface())) {
                throw new IllegalStateException("<dubbo:reference interface=\"\" /> interface not allow null!");
            }
            rc.completeCompoundConfigs();
            // get consumer's global configuration
            rc.checkDefault();
            rc.refresh();
            if (rc.getGeneric() == null && rc.getConsumer() != null) {
                rc.setGeneric(rc.getConsumer().getGeneric());
            }
            Class<?> interfaceClass = null;
            if (ProtocolUtils.isGeneric(rc.getGeneric())) {
                interfaceClass = GenericService.class;
            } else {
                try {
                    interfaceClass = Class.forName(rc.getInterface(), true, Thread.currentThread()
                            .getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                rc.checkInterfaceAndMethods(interfaceClass, rc.getMethods());
            }
            rc.setInterface(interfaceClass);
            rc.resolveFile();
            rc.checkApplication();
            rc.checkMetadataReport();
            rc.appendParameters();
        }


        /**
         * Figure out should refer the service in the same JVM from configurations. The default behavior is true
         * 1. if injvm is specified, then use it
         * 2. then if a url is specified, then assume it's a remote call
         * 3. otherwise, check scope parameter
         * 4. if scope is not specified but the target service is provided in the same JVM, then prefer to make the local
         * call, which is the default behavior
         */
        protected static boolean shouldJvmRefer(Map<String, String> map, ReferenceConfig<?> rc) {
            URL tmpUrl = new URL("temp", "localhost", 0, map);
            boolean isJvmRefer;
            if (rc.isInjvm() == null) {
                // if a url is specified, don't do local reference
                if (rc.getUrl() != null && rc.getUrl().length() > 0) {
                    isJvmRefer = false;
                } else {
                    // by default, reference local service if there is
                    isJvmRefer = InjvmProtocol.getInjvmProtocol().isInjvmRefer(tmpUrl);
                }
            } else {
                isJvmRefer = rc.isInjvm();
            }
            return isJvmRefer;
        }

        /**
         * Dispatch an {@link Event event}
         *
         * @param event an {@link Event event}
         * @since 2.7.4
         */
        protected static void dispatch(Event event) {
            EventDispatcher.getDefaultExtension().dispatch(event);
        }
    }
}
