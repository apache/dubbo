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
package org.apache.dubbo.registry.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.integration.RegistryDirectory;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.RouterChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.CLUSTER_STICKY_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.Constants.LAZY_CONNECT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.remoting.Constants.CONNECT_TIMEOUT_KEY;
import static org.apache.dubbo.remoting.Constants.RECONNECT_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.CALLBACK_INSTANCES_LIMIT_KEY;

/**
 * DubboRegistryFactory
 *
 */
public class DubboRegistryFactory extends AbstractRegistryFactory {

    private Protocol protocol;
    private ProxyFactory proxyFactory;
    private Cluster cluster;

    private static URL getRegistryURL(URL url) {
        return URLBuilder.from(url)
                .setPath(RegistryService.class.getName())
                .removeParameter(EXPORT_KEY).removeParameter(REFER_KEY)
                .addParameter(INTERFACE_KEY, RegistryService.class.getName())
                .addParameter(CLUSTER_STICKY_KEY, "true")
                .addParameter(LAZY_CONNECT_KEY, "true")
                .addParameter(RECONNECT_KEY, "false")
                .addParameterIfAbsent(TIMEOUT_KEY, "10000")
                .addParameterIfAbsent(CALLBACK_INSTANCES_LIMIT_KEY, "10000")
                .addParameterIfAbsent(CONNECT_TIMEOUT_KEY, "10000")
                .addParameter(METHODS_KEY, StringUtils.join(new HashSet<>(Arrays.asList(Wrapper.getWrapper(RegistryService.class).getDeclaredMethodNames())), ","))
                //.addParameter(Constants.STUB_KEY, RegistryServiceStub.class.getName())
                //.addParameter(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString()) //for event dispatch
                //.addParameter(Constants.ON_DISCONNECT_KEY, "disconnect")
                .addParameter("subscribe.1.callback", "true")
                .addParameter("unsubscribe.1.callback", "false")
                .build();
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public Registry createRegistry(URL url) {
        url = getRegistryURL(url);
        List<URL> urls = new ArrayList<>();
        urls.add(url.removeParameter(BACKUP_KEY));
        String backup = url.getParameter(BACKUP_KEY);
        if (backup != null && backup.length() > 0) {
            String[] addresses = COMMA_SPLIT_PATTERN.split(backup);
            for (String address : addresses) {
                urls.add(url.setAddress(address));
            }
        }
        RegistryDirectory<RegistryService> directory = new RegistryDirectory<>(RegistryService.class, url.addParameter(INTERFACE_KEY, RegistryService.class.getName()).addParameterAndEncoded(REFER_KEY, url.toParameterString()));
        Invoker<RegistryService> registryInvoker = cluster.join(directory);
        RegistryService registryService = proxyFactory.getProxy(registryInvoker);
        DubboRegistry registry = new DubboRegistry(registryInvoker, registryService);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        directory.setRouterChain(RouterChain.buildChain(url));
        directory.notify(urls);
        directory.subscribe(new URL(CONSUMER_PROTOCOL, NetUtils.getLocalHost(), 0, RegistryService.class.getName(), url.getParameters()));
        return registry;
    }
}
