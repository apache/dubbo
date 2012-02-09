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
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
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
    
    private final Map<String, Exporter<?>> bounds = new ConcurrentHashMap<String, Exporter<?>>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        String export = invoker.getUrl().getParameterAndDecoded(RpcConstants.EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + invoker.getUrl());
        }
        
        URL url = URL.valueOf(export);
        final String key = url.removeParameters("dynamic", "enabled").toFullString();
        Exporter<T> exporter = (Exporter) bounds.get(key);
        if (exporter == null) {
            synchronized (bounds) {
                exporter = (Exporter) bounds.get(key);
                if (exporter == null) {
                    exporter = protocol.export(new InvokerWrapper<T>(invoker, url));
                    bounds.put(key, exporter);
                }
            }
        }
        
        URL registryUrl = invoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY);
        }
        final Exporter<T> serviceExporter = exporter;
        final URL serviceUrl = url.removeParameters(getFilteredKeys(url));
        final Registry registry = registryFactory.getRegistry(registryUrl);
        registry.register(serviceUrl, null);
        
        return new Exporter<T>() {
            public Invoker<T> getInvoker() {
                return invoker;
            }
            public void unexport() {
                bounds.remove(key);
                try {
                    registry.unregister(serviceUrl, null);
                } finally {
                    serviceExporter.unexport();
                }
            }
        };
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
    }

}