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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;

import static org.apache.dubbo.common.extension.ExtensionScope.APPLICATION;

/**
 * The factory to create {@link ServiceDiscovery}
 *
 * @see ServiceDiscovery
 * @since 2.7.5
 */
@SPI(value = "default", scope = APPLICATION)
public interface ServiceDiscoveryFactory {

    /**
     * Get the instance of {@link ServiceDiscovery}
     *
     * @param registryURL the {@link URL} to connect the registry
     * @param model, the application model context
     * @return non-null
     */
    ServiceDiscovery getServiceDiscovery(URL registryURL);

    /**
     * Get the extension instance of {@link ServiceDiscoveryFactory} by {@link URL#getProtocol() the protocol}
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return non-null
     */
    static ServiceDiscoveryFactory getExtension(URL registryURL) {
        String protocol = registryURL.getProtocol();
        ExtensionLoader<ServiceDiscoveryFactory> loader = registryURL.getOrDefaultApplicationModel().getExtensionLoader(ServiceDiscoveryFactory.class);
        return loader.getOrDefaultExtension(protocol);
    }
}
