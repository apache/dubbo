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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;

public class MetadataUtils {

    private static final Object REMOTE_LOCK = new Object();

    public static ConcurrentMap<String, MetadataService> metadataServiceProxies = new ConcurrentHashMap<>();

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    public static RemoteMetadataServiceImpl remoteMetadataService;

    /**
     * 实际为InMemoryWritableMetadataService
     * @return
     */
    public static RemoteMetadataServiceImpl getRemoteMetadataService() {
        if (remoteMetadataService == null) {
            synchronized (REMOTE_LOCK) {
                if (remoteMetadataService == null) {
                    /**
                     * WritableMetadataService实际为InMemoryWritableMetadataService
                     */
                    remoteMetadataService = new RemoteMetadataServiceImpl(WritableMetadataService.getDefaultExtension());
                }
            }
        }
        return remoteMetadataService;
    }

    public static void publishServiceDefinition(URL url) {
        // store in local
        /**
         * 本地缓存
         */
        WritableMetadataService.getDefaultExtension().publishServiceDefinition(url);
        // send to remote
//        if (REMOTE_METADATA_STORAGE_TYPE.equals(url.getParameter(METADATA_KEY))) {
        /**
         * 配置中心写入
         */
        getRemoteMetadataService().publishServiceDefinition(url);
//        }
    }

    /**
     *
     * @param instance
     * @param serviceDiscovery
     * @return
     */
    public static MetadataService getMetadataServiceProxy(ServiceInstance instance, ServiceDiscovery serviceDiscovery) {
        //dubbo-demo-annotation-provider##8BE83CC30467D06A691D5E9CAA26F913
        String key = instance.getServiceName() + "##" +
                ServiceInstanceMetadataUtils.getExportedServicesRevision(instance);
        return metadataServiceProxies.computeIfAbsent(key, k -> {
            MetadataServiceURLBuilder builder = null;
            ExtensionLoader<MetadataServiceURLBuilder> loader
                    = ExtensionLoader.getExtensionLoader(MetadataServiceURLBuilder.class);
            /**
             * 获取元数据
             */
            Map<String, String> metadata = instance.getMetadata();
            // METADATA_SERVICE_URLS_PROPERTY_NAME is a unique key exists only on instances of spring-cloud-alibaba.
            /**
             * dubbo.metadata-service.urls  spring-cloud-alibaba独有
             */
            String dubboURLsJSON = metadata.get(METADATA_SERVICE_URLS_PROPERTY_NAME);
            if (StringUtils.isNotEmpty(dubboURLsJSON)) {
                builder = loader.getExtension(SpringCloudMetadataServiceURLBuilder.NAME);
            } else {
                builder = loader.getExtension(StandardMetadataServiceURLBuilder.NAME);
            }

            /**
             * 获取instance对应的服务提供方元数据服务的url
             */
            List<URL> urls = builder.build(instance);
            if (CollectionUtils.isEmpty(urls)) {
                throw new IllegalStateException("You have enabled introspection service discovery mode for instance "
                        + instance + ", but no metadata service can build from it.");
            }

            // Simply rely on the first metadata url, as stated in MetadataServiceURLBuilder.
            /**
             * 根据元数据服务的url  生成对应的invoker   AbstractProtocol----DubboProtocol
             * 根据元数据服务的url  生成对应的invoker   AbstractProtocol----DubboProtocol
             * 根据元数据服务的url  生成对应的invoker   AbstractProtocol----DubboProtocol
             */
            Invoker<MetadataService> invoker = protocol.refer(MetadataService.class, urls.get(0));

            return proxyFactory.getProxy(invoker);
        });
    }
}
