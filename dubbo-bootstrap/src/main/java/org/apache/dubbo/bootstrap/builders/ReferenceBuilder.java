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
package org.apache.dubbo.bootstrap.builders;

import org.apache.dubbo.bootstrap.BootstrapUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.AvailableCluster;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;

/**
 * This is a builder for build {@link ReferenceConfig}.
 *
 * @since 2.7
 */
public class ReferenceBuilder<T> extends AbstractReferenceBuilder<ReferenceConfig, ReferenceBuilder<T>> {
    /**
     * The interface name of the reference service
     */
    private String interfaceName;

    /**
     * The interface class of the reference service
     */
    private Class<?> interfaceClass;

    /**
     * client type
     */
    private String client;

    /**
     * The url for peer-to-peer invocation
     */
    private String url;

    /**
     * The method configs
     */
    private List<MethodConfig> methods;

    /**
     * The consumer config (default)
     */
    private ConsumerConfig consumer;

    /**
     * Only the service provider of the specified protocol is invoked, and other protocols are ignored.
     */
    private String protocol;

    public static ReferenceBuilder newBuilder() {
        return new ReferenceBuilder();
    }

    public ReferenceBuilder<T> id(String id) {
        return super.id(id);
    }

    public ReferenceBuilder<T> interfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
        return getThis();
    }

    public ReferenceBuilder<T> interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return getThis();
    }

    public ReferenceBuilder<T> client(String client) {
        this.client = client;
        return getThis();
    }

    public ReferenceBuilder<T> url(String url) {
        this.url = url;
        return getThis();
    }

    public ReferenceBuilder<T> addMethods(List<MethodConfig> methods) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.addAll(methods);
        return getThis();
    }

    public ReferenceBuilder<T> addMethod(MethodConfig method) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.add(method);
        return getThis();
    }

    public ReferenceBuilder<T> consumer(ConsumerConfig consumer) {
        this.consumer = consumer;
        return getThis();
    }

    public ReferenceBuilder<T> protocol(String protocol) {
        this.protocol = protocol;
        return getThis();
    }

    public ReferenceConfig<T> build() {
        ReferenceConfig<T> reference = new ReferenceConfig<>();
        super.build(reference);

        reference.setInterface(interfaceName);
        if (interfaceClass != null) {
            reference.setInterface(interfaceClass);
        }
        reference.setClient(client);
        reference.setUrl(url);
        reference.setMethods(methods);
        reference.setConsumer(consumer);
        reference.setProtocol(protocol);

        return reference;
    }

    @Override
    protected ReferenceBuilder<T> getThis() {
        return this;
    }

    public static class Helper {
        public static final Logger logger = LoggerFactory.getLogger(Helper.class);

        private static final Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
        private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

        public static <T> T refer(ReferenceConfig<T> rc) {
            T ref = null;
            if (interfaceName == null || interfaceName.length() == 0) {
                throw new IllegalStateException("<dubbo:reference interface=\"\" /> interface not allow null!");
            }
            if (getGeneric() == null && getConsumer() != null) {
                setGeneric(getConsumer().getGeneric());
            }
            if (ProtocolUtils.isGeneric(getGeneric())) {
                interfaceClass = GenericService.class;
            } else {
                try {
                    interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                            .getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                checkInterfaceAndMethods(interfaceClass, methods);
            }
            String resolve = System.getProperty(interfaceName);
            String resolveFile = null;
            if (resolve == null || resolve.length() == 0) {
                resolveFile = System.getProperty("dubbo.resolve.file");
                if (resolveFile == null || resolveFile.length() == 0) {
                    File userResolveFile = new File(new File(System.getProperty("user.home")), "dubbo-resolve.properties");
                    if (userResolveFile.exists()) {
                        resolveFile = userResolveFile.getAbsolutePath();
                    }
                }
                if (resolveFile != null && resolveFile.length() > 0) {
                    Properties properties = new Properties();
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(new File(resolveFile));
                        properties.load(fis);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unload " + resolveFile + ", cause: " + e.getMessage(), e);
                    } finally {
                        try {
                            if (null != fis) fis.close();
                        } catch (IOException e) {
                            logger.warn(e.getMessage(), e);
                        }
                    }
                    resolve = properties.getProperty(interfaceName);
                }
            }
            if (resolve != null && resolve.length() > 0) {
                url = resolve;
                if (logger.isWarnEnabled()) {
                    if (resolveFile != null) {
                        logger.warn("Using default dubbo resolve file " + resolveFile + " replace " + interfaceName + "" + resolve + " to p2p invoke remote service.");
                    } else {
                        logger.warn("Using -D" + interfaceName + "=" + resolve + " to p2p invoke remote service.");
                    }
                }
            }
            checkStubAndMock(interfaceClass);
            Map<String, String> map = new HashMap<>();
            Map<Object, Object> attributes = new HashMap<Object, Object>();
            map.put(SIDE_KEY, Constants.CONSUMER_SIDE);
            map.put(DUBBO_VERSION_KEY, Version.getProtocolVersion());
            map.put(TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
            if (ConfigUtils.getPid() > 0) {
                map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
            }
            if (!isGeneric()) {
                String revision = Version.getVersion(interfaceClass, version);
                if (revision != null && revision.length() > 0) {
                    map.put("revision", revision);
                }

                String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
                if (methods.length == 0) {
                    logger.warn("NO method found in service interface " + interfaceClass.getName());
                    map.put("methods", Constants.ANY_VALUE);
                } else {
                    map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
                }
            }
            map.put(Constants.INTERFACE_KEY, interfaceName);

            map.putAll(ConfigConverter.configToMap(application, null));
            map.putAll(ConfigConverter.configToMap(module, null));
            map.putAll(ConfigConverter.configToMap(consumer, Constants.DEFAULT_KEY));
            map.putAll(ConfigConverter.configToMap(this, null));
            String prefix = StringUtils.getServiceKey(map);
            if (methods != null && !methods.isEmpty()) {
                for (MethodConfig method : methods) {
                    map.putAll(ConfigConverter.configToMap(method, method.getName()));
                    String retryKey = method.getName() + ".retry";
                    if (map.containsKey(retryKey)) {
                        String retryValue = map.remove(retryKey);
                        if ("false".equals(retryValue)) {
                            map.put(method.getName() + ".retries", "0");
                        }
                    }
                    appendAttributes(attributes, method, prefix + "." + method.getName());
                    checkAndConvertImplicitConfig(method, map, attributes);
                }
            }

            String hostToRegistry = ConfigUtils.getSystemProperty(Constants.DUBBO_IP_TO_REGISTRY);
            if (hostToRegistry == null || hostToRegistry.length() == 0) {
                hostToRegistry = NetUtils.getLocalHost();
            } else if (isInvalidLocalHost(hostToRegistry)) {
                throw new IllegalArgumentException("Specified invalid registry ip from property:" + Constants.DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
            }
            map.put(Constants.REGISTER_IP_KEY, hostToRegistry);

            //attributes are stored by system context.
            StaticContext.getSystemContext().putAll(attributes);
            ref = createProxy(map);
            ConsumerModel consumerModel = new ConsumerModel(getUniqueServiceName(), this, ref, interfaceClass.getMethods());
            ApplicationModel.initConsumerModel(getUniqueServiceName(), consumerModel);
            return ref;
        }

        @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
        private T createProxy(Map<String, String> map) {
            URL tmpUrl = new URL("temp", "localhost", 0, map);
            final boolean isJvmRefer;
            if (isInjvm() == null) {
                if (url != null && url.length() > 0) { // if a url is specified, don't do local reference
                    isJvmRefer = false;
                } else if (InjvmProtocol.getInjvmProtocol().isInjvmRefer(tmpUrl)) {
                    // by default, reference local service if there is
                    isJvmRefer = true;
                } else {
                    isJvmRefer = false;
                }
            } else {
                isJvmRefer = isInjvm().booleanValue();
            }

            if (isJvmRefer) {
                URL url = new URL(Constants.LOCAL_PROTOCOL, NetUtils.LOCALHOST, 0, interfaceClass.getName()).addParameters(map);
                invoker = refprotocol.refer(interfaceClass, url);
                if (logger.isInfoEnabled()) {
                    logger.info("Using injvm service " + interfaceClass.getName());
                }
            } else {
                if (url != null && url.length() > 0) { // user specified URL, could be peer-to-peer address, or register center's address.
                    String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(url);
                    if (us != null && us.length > 0) {
                        for (String u : us) {
                            URL url = URL.valueOf(u);
                            if (url.getPath() == null || url.getPath().length() == 0) {
                                url = url.setPath(interfaceName);
                            }
                            if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                                urls.add(url.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                            } else {
                                urls.add(ClusterUtils.mergeUrl(url, map));
                            }
                        }
                    }
                } else { // assemble URL from register center's configuration
                    List<URL> us = BootstrapUtils.loadRegistries(this, false);
                    if (us != null && !us.isEmpty()) {
                        for (URL u : us) {
                            URL monitorUrl = BootstrapUtils.loadMonitor(this, u);
                            if (monitorUrl != null) {
                                map.put(Constants.MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                            }
                            urls.add(u.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                        }
                    }
                    if (urls.isEmpty()) {
                        throw new IllegalStateException("No such any registry to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                    }
                }

                if (urls.size() == 1) {
                    invoker = refprotocol.refer(interfaceClass, urls.get(0));
                } else {
                    List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                    URL registryURL = null;
                    for (URL url : urls) {
                        invokers.add(refprotocol.refer(interfaceClass, url));
                        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                            registryURL = url; // use last registry url
                        }
                    }
                    if (registryURL != null) { // registry url is available
                        // use AvailableCluster only when register's cluster is available
                        URL u = registryURL.addParameter(Constants.CLUSTER_KEY, AvailableCluster.NAME);
                        invoker = cluster.join(new StaticDirectory(u, invokers));
                    } else { // not a registry url
                        invoker = cluster.join(new StaticDirectory(invokers));
                    }
                }
            }

            Boolean c = check;
            if (c == null && consumer != null) {
                c = consumer.isCheck();
            }
            if (c == null) {
                c = true; // default true
            }
            if (c && !invoker.isAvailable()) {
                throw new IllegalStateException("Failed to check the status of the service " + interfaceName + ". No provider available for the service " + (group == null ? "" : group + "/") + interfaceName + (version == null ? "" : ":" + version) + " from the url " + invoker.getUrl() + " to the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion());
            }
            if (logger.isInfoEnabled()) {
                logger.info("Refer dubbo service " + interfaceClass.getName() + " from url " + invoker.getUrl());
            }
            // create service proxy
            return (T) proxyFactory.getProxy(invoker);
        }
    }
}
