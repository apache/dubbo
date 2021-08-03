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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;

public class MetadataUtils {

    private static final Object REMOTE_LOCK = new Object();

    public static ConcurrentMap<String, MetadataService> metadataServiceProxies = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, Invoker<?>> metadataServiceInvokers = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, Lock> metadataServiceLocks = new ConcurrentHashMap<>();

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    public static RemoteMetadataServiceImpl remoteMetadataService;

    public static WritableMetadataService localMetadataService;

    public static WritableMetadataService getLocalMetadataService() {
        if (localMetadataService == null) {
            localMetadataService = WritableMetadataService.getDefaultExtension();
        }
        return localMetadataService;
    }

    public static RemoteMetadataServiceImpl getRemoteMetadataService() {
        if (remoteMetadataService == null) {
            synchronized (REMOTE_LOCK) {
                if (remoteMetadataService == null) {
                    remoteMetadataService = new RemoteMetadataServiceImpl(getLocalMetadataService());
                }
            }
        }
        return remoteMetadataService;
    }

    public static void publishServiceDefinition(URL url) {
        // store in local
        WritableMetadataService.getDefaultExtension().publishServiceDefinition(url);
        // send to remote
        if (REMOTE_METADATA_STORAGE_TYPE.equalsIgnoreCase(url.getParameter(METADATA_KEY))) {
            getRemoteMetadataService().publishServiceDefinition(url);
        }
    }

    public static String computeKey(ServiceInstance serviceInstance) {
        return serviceInstance.getServiceName() + "##" + serviceInstance.getAddress() + "##" +
                ServiceInstanceMetadataUtils.getExportedServicesRevision(serviceInstance);
    }

    public static MetadataService getMetadataServiceProxy(ServiceInstance instance) {
        String key = computeKey(instance);
        Lock lock = metadataServiceLocks.computeIfAbsent(key, k -> new ReentrantLock());

        lock.lock();
        try {
            return metadataServiceProxies.computeIfAbsent(key, k -> referProxy(k, instance));
        } finally {
            lock.unlock();
        }
    }

    public static void destroyMetadataServiceProxy(ServiceInstance instance) {
        String key = computeKey(instance);
        Lock lock = metadataServiceLocks.computeIfAbsent(key, k -> new ReentrantLock());

        lock.lock();
        try {
            if (metadataServiceProxies.containsKey(key)) {
                metadataServiceProxies.remove(key);
                Invoker<?> invoker = metadataServiceInvokers.remove(key);
                invoker.destroy();
            }
        } finally {
            lock.unlock();
        }
    }

    private static MetadataService referProxy(String key, ServiceInstance instance) {
        MetadataServiceURLBuilder builder = null;
        ExtensionLoader<MetadataServiceURLBuilder> loader
                = ExtensionLoader.getExtensionLoader(MetadataServiceURLBuilder.class);

        Map<String, String> metadata = instance.getMetadata();
        // METADATA_SERVICE_URLS_PROPERTY_NAME is a unique key exists only on instances of spring-cloud-alibaba.
        String dubboURLsJSON = metadata.get(METADATA_SERVICE_URLS_PROPERTY_NAME);
        if (metadata.isEmpty() || StringUtils.isEmpty(dubboURLsJSON)) {
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
        Invoker<MetadataService> invoker = protocol.refer(MetadataService.class, urls.get(0));
        metadataServiceInvokers.put(key, invoker);

        return proxyFactory.getProxy(invoker);
    }

    public static void saveMetadataURL(URL url) {
        // store in local
        getLocalMetadataService().setMetadataServiceURL(url);
    }
}
