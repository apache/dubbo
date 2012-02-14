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
package com.alibaba.dubbo.registry.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;

/**
 * RegistryProtocol
 * 
 * @author william.liangf
 * @author chao.liuc
 */
@Extension(Constants.REGISTRY_PROTOCOL)
public class RegistryProtocol implements Protocol {

    private Cluster cluster;
    
    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }
    
    private Protocol protocol;
    
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    private RegistryFactory registryFactory;
    
    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    public int getDefaultPort() {
        return 9090;
    }
    
    private static RegistryProtocol INSTANCE;

    public RegistryProtocol() {
        INSTANCE = this;
    }
    
    public static RegistryProtocol getRegistryProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(Constants.REGISTRY_PROTOCOL); // load
        }
        return INSTANCE;
    }
    
    //用于解决rmi重复暴露端口冲突的问题
    private final Map<String, ExporterWrapper<?>> bounds = new ConcurrentHashMap<String, ExporterWrapper<?>>();
    
    private final NotifyListener listener = new OverrideListener();
    
    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);
    
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        ExporterWrapper<T>  exporter = doLocolExport(invoker);
        doRegister(exporter);
        return exporter;
    }
    
    @SuppressWarnings("unchecked")
    private <T> ExporterWrapper<T>  doLocolExport(final Invoker<T> originInvoker){
        String key = getCacheKey(originInvoker);
        ExporterWrapper<T> exporter = (ExporterWrapper<T>) bounds.get(key);
        if (exporter == null) {
            synchronized (bounds) {
                exporter = (ExporterWrapper<T>) bounds.get(key);
                if (exporter == null) {
                    Invoker<?> dinvoker = new InvokerDelegete<T>(originInvoker, getProviderUrl(originInvoker));
                    exporter = new ExporterWrapper<T>((Exporter<T>)protocol.export(dinvoker), originInvoker);
                    bounds.put(key, exporter);
                }
            }
        }
        return (ExporterWrapper<T>) exporter;
    }
    
    @SuppressWarnings("unchecked")
    private <T> void doChangeLocolExport(final Invoker<T> originInvoker, URL newInvokerUrl){
        String key = getCacheKey(originInvoker);
        ExporterWrapper<T> exporter = (ExporterWrapper<T>) bounds.get(key);
        if (exporter == null){
            logger.warn(new IllegalStateException("error state, exporter should not be null"));
            return ;//不存在是异常场景 直接返回 
        } else {
            final Invoker<T> invokerDelegete = new InvokerDelegete<T>(originInvoker, newInvokerUrl);
            exporter.setExporter(protocol.export(invokerDelegete));
        }
    }

    private void doRegister(final ExporterWrapper<?>  exporter){
        final Registry registry = getRegistry(exporter);
        final URL registedProviderUrl = getRegistedProviderUrl(exporter);
        registry.register(registedProviderUrl, listener);
    }

    private Registry getRegistry(final ExporterWrapper<?>  exporter){
        URL registryUrl = exporter.getOriginInvoker().getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY);
        }
        return registryFactory.getRegistry(registryUrl);
    }

    private URL getRegistedProviderUrl(final ExporterWrapper<?>  exporter){
        URL providerUrl = getProviderUrl(exporter.getOriginInvoker());
        //注册中心看到的地址
        final URL registedProviderUrl = providerUrl.removeParameters(getFilteredKeys(providerUrl));
        return registedProviderUrl;
    }

    private URL getProviderUrl(final Invoker<?> origininvoker){
        String export = origininvoker.getUrl().getParameterAndDecoded(RpcConstants.EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + origininvoker.getUrl());
        }
        
        URL providerUrl = URL.valueOf(export);
        return providerUrl;
    }

    private String getCacheKey(final Invoker<?> originInvoker){
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString();
        return key;
    }
    
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);
        Registry registry = registryFactory.getRegistry(url);
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        registry.subscribe(new URL(Constants.SUBSCRIBE_PROTOCOL, NetUtils.getLocalHost(), 0, type.getName(), directory.getUrl().getParameters()), directory);
        return cluster.merge(directory);
    }

    //过滤URL中不需要输出的参数(以点号开头的)
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
            return new String[] {};
        }
    }
    
    public void destroy() {
        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
        for(Exporter<?> exporter :exporters){
            exporter.unexport();
        }
        bounds.clear();
    }
    
    
    /*重新export 1.protocol中的exporter destory问题 
     *1.要求registryprotocol返回的exporter可以正常destroy
     *2.notify后不需要重新向注册中心注册 
     *3.export 方法传入的invoker最好能一直作为exporter的invoker.
     */
    private class OverrideListener implements NotifyListener {

        /*
         *  provider 端可识别的override url只有这两种.
         *  override://0.0.0.0/serviceName?timeout=10
         *  override://0.0.0.0/?timeout=10
         */
        public void notify(List<URL> urls) {
            List<ExporterWrapper<?>> exporters = new ArrayList<ExporterWrapper<?>>(bounds.values());
            for (ExporterWrapper<?> exporter : exporters){
                Invoker<?> invoker = exporter.getOriginInvoker();
                final Invoker<?> originInvoker ;
                if (invoker instanceof InvokerDelegete){
                    originInvoker = ((InvokerDelegete<?>)invoker).getInvoker();
                }else {
                    originInvoker = invoker;
                }
                
                URL originUrl = RegistryProtocol.this.getProviderUrl(originInvoker);
                URL newUrl = getNewInvokerUrl(originUrl, urls);
                
                if (! originUrl.toFullString().equals(newUrl.toFullString())){
                    RegistryProtocol.this.doChangeLocolExport(originInvoker, newUrl);
                }
            }
        }
        
        private URL getNewInvokerUrl(final URL originUrl, final List<URL> urls){
            URL newUrl = originUrl;
            //override://0.0.0.0/?timeout=10 ip:port无意义
            for (URL overrideUrl : urls){
                if (overrideUrl.getServiceName() == null){
                    newUrl = newUrl.addParameters(overrideUrl.getParameters());
                }
            }
            //override://0.0.0.0/serviceName?timeout=10
            for (URL overrideUrl : urls){
                if (originUrl.getServiceKey().equals(overrideUrl.getServiceKey())){
                    newUrl = newUrl.addParameters(overrideUrl.getParameters());
                }
            }
            
            return newUrl;
        }
    }
    
    public static class InvokerDelegete<T> extends InvokerWrapper<T>{
        private final Invoker<T> invoker;
        public InvokerDelegete(Invoker<T> invoker, URL url){
            super(invoker, url);
            this.invoker = invoker;
        }
        public Invoker<T> getInvoker(){
            if (invoker instanceof InvokerDelegete){
                return ((InvokerDelegete<T>)invoker).getInvoker();
            } else {
                return invoker;
            }
        }
    }
    
    /**
     * exporter代理 可替换exporter
     * @author chao.liuc
     *
     * @param <T>
     */
    private class ExporterWrapper<T> implements Exporter<T>{
        private Exporter<T> exporter;
        private final Invoker<T> originInvoker;

        public ExporterWrapper(Exporter<T> exporter, Invoker<T> originInvoker){
            this.exporter = exporter;
            this.originInvoker = originInvoker;
        }
        
        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }

        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }
        
        public void setExporter(Exporter<T> exporter){
            this.exporter = exporter;
        }
        
        public void unexport() {
            Registry registry = getRegistry(this);
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);
            try {
                registry.unregister(getRegistedProviderUrl(this), listener);
            } finally {
                exporter.unexport();
            }
        }
    }
}