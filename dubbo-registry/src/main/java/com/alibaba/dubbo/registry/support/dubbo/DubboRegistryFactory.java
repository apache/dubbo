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
package com.alibaba.dubbo.registry.support.dubbo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.registry.directory.RegistryDirectory;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.proxy.ProxyFactory;

/**
 * DubboRegistryFactory
 * 
 * @author william.liangf
 */
@Extension("dubbo")
public class DubboRegistryFactory extends AbstractRegistryFactory {
    
    private Protocol protocol;

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    private ProxyFactory proxyFactory;

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    private Cluster cluster;

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }
    
    public Registry createRegistry(URL url) {
        url = getRegistryURL(url);
        List<URL> urls = new ArrayList<URL>();
        urls.add(url.removeParameter(Constants.BACKUP_KEY));
        String backup = url.getParameter(Constants.BACKUP_KEY);
        if (backup != null && backup.length() > 0) {
            String[] addresses = Constants.COMMA_SPLIT_PATTERN.split(backup);
            for (String address : addresses) {
                urls.add(url.setAddress(address));
            }
        }
        RegistryDirectory<RegistryService> directory = new RegistryDirectory<RegistryService>(RegistryService.class, url.addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName()).addParameterAndEncoded(RpcConstants.REFER_KEY, url.toParameterString()));
        Invoker<RegistryService> registryInvoker = cluster.merge(directory);
        RegistryService registryService = proxyFactory.getProxy(registryInvoker);
        DubboRegistry registry = new DubboRegistry(registryInvoker, registryService);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        directory.notify(urls);
        registryService.subscribe(url, directory);
        return registry;
    }
    
    private static URL getRegistryURL(URL url) {
        return url.setPath(RegistryService.class.getName())
                .removeParameter(RpcConstants.EXPORT_KEY).removeParameter(RpcConstants.REFER_KEY)
                .addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName())
                .addParameter(RpcConstants.CLUSTER_STICKY_KEY, "true")
                .addParameter(RpcConstants.LAZY_CONNECT_KEY, "true")
                .addParameter(Constants.RECONNECT_KEY, "false")
                .addParameterIfAbsent(Constants.TIMEOUT_KEY, "10000")
                .addParameterIfAbsent(RpcConstants.CALLBACK_INSTANCES_LIMIT_KEY, "10000")
                .addParameterIfAbsent(Constants.CONNECT_TIMEOUT_KEY, "10000")
                .addParameter(Constants.METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(Wrapper.getWrapper(RegistryService.class).getDeclaredMethodNames())), ","))
                //.addParameter(Constants.STUB_KEY, RegistryServiceStub.class.getName())
                //.addParameter(RpcConstants.STUB_EVENT_KEY, Boolean.TRUE.toString()) //for event dispatch
                //.addParameter(RpcConstants.ON_DISCONNECT_KEY, "disconnect")
                .addParameter("subscribe.1.callback", "true")
                .addParameter("unsubscribe.1.callback", "false");
    }
}