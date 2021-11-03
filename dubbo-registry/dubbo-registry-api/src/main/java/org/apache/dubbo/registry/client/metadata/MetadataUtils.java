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
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;

public class MetadataUtils {

    public static ConcurrentMap<String, MetadataService> metadataServiceProxies = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, Invoker<?>> metadataServiceInvokers = new ConcurrentHashMap<>();

    public static RemoteMetadataServiceImpl getRemoteMetadataService(ScopeModel scopeModel) {
        return scopeModel.getBeanFactory().getBean(RemoteMetadataServiceImpl.class);
    }

    public static void publishServiceDefinition(URL url) {
        // store in local
        WritableMetadataService.getDefaultExtension(url.getScopeModel()).publishServiceDefinition(url);
        // send to remote
        if (REMOTE_METADATA_STORAGE_TYPE.equalsIgnoreCase(url.getParameter(METADATA_KEY))) {
            getRemoteMetadataService(url.getOrDefaultApplicationModel()).publishServiceDefinition(url);
        }
    }

    public static String computeKey(ServiceInstance serviceInstance) {
        return serviceInstance.getServiceName() + "##" + serviceInstance.getAddress() + "##" +
                ServiceInstanceMetadataUtils.getExportedServicesRevision(serviceInstance);
    }

    public static synchronized MetadataService getMetadataServiceProxy(ServiceInstance instance) {
        return metadataServiceProxies.computeIfAbsent(computeKey(instance), k -> referProxy(k, instance));
    }

    public static synchronized void destroyMetadataServiceProxy(ServiceInstance instance) {
        String key = computeKey(instance);
        if (metadataServiceProxies.containsKey(key)) {
            metadataServiceProxies.remove(key);
            Invoker<?> invoker = metadataServiceInvokers.remove(key);
            invoker.destroy();
        }
    }

    private static MetadataService referProxy(String key, ServiceInstance instance) {
        MetadataServiceURLBuilder builder;
        ExtensionLoader<MetadataServiceURLBuilder> loader = instance.getApplicationModel()
            .getExtensionLoader(MetadataServiceURLBuilder.class);

        Map<String, String> metadata = instance.getMetadata();
        // METADATA_SERVICE_URLS_PROPERTY_NAME is a unique key exists only on instances of spring-cloud-alibaba.
        String dubboUrlsForJson = metadata.get(METADATA_SERVICE_URLS_PROPERTY_NAME);
        if (metadata.isEmpty() || StringUtils.isEmpty(dubboUrlsForJson)) {
            builder = loader.getExtension(StandardMetadataServiceURLBuilder.NAME);
        } else {
            builder = loader.getExtension(SpringCloudMetadataServiceURLBuilder.NAME);
        }

        List<URL> urls = builder.build(instance);
        if (CollectionUtils.isEmpty(urls)) {
            throw new IllegalStateException("Introspection service discovery mode is enabled "
                    + instance + ", but no metadata service can build from it.");
        }

        // Simply rely on the first metadata url, as stated in MetadataServiceURLBuilder.
        ScopeModel scopeModel = instance.getApplicationModel();
        Protocol protocol = scopeModel.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        Invoker<MetadataService> invoker = protocol.refer(MetadataService.class, urls.get(0));
        metadataServiceInvokers.put(key, invoker);

        ProxyFactory proxyFactory = scopeModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        return proxyFactory.getProxy(invoker);
    }

    public static ConcurrentMap<String, MetadataService> getMetadataServiceProxies() {
        return metadataServiceProxies;
    }

    public static ConcurrentMap<String, Invoker<?>> getMetadataServiceInvokers() {
        return metadataServiceInvokers;
    }
}
