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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.AvailableCluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.registry.client.metadata.MetadataServiceURLBuilder.composite;

/**
 * The default {@link MetadataServiceProxyFactory} to get the proxy of {@link MetadataService}
 *
 * @since 2.7.4
 */
public class DefaultMetadataServiceProxyFactory implements MetadataServiceProxyFactory {

    private final Map<String, MetadataService> proxies = new HashMap<>();

    private ProxyFactory proxyFactory;

    private Protocol protocol;

    private Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getExtension(AvailableCluster.NAME);

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public MetadataService getProxy(ServiceInstance serviceInstance) {
        return proxies.computeIfAbsent(serviceInstance.getId(), id -> createProxy(serviceInstance));
    }

    protected MetadataService createProxy(ServiceInstance serviceInstance) {
        List<URL> urls = composite().build(serviceInstance);
        List<Invoker<MetadataService>> invokers = urls.stream()
                .map(url -> protocol.refer(MetadataService.class, url))
                .collect(Collectors.toList());

        Invoker<MetadataService> invoker = cluster.join(new StaticDirectory<>(invokers));
        return proxyFactory.getProxy(invoker);
    }
}
