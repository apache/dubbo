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
package org.apache.dubbo.config;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ClassHelper;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.invoker.DelegateProviderMetaDataInvoker;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.ServiceClassHolder;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
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
 * ServiceConfig
 *
 * <pre>
 *     我们以 ServiceConfig 和 ProviderConfig 来举例子，两者都继承 AbstractServiceConfig。
 * 从属性上，两者有相同的属性，例如 group / version 。
 * 同时，也存在着一些差异，例如 ServiceConfig.interfaceName / ProviderConfig.host 。
 * 另外，我们在看看 ServiceConfig 和 MethodConfig ，两者都继承 AbstractMethodConfig。
 * 在 ServiceConfig 中，可以配置下属所有方法的 retries 次数，也可以在 MethodConfig 中自定义 retries 次数。
 * 通过继承，获得相同的属性。
 * </pre>
 * <p>
 * 服务提供者 暴露服务配置类
 *
 * @export
 */
public class ServiceConfig<T> extends AbstractServiceConfig {

    private static final long serialVersionUID = 3033787999037024738L;
    /**
     * 自适应 Protocol 实现对象
     */
    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    /**
     * 自适应 ProxyFactory 实现对象
     * Adaptive 这个东西非常关键
     */
    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    /**
     * 协议名对应生成的随机端口
     * <p>
     * key ：协议名 value ：端口
     */
    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>();
    /**
     * 延迟暴露执行器
     */
    private static final ScheduledExecutorService delayExportExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));
    /**
     * 服务配置对应的 Dubbo URL 数组
     * <p>
     * 非配置。
     */
    private final List<URL> urls = new ArrayList<URL>();
    /**
     * <pre>
     * 服务配置暴露的 Exporter 。
     * URL ：Exporter 不一定是 1：1 的关系。
     * 例如 {@link #scope} 未设置时，会暴露 Local + Remote 两个，也就是 URL ：Exporter = 1：2
     *      {@link #scope} 设置为空时，不会暴露，也就是 URL ：Exporter = 1：0
     *      {@link #scope} 设置为 Local 或 Remote 任一时，会暴露 Local 或 Remote 一个，也就是 URL ：Exporter = 1：1
     *
     * 非配置。
     * </pre>
     */
    private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();
    /**
     * interface type
     */
    private String interfaceName;
    /**
     * {@link #interfaceName} 对应的接口类
     * <p>
     * 非配置
     */
    private Class<?> interfaceClass;
    /**
     * Service 对象 reference to interface impl
     */
    private T ref;
    /**
     * service name
     */
    private String path;
    /**
     * method configuration
     */
    private List<MethodConfig> methods;
    private ProviderConfig provider;
    /**
     * 是否已经暴露服务，参见 {@link #doExport()} 方法。
     * <p>
     * 非配置。
     */
    private transient volatile boolean exported;
    /**
     * 是否已取消暴露服务，参见 {@link #unexport()} 方法。
     * <p>
     * 非配置。
     */
    private transient volatile boolean unexported;
    /**
     * 是否泛化实现，参见 <a href="https://dubbo.gitbooks.io/dubbo-user-book/demos/generic-service.html">实现泛化调用</a>
     * true / false
     * <p>
     * 状态字段，非配置。
     */
    private volatile String generic;

    public ServiceConfig() {
    }

    public ServiceConfig(Service service) {
        appendAnnotation(Service.class, service);
    }

    @Deprecated
    private static List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }
        return protocols;
    }

    @Deprecated
    private static List<ProviderConfig> convertProtocolToProvider(List<ProtocolConfig> protocols) {
        if (protocols == null || protocols.isEmpty()) {
            return null;
        }
        List<ProviderConfig> providers = new ArrayList<ProviderConfig>(protocols.size());
        for (ProtocolConfig provider : protocols) {
            providers.add(convertProtocolToProvider(provider));
        }
        return providers;
    }

    @Deprecated
    private static ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName(provider.getProtocol().getName());
        protocol.setServer(provider.getServer());
        protocol.setClient(provider.getClient());
        protocol.setCodec(provider.getCodec());
        protocol.setHost(provider.getHost());
        protocol.setPort(provider.getPort());
        protocol.setPath(provider.getPath());
        protocol.setPayload(provider.getPayload());
        protocol.setThreads(provider.getThreads());
        protocol.setParameters(provider.getParameters());
        return protocol;
    }

    @Deprecated
    private static ProviderConfig convertProtocolToProvider(ProtocolConfig protocol) {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol(protocol);
        provider.setServer(protocol.getServer());
        provider.setClient(protocol.getClient());
        provider.setCodec(protocol.getCodec());
        provider.setHost(protocol.getHost());
        provider.setPort(protocol.getPort());
        provider.setPath(protocol.getPath());
        provider.setPayload(protocol.getPayload());
        provider.setThreads(protocol.getThreads());
        provider.setParameters(protocol.getParameters());
        return provider;
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

    public URL toUrl() {
        return urls.isEmpty() ? null : urls.iterator().next();
    }

    public List<URL> toUrls() {
        return urls;
    }

    @Parameter(excluded = true)
    public boolean isExported() {
        return exported;
    }

    @Parameter(excluded = true)
    public boolean isUnexported() {
        return unexported;
    }

    public synchronized void export() {
        //当export或者delay 未配置 从providerConfig 对象读取
        if (provider != null) {
            if (export == null) {
                export = provider.getExport();
            }
            if (delay == null) {
                delay = provider.getDelay();
            }
        }
        //不暴露服务 则不进行暴露服务逻辑
        if (export != null && !export) {
            return;
        }
        //延迟暴露
        if (delay != null && delay > 0) {
            delayExportExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    doExport();
                }
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            //立即暴露
            doExport();
        }
    }

    protected synchronized void doExport() {
        //检查是否可以暴露 若可以 标记已经暴露
        if (unexported) {
            throw new IllegalStateException("Already unexported!");
        }
        if (exported) {
            return;
        }
        exported = true;
        //校验接口名非空
        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
        }
        //拼接属性配置（环境变量 + properties 属性）到 ProviderConfig 对象
        checkDefault();
        //从ProviderConfig对象中读取配置对象
        if (provider != null) {
            if (application == null) {
                application = provider.getApplication();
            }
            if (module == null) {
                module = provider.getModule();
            }
            if (registries == null) {
                registries = provider.getRegistries();
            }
            if (monitor == null) {
                monitor = provider.getMonitor();
            }
            if (protocols == null) {
                protocols = provider.getProtocols();
            }
        }
        //从moduleConfig 对象中读取配置对象
        if (module != null) {
            if (registries == null) {
                registries = module.getRegistries();
            }
            if (monitor == null) {
                monitor = module.getMonitor();
            }
        }
        if (application != null) {
            if (registries == null) {
                registries = application.getRegistries();
            }
            if (monitor == null) {
                monitor = application.getMonitor();
            }
        }
        //泛化接口的实现
        if (ref instanceof GenericService) {
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                generic = Boolean.TRUE.toString();
            }
        } else {
            //普通接口的实现
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            //校验接口和方法
            checkInterfaceAndMethods(interfaceClass, methods);
            //校验指向的service对象
            checkRef();
            generic = Boolean.FALSE.toString();
        }
        // 处理服务接口客户端本地代理( `local` )相关。实际目前已经废弃，使用 `stub` 属性，参见 `AbstractInterfaceConfig#setLocal` 方法。
        if (local != null) {
            // 设为 true，表示使用缺省代理类名，即：接口名 + Local 后缀
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
        //处理服务接口客户端本地代理 stub 相关
        if (stub != null) {
            // 设为 true，表示使用缺省代理类名，即：接口名 + Stub 后缀
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

        //校验ApplicationConfig配置
        checkApplication();
        //校验RegistryConfig配置
        checkRegistry();
        //校验ProtocolConfig配置数组
        checkProtocol();
        //读取环境变量和properties配置到ServiceConfig对象
        appendProperties(this);
        //校验stub和mock相关配置
        checkStubAndMock(interfaceClass);
        //服务路径 缺省接口名
        if (path == null || path.length() == 0) {
            path = interfaceName;
        }
        //暴露服务
        doExportUrls();
        ProviderModel providerModel = new ProviderModel(getUniqueServiceName(), ref, interfaceClass);
        ApplicationModel.initProviderModel(getUniqueServiceName(), providerModel);
    }

    private void checkRef() {
        // reference should not be null, and is the implementation of the given interface
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        if (!interfaceClass.isInstance(ref)) {
            throw new IllegalStateException("The class "
                    + ref.getClass().getName() + " unimplemented interface "
                    + interfaceClass + "!");
        }
    }

    public synchronized void unexport() {
        if (!exported) {
            return;
        }
        if (unexported) {
            return;
        }
        if (!exporters.isEmpty()) {
            for (Exporter<?> exporter : exporters) {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn("unexpected err when unexport" + exporter, t);
                }
            }
            exporters.clear();
        }
        unexported = true;
    }

    /**
     * 暴露 Dubbo URL
     *
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doExportUrls() {
        // 加载注册中心 URL 数组
        List<URL> registryURLs = loadRegistries(true);
        // 循环 `protocols` ，向逐个注册中心分组暴露服务。
        for (ProtocolConfig protocolConfig : protocols) {
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }

    /**
     * 基于单个协议，暴露服务
     * <pre>
     *     为什么会有本地暴露和远程暴露呢?不从场景考虑讨论技术的没有意义是.在dubbo中我们一个服务可能既是Provider,又是Consumer,
     *     因此就存在他自己调用自己服务的情况,如果再通过网络去访问,那自然是舍近求远,
     *     因此他是有本地暴露服务的这个设计.从这里我们就知道这个两者的区别
     *
     * 本地暴露是暴露在JVM中,不需要网络通信.
     * 远程暴露是将ip,端口等信息暴露给远程客户端,调用时需要网络通信.
     *
     * </pre>
     *
     * @param protocolConfig 协议配置对象
     * @param registryURLs   注册中心链接对象数组
     */
    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        //获取协议名
        String name = protocolConfig.getName();
        if (name == null || name.length() == 0) {
            name = "dubbo";
        }

        Map<String, String> map = new HashMap<String, String>();
        //将side,dubbo版本号,timestamp,pid 参数添加到map集合中
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
        map.put(Constants.DUBBO_VERSION_KEY, Version.getProtocolVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        //将各种配置对象 添加到map集合中
        appendParameters(map, application);
        appendParameters(map, module);
        appendParameters(map, provider, Constants.DEFAULT_KEY);
        appendParameters(map, protocolConfig);
        appendParameters(map, this);
        if (methods != null && !methods.isEmpty()) {
            //将methodConfig对象数组添加到map集合中
            for (MethodConfig method : methods) {
                //将methodConfig对象 添加到map集合中
                appendParameters(map, method, method.getName());

                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    //当配置了MethodConfig.retry = false 时 强制禁用重试
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                //将 ArgumentConfig 对象数组 添加到map集合中
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
                                                appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                            } else {
                                                throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else {
                                            // multiple callbacks in the method
                                            for (int j = 0; j < argtypes.length; j++) {
                                                Class<?> argclazz = argtypes[j];
                                                if (argclazz.getName().equals(argument.getType())) {
                                                    appendParameters(map, argument, method.getName() + "." + j);
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j) {
                                                        throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (argument.getIndex() != -1) { //指定单个参数的位置
                            //将ArgumentConfig 对象 添加到map集合中
                            appendParameters(map, argument, method.getName() + "." + argument.getIndex()); //${methodName}.${index}
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
                map.put("revision", revision); //修订号
            }
            //获取所有的方法
            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put(Constants.METHODS_KEY, Constants.ANY_VALUE);
            } else {
                map.put(Constants.METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        // token ，参见《令牌校验》https://dubbo.gitbooks.io/dubbo-user-book/demos/token-authorization.html
        if (!ConfigUtils.isEmpty(token)) {
            if (ConfigUtils.isDefault(token)) {
                map.put(Constants.TOKEN_KEY, UUID.randomUUID().toString());
            } else {
                map.put(Constants.TOKEN_KEY, token);
            }
        }
        //协议为 injvm 时 不注册 不通知
        if (Constants.LOCAL_PROTOCOL.equals(protocolConfig.getName())) {
            protocolConfig.setRegister(false);
            map.put("notify", "false");
        }
        // export service
        String contextPath = protocolConfig.getContextpath();
        if ((contextPath == null || contextPath.length() == 0) && provider != null) {
            contextPath = provider.getContextpath();
        }
        //host port 获取IP 和 端口
        String host = this.findConfigedHosts(protocolConfig, registryURLs, map);
        Integer port = this.findConfigedPorts(protocolConfig, name, map);
        //创建dubbo URL对象
        URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);
        // 配置规则，参见《配置规则》https://dubbo.gitbooks.io/dubbo-user-book/demos/config-rule.html
        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }
        //////////////////////////////////////以上为创建服务的URL对象
        String scope = url.getParameter(Constants.SCOPE_KEY);
        // don't export when none is configured
        if (!Constants.SCOPE_NONE.equalsIgnoreCase(scope)) {

            // export to local if the config is not remote (export to remote only when config is remote)
            //当非 "remote" 时，调用 #exportLocal(url) 方法，本地暴露服务
            if (!Constants.SCOPE_REMOTE.equalsIgnoreCase(scope)) {
                exportLocal(url);
            }
            // 服务远程暴露
            // export to remote if the config is not local (export to local only when config is local)
            if (!Constants.SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                if (logger.isInfoEnabled()) {
                    logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                }
                if (registryURLs != null && !registryURLs.isEmpty()) {
                    for (URL registryURL : registryURLs) {
                        // "dynamic" ：服务是否动态注册，如果设为false，注册后将显示后disable状态，需人工启用，并且服务提供者停止时，也不会自动取消册，需人工禁用。
                        url = url.addParameterIfAbsent(Constants.DYNAMIC_KEY, registryURL.getParameter(Constants.DYNAMIC_KEY));
                        // 获得监控中心 URL
                        URL monitorUrl = loadMonitor(registryURL);
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
                        // 使用 ProxyFactory 创建 Invoker 对象
                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                        // 创建 DelegateProviderMetaDataInvoker 对象
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);
                        // 使用 Protocol 暴露 Invoker 对象
                        Exporter<?> exporter = protocol.export(wrapperInvoker);
                        // 添加到 `exporters`
                        exporters.add(exporter);
                    }
                } else {
                    // 用于被服务消费者直连服务提供者，参见文档 https://dubbo.gitbooks.io/dubbo-user-book/demos/explicit-target.html 。主要用于开发测试环境使用。
                    // 使用 ProxyFactory 创建 Invoker 对象
                    Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);
                    // 创建 DelegateProviderMetaDataInvoker 对象
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);
                    // 使用 Protocol 暴露 Invoker 对象
                    Exporter<?> exporter = protocol.export(wrapperInvoker);
                    // 添加到 `exporters`
                    exporters.add(exporter);
                }
            }
        }
        this.urls.add(url);
    }

    /**
     * 本地暴露服务
     *
     * @param url 注册中心 URL
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void exportLocal(URL url) {
        if (!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            //基于原有 url ，创建新的服务的本地 Dubbo URL 对象，并设置属性 protocol=injvm host=127.0.0.1 port=0 。因为 url 在后面远程暴露服务会使用，所以要新创建。
            URL local = URL.valueOf(url.toFullString())
                    .setProtocol(Constants.LOCAL_PROTOCOL) //injvm
                    .setHost(LOCALHOST) //本地
                    .setPort(0); //端口=0
            // 添加服务的真实类名，例如 DemoServiceImpl ，仅用于 RestProtocol 中。
            ServiceClassHolder.getInstance().pushServiceClass(getServiceClass(ref));
            // 使用 ProxyFactory 创建 Invoker 对象
            // 使用 Protocol 暴露 Invoker 对象
            // 1.此处 Dubbo SPI 自适应的特性的好处就出来了，可以自动根据 URL 参数，获得对应的拓展实现。
            // 例如，invoker 传入后，根据 invoker.url 自动获得对应 Protocol 拓展实现为 InjvmProtocol 。
            // 2.实际上，Protocol 有两个 Wrapper 拓展实现类： ProtocolFilterWrapper、ProtocolListenerWrapper 。
            // 所以，#export(...) 方法的调用顺序是：Protocol$Adaptive => ProtocolFilterWrapper => ProtocolListenerWrapper => InjvmProtocol 。
            Exporter<?> exporter = protocol.export(
                    proxyFactory.getInvoker(ref, (Class) interfaceClass, local));
            // 添加到 `exporters`
            exporters.add(exporter);
            logger.info("Export dubbo service " + interfaceClass.getName() + " to local registry");
        }
    }

    protected Class getServiceClass(T ref) {
        return ref.getClass();
    }

    /**
     * 查找主机 Host ，参见文档《主机绑定》https://dubbo.gitbooks.io/dubbo-user-book/demos/hostname-binding.html
     *
     * 推荐阅读文章《dubbo注册服务IP解析异常及IP解析源码分析》 https://segmentfault.com/a/1190000010550512
     *
     * Register & bind IP address for service provider, can be configured separately.
     * Configuration priority: environment variables -> java system properties -> host property in config file ->
     * /etc/hosts -> default network address -> first available network address
     *
     * @param protocolConfig 协议配置对象
     * @param registryURLs 注册中心 URL 数组
     * @param map 参数集合
     * @return
     */
    private String findConfigedHosts(ProtocolConfig protocolConfig, List<URL> registryURLs, Map<String, String> map) {
        boolean anyhost = false;

        // 第一优先级，从环境变量，获得绑定的 Host 。可强制指定，参见仓库 https://github.com/dubbo/dubbo-docker-sample
        String hostToBind = getValueFromConfig(protocolConfig, Constants.DUBBO_IP_TO_BIND);
        if (hostToBind != null && hostToBind.length() > 0 && isInvalidLocalHost(hostToBind)) { // 若是非法的本地 Host 抛出异常
            throw new IllegalArgumentException("Specified invalid bind ip from property:" + Constants.DUBBO_IP_TO_BIND + ", value:" + hostToBind);
        }

        // if bind ip is not found in environment, keep looking up
        if (hostToBind == null || hostToBind.length() == 0) {
            // 第二优先级，从 ProtocolConfig 获得 Host 。
            hostToBind = protocolConfig.getHost();
            if (provider != null && (hostToBind == null || hostToBind.length() == 0)) {
                hostToBind = provider.getHost();
            }
            // 第三优先级，若非合法的本地 Host ，使用 InetAddress.getLocalHost().getHostAddress() 获得 Host
            if (isInvalidLocalHost(hostToBind)) {
                anyhost = true;
                try {
                    hostToBind = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.warn(e.getMessage(), e);
                }
                // 第四优先级，若是非法的本地 Host ，通过使用 `registryURLs` 启动 Server ，并本地连接，获得 Host 。
                if (isInvalidLocalHost(hostToBind)) {
                    if (registryURLs != null && !registryURLs.isEmpty()) {
                        for (URL registryURL : registryURLs) {
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
                    // 第五优先级，若是非法的本地 Host ，获得本地网卡，第一个合法的 IP 。
                    if (isInvalidLocalHost(hostToBind)) {
                        hostToBind = getLocalHost();
                    }
                }
            }
        }

        map.put(Constants.BIND_IP_KEY, hostToBind);

        // 获得 `hostToRegistry` ，默认使用 `hostToBind` 。可强制指定，参见仓库 https://github.com/dubbo/dubbo-docker-sample
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
     * 查找端口，参见文档《主机绑定》https://dubbo.gitbooks.io/dubbo-user-book/demos/hostname-binding.html
     *
     * Register port and bind port for the provider, can be configured separately
     * Configuration priority: environment variable -> java system properties -> port property in protocol config file
     * -> protocol default port
     *
     * @param protocolConfig 协议配置对象
     * @param name 协议名
     * @return 端口
     */
    private Integer findConfigedPorts(ProtocolConfig protocolConfig, String name, Map<String, String> map) {
        // 第一优先级，从环境变量，获得绑定的 Port 。可强制指定，参见仓库 https://github.com/dubbo/dubbo-docker-sample
        // parse bind port from environment
        String port = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_BIND);
        Integer portToBind = parsePort(port);

        // if there's no bind port found from environment, keep looking up.
        if (portToBind == null) {
            // 第二优先级，从 ProtocolConfig 获得 Port 。
            portToBind = protocolConfig.getPort();
            if (provider != null && (portToBind == null || portToBind == 0)) {
                portToBind = provider.getPort();
            }
            // 第三优先级，获得协议对应的缺省端口，
            final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
            if (portToBind == null || portToBind == 0) {
                portToBind = defaultPort;
            }
            // 第四优先级，随机获得端口
            if (portToBind <= 0) {
                portToBind = getRandomPort(name); // 先从缓存中获得端口
                if (portToBind == null || portToBind < 0) {
                    // 获得可用端口
                    portToBind = getAvailablePort(defaultPort);
                    // 添加到缓存
                    putRandomPort(name, portToBind);
                }
                logger.warn("Use random available port(" + portToBind + ") for protocol " + name);
            }
        }

        // save bind port, used as url's key later
        map.put(Constants.BIND_PORT_KEY, String.valueOf(portToBind));

        // 获得 `portToRegistry` ，默认使用 `portToBind` 。可强制指定，参见仓库 https://github.com/dubbo/dubbo-docker-sample
        // registry port, not used as bind port by default
        String portToRegistryStr = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_REGISTRY);
        Integer portToRegistry = parsePort(portToRegistryStr);
        if (portToRegistry == null) {
            portToRegistry = portToBind;
        }

        return portToRegistry;
    }

    /**
     * 解析端口字符串
     *
     * @param configPort 端口字符串
     * @return 端口
     */
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

    /**
     * 从协议配置对象解析对应的配置项
     *
     * @param protocolConfig 协议配置对象
     * @param key 配置项
     * @return 值
     */
    private String getValueFromConfig(ProtocolConfig protocolConfig, String key) {
        String protocolPrefix = protocolConfig.getName().toUpperCase() + "_";
        String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
        if (port == null || port.length() == 0) {
            port = ConfigUtils.getSystemProperty(key);
        }
        return port;
    }

    /**
     * 校验 ProviderConfig 配置。
     * 实际上，会拼接属性配置（环境变量 + properties 属性）到 ProviderConfig 对象。
     */
    private void checkDefault() {
        if (provider == null) {
            provider = new ProviderConfig();
        }
        appendProperties(provider);
    }

    /**
     * 校验 ProtocolConfig 配置数组。
     * 实际上，会拼接属性配置（环境变量 + properties 属性）到 ProtocolConfig 对象数组。
     */
    private void checkProtocol() {
        // 当 ProtocolConfig 对象数组为空时，优先使用 `ProviderConfig.protocols` 。其次，进行创建。
        if ((protocols == null || protocols.isEmpty())
                && provider != null) {
            setProtocols(provider.getProtocols());
        }
        // backward compatibility 向后兼容
        if (protocols == null || protocols.isEmpty()) {
            setProtocol(new ProtocolConfig());
        }
        // 拼接属性配置（环境变量 + properties 属性）到 ProtocolConfig 对象数组
        for (ProtocolConfig protocolConfig : protocols) {
            if (StringUtils.isEmpty(protocolConfig.getName())) {
                protocolConfig.setName("dubbo");
            }
            appendProperties(protocolConfig);
        }
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (ref instanceof GenericService) {
            return GenericService.class;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                this.interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }
        return interfaceClass;
    }

    /**
     * @see #setInterface(Class)
     * @deprecated
     */
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (id == null || id.length() == 0) {
            id = interfaceName;
        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    @Parameter(excluded = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        checkPathName(Constants.PATH_KEY, path);
        this.path = path;
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    // ======== Deprecated ========

    @SuppressWarnings("unchecked")
    public void setMethods(List<? extends MethodConfig> methods) {
        this.methods = (List<MethodConfig>) methods;
    }

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }

    public String getGeneric() {
        return generic;
    }

    public void setGeneric(String generic) {
        if (StringUtils.isEmpty(generic)) {
            return;
        }
        if (ProtocolUtils.isGeneric(generic)) {
            this.generic = generic;
        } else {
            throw new IllegalArgumentException("Unsupported generic type " + generic);
        }
    }

    public List<URL> getExportedUrls() {
        return urls;
    }

    /**
     * @deprecated Replace to getProtocols()
     */
    @Deprecated
    public List<ProviderConfig> getProviders() {
        return convertProtocolToProvider(protocols);
    }

    /**
     * @deprecated Replace to setProtocols()
     */
    @Deprecated
    public void setProviders(List<ProviderConfig> providers) {
        this.protocols = convertProviderToProtocol(providers);
    }

    @Parameter(excluded = true)
    public String getUniqueServiceName() {
        StringBuilder buf = new StringBuilder();
        if (group != null && group.length() > 0) {
            buf.append(group).append("/");
        }
        buf.append(interfaceName);
        if (version != null && version.length() > 0) {
            buf.append(":").append(version);
        }
        return buf.toString();
    }
}
