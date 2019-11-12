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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.MetadataServiceURLBuilder;
import org.apache.dubbo.registry.client.metadata.SpringCloudMetadataServiceURLBuilder;
import org.apache.dubbo.registry.client.metadata.StandardMetadataServiceURLBuilder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

import java.util.List;
import java.util.Map;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;

/**
 * Works on Consumer side, useful when using local metadata mode.
 *
 * Use this implementation to generate the proxy on Consumer side representing the remote MetadataService
 * exposed on the Provider side. Also see {@link RemoteMetadataServiceProxyFactory}
 *
 * @since 2.7.5
 */
public class DefaultMetadataServiceProxyFactory extends BaseMetadataServiceProxyFactory implements MetadataServiceProxyFactory {

    private ProxyFactory proxyFactory;

    private Protocol protocol;

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }


    protected MetadataService createProxy(ServiceInstance serviceInstance) {
        MetadataServiceURLBuilder builder = null;
        ExtensionLoader<MetadataServiceURLBuilder> loader
                = ExtensionLoader.getExtensionLoader(MetadataServiceURLBuilder.class);

        Map<String, String> metadata = serviceInstance.getMetadata();
        // METADATA_SERVICE_URLS_PROPERTY_NAME is a unique key exists only on instances of spring-cloud-alibaba.
        String dubboURLsJSON = metadata.get(METADATA_SERVICE_URLS_PROPERTY_NAME);
        if (StringUtils.isNotEmpty(dubboURLsJSON)) {
            builder = loader.getExtension(SpringCloudMetadataServiceURLBuilder.NAME);
        } else {
            builder = loader.getExtension(StandardMetadataServiceURLBuilder.NAME);
        }

        List<URL> urls = builder.build(serviceInstance);
        if (CollectionUtils.isEmpty(urls)) {
            throw new IllegalStateException("You have enabled introspection service discovery mode for instance "
                    + serviceInstance + ", but no metadata service can build from it.");
        }

        // Simply rely on the first metadata url, as stated in MetadataServiceURLBuilder.
        Invoker<MetadataService> invoker = protocol.refer(MetadataService.class, urls.get(0));

        return proxyFactory.getProxy(invoker);
    }
}
