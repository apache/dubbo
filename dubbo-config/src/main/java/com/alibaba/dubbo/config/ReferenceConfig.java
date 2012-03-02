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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.StaticContext;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.support.AvailableCluster;
import com.alibaba.dubbo.rpc.cluster.support.ClusterUtils;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * ReferenceConfig
 * 
 * @author william.liangf
 */
public class ReferenceConfig<T> extends AbstractConsumerConfig {

    private static final long    serialVersionUID        = -5864351140409987595L;

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
    
    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    // 接口类型
    private String               interfaceName;

    private Class<?>             interfaceClass;

    // 版本
    private String               version;

    // 服务分组
    private String               group;

    // 客户端类型
    private String               client;

    // 点对点直连服务提供地址
    private String               url;

    // 方法配置
    private List<MethodConfig>   methods;

    // 缺省配置
    private ConsumerConfig       consumer;

    // 接口代理类引用
    private transient T          ref;

    private transient Invoker<?> invoker;

    private transient boolean    initialized;

    private transient boolean    destroyed;

    private final List<URL> urls = new ArrayList<URL>();
    
    public List<URL> toUrls() {
        return urls;
    }

    public synchronized T get() {
        if (destroyed){
            throw new IllegalStateException("Already destroyed!");
        }
    	if (ref == null) {
    		init();
    	}
    	return ref;
    }
    
    public synchronized void destroy() {
        if (ref == null) {
            return;
        }
        if (destroyed){
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
	    if (initialized) {
	        return;
	    }
	    initialized = true;
    	if (interfaceName == null || interfaceName.length() == 0) {
    	    throw new IllegalStateException("<dubbo:reference interface=\"\" /> interface not allow null!");
    	}
    	// 获取消费者全局配置
    	checkDefault();
        if (generic == null && consumer != null) {
            generic = consumer.isGeneric();
        }
        if (generic == null) {
        	generic = false;
        }
        if (generic) {
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
                        if(null != fis) fis.close();
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
        		if (resolveFile != null && resolveFile.length() > 0) {
        			logger.warn("Using default dubbo resolve file " + resolveFile + " replace " + interfaceName + "" + resolve + " to p2p invoke remote service.");
        		} else {
        			logger.warn("Using -D" + interfaceName + "=" + resolve + " to p2p invoke remote service.");
        		}
    		}
        }
        if (consumer != null) {
            if (application == null) {
                application = consumer.getApplication();
            }
            if (registries == null) {
                registries = consumer.getRegistries();
            }
            if (monitor == null) {
                monitor = consumer.getMonitor();
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
        checkApplication();
        checkStubAndMock(interfaceClass);
        Map<String, String> map = new HashMap<String, String>();
        Map<Object, Object> attributes = new HashMap<Object, Object>();
        map.put("dubbo", Version.getVersion());
        if (! generic) {
            map.put("revision", Version.getVersion(interfaceClass, version));
            map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(Wrapper.getWrapper(interfaceClass).getDeclaredMethodNames())), ","));
        }
        map.put(Constants.INTERFACE_KEY, interfaceName);
        appendParameters(map, application);
        appendParameters(map, consumer, Constants.DEFAULT_KEY);
        appendParameters(map, this);
        String prifix = StringUtils.getServiceKey(map);
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
                appendAttributes(attributes, method, prifix + "." + method.getName());
                checkAndConvertImplicitConfig(method, map, attributes);
            }
        }
        //attributes通过系统context进行存储.
        StaticContext.getSystemContext().putAll(attributes);
        ref = createProxy(map);
    }
    
    private static void checkAndConvertImplicitConfig(MethodConfig method, Map<String,String> map, Map<Object,Object> attributes){
      //check config conflict
        if (Boolean.FALSE.equals(method.isReturn()) && (method.getOnreturn() != null || method.getOnthrow() != null)) {
            throw new IllegalStateException("method config error : return attribute must be set true when onreturn or onthrow has been setted.");
        }
        //convert onreturn methodName to Method
        String onReturnMethodKey = StaticContext.getKey(map,method.getName(),RpcConstants.ON_RETURN_METHOD_KEY);
        Object onReturnMethod = attributes.get(onReturnMethodKey);
        if (onReturnMethod != null && onReturnMethod instanceof String){
            attributes.put(onReturnMethodKey, getMethodByName(method.getOnreturn().getClass(), onReturnMethod.toString()));
        }
        //convert onthrow methodName to Method
        String onThrowMethodKey = StaticContext.getKey(map,method.getName(),RpcConstants.ON_THROW_METHOD_KEY);
        Object onThrowMethod = attributes.get(onThrowMethodKey);
        if (onThrowMethod != null && onThrowMethod instanceof String){
            attributes.put(onThrowMethodKey, getMethodByName(method.getOnthrow().getClass(), onThrowMethod.toString()));
        }
        //convert oninvoke methodName to Method
        String onInvokeMethodKey = StaticContext.getKey(map,method.getName(),RpcConstants.ON_INVOKE_METHOD_KEY);
        Object onInvokeMethod = attributes.get(onInvokeMethodKey);
        if (onInvokeMethod != null && onInvokeMethod instanceof String){
            attributes.put(onInvokeMethodKey, getMethodByName(method.getOninvoke().getClass(), onInvokeMethod.toString()));
        }
    }

    private static Method getMethodByName(Class<?> clazz, String methodName){
        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private T createProxy(Map<String, String> map) {
	    Boolean j = injvm;
        if (j == null && consumer != null) {
            j = consumer.isInjvm();
        }
        if (j != null && j) {
            URL url = new URL("injvm", NetUtils.LOCALHOST, 0, interfaceClass.getName()).addParameters(map);
            invoker = protocol.refer(interfaceClass, url);
            if (logger.isInfoEnabled()) {
                logger.info("Using injvm service " + interfaceClass.getName());
            }
        } else {
            if (url != null && url.length() > 0) { // 用户指定URL，指定的URL可能是对点对直连地址，也可能是注册中心URL
                String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(url);
                if (us != null && us.length > 0) {
                    for (String u : us) {
                        URL url = URL.valueOf(u);
                        if (url.getPath() == null || url.getPath().length() == 0) {
                            url = url.setPath(interfaceName);
                        }
                        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                            urls.add(url.addParameterAndEncoded(RpcConstants.REFER_KEY, StringUtils.toQueryString(map)));
                        } else {
                            urls.add(ClusterUtils.mergeUrl(url, map));
                        }
                    }
                }
            } else { // 通过注册中心配置拼装URL
            	List<URL> us = loadRegistries();
            	if (us != null && us.size() > 0) {
                	for (URL u : us) {
                	    URL monitorUrl = loadMonitor(u);
                        if (monitorUrl != null) {
                            map.put(Constants.MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                        }
                	    urls.add(u.addParameterAndEncoded(RpcConstants.REFER_KEY, StringUtils.toQueryString(map)));
                    }
            	}
            	if (urls == null || urls.size() == 0) {
                    throw new IllegalStateException("No such any registry to reference " + interfaceName  + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                }
            }
            if (urls.size() == 1) {
                invoker = protocol.refer(interfaceClass, urls.get(0));
            } else {
                List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                URL registryURL = null;
                for (URL url : urls) {
                    invokers.add(protocol.refer(interfaceClass, url));
                    if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                        registryURL = url; // 用了最后一个registry url
                    }
                }
                if (registryURL != null) { // 有 注册中心协议的URL
                    // 对有注册中心的Cluster 只用 AvailableCluster
                    URL u = registryURL.addParameter(Constants.CLUSTER_KEY, AvailableCluster.NAME); 
                    invoker = cluster.merge(new StaticDirectory(u, invokers));
                }  else { // 不是 注册中心的URL
                    invoker = cluster.merge(new StaticDirectory(invokers));
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
        if (c && ! invoker.isAvailable()) {
            throw new IllegalStateException("Failed to check the status of the service " + interfaceName + ". No provider available for the service " + (group == null ? "" : group + "/") + interfaceName + (version == null ? "" : ":" + version) + " from the url " + invoker.getUrl() + " to the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion());
        }
        if (logger.isInfoEnabled()) {
            logger.info("Refer dubbo service " + interfaceClass.getName() + " from url " + invoker.getUrl());
        }
        // 创建服务代理
        return (T) proxyFactory.getProxy(invoker);
    }

    private void checkDefault() {
        if (consumer == null) {
            consumer = new ConsumerConfig();
            String t = ConfigUtils.getProperty("dubbo.service.invoke.timeout");
            if (t != null && t.length() > 0) {
                consumer.setTimeout(Integer.parseInt(t.trim()));
            }
            String r = ConfigUtils.getProperty("dubbo.service.max.retry.providers");
            if (r != null && r.length() > 0) {
                consumer.setRetries(Integer.parseInt(r.trim()) - 1);
            }
            String c = ConfigUtils.getProperty("dubbo.service.allow.no.provider");
            if (c != null && c.length() > 0) {
                consumer.setCheck(!Boolean.parseBoolean(c));
            }
        }
        appendProperties(consumer, "dubbo.consumer");
    }

	public Class<?> getInterfaceClass() {
	    if (interfaceClass != null) {
	        return interfaceClass;
	    }
	    if ((generic != null && generic.booleanValue())
	            || (consumer != null && consumer.isGeneric() != null 
	                && consumer.isGeneric().booleanValue())) {
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
	@Deprecated
	public void setInterfaceClass(Class<?> interfaceClass) {
	    setInterface(interfaceClass);
	}

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    
    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && ! interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        this.interfaceName = interfaceClass == null ? null : interfaceClass.getName();
    }
    
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        checkName("version", version);
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        checkName("group", group);
        this.group = group;
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

}