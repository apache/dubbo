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
package org.apache.dubbo.registry.support.cloud;

import java.util.List;

/**
 * Cloud {@link ServiceInstance Service} Discovery
 *
 * @param <S> The subclass of {@link ServiceInstance}
 * @since 2.7.1
 */
public interface CloudServiceDiscovery<S extends ServiceInstance> {

    /**
     * The total number of all services.
     *
     * @return must be equal or more than 0
     */
    default long getTotalServices() {
        return getServices().size();
    }

    /**
     * Get all service names
     *
     * @return non-null read-only {@link List}
     */
    List<String> getServices();


    /**
     * Get all service instances by the specified name
     *
     * @param serviceName the service name
     * @return non-null read-only {@link List}
     */
    List<S> getServiceInstances(String serviceName);

    /**
     * Supports the specified name of Cloud Service or not
     *
     * @param serviceName the specified service name
     * @return if supports, return <code>true</code>, or <code>false</code>
     */
    boolean supports(String serviceName);
}
