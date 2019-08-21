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
package org.apache.dubbo.registry.eureka;

import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

import com.netflix.appinfo.InstanceInfo;

/**
 * The Eureka {@link ServiceInstance} Builder
 *
 * @since 2.7.4
 */
public class EurekaServiceInstanceBuilder {

    public ServiceInstance build(InstanceInfo instance) {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(instance.getId(), instance.getAppName(),
                instance.getHostName(),
                instance.isPortEnabled(InstanceInfo.PortType.SECURE) ? instance.getSecurePort() : instance.getPort());
        serviceInstance.setMetadata(instance.getMetadata());
        return serviceInstance;
    }


}
