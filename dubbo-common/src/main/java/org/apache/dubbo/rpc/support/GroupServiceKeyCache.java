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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.BaseServiceMetadata;
import org.apache.dubbo.common.utils.LRUCache;

import java.util.Map;

/**
 * GroupServiceKeyCache
 */
public final class GroupServiceKeyCache {

    private static final int CACHE_SIZE = 512;

    private final String serviceGroup;

    /**
     * Cache for service keys.
     */
    private final Map<String, String> cache = new LRUCache<>(CACHE_SIZE);

    /**
     * @param serviceGroup serviceGroup
     */
    public GroupServiceKeyCache(final String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    /**
     * Get service key from cache or build it.
     *
     * @param serviceName    serviceName
     * @param serviceVersion serviceVersion
     * @param port           port
     * @return serviceKey
     */
    public String getServiceKey(final String serviceName, final String serviceVersion, final int port) {
        String fullServiceName = serviceName + ":" + serviceVersion + ":" + port;
        return cache.computeIfAbsent(
                fullServiceName, k -> BaseServiceMetadata.buildServiceKey(serviceName, serviceGroup, serviceVersion));
    }
}
