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

import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;

/**
 * base class for remote and local implementations.
 *
 * @since 2.7.5
 */
abstract class BaseMetadataServiceProxyFactory implements MetadataServiceProxyFactory {

    private final ConcurrentMap<String, MetadataService> proxiesCache = new ConcurrentHashMap<>();

    public final MetadataService getProxy(ServiceInstance serviceInstance) {
        return proxiesCache.computeIfAbsent(createProxyCacheKey(serviceInstance), id -> createProxy(serviceInstance));
    }

    /**
     * Create the cache key of the proxy of {@link MetadataService}
     *
     * @param serviceInstance {@link ServiceInstance}
     * @return non-null
     * @since 2.7.8
     */
    protected String createProxyCacheKey(ServiceInstance serviceInstance) {
        return serviceInstance.getServiceName() + "#" + getExportedServicesRevision(serviceInstance);
    }

    /**
     * Create the instance proxy of {@link MetadataService}
     *
     * @param serviceInstance {@link ServiceInstance}
     * @return non-null
     */
    protected abstract MetadataService createProxy(ServiceInstance serviceInstance);
}
