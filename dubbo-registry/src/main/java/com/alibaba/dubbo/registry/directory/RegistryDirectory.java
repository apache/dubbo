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
package com.alibaba.dubbo.registry.directory;

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
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;
import com.alibaba.dubbo.rpc.cluster.support.AbstractDirectory;
import com.alibaba.dubbo.rpc.cluster.support.ClusterUtils;

/**
 * RegistryDirectory
 * 
 * @author william.liangf
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);
    
    private static final String ALL_METHOD_NAME = "*";
    
    private volatile boolean forbidden = false;
    
    private volatile boolean destroyed = false;
    
    private final String serviceKey;

    private final Class<T> serviceType;

    private volatile URL directoryUrl;

    private volatile Map<String, String> queryMap;
    
    // Map<url, Invoker> cache service url to invoker mapping.
    private Map<String, Invoker<T>> urlInvokerMap = new ConcurrentHashMap<String, Invoker<T>>();
    
    // Map<methodName, Invoker> cache service method to invokers mapping.
    private volatile Map<String, List<Invoker<T>>> methodInvokerMap;

    private volatile Protocol protocol;

    private volatile Registry registry;
    
    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        if(serviceType == null )
            throw new IllegalArgumentException("service type is null.");
        if(url.getServiceKey() == null || url.getServiceKey().length() == 0)
            throw new IllegalArgumentException("registry serviceKey is null.");
        this.serviceType = serviceType;
        this.serviceKey = url.getServiceKey();
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(RpcConstants.REFER_KEY));
        this.directoryUrl = url.removeParameter(RpcConstants.REFER_KEY).addParameters(queryMap);
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void destroy() {
        if(destroyed) {
            return;
        }
        destroyed = true;
        // unsubscribe.
        try {
            if(registry != null && registry.isAvailable()) {
                registry.unsubscribe(directoryUrl, this);
            }
        } catch (Throwable t) {
            logger.warn("unexpeced error when unsubscribe service " + serviceKey + "from registry" + registry.getUrl().getAddress(), t);
        }
        try {
            destroyAllInvokers();
        } catch (Throwable t) {
            logger.warn("Failed to destroy service " + serviceKey, t);
        }
    }

    public synchronized void notify(List<URL> urls) {
        if (urls == null || urls.size() == 0) { // 黑白名单限制
            this.forbidden = true; // 禁止访问
            this.methodInvokerMap = null; // 置空列表
            destroyAllInvokers(); // 关闭所有Invoker
        } else {
            this.forbidden = false; // 允许访问
            List<URL> invokerUrls = new ArrayList<URL>();
            List<URL> routerUrls = new ArrayList<URL>();
            for (URL url : urls) {
                if (RpcConstants.ROUTE_PROTOCOL.equals(url.getProtocol())) {
                    if (! routerUrls.contains(url)) {
                        routerUrls.add(url);
                    }
                } else if (ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(url.getProtocol())) {
                    if (! invokerUrls.contains(url)) {
                        invokerUrls.add(url);
                    }
                } else {
                    logger.error("Unsupported protocol " + url.getProtocol() + " in notified url " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
                }
            }
            
            Map<String, Invoker<T>> newUrlInvokerMap = null ;
            Map<String, List<Invoker<T>>> newMethodInvokerMap = null ;
            List<Router> routers = null ;
            Map<String, Invoker<T>> oldUrlInvokerMap = urlInvokerMap;
            //transaction convert
            {
                routers = toRouters(routerUrls);
                newUrlInvokerMap = toInvokers(invokerUrls); // 将URL列表转成Invoker列表
                newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap); // 换方法名映射Invoker列表
            }
            // state change
            {
                this.methodInvokerMap = newMethodInvokerMap;
                this.urlInvokerMap = newUrlInvokerMap;
                if(routers != null){ // null - do nothing
                    setRouters(routers);
                }
                try{
                    destroyUnusedInvokers(oldUrlInvokerMap,newUrlInvokerMap); // 关闭未使用的Invoker
                }catch (Exception e) {
                    logger.warn("destroyUnusedInvokers error. ", e);
                }
            }
        }
    }
    
    /**
     * 
     * @param urls
     * @return null : no routers ,do nothing
     *         else :routers list
     */
    private List<Router> toRouters(List<URL> urls) {
        //no router urls , do nothing
        if(urls == null || urls.size() < 1){
            return null ;
        }
        List<Router> routers = new ArrayList<Router>();
        
        // on these conditions: clear all current routers
        // 1. there is only one route url
        // 2. with type = clear
        if(urls.size() == 1){
           URL u = urls.get(0);
           // clean current routers
           if(RpcConstants.ROUTER_TYPE_CLEAR.equals(u.getParameter(RpcConstants.ROUTER_KEY))){
               return routers;
           }
        }
        
        if (urls != null && urls.size() > 0) {
            for (URL url : urls) {
                String router_type = url.getParameter(RpcConstants.ROUTER_KEY);
                if (router_type == null || router_type.length() == 0){
                    logger.warn("Router url:\"" + url.toString() + "\" does not contain " + RpcConstants.ROUTER_KEY + ", router creation ignored!");
                    continue;
                }
                routers.add(ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(router_type).getRouter(url));
//                routers.add(routerFactory.getRouter(url.setProtocol(router_type)));
            }
        }
        return routers;
    }
    
    /**
     * 将urls转成invokers
     * 
     * @param urls
     * @param query
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        if(urls == null || urls.size() == 0){
            return null;
        }
        Map<String, Invoker<T>> newUrlInvokerMap = new ConcurrentHashMap<String, Invoker<T>>();
        Set<String> keys = new HashSet<String>();
        for (URL url : urls) {
            String key = url.toFullString(); // URL参数是排序的
            if (keys.contains(key)) { // 重复URL
                continue;
            }
            keys.add(key);
            // 缓存key为没有合并消费端参数的URL，不管消费端如何合并参数，如果服务端URL发生变化，则重新refer
            Invoker<T> invoker = urlInvokerMap.get(key);
            if (invoker == null) { // 缓存中没有，重新refer
                try {
                    if ((url.getPath() == null || url.getPath().length() == 0)
                            && "dubbo".equals(url.getProtocol())) { // 兼容1.0
                        String path = directoryUrl.getPath();
                        int i = path.indexOf('/');
                        if (i >= 0) {
                            path = path.substring(i + 1);
                        }
                        i = path.lastIndexOf(':');
                        if (i >= 0) {
                            path = path.substring(0, i);
                        }
                        url = url.setPath(path);
                    }
                    url = ClusterUtils.mergeUrl(url, queryMap); // 合并消费端参数
                    this.directoryUrl = this.directoryUrl.addParametersIfAbsent(url.getParameters()); // 合并提供者参数
                    url = url.addParameter(Constants.CHECK_KEY, String.valueOf(false));// 不检查连接是否成功，总是创建Invoker
                    invoker = protocol.refer(serviceType, url);
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
     * 将invokers列表转成与方法的映射关系
     * 
     * @param invokersMap Invoker列表
     * @return Invoker与方法的映射关系
     */
    private Map<String, List<Invoker<T>>> toMethodInvokers(Map<String, Invoker<T>> invokersMap) {
        Map<String, List<Invoker<T>>> methodInvokerMap = new HashMap<String, List<Invoker<T>>>();
        if (invokersMap != null && invokersMap.size() > 0) {
            List<Invoker<T>> invokersList = new ArrayList<Invoker<T>>();
            for (Invoker<T> invoker : invokersMap.values()) {
                String parameter = invoker.getUrl().getParameter(Constants.METHODS_KEY);
                if (parameter != null && parameter.length() > 0) {
                    String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                    if (methods != null && methods.length > 0) {
                        for (String method : methods) {
                            if (method != null && method.length() > 0 
                                    && ! ALL_METHOD_NAME.equals(method)) {
                                List<Invoker<T>> methodInvokers = methodInvokerMap.get(method);
                                if (methodInvokers == null) {
                                    methodInvokers = new ArrayList<Invoker<T>>();
                                    methodInvokerMap.put(method, methodInvokers);
                                }
                                methodInvokers.add(invoker);
                            }
                        }
                    }
                }
                invokersList.add(invoker);
            }
            methodInvokerMap.put(ALL_METHOD_NAME, invokersList);
        }
        // sort and unmodifiable
        for (String method : new HashSet<String>(methodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = methodInvokerMap.get(method);
            Collections.sort(methodInvokers, InvokerComparator.getComparator());
            methodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }
        return Collections.unmodifiableMap(methodInvokerMap);
    }

    /**
     * 关闭所有Invoker
     */
    private void destroyAllInvokers() {
        if(urlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(urlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            urlInvokerMap.clear();
        }
        methodInvokerMap = null;
    }

    /**
     * 检查缓存中的invoker是否需要被destroy
     * 如果url中指定refer.autodestroy=false，则只增加不减少，可能会有refer泄漏，
     * 
     * @param invokers
     */
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
        boolean autodestroy = true ;
        // 如果URL中指定了autodestroy=false，则不关闭旧的连接
        for(Invoker<T> invoker : newUrlInvokerMap.values() ){
            if (invoker != null && invoker.getUrl() != null){
                autodestroy = invoker.getUrl().getBooleanParameter("refer.autodestroy", true);
            }
            break;
        }
        if(! autodestroy) {
            if(logger.isWarnEnabled()) {
                logger.warn("url.param[refer.autodestroy=false] is false. may have reference leak. recommend(default) true");
            }
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
        if (methodInvokerMap != null && methodInvokerMap.size() > 0) {
            String methodName = invocation.getMethodName();
            Object[] args = invocation.getArguments();
            
            // Generic invoke: Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;
            if (Constants.$INVOKE.equals(methodName) 
                    && args != null && args.length == 3
                    && args[0] instanceof String
                    && args[2] instanceof Object[]) { 
                methodName = (String) args[0];
                args = (Object[]) args[2];
            }
            if(args != null && args.length > 0 && args[0] != null
                    && (args[0] instanceof String || args[0].getClass().isEnum())) {
                invokers = methodInvokerMap.get(methodName + "." + args[0]); // 可根据第一个参数枚举路由
            }
            if(invokers == null) {
                invokers = methodInvokerMap.get(methodName);
            }
            if(invokers == null) {
                invokers = methodInvokerMap.get(ALL_METHOD_NAME);
            }
            if(invokers == null) {
                Iterator<List<Invoker<T>>> iterator = methodInvokerMap.values().iterator();
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
        return directoryUrl;
    }

    public boolean isAvailable() {
        Map<String, Invoker<T>> map = urlInvokerMap;
        if (map != null && map.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(map.values())) {
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

}