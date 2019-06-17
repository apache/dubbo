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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;

/**
 * The {@link ServiceInstanceCustomizer} to customize the {@link ServiceInstance#getPort() port} of service instance.
 *
 * @since 2.7.3
 */
public class ServiceInstancePortCustomizer implements ServiceInstanceCustomizer {

    @Override
    public void customize(ServiceInstance serviceInstance) {

        if (serviceInstance.getPort() != null
                || serviceInstance.getPort().intValue() < 1) {
            return;
        }

        ConfigManager.getInstance()
                .getProtocols()
                .values()
                .stream()
                .findFirst()
                .ifPresent(protocolConfig -> {
                    if (serviceInstance instanceof DefaultServiceInstance) {
                        DefaultServiceInstance instance = (DefaultServiceInstance) serviceInstance;
                        if (protocolConfig.getPort() != null) {
                            instance.setPort(protocolConfig.getPort());
                        }
                    }
                });
    }
}
