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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.model.ApplicationModel;
import com.alibaba.dubbo.config.model.ConsumerModel;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.StaticContext;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.support.AvailableCluster;
import com.alibaba.dubbo.rpc.cluster.support.ClusterUtils;
import com.alibaba.dubbo.rpc.protocol.injvm.InjvmProtocol;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.alibaba.dubbo.common.utils.NetUtils.isInvalidLocalHost;

/**
 * ReferenceConfig
 *
 * 服务消费者引用服务配置。
 * 参数详细：https://dubbo.gitbooks.io/dubbo-user-book/references/xml/dubbo-reference.html
 *
 * @export
 */
public class ReferenceConfig<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = -5864351140409987595L;

    /**
     * 自适应 Protocol 实现对象
     */
    private static final Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

    /**
     * 自适应 ProxyFactory 实现对象
     */
    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    /**
     * 服务引用 URL 数组
     */
    private final List<URL> urls = new ArrayList<URL>();
    // interface name
    private String interfaceName;
    /**
     * {@link #interfaceName} 对应的接口类
     *
     * 非配置
     */
    private Class<?> interfaceClass;
    // client type
    private String client;
    /**
     * 直连服务地址
     *
     * 1. 可以是注册中心，也可以是服务提供者
     * 2. 可配置多个，使用 ; 分隔
     */
    // url for peer-to-peer invocation
    private String url;
    // method configs
    private List<MethodConfig> methods; // TODO 芋艿
    // default config
    private ConsumerConfig consumer;
    private String protocol;
    // interface proxy reference
    /**
     * Service 对象
     */
    private transient volatile T ref;
    private transient volatile Invoker<?> invoker; // TODO 芋艿
    /**
     * 是否已经初始化应用服务，参见 {@link #init()} 方法。
     *
     * 非配置。
     */
    private transient volatile boolean initialized;
    /**
     * 是否已经销毁引用服务，参见 {@link #destroy()} 方法。
     *
     * 非配置。
     */
    private transient volatile boolean destroyed;

    @SuppressWarnings("unused")
    private final Object finalizerGuardian = new Object() {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            if (!ReferenceConfig.this.destroyed) {
                logger.warn("ReferenceConfig(" + url + ") is not DESTROYED when FINALIZE");

                /* don't destroy for now
                try {
                    ReferenceConfig.this.destroy();
                } catch (Throwable t) {
                        logger.warn("Unexpected err when destroy invoker of ReferenceConfig(" + url + ") in finalize method!", t);
                }
                */
            }
        }
    };

    public ReferenceConfig() {
    }

    public ReferenceConfig(Reference reference) {
        appendAnnotation(Reference.class, reference);
    }

    /**
     * 检查属性集合中的事件通知方法是否正确。
     * 因为，此时方法配置的是字符串，需要通过反射获得 Method ，并添加到 attributes 。其中，键为 {@link StaticContext#getKey(Map, String, String)} 方法获取。
     *
     * @param method 方法配置对象
     * @param map 参数集合
     * @param attributes 属性集合
     */
    private static void checkAndConvertImplicitConfig(MethodConfig method, Map<String, String> map, Map<Object, Object> attributes) {
        //check config conflict
        if (Boolean.FALSE.equals(method.isReturn()) && (method.getOnreturn() != null || method.getOnthrow() != null)) {
            throw new IllegalStateException("method config error : return attribute must be set true when onreturn or onthrow has been setted.");
        }
        // onreturn：将方法名字符串转换成方法
        //convert onreturn methodName to Method
        String onReturnMethodKey = StaticContext.getKey(map, method.getName(), Constants.ON_RETURN_METHOD_KEY);
        Object onReturnMethod = attributes.get(onReturnMethodKey);
        if (onReturnMethod != null && onReturnMethod instanceof String) {
            attributes.put(onReturnMethodKey, getMethodByName(method.getOnreturn().getClass(), onReturnMethod.toString()));
        }
        // onthrow：将方法名字符串转换成方法
        //convert onthrow methodName to Method
        String onThrowMethodKey = StaticContext.getKey(map, method.getName(), Constants.ON_THROW_METHOD_KEY);
        Object onThrowMethod = attributes.get(onThrowMethodKey);
        if (onThrowMethod != null && onThrowMethod instanceof String) {
            attributes.put(onThrowMethodKey, getMethodByName(method.getOnthrow().getClass(), onThrowMethod.toString()));
        }
        // oninvoke：将方法名字符串转换成方法
        //convert oninvoke methodName to Method
        String onInvokeMethodKey = StaticContext.getKey(map, method.getName(), Constants.ON_INVOKE_METHOD_KEY);
        Object onInvokeMethod = attributes.get(onInvokeMethodKey);
        if (onInvokeMethod != null && onInvokeMethod instanceof String) {
            attributes.put(onInvokeMethodKey, getMethodByName(method.getOninvoke().getClass(), onInvokeMethod.toString()));
        }
    }

    private static Method getMethodByName(Class<?> clazz, String methodName) {
        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public URL toUrl() {
        return urls == null || urls.isEmpty() ? null : urls.iterator().next();
    }

    public List<URL> toUrls() {
        return urls;
    }

    public synchronized T get() {
        // 已销毁，不可获得
        if (destroyed) {
            throw new IllegalStateException("Already destroyed!");
        }
        // 初始化
        if (ref == null) {
            init();
        }
        return ref;
    }

    public synchronized void destroy() {
        if (ref == null) {
            return;
        }
        if (destroyed) {
            return;
        }
        destroyed = true;
        try {
            invoker.destroy();
        } catch (Throwable t) {
            logger.warn("Unexpected err when destroy invoker of ReferenceConfig(" + url + ").", t);
        }
        invoker = null;
        ref = null;
    }

    private void init() {
        // 已经初始化，直接返回
        if (initialized) {
            return;
        }
        initialized = true;
        // 校验接口名非空
        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("<dubbo:reference interface=\"\" /> interface not allow null!");
        }
        // 拼接属性配置（环境变量 + properties 属性）到 ConsumerConfig 对象
        // get consumer's global configuration
        checkDefault();
        // 拼接属性配置（环境变量 + properties 属性）到 ReferenceConfig 对象
        appendProperties(this);
        // 若未设置 `generic` 属性，使用 `ConsumerConfig.generic` 属性。
        if (getGeneric() == null && getConsumer() != null) {
            setGeneric(getConsumer().getGeneric());
        }
        // 泛化接口的实现
        if (ProtocolUtils.isGeneric(getGeneric())) {
            interfaceClass = GenericService.class;
        // 普通接口的实现
        } else {
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            // 校验接口和方法
            checkInterfaceAndMethods(interfaceClass, methods);
        }
        // 直连提供者，参见文档《直连提供者》https://dubbo.gitbooks.io/dubbo-user-book/demos/explicit-target.html
        // 【直连提供者】第一优先级，通过 -D 参数指定 ，例如 java -Dcom.alibaba.xxx.XxxService=dubbo://localhost:20890
        String resolve = System.getProperty(interfaceName);
        String resolveFile = null;
        // 【直连提供者】第二优先级，通过文件映射，例如 com.alibaba.xxx.XxxService=dubbo://localhost:20890
        if (resolve == null || resolve.length() == 0) {
            // 默认先加载，`${user.home}/dubbo-resolve.properties` 文件 ，无需配置
            resolveFile = System.getProperty("dubbo.resolve.file");
            if (resolveFile == null || resolveFile.length() == 0) {
                File userResolveFile = new File(new File(System.getProperty("user.home")), "dubbo-resolve.properties");
                if (userResolveFile.exists()) {
                    resolveFile = userResolveFile.getAbsolutePath();
                }
            }
            // 存在 resolveFile ，则进行文件读取加载。
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
        // 设置直连提供者的 url
        if (resolve != null && resolve.length() > 0) {
            url = resolve;
            if (logger.isWarnEnabled()) {
                if (resolveFile != null && resolveFile.length() > 0) {
                    logger.warn("Using default dubbo resolve file " + resolveFile + " replace " + interfaceName + "" + resolve + " to p2p invoke remote service.");
                } else {
                    logger.warn("Using -D" + interfaceName + "=" + resolve + " to p2p invoke remote service.");
                }
            }
        }
        // 从 ConsumerConfig 对象中，读取 application、module、registries、monitor 配置对象。
        if (consumer != null) {
            if (application == null) {
                application = consumer.getApplication();
            }
            if (module == null) {
                module = consumer.getModule();
            }
            if (registries == null) {
                registries = consumer.getRegistries();
            }
            if (monitor == null) {
                monitor = consumer.getMonitor();
            }
        }
        // 从 ModuleConfig 对象中，读取 registries、monitor 配置对象。
        if (module != null) {
            if (registries == null) {
                registries = module.getRegistries();
            }
            if (monitor == null) {
                monitor = module.getMonitor();
            }
        }
        // 从 ApplicationConfig 对象中，读取 registries、monitor 配置对象。
        if (application != null) {
            if (registries == null) {
                registries = application.getRegistries();
            }
            if (monitor == null) {
                monitor = application.getMonitor();
            }
        }
        // 校验 ApplicationConfig 配置。
        checkApplication();
        // 校验 Stub 和 Mock 相关的配置
        checkStubAndMock(interfaceClass);
        // 将 `side`，`dubbo`，`timestamp`，`pid` 参数，添加到 `map` 集合中。
        Map<String, String> map = new HashMap<String, String>();
        Map<Object, Object> attributes = new HashMap<Object, Object>();
        map.put(Constants.SIDE_KEY, Constants.CONSUMER_SIDE);
        map.put(Constants.DUBBO_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        // methods、revision、interface
        if (!isGeneric()) {
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put("revision", revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames(); // 获得方法数组
            if (methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put("methods", Constants.ANY_VALUE);
            } else {
                map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        map.put(Constants.INTERFACE_KEY, interfaceName);
        // 将各种配置对象，添加到 `map` 集合中。
        appendParameters(map, application);
        appendParameters(map, module);
        appendParameters(map, consumer, Constants.DEFAULT_KEY);
        appendParameters(map, this);
        // 获得服务键，作为前缀
        String prefix = StringUtils.getServiceKey(map);
        // 将 MethodConfig 对象数组，添加到 `map` 集合中。
        if (methods != null && !methods.isEmpty()) {
            for (MethodConfig method : methods) {
                // 将 MethodConfig 对象，添加到 `map` 集合中。
                appendParameters(map, method, method.getName());
                // 当 配置了 `MethodConfig.retry = false` 时，强制禁用重试
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                // 将带有 @Parameter(attribute = true) 配置对象的属性，添加到参数集合。参见《事件通知》http://dubbo.io/books/dubbo-user-book/demos/events-notify.html
                appendAttributes(attributes, method, prefix + "." + method.getName());
                // 检查属性集合中的事件通知方法是否正确。若正确，进行转换。
                checkAndConvertImplicitConfig(method, map, attributes);
            }
        }

        // 以系统环境变量( DUBBO_IP_TO_REGISTRY ) 作为服务注册地址，参见 https://github.com/dubbo/dubbo-docker-sample 项目。
        String hostToRegistry = ConfigUtils.getSystemProperty(Constants.DUBBO_IP_TO_REGISTRY);
        if (hostToRegistry == null || hostToRegistry.length() == 0) {
            hostToRegistry = NetUtils.getLocalHost();
        } else if (isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + Constants.DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(Constants.REGISTER_IP_KEY, hostToRegistry);

        // 添加到 StaticContext 进行缓存
        //attributes are stored by system context.
        StaticContext.getSystemContext().putAll(attributes);

        // 创建 Service 代理对象
        ref = createProxy(map);

        // TODO 芋艿
        ConsumerModel consumerModel = new ConsumerModel(getUniqueServiceName(), this, ref, interfaceClass.getMethods());
        ApplicationModel.initConsumerModel(getUniqueServiceName(), consumerModel);
    }

    /**
     * 创建 Service 代理对象
     *
     * @param map 集合
     * @return 代理对象
     */
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    private T createProxy(Map<String, String> map) {
        URL tmpUrl = new URL("temp", "localhost", 0, map);
        // 是否本地引用
        final boolean isJvmRefer;
        // injvm 属性为空，不通过该属性判断
        if (isInjvm() == null) {
            // 直连服务提供者，参见文档《直连提供者》https://dubbo.gitbooks.io/dubbo-user-book/demos/explicit-target.html
            if (url != null && url.length() > 0) { // if a url is specified, don't do local reference
                isJvmRefer = false;
            // 通过 `tmpUrl` 判断，是否需要本地引用
            } else if (InjvmProtocol.getInjvmProtocol().isInjvmRefer(tmpUrl)) {
                // by default, reference local service if there is
                isJvmRefer = true;
            // 默认不是
            } else {
                isJvmRefer = false;
            }
        // 通过 injvm 属性。
        } else {
            isJvmRefer = isInjvm();
        }

        // 本地引用
        if (isJvmRefer) {
            // 创建本地服务引用 URL 对象。
            URL url = new URL(Constants.LOCAL_PROTOCOL, NetUtils.LOCALHOST, 0, interfaceClass.getName()).addParameters(map);
            // 引用服务，返回 Invoker 对象
            invoker = refprotocol.refer(interfaceClass, url);
            if (logger.isInfoEnabled()) {
                logger.info("Using injvm service " + interfaceClass.getName());
            }
        // 正常流程，一般为远程引用
        } else {
            // 定义直连地址，可以是服务提供者的地址，也可以是注册中心的地址
            if (url != null && url.length() > 0) { // user specified URL, could be peer-to-peer address, or register center's address.
                // 拆分地址成数组，使用 ";" 分隔。
                String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(url);
                // 循环数组，添加到 `url` 中。
                if (us != null && us.length > 0) {
                    for (String u : us) {
                        // 创建 URL 对象
                        URL url = URL.valueOf(u);
                        // 设置默认路径
                        if (url.getPath() == null || url.getPath().length() == 0) {
                            url = url.setPath(interfaceName);
                        }
                        // 注册中心的地址，带上服务引用的配置参数
                        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                            urls.add(url.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                        // 服务提供者的地址
                        } else {
                            urls.add(ClusterUtils.mergeUrl(url, map));
                        }
                    }
                }
            // 注册中心
            } else { // assemble URL from register center's configuration
                // 加载注册中心 URL 数组
                List<URL> us = loadRegistries(false);
                // 循环数组，添加到 `url` 中。
                if (us != null && !us.isEmpty()) {
                    for (URL u : us) {
                        // 加载监控中心 URL
                        URL monitorUrl = loadMonitor(u);
                        // 服务引用配置对象 `map`，带上监控中心的 URL
                        if (monitorUrl != null) {
                            map.put(Constants.MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                        }
                        // 注册中心的地址，带上服务引用的配置参数
                        urls.add(u.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map))); // 注册中心，带上服务引用的配置参数
                    }
                }
                if (urls.isEmpty()) {
                    throw new IllegalStateException("No such any registry to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                }
            }

            // 单 `urls` 时，引用服务，返回 Invoker 对象
            if (urls.size() == 1) {
                // 引用服务
                invoker = refprotocol.refer(interfaceClass, urls.get(0));
            } else {
                // 循环 `urls` ，引用服务，返回 Invoker 对象
                List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                URL registryURL = null;
                for (URL url : urls) {
                    // 引用服务
                    invokers.add(refprotocol.refer(interfaceClass, url));
                    // 使用最后一个注册中心的 URL
                    if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                        registryURL = url; // use last registry url
                    }
                }
                // 有注册中心
                if (registryURL != null) { // registry url is available
                    // 对有注册中心的 Cluster 只用 AvailableCluster
                    // use AvailableCluster only when register's cluster is available
                    URL u = registryURL.addParameter(Constants.CLUSTER_KEY, AvailableCluster.NAME);
                    invoker = cluster.join(new StaticDirectory(u, invokers));
                // 无注册中心，全部都是服务直连
                } else { // not a registry url
                    invoker = cluster.join(new StaticDirectory(invokers));
                }
            }
        }

        // 启动时检查
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

        // 创建 Service 代理对象
        // create service proxy
        return (T) proxyFactory.getProxy(invoker);
    }

    /**
     * 校验 ConsumerConfig 配置。
     * 实际上，会拼接属性配置（环境变量 + properties 属性）到 ConsumerConfig 对象。
     */
    private void checkDefault() {
        if (consumer == null) {
            consumer = new ConsumerConfig();
        }
        appendProperties(consumer);
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (isGeneric()
                || (getConsumer() != null && getConsumer().isGeneric())) {
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
     * @param interfaceClass
     * @see #setInterface(Class)
     * @deprecated
     */
    @Deprecated
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (id == null || id.length() == 0) {
            id = interfaceName;
        }
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        checkName("client", client);
        this.client = client;
    }

    @Parameter(excluded = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    @SuppressWarnings("unchecked")
    public void setMethods(List<? extends MethodConfig> methods) {
        this.methods = (List<MethodConfig>) methods;
    }

    public ConsumerConfig getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerConfig consumer) {
        this.consumer = consumer;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    // just for test
    Invoker<?> getInvoker() {
        return invoker;
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
