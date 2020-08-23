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
package org.apache.dubbo.registry.client.event.listener;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;

/**
 * Customize the {@link ServiceInstance} before registering to Registry.
 *
 * @since 2.7.5
 * @deprecated 2.7.8 Current class will be removed since 3.0.0
 */
@Deprecated
public class CustomizableServiceInstanceListener implements EventListener<ServiceInstancePreRegisteredEvent> {

    @Override
    public void onEvent(ServiceInstancePreRegisteredEvent event) {
        ExtensionLoader<ServiceInstanceCustomizer> loader =
                ExtensionLoader.getExtensionLoader(ServiceInstanceCustomizer.class);
        // FIXME, sort customizer before apply
        loader.getSupportedExtensionInstances().forEach(customizer -> {
            // customizes
            customizer.customize(event.getServiceInstance());
        });
    }
}
