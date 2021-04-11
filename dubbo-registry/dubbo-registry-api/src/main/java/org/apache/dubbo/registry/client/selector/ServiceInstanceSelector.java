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
package org.apache.dubbo.registry.client.selector;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.List;

/**
 * The {@link ServiceInstance} Selector
 *
 * @since 2.7.5
 */
@SPI("random")
public interface ServiceInstanceSelector {

    /**
     * Select an instance of {@link ServiceInstance} by the specified {@link ServiceInstance service instances}
     *
     * @param registryURL      The {@link URL url} of registry
     * @param serviceInstances the specified {@link ServiceInstance service instances}
     * @return an instance of {@link ServiceInstance} if available, or <code>null</code>
     */
    @Adaptive("service-instance-selector")
    ServiceInstance select(URL registryURL, List<ServiceInstance> serviceInstances);
}
