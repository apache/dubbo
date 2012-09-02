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
package com.alibaba.dubbo.registry.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;
import com.alibaba.dubbo.rpc.cluster.directory.AbstractDirectory;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.support.ClusterUtils;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;
import com.alibaba.dubbo.rpc.support.RpcUtils;

/**
 * RegistryDirectory
 * 
 * @author william.liangf
 * @author chao.liuc
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);
    
    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
    
    private static final RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getAdaptiveExtension();

    private static final ConfiguratorFactory configuratorFactory = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getAdaptiveExtension();

    private Protocol protocol; // 注入时初始化，断言不为null

    private Registry registry; // 注入时初始化，断言不为null

    private final String serviceKey; // 构造时初始化，断言不为null

    private final Class<T> serviceType; // 构造时初始化，断言不为null
    
    private final Map<String, String> queryMap; // 构造时初始化，断言不为null

    private final URL directoryUrl; // 构造时初始化，断言不为null，并且总是赋非null值
    
    private final String[] serviceMethods;

    private final boolean multiGroup;

    private volatile boolean forbidden = false;
    
    private volatile URL overrideDirectoryUrl; // 构造时初始化，断言不为null，并且总是赋非null值

    /*override规则 
     * 优先级：override>-D>consumer>provider
     * 第一种规则：针对某个provider <ip:port,timeout=100>
     * 第二种规则：针对所有provider <* ,timeout=5000>
     */
    private volatile List<Configurator> configurators; // 初始为null以及中途可能被赋为null，请使用局部变量引用
    
    // Map<url, Invoker> cache service url to invoker mapping.
    private volatile Map<String, Invoker<T>> urlInvokerMap; // 初始为null以及中途可能被赋为null，请使用局部变量引用
    
    // Map<methodName, Invoker> cache service method to invokers mapping.
    private volatile Map<String, List<Invoker<T>>> methodInvokerMap; // 初始为null以及中途可能被赋为null，请使用局部变量引用
    
    // Set<invokerUrls> cache invokeUrls to invokers mapping.
    private volatile Set<URL> cachedInvokerUrls; // 初始为null以及中途可能被赋为null，请使用局部变量引用

    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        if(serviceType == null )
            throw new IllegalArgumentException("service type is null.");
        if(url.getServiceKey() == null || url.getServiceKey().length() == 0)
            throw new IllegalArgumentException("registry serviceKey is null.");
        this.serviceType = serviceType;
        this.serviceKey = url.getServiceKey();
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        this.overrideDirectoryUrl = this.directoryUrl = url.setPath(url.getServiceInterface()).clearParameters().addParameters(queryMap).removeParameter(Constants.MONITOR_KEY);
        String group = directoryUrl.getParameter( Constants.GROUP_KEY, "" );
        this.multiGroup = group != null && ("*".equals(group) || group.contains( "," ));
        String methods = queryMap.get(Constants.METHODS_KEY);
        this.serviceMethods = methods == null ? null : Constants.COMMA_SPLIT_PATTERN.split(methods);
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
    
    public void subscribe(URL url) {
        setConsumerUrl(url);
        registry.subscribe(url, this);
    }

    public void destroy() {
        if(isDestroyed()) {
            return;
        }
        // unsubscribe.
        try {
            if(getConsumerUrl() != null && registry != null && registry.isAvailable()) {
                registry.unsubscribe(getConsumerUrl(), this);
            }
        } catch (Throwable t) {
            logger.warn("unexpeced error when unsubscribe service " + serviceKey + "from registry" + registry.getUrl(), t);
        }
        super.destroy(); // 必须在unsubscribe之后执行
        try {
            destroyAllInvokers();
        } catch (Throwable t) {
            logger.warn("Failed to destroy service " + serviceKey, t);
        }
    }

    public synchronized void notify(List<URL> urls) {
        List<URL> invokerUrls = new ArrayList<URL>();
        List<URL> routerUrls = new ArrayList<URL>();
        List<URL> configuratorUrls = new ArrayList<URL>();
        for (URL url : urls) {
            String protocol = url.getProtocol();
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            if (Constants.ROUTERS_CATEGORY.equals(category) 
                    || Constants.ROUTE_PROTOCOL.equals(protocol)) {
                routerUrls.add(url);
            } else if (Constants.CONFIGURATORS_CATEGORY.equals(category) 
                    || Constants.OVERRIDE_PROTOCOL.equals(protocol)) {
                configuratorUrls.add(url);
            } else if (Constants.PROVIDERS_CATEGORY.equals(category)) {
                invokerUrls.add(url);
            } else {
                logger.warn("Unsupported category " + category + " in notified url: " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
            }
        }
        // configurators 
        if (configuratorUrls != null && configuratorUrls.size() >0 ){
            this.configurators = toConfigurators(configuratorUrls);
        }
        // routers
        if (routerUrls != null && routerUrls.size() >0 ){
            List<Router> routers = toRouters(routerUrls);
            if(routers != null){ // null - do nothing
                setRouters(routers);
            }
        }
        List<Configurator> localConfigurators = this.configurators; // local reference
        // 合并override参数
        this.overrideDirectoryUrl = directoryUrl;
        if (localConfigurators != null && localConfigurators.size() > 0) {
            for (Configurator configurator : localConfigurators) {
                this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl);
            }
        }
        // providers
        refreshInvoker(invokerUrls);
    }
    
    
    /**
     * 根据invokerURL列表转换为invoker列表。转换规则如下：
     * 1.如果url已经被转换为invoker，则不在重新引用，直接从缓存中获取，注意如果url中任何一个参数变更也会重新引用
     * 2.如果传入的invoker列表不为空，则表示最新的invoker列表
     * 3.如果传入的invokerUrl列表是空，则表示只是下发的override规则或route规则，需要重新交叉对比，决定是否需要重新引用。
     * @param invokerUrls 传入的参数不能为null
     */
    private void refreshInvoker(List<URL> invokerUrls){
        if (invokerUrls != null && invokerUrls.size() == 1 && invokerUrls.get(0) != null
                && Constants.EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            this.forbidden = true; // 禁止访问
            overrideDirectoryUrl = overrideDirectoryUrl.addParameters(
                    Constants.INVOKER_INSIDE_INVOKERS_KEY, "",
                    Constants.INVOKER_INSIDE_INVOKER_COUNT_KEY, "0");
            this.methodInvokerMap = null; // 置空列表
            destroyAllInvokers(); // 关闭所有Invoker
        } else {
            this.forbidden = false; // 允许访问
            Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap; // local reference
            if (invokerUrls.size() == 0 && this.cachedInvokerUrls != null){
                invokerUrls.addAll(this.cachedInvokerUrls);
            } else {
                this.cachedInvokerUrls = new HashSet<URL>();
                this.cachedInvokerUrls.addAll(invokerUrls);//缓存invokerUrls列表，便于交叉对比
            }
            if (invokerUrls.size() ==0 ){
            	return;
            }
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls) ;// 将URL列表转成Invoker列表
            Map<String, List<Invoker<T>>> newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap); // 换方法名映射Invoker列表
            // state change
            //如果计算错误，则不进行处理.
            if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0 ){
                logger.error(new IllegalStateException("urls to invokers error .invokerUrls.size :"+invokerUrls.size() + ", invoker.size :0. urls :"+invokerUrls.toString()));
                return ;
            }
            this.methodInvokerMap = multiGroup ? toMergeMethodInvokerMap(newMethodInvokerMap) : newMethodInvokerMap;
            this.urlInvokerMap = newUrlInvokerMap;
            try{
                destroyUnusedInvokers(oldUrlInvokerMap,newUrlInvokerMap); // 关闭未使用的Invoker
            }catch (Exception e) {
                logger.warn("destroyUnusedInvokers error. ", e);
            }

            List<String> invokerUrlString = new ArrayList<String>();
            for(URL invoker : invokerUrls) {
                invokerUrlString.add(invoker.toString());
            }
            overrideDirectoryUrl = overrideDirectoryUrl.addParameters(
                    Constants.INVOKER_INSIDE_INVOKERS_KEY, URL.encode(CollectionUtils.join(invokerUrlString, ";")),
                    Constants.INVOKER_INSIDE_INVOKER_COUNT_KEY, String.valueOf(invokerUrls.size()));
        }
    }
    
    private Map<String, List<Invoker<T>>> toMergeMethodInvokerMap(Map<String, List<Invoker<T>>> methodMap) {
        Map<String, List<Invoker<T>>> result = new HashMap<String, List<Invoker<T>>>();
        for (Map.Entry<String, List<Invoker<T>>> entry : methodMap.entrySet()) {
            String method = entry.getKey();
            List<Invoker<T>> invokers = entry.getValue();
            Map<String, List<Invoker<T>>> groupMap = new HashMap<String, List<Invoker<T>>>();
            for (Invoker<T> invoker : invokers) {
                String group = invoker.getUrl().getParameter(Constants.GROUP_KEY, "");
                List<Invoker<T>> groupInvokers = groupMap.get(group);
                if (groupInvokers == null) {
                    groupInvokers = new ArrayList<Invoker<T>>();
                    groupMap.put(group, groupInvokers);
                }
                groupInvokers.add(invoker);
            }
            if (groupMap.size() == 1) {
                result.put(method, groupMap.values().iterator().next());
            } else if (groupMap.size() > 1) {
                List<Invoker<T>> groupInvokers = new ArrayList<Invoker<T>>();
                for (List<Invoker<T>> groupList : groupMap.values()) {
                    groupInvokers.add(cluster.join(new StaticDirectory<T>(groupList)));
                }
                result.put(method, groupInvokers);
            } else {
                result.put(method, invokers);
            }
        }
        return result;
    }
    
    /**
     * 将overrideURL转换为map，供重新refer时使用.
     * 每次下发全部规则，全部重新组装计算
     * @param urls
     * 契约：
     * </br>1.override://0.0.0.0/...(或override://ip:port...?anyhost=true)&para1=value1...表示全局规则(对所有的提供者全部生效)
     * </br>2.override://ip:port...?anyhost=false 特例规则（只针对某个提供者生效）
     * </br>3.不支持override://规则... 需要注册中心自行计算.
     * </br>4.不带参数的override://0.0.0.0/ 表示清除override 
     * @return
     */
    public static List<Configurator> toConfigurators(List<URL> urls){
        List<Configurator> configurators = new ArrayList<Configurator>(urls.size());
        if (urls == null || urls.size() == 0){
            return configurators;
        }
        for(URL url : urls){
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                configurators.clear();
                break;
            }
            Map<String,String> override = new HashMap<String, String>(url.getParameters());
            //override 上的anyhost可能是自动添加的，不能影响改变url判断
            override.remove(Constants.ANYHOST_KEY);
            if (override.size() == 0){
                configurators.clear();
                continue;
            }
            configurators.add(configuratorFactory.getConfigurator(url));
        }
        Collections.sort(configurators);
        return configurators;
    }
    
    /**
     * 
     * @param urls
     * @return null : no routers ,do nothing
     *         else :routers list
     */
    private List<Router> toRouters(List<URL> urls) {
        List<Router> routers = new ArrayList<Router>();
        if(urls == null || urls.size() < 1){
            return routers ;
        }
        if (urls != null && urls.size() > 0) {
            for (URL url : urls) {
                if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                    continue;
                }
                String routerType = url.getParameter(Constants.ROUTER_KEY);
                if (routerType != null && routerType.length() > 0){
                    url = url.setProtocol(routerType);
                }
                try{
                    Router router = routerFactory.getRouter(url);
                    if (!routers.contains(router))
                        routers.add(router);
                } catch (Throwable t) {
                    logger.error("convert router url to router error, url: "+ url, t);
                }
            }
        }
        return routers;
    }
    
    /**
     * 将urls转成invokers,如果url已经被refer过，不再重新引用。
     * 
     * @param urls
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<String, Invoker<T>>();
        if(urls == null || urls.size() == 0){
            return newUrlInvokerMap;
        }
        Set<String> keys = new HashSet<String>();
        String queryProtocols = this.queryMap.get(Constants.PROTOCOL_KEY);
        for (URL providerUrl : urls) {
        	//如果reference端配置了protocol，则只选择匹配的protocol
        	if (queryProtocols != null && queryProtocols.length() >0) {
        		boolean accept = false;
        		String[] acceptProtocols = queryProtocols.split(",");
        		for (String acceptProtocol : acceptProtocols) {
        			if (providerUrl.getProtocol().equals(acceptProtocol)) {
        				accept = true;
        				break;
        			}
        		}
        		if (!accept) {
        			continue;
        		}
        	}
            if (Constants.EMPTY_PROTOCOL.equals(providerUrl.getProtocol())) {
                continue;
            }
            if (! ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(providerUrl.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + providerUrl.getProtocol() + " in notified url: " + providerUrl + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost() 
                        + ", supported protocol: "+ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }
            URL url = mergeUrl(providerUrl);
            
            String key = url.toFullString(); // URL参数是排序的
            if (keys.contains(key)) { // 重复URL
                continue;
            }
            keys.add(key);
            // 缓存key为没有合并消费端参数的URL，不管消费端如何合并参数，如果服务端URL发生变化，则重新refer
            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
            Invoker<T> invoker = localUrlInvokerMap == null ? null : localUrlInvokerMap.get(key);
            if (invoker == null) { // 缓存中没有，重新refer
                try {
                	boolean enabled = true;
                	if (url.hasParameter(Constants.DISABLED_KEY)) {
                		enabled = ! url.getParameter(Constants.DISABLED_KEY, false);
                	} else {
                		enabled = url.getParameter(Constants.ENABLED_KEY, true);
                	}
                	if (enabled) {
                		invoker = new InvokerDelegete<T>(protocol.refer(serviceType, url), url, providerUrl);
                	}
                } catch (Throwable t) {
                    logger.error("Failed to refer invoker for interface:"+serviceType+",url:("+url+")" + t.getMessage(), t);
                }
                if (invoker != null) { // 将新的引用放入缓存
                    newUrlInvokerMap.put(key, invoker);
                }
            }else {
                newUrlInvokerMap.put(key, invoker);
            }
        }
        keys.clear();
        return newUrlInvokerMap;
    }
    
    /**
     * 合并url参数 顺序为override > -D >Consumer > Provider
     * @param providerUrl
     * @return
     */
    private URL mergeUrl(URL providerUrl){
        providerUrl = ClusterUtils.mergeUrl(providerUrl, queryMap); // 合并消费端参数
        
        List<Configurator> localConfigurators = this.configurators; // local reference
        if (localConfigurators != null && localConfigurators.size() > 0) {
            for (Configurator configurator : localConfigurators) {
                providerUrl = configurator.configure(providerUrl);
            }
        }
        
        providerUrl = providerUrl.addParameter(Constants.CHECK_KEY, String.valueOf(false)); // 不检查连接是否成功，总是创建Invoker！
        
        //directoryUrl 与 override 合并是在notify的最后，这里不能够处理
        this.overrideDirectoryUrl = this.overrideDirectoryUrl.addParametersIfAbsent(providerUrl.getParameters()); // 合并提供者参数        
        
        if ((providerUrl.getPath() == null || providerUrl.getPath().length() == 0)
                && "dubbo".equals(providerUrl.getProtocol())) { // 兼容1.0
            //fix by tony.chenl DUBBO-44
            String path = directoryUrl.getParameter(Constants.INTERFACE_KEY);
            if (path != null) {
                int i = path.indexOf('/');
                if (i >= 0) {
                    path = path.substring(i + 1);
                }
                i = path.lastIndexOf(':');
                if (i >= 0) {
                    path = path.substring(0, i);
                }
                providerUrl = providerUrl.setPath(path);
            }
        }
        return providerUrl;
    }

    private List<Invoker<T>> route(List<Invoker<T>> invokers, String method) {
        Invocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
        List<Router> routers = getRouters(); 
        if (routers != null) {
            for (Router router : routers) {
                if (router.getUrl() != null && ! router.getUrl().getParameter(Constants.RUNTIME_KEY, true)) {
                    invokers = router.route(invokers, getConsumerUrl(), invocation);
                }
            }
        }
        return invokers;
    }

    /**
     * 将invokers列表转成与方法的映射关系
     * 
     * @param invokersMap Invoker列表
     * @return Invoker与方法的映射关系
     */
    private Map<String, List<Invoker<T>>> toMethodInvokers(Map<String, Invoker<T>> invokersMap) {
        Map<String, List<Invoker<T>>> newMethodInvokerMap = new HashMap<String, List<Invoker<T>>>();
        // 按提供者URL所声明的methods分类，兼容注册中心执行路由过滤掉的methods
        List<Invoker<T>> invokersList = new ArrayList<Invoker<T>>();
        if (invokersMap != null && invokersMap.size() > 0) {
            for (Invoker<T> invoker : invokersMap.values()) {
                String parameter = invoker.getUrl().getParameter(Constants.METHODS_KEY);
                if (parameter != null && parameter.length() > 0) {
                    String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                    if (methods != null && methods.length > 0) {
                        for (String method : methods) {
                            if (method != null && method.length() > 0 
                                    && ! Constants.ANY_VALUE.equals(method)) {
                                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                                if (methodInvokers == null) {
                                    methodInvokers = new ArrayList<Invoker<T>>();
                                    newMethodInvokerMap.put(method, methodInvokers);
                                }
                                methodInvokers.add(invoker);
                            }
                        }
                    }
                }
                invokersList.add(invoker);
            }
        }
        newMethodInvokerMap.put(Constants.ANY_VALUE, invokersList);
        if (serviceMethods != null && serviceMethods.length > 0) {
            for (String method : serviceMethods) {
                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                if (methodInvokers == null || methodInvokers.size() == 0) {
                    methodInvokers = invokersList;
                }
                newMethodInvokerMap.put(method, route(methodInvokers, method));
            }
        }
        // sort and unmodifiable
        for (String method : new HashSet<String>(newMethodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
            Collections.sort(methodInvokers, InvokerComparator.getComparator());
            newMethodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }
        return Collections.unmodifiableMap(newMethodInvokerMap);
    }

    /**
     * 关闭所有Invoker
     */
    private void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
        if(localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            localUrlInvokerMap.clear();
        }
        methodInvokerMap = null;
    }
    
    /**
     * 检查缓存中的invoker是否需要被destroy
     * 如果url中指定refer.autodestroy=false，则只增加不减少，可能会有refer泄漏，
     */
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
        // check deleted invoker
        List<String> deleted = null;
        if (oldUrlInvokerMap != null) {
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()){
                if (! newInvokers.contains(entry.getValue())) {
                    if (deleted == null) {
                        deleted = new ArrayList<String>();
                    }
                    deleted.add(entry.getKey());
                }
            }
        }
        
        if (deleted != null) {
            for (String url : deleted){
                if (url != null ) {
                    Invoker<T> invoker = oldUrlInvokerMap.remove(url);
                    if (invoker != null) {
                        try {
                            invoker.destroy();
                            if(logger.isDebugEnabled()){
                                logger.debug("destory invoker["+invoker.getUrl()+"] success. ");
                            }
                        } catch (Exception e) {
                            logger.warn("destory invoker["+invoker.getUrl()+"] faild. " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    public List<Invoker<T>> doList(Invocation invocation) {
        if (forbidden) {
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION, "Forbid consumer " +  NetUtils.getLocalHost() + " access service " + getInterface().getName() + " from registry " + getUrl().getAddress() + " use dubbo version " + Version.getVersion() + ", Please check registry access list (whitelist/blacklist).");
        }
        List<Invoker<T>> invokers = null;
        Map<String, List<Invoker<T>>> localMethodInvokerMap = this.methodInvokerMap; // local reference
        if (localMethodInvokerMap != null && localMethodInvokerMap.size() > 0) {
            String methodName = RpcUtils.getMethodName(invocation);
            Object[] args = RpcUtils.getArguments(invocation);
            if(args != null && args.length > 0 && args[0] != null
                    && (args[0] instanceof String || args[0].getClass().isEnum())) {
                invokers = localMethodInvokerMap.get(methodName + "." + args[0]); // 可根据第一个参数枚举路由
            }
            if(invokers == null) {
                invokers = localMethodInvokerMap.get(methodName);
            }
            if(invokers == null) {
                invokers = localMethodInvokerMap.get(Constants.ANY_VALUE);
            }
            if(invokers == null) {
                Iterator<List<Invoker<T>>> iterator = localMethodInvokerMap.values().iterator();
                if (iterator.hasNext()) {
                    invokers = iterator.next();
                }
            }
        }
        return invokers == null ? new ArrayList<Invoker<T>>(0) : invokers;
    }
    
    public Class<T> getInterface() {
        return serviceType;
    }

    public URL getUrl() {
        if(registry != null) {
            overrideDirectoryUrl = overrideDirectoryUrl.addParameter(Constants.INVOKER_CONNECTED_KEY, registry.isAvailable());
        }
    	return overrideDirectoryUrl;
    }

    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (localUrlInvokerMap != null && localUrlInvokerMap.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                if (invoker.isAvailable()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Haomin: added for test purpose
     */
    public Map<String, Invoker<T>> getUrlInvokerMap(){
        return urlInvokerMap;
    }
    
    /**
     * Haomin: added for test purpose
     */
    public Map<String, List<Invoker<T>>> getMethodInvokerMap(){
        return methodInvokerMap;
    } 
    
    private static class InvokerComparator implements Comparator<Invoker<?>> {
        
        private static final InvokerComparator comparator = new InvokerComparator();
        
        public static InvokerComparator getComparator() {
            return comparator;
        }
        
        private InvokerComparator() {}

        public int compare(Invoker<?> o1, Invoker<?> o2) {
            return o1.getUrl().toString().compareTo(o2.getUrl().toString());
        }

    }
    
    /**
     * 代理类，主要用于存储注册中心下发的url地址，用于重新重新refer时能够根据providerURL queryMap overrideMap重新组装
     * 
     * @author chao.liuc
     *
     * @param <T>
     */
    private static class InvokerDelegete<T> extends InvokerWrapper<T>{
        private URL providerUrl;
        public InvokerDelegete(Invoker<T> invoker, URL url, URL providerUrl) {
            super(invoker, url);
            this.providerUrl = providerUrl;
        }
        public URL getProviderUrl() {
            return providerUrl;
        }
    }
}