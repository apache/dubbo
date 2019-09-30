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
import org.apache.dubbo.common.utils.ClassHelper;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.invoker.DelegateProviderMetaDataInvoker;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.utils.NetUtils.LOCALHOST;
import static org.apache.dubbo.common.utils.NetUtils.getAvailablePort;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidPort;

/**
 * This is a builder for build {@link ServiceConfig}.
 *
 * @since 2.7
 */
public class ServiceBuilder<U> extends AbstractServiceBuilder<ServiceConfig, ServiceBuilder<U>> {
    /**
     * The interface name of the exported service
     */
    private String interfaceName;

    /**
     * The interface class of the exported service
     */
    private Class<?> interfaceClass;

    /**
     * The reference of the interface implementation
     */
    private U ref;

    /**
     * The service name
     */
    private String path;

    /**
     * The method configuration
     */
    private List<MethodConfig> methods;

    /**
     * The provider configuration
     */
    private ProviderConfig provider;

    /**
     * The providerIds
     */
    private String providerIds;
    /**
     * whether it is a GenericService
     */
    private String generic;

    public static ServiceBuilder newBuilder() {
        return new ServiceBuilder();
    }

    public ServiceBuilder id(String id) {
        return super.id(id);
    }

    public ServiceBuilder<U> interfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
        return getThis();
    }

    public ServiceBuilder<U> interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return getThis();
    }

    public ServiceBuilder<U> ref(U ref) {
        this.ref = ref;
        return getThis();
    }

    public ServiceBuilder<U> path(String path) {
        this.path = path;
        return getThis();
    }

    public ServiceBuilder<U> addMethod(MethodConfig method) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.add(method);
        return getThis();
    }

    public ServiceBuilder<U> addMethods(List<? extends MethodConfig> methods) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.addAll(methods);
        return getThis();
    }

    public ServiceBuilder<U> provider(ProviderConfig provider) {
        this.provider = provider;
        return getThis();
    }

    public ServiceBuilder<U> providerIds(String providerIds) {
        this.providerIds = providerIds;
        return getThis();
    }

    public ServiceBuilder<U> generic(String generic) {
        this.generic = generic;
        return getThis();
    }

    @Override
    public ServiceBuilder<U> mock(String mock) {
        throw new IllegalArgumentException("mock doesn't support on provider side");
    }

    @Override
    public ServiceBuilder<U> mock(Boolean mock) {
        throw new IllegalArgumentException("mock doesn't support on provider side");
    }

    public ServiceConfig<U> build() {
        ServiceConfig<U> serviceConfig = new ServiceConfig<>();
        super.build(serviceConfig);

        serviceConfig.setInterface(interfaceName);
        serviceConfig.setInterface(interfaceClass);
        serviceConfig.setRef(ref);
        serviceConfig.setPath(path);
        serviceConfig.setMethods(methods);
        serviceConfig.setProvider(provider);
        serviceConfig.setProviderIds(providerIds);
        serviceConfig.setGeneric(generic);

        return serviceConfig;
    }

    @Override
    protected ServiceBuilder<U> getThis() {
        return this;
    }

    public static class Helper {
        public static final Logger logger = LoggerFactory.getLogger(Helper.class);

        private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        private static final ScheduledExecutorService delayExportExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));

        public static <T> List<Exporter<T>> export(ServiceConfig<T> sc) {
            if (provider != null) {
                if (export == null) {
                    export = provider.getExport();
                }
                if (delay == null) {
                    delay = provider.getDelay();
                }
            }
            if (export != null && !export) {
                return;
            }

            if (delay != null && delay > 0) {
                delayExportExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        doExport();
                    }
                }, delay, TimeUnit.MILLISECONDS);
            } else {
                doExport();
            }
        }

        protected void doExport() {
            if (unexported) {
                throw new IllegalStateException("Already unexported!");
            }
            if (exported) {
                return;
            }
            exported = true;

            if (interfaceName == null || interfaceName.length() == 0) {
                throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
            }
            if (ref instanceof GenericService) {
                interfaceClass = GenericService.class;
                if (StringUtils.isEmpty(generic)) {
                    generic = Boolean.TRUE.toString();
                }
            } else {
                try {
                    interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                            .getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                checkInterfaceAndMethods(interfaceClass, methods);
                checkRef();
                generic = Boolean.FALSE.toString();
            }
            if (local != null) {
                if ("true".equals(local)) {
                    local = interfaceName + "Local";
                }
                Class<?> localClass;
                try {
                    localClass = ClassHelper.forNameWithThreadContextClassLoader(local);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                if (!interfaceClass.isAssignableFrom(localClass)) {
                    throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceName);
                }
            }
            if (stub != null) {
                if ("true".equals(stub)) {
                    stub = interfaceName + "Stub";
                }
                Class<?> stubClass;
                try {
                    stubClass = ClassHelper.forNameWithThreadContextClassLoader(stub);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                if (!interfaceClass.isAssignableFrom(stubClass)) {
                    throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
                }
            }
            //TODO The properties may still not set yet, find a better way to check
            checkStubAndMock(interfaceClass);
            if (path == null || path.length() == 0) {
                path = interfaceName;
            }
            doExportUrls();
            ProviderModel providerModel = new ProviderModel(getUniqueServiceName(), this, ref);
            ApplicationModel.initProviderModel(getUniqueServiceName(), providerModel);
        }

        private void doExportUrls() {
            List<URL> registryURLs = BootstrapUtils.loadRegistries(this, true);
            for (ProtocolConfig protocolConfig : protocols) {
                doExportUrlsFor1Protocol(protocolConfig, registryURLs);
            }
        }

        private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
            URL url = this.generateUrl(protocolConfig, registryURLs);
            if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .hasExtension(url.getProtocol())) {
                url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                        .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
            }

            String scope = url.getParameter(Constants.SCOPE_KEY);
            // don't export when none is configured
            if (!Constants.SCOPE_NONE.toString().equalsIgnoreCase(scope)) {

                // export to local if the config is not remote (export to remote only when config is remote)
                if (!Constants.SCOPE_REMOTE.toString().equalsIgnoreCase(scope)) {
                    exportLocal(url);
                }
                // export to remote if the config is not local (export to local only when config is local)
                if (!Constants.SCOPE_LOCAL.toString().equalsIgnoreCase(scope)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                    }
                    if (registryURLs != null && !registryURLs.isEmpty()) {
                        for (URL registryURL : registryURLs) {
                            url = url.addParameterIfAbsent(Constants.DYNAMIC_KEY, registryURL.getParameter(Constants.DYNAMIC_KEY));
                            URL monitorUrl = BootstrapUtils.loadMonitor(this, registryURL);
                            if (monitorUrl != null) {
                                url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                            }
                            if (logger.isInfoEnabled()) {
                                logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                            }

                            // For providers, this is used to enable custom proxy to generate invoker
                            String proxy = url.getParameter(Constants.PROXY_KEY);
                            if (StringUtils.isNotEmpty(proxy)) {
                                registryURL = registryURL.addParameter(Constants.PROXY_KEY, proxy);
                            }

                            Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                            DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                            Exporter<?> exporter = protocol.export(wrapperInvoker);
                            exporters.add(exporter);
                        }
                    } else {
                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                        Exporter<?> exporter = protocol.export(wrapperInvoker);
                        exporters.add(exporter);
                    }
                }
            }
            this.urls.add(url);
        }

        private URL generateUrl(ProtocolConfig protocolConfig, List<URL> registryURLs) {
            String name = protocolConfig.getName();
            if (name == null || name.length() == 0) {
                name = "dubbo";
            }
            Map<String, String> map = new HashMap<String, String>();
            map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
            map.put(Constants.DUBBO_VERSION_KEY, Version.getProtocolVersion());
            map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
            if (ConfigUtils.getPid() > 0) {
                map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
            }
            map.putAll(ConfigConverter.configToMap(application, null));
            map.putAll(ConfigConverter.configToMap(module, null));
            map.putAll(ConfigConverter.configToMap(provider, Constants.DEFAULT_KEY));
            map.putAll(ConfigConverter.configToMap(protocolConfig, null));
            map.putAll(ConfigConverter.configToMap(this, null));
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
                    List<ArgumentConfig> arguments = method.getArguments();
                    if (arguments != null && !arguments.isEmpty()) {
                        for (ArgumentConfig argument : arguments) {
                            // convert argument type
                            if (argument.getType() != null && argument.getType().length() > 0) {
                                Method[] methods = interfaceClass.getMethods();
                                // visit all methods
                                if (methods != null && methods.length > 0) {
                                    for (int i = 0; i < methods.length; i++) {
                                        String methodName = methods[i].getName();
                                        // target the method, and get its signature
                                        if (methodName.equals(method.getName())) {
                                            Class<?>[] argtypes = methods[i].getParameterTypes();
                                            // one callback in the method
                                            if (argument.getIndex() != -1) {
                                                if (argtypes[argument.getIndex()].getName().equals(argument.getType())) {
                                                    map.putAll(ConfigConverter.configToMap(argument, method.getName() + "." + argument.getIndex()));
                                                } else {
                                                    throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                }
                                            } else {
                                                // multiple callbacks in the method
                                                for (int j = 0; j < argtypes.length; j++) {
                                                    Class<?> argclazz = argtypes[j];
                                                    if (argclazz.getName().equals(argument.getType())) {
                                                        map.putAll(ConfigConverter.configToMap(argument, method.getName() + "." + j));
                                                        if (argument.getIndex() != -1 && argument.getIndex() != j) {
                                                            throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (argument.getIndex() != -1) {
                                map.putAll(ConfigConverter.configToMap(argument, method.getName() + "." + argument.getIndex()));
                            } else {
                                throw new IllegalArgumentException("argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                            }

                        }
                    }
                } // end of methods for
            }

            if (ProtocolUtils.isGeneric(generic)) {
                map.put(Constants.GENERIC_KEY, generic);
                map.put(Constants.METHODS_KEY, Constants.ANY_VALUE);
            } else {
                String revision = Version.getVersion(interfaceClass, version);
                if (revision != null && revision.length() > 0) {
                    map.put("revision", revision);
                }

                String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
                if (methods.length == 0) {
                    logger.warn("NO method found in service interface " + interfaceClass.getName());
                    map.put(Constants.METHODS_KEY, Constants.ANY_VALUE);
                } else {
                    map.put(Constants.METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
                }
            }
            if (!ConfigUtils.isEmpty(token)) {
                if (ConfigUtils.isDefault(token)) {
                    map.put(Constants.TOKEN_KEY, UUID.randomUUID().toString());
                } else {
                    map.put(Constants.TOKEN_KEY, token);
                }
            }
            if (Constants.LOCAL_PROTOCOL.equals(protocolConfig.getName())) {
                protocolConfig.setRegister(false);
                map.put("notify", "false");
            }
            // export service
            String contextPath = protocolConfig.getContextpath();
            if ((contextPath == null || contextPath.length() == 0) && provider != null) {
                contextPath = provider.getContextpath();
            }

            String host = this.findConfigedHosts(protocolConfig, registryURLs, map);
            Integer port = this.findConfigedPorts(protocolConfig, map);
            return new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void exportLocal(URL url) {
            if (!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                URL local = URL.valueOf(url.toFullString())
                        .setProtocol(Constants.LOCAL_PROTOCOL)
                        .setHost(LOCALHOST)
                        .setPort(0);
                ServiceClassHolder.getInstance().pushServiceClass(getServiceClass(ref));
                Exporter<?> exporter = protocol.export(
                        proxyFactory.getInvoker(ref, (Class) interfaceClass, local));
                exporters.add(exporter);
                logger.info("Export dubbo service " + interfaceClass.getName() + " to local registry");
            }
        }

        /**
         * Register & bind IP address for service provider, can be configured separately.
         * Configuration priority: context variables -> java system properties -> host property in config file ->
         * /etc/hosts -> default network address -> first available network address
         *
         * @param protocolConfig
         * @param registryURLs
         * @param map
         * @return
         */
        private String findConfigedHosts(ProtocolConfig protocolConfig, List<URL> registryURLs, Map<String, String> map) {
            boolean anyhost = false;

            String hostToBind = getValueFromConfig(protocolConfig, Constants.DUBBO_IP_TO_BIND);
            if (hostToBind != null && hostToBind.length() > 0 && isInvalidLocalHost(hostToBind)) {
                throw new IllegalArgumentException("Specified invalid bind ip from property:" + Constants.DUBBO_IP_TO_BIND + ", value:" + hostToBind);
            }

            // if bind ip is not found in context, keep looking up
            if (hostToBind == null || hostToBind.length() == 0) {
                hostToBind = protocolConfig.getHost();
                if (provider != null && (hostToBind == null || hostToBind.length() == 0)) {
                    hostToBind = provider.getHost();
                }
                if (isInvalidLocalHost(hostToBind)) {
                    anyhost = true;
                    try {
                        hostToBind = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        logger.warn(e.getMessage(), e);
                    }
                    if (isInvalidLocalHost(hostToBind)) {
                        if (registryURLs != null && !registryURLs.isEmpty()) {
                            for (URL registryURL : registryURLs) {
                                if (Constants.MULTICAST.equalsIgnoreCase(registryURL.getParameter("registry"))) {
                                    // skip multicast registry since we cannot connect to it via Socket
                                    continue;
                                }
                                try {
                                    Socket socket = new Socket();
                                    try {
                                        SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                        socket.connect(addr, 1000);
                                        hostToBind = socket.getLocalAddress().getHostAddress();
                                        break;
                                    } finally {
                                        try {
                                            socket.close();
                                        } catch (Throwable e) {
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.warn(e.getMessage(), e);
                                }
                            }
                        }
                        if (isInvalidLocalHost(hostToBind)) {
                            hostToBind = getLocalHost();
                        }
                    }
                }
            }

            map.put(Constants.BIND_IP_KEY, hostToBind);

            // registry ip is not used for bind ip by default
            String hostToRegistry = getValueFromConfig(protocolConfig, Constants.DUBBO_IP_TO_REGISTRY);
            if (hostToRegistry != null && hostToRegistry.length() > 0 && isInvalidLocalHost(hostToRegistry)) {
                throw new IllegalArgumentException("Specified invalid registry ip from property:" + Constants.DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
            } else if (hostToRegistry == null || hostToRegistry.length() == 0) {
                // bind ip is used as registry ip by default
                hostToRegistry = hostToBind;
            }

            map.put(Constants.ANYHOST_KEY, String.valueOf(anyhost));

            return hostToRegistry;
        }

        /**
         * Register port and bind port for the provider, can be configured separately
         * Configuration priority: context variable -> java system properties -> port property in protocol config file
         * -> protocol default port
         *
         * @param protocolConfig
         * @return
         */
        private Integer findConfigedPorts(ProtocolConfig protocolConfig, Map<String, String> map) {
            String name = protocolConfig.getName();
            Integer portToBind = null;

            // parse bind port from context
            String port = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_BIND);
            portToBind = parsePort(port);

            // if there's no bind port found from context, keep looking up.
            if (portToBind == null) {
                portToBind = protocolConfig.getPort();
                if (provider != null && (portToBind == null || portToBind == 0)) {
                    portToBind = provider.getPort();
                }
                final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
                if (portToBind == null || portToBind == 0) {
                    portToBind = defaultPort;
                }
                if (portToBind == null || portToBind <= 0) {
                    portToBind = getRandomPort(name);
                    if (portToBind == null || portToBind < 0) {
                        portToBind = getAvailablePort(defaultPort);
                        putRandomPort(name, portToBind);
                    }
                    logger.warn("Use random available port(" + portToBind + ") for protocol " + name);
                }
            }

            // save bind port, used as url's key later
            map.put(Constants.BIND_PORT_KEY, String.valueOf(portToBind));

            // registry port, not used as bind port by default
            String portToRegistryStr = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_REGISTRY);
            Integer portToRegistry = parsePort(portToRegistryStr);
            if (portToRegistry == null) {
                portToRegistry = portToBind;
            }

            return portToRegistry;
        }

        private Integer parsePort(String configPort) {
            Integer port = null;
            if (configPort != null && configPort.length() > 0) {
                try {
                    Integer intPort = Integer.parseInt(configPort);
                    if (isInvalidPort(intPort)) {
                        throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                    }
                    port = intPort;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                }
            }
            return port;
        }

        private String getValueFromConfig(ProtocolConfig protocolConfig, String key) {
            String protocolPrefix = protocolConfig.getName().toUpperCase() + "_";
            String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
            if (port == null || port.length() == 0) {
                port = ConfigUtils.getSystemProperty(key);
            }
            return port;
        }

        private static Integer getRandomPort(String protocol) {
            protocol = protocol.toLowerCase();
            if (RANDOM_PORT_MAP.containsKey(protocol)) {
                return RANDOM_PORT_MAP.get(protocol);
            }
            return Integer.MIN_VALUE;
        }

        private static void putRandomPort(String protocol, Integer port) {
            protocol = protocol.toLowerCase();
            if (!RANDOM_PORT_MAP.containsKey(protocol)) {
                RANDOM_PORT_MAP.put(protocol, port);
            }
        }
    }

}
