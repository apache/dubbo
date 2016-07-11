/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config;

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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

/**
 * ServiceConfig
 * 
 * @author william.liangf
 * @export
 */
public class ServiceConfig<T> extends AbstractServiceConfig {

    private static final long   serialVersionUID = 3033787999037024738L;

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    
    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>();

    // 接口类型
    private String              interfaceName;

    private Class<?>            interfaceClass;

    // 接口实现类引用
    private T                   ref;

    // 服务名称
    private String              path;

    // 方法配置
    private List<MethodConfig>  methods;

    private ProviderConfig provider;

    private final List<URL> urls = new ArrayList<URL>();
    
    private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();

    private transient volatile boolean exported;

	private transient volatile boolean unexported;
    
    private volatile String generic;

    public ServiceConfig() {
    }

    public ServiceConfig(Service service) {
        appendAnnotation(Service.class, service);
    }

    public URL toUrl() {
        return urls == null || urls.size() == 0 ? null : urls.iterator().next();
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
        if (provider != null) {
            if (export == null) {
                export = provider.getExport();
            }
            if (delay == null) {
                delay = provider.getDelay();
            }
        }
        if (export != null && ! export.booleanValue()) {
            return;
        }
        if (delay != null && delay > 0) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(delay);
                    } catch (Throwable e) {
                    }
                    doExport();
                }
            });
            thread.setDaemon(true);
            thread.setName("DelayExportServiceThread");
            thread.start();
        } else {
            doExport();
        }
    }
    
    protected synchronized void doExport() {
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
        checkDefault();
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
        if(local !=null){
            if("true".equals(local)){
                local=interfaceName+"Local";
            }
            Class<?> localClass;
            try {
                localClass = ClassHelper.forNameWithThreadContextClassLoader(local);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if(!interfaceClass.isAssignableFrom(localClass)){
                throw new IllegalStateException("The local implemention class " + localClass.getName() + " not implement interface " + interfaceName);
            }
        }
        if(stub !=null){
            if("true".equals(stub)){
                stub=interfaceName+"Stub";
            }
            Class<?> stubClass;
            try {
                stubClass = ClassHelper.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if(!interfaceClass.isAssignableFrom(stubClass)){
                throw new IllegalStateException("The stub implemention class " + stubClass.getName() + " not implement interface " + interfaceName);
            }
        }
        checkApplication();
        checkRegistry();
        checkProtocol();
        appendProperties(this);
        checkStubAndMock(interfaceClass);
        if (path == null || path.length() == 0) {
            path = interfaceName;
        }
        doExportUrls();
    }

    private void checkRef() {
        // 检查引用不为空，并且引用必需实现接口
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        if (! interfaceClass.isInstance(ref)) {
            throw new IllegalStateException("The class "
                    + ref.getClass().getName() + " unimplemented interface "
                    + interfaceClass + "!");
        }
    }

    public synchronized void unexport() {
        if (! exported) {
            return;
        }
        if (unexported) {
            return;
        }
    	if (exporters != null && exporters.size() > 0) {
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
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void doExportUrls() {
        List<URL> registryURLs = loadRegistries(true);
        for (ProtocolConfig protocolConfig : protocols) {
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }

    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        String name = protocolConfig.getName();
        if (name == null || name.length() == 0) {
            name = "dubbo";
        }

        String host = protocolConfig.getHost();
        if (provider != null && (host == null || host.length() == 0)) {
            host = provider.getHost();
        }
        boolean anyhost = false;
        if (NetUtils.isInvalidLocalHost(host)) {
            anyhost = true;
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                logger.warn(e.getMessage(), e);
            }
            if (NetUtils.isInvalidLocalHost(host)) {
                if (registryURLs != null && registryURLs.size() > 0) {
                    for (URL registryURL : registryURLs) {
                        try {
                            Socket socket = new Socket();
                            try {
                                SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                socket.connect(addr, 1000);
                                host = socket.getLocalAddress().getHostAddress();
                                break;
                            } finally {
                                try {
                                    socket.close();
                                } catch (Throwable e) {}
                            }
                        } catch (Exception e) {
                            logger.warn(e.getMessage(), e);
                        }
                    }
                }
                if (NetUtils.isInvalidLocalHost(host)) {
                    host = NetUtils.getLocalHost();
                }
            }
        }

        Integer port = protocolConfig.getPort();
        if (provider != null && (port == null || port == 0)) {
            port = provider.getPort();
        }
        final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
        if (port == null || port == 0) {
            port = defaultPort;
        }
        if (port == null || port <= 0) {
            port = getRandomPort(name);
            if (port == null || port < 0) {
                port = NetUtils.getAvailablePort(defaultPort);
                putRandomPort(name, port);
            }
            logger.warn("Use random available port(" + port + ") for protocol " + name);
        }

        Map<String, String> map = new HashMap<String, String>();
        if (anyhost) {
            map.put(Constants.ANYHOST_KEY, "true");
        }
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
        map.put(Constants.DUBBO_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        appendParameters(map, application);
        appendParameters(map, module);
        appendParameters(map, provider, Constants.DEFAULT_KEY);
        appendParameters(map, protocolConfig);
        appendParameters(map, this);
        if (methods != null && methods.size() > 0) {
            for (MethodConfig method : methods) {
                appendParameters(map, method, method.getName());
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                List<ArgumentConfig> arguments = method.getArguments();
                if (arguments != null && arguments.size() > 0) {
                    for (ArgumentConfig argument : arguments) {
                        //类型自动转换.
                        if(argument.getType() != null && argument.getType().length() >0){
                            Method[] methods = interfaceClass.getMethods();
                            //遍历所有方法
                            if(methods != null && methods.length > 0){
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();
                                    //匹配方法名称，获取方法签名.
                                    if(methodName.equals(method.getName())){
                                        Class<?>[] argtypes = methods[i].getParameterTypes();
                                        //一个方法中单个callback
                                        if (argument.getIndex() != -1 ){
                                            if (argtypes[argument.getIndex()].getName().equals(argument.getType())){
                                                appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                            }else {
                                                throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :"+argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else {
                                            //一个方法中多个callback
                                            for (int j = 0 ;j<argtypes.length ;j++) {
                                                Class<?> argclazz = argtypes[j];
                                                if (argclazz.getName().equals(argument.getType())){
                                                    appendParameters(map, argument, method.getName() + "." + j);
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j){
                                                        throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :"+argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }else if(argument.getIndex() != -1){
                            appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                        }else {
                            throw new IllegalArgumentException("argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                        }

                    }
                }
            } // end of methods for
        }

        if (ProtocolUtils.isGeneric(generic)) {
            map.put("generic", generic);
            map.put("methods", Constants.ANY_VALUE);
        } else {
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put("revision", revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if(methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put("methods", Constants.ANY_VALUE);
            }
            else {
                map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        if (! ConfigUtils.isEmpty(token)) {
            if (ConfigUtils.isDefault(token)) {
                map.put("token", UUID.randomUUID().toString());
            } else {
                map.put("token", token);
            }
        }
        if ("injvm".equals(protocolConfig.getName())) {
            protocolConfig.setRegister(false);
            map.put("notify", "false");
        }
        // 导出服务
        String contextPath = protocolConfig.getContextpath();
        if ((contextPath == null || contextPath.length() == 0) && provider != null) {
            contextPath = provider.getContextpath();
        }
        URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);

        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }

        String scope = url.getParameter(Constants.SCOPE_KEY);
        //配置为none不暴露
        if (! Constants.SCOPE_NONE.toString().equalsIgnoreCase(scope)) {

            //配置不是remote的情况下做本地暴露 (配置为remote，则表示只暴露远程服务)
            if (!Constants.SCOPE_REMOTE.toString().equalsIgnoreCase(scope)) {
                exportLocal(url);
            }
            //如果配置不是local则暴露为远程服务.(配置为local，则表示只暴露本地服务)
            if (! Constants.SCOPE_LOCAL.toString().equalsIgnoreCase(scope) ){
                if (logger.isInfoEnabled()) {
                    logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                }
                if (registryURLs != null && registryURLs.size() > 0
                        && url.getParameter("register", true)) {
                    for (URL registryURL : registryURLs) {
                        url = url.addParameterIfAbsent("dynamic", registryURL.getParameter("dynamic"));
                        URL monitorUrl = loadMonitor(registryURL);
                        if (monitorUrl != null) {
                            url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                        }
                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));

                        Exporter<?> exporter = protocol.export(invoker);
                        exporters.add(exporter);
                    }
                } else {
                    Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);

                    Exporter<?> exporter = protocol.export(invoker);
                    exporters.add(exporter);
                }
            }
        }
        this.urls.add(url);
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void exportLocal(URL url) {
        if (!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            URL local = URL.valueOf(url.toFullString())
                    .setProtocol(Constants.LOCAL_PROTOCOL)
                    .setHost(NetUtils.LOCALHOST)
                    .setPort(0);
            Exporter<?> exporter = protocol.export(
                    proxyFactory.getInvoker(ref, (Class) interfaceClass, local));
            exporters.add(exporter);
            logger.info("Export dubbo service " + interfaceClass.getName() +" to local registry");
        }
    }

    private void checkDefault() {
        if (provider == null) {
            provider = new ProviderConfig();
        }
        appendProperties(provider);
    }

    private void checkProtocol() {
        if ((protocols == null || protocols.size() == 0)
                && provider != null) {
            setProtocols(provider.getProtocols());
        }
    	// 兼容旧版本
        if (protocols == null || protocols.size() == 0) {
            setProtocol(new ProtocolConfig());
        }
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
     * @deprecated
     * @see #setInterface(Class)
     * @param interfaceClass
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
        if (interfaceClass != null && ! interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? (String) null : interfaceClass.getName());
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
        checkPathName("path", path);
        this.path = path;
    }

	public List<MethodConfig> getMethods() {
		return methods;
	}

	@SuppressWarnings("unchecked")
    public void setMethods(List<? extends MethodConfig> methods) {
		this.methods = (List<MethodConfig>) methods;
	}

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setGeneric(String generic) {
        if (StringUtils.isEmpty(generic)) { return; }
        if (ProtocolUtils.isGeneric(generic)) {
            this.generic = generic;
        } else {
            throw new IllegalArgumentException("Unsupported generic type " + generic);
        }
    }

    public String getGeneric() {
        return generic;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }
    
    public List<URL> getExportedUrls(){
        return urls;
    }
    
    // ======== Deprecated ========

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

    @Deprecated
    private static final List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (providers == null || providers.size() == 0) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }
        return protocols;
    }
    
    @Deprecated
    private static final List<ProviderConfig> convertProtocolToProvider(List<ProtocolConfig> protocols) {
        if (protocols == null || protocols.size() == 0) {
            return null;
        }
        List<ProviderConfig> providers = new ArrayList<ProviderConfig>(protocols.size());
        for (ProtocolConfig provider : protocols) {
            providers.add(convertProtocolToProvider(provider));
        }
        return providers;
    }
    
    @Deprecated
    private static final ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
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
    private static final ProviderConfig convertProtocolToProvider(ProtocolConfig protocol) {
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
}
