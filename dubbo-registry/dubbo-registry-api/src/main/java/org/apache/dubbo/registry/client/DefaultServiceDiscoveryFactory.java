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

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The default {@link SPI} implementation of {@link ServiceDiscoveryFactory} to {@link #getServiceDiscovery(URL) get the
 * instance of ServiceDiscovery} via the {@link URL#getProtocol() protocol} from the {@link URL} that will connect
 * the infrastructure of Service registration and discovery. The {@link URL#getProtocol() protocol} will be used as the
 * extension name by which the {@link ServiceDiscovery} instance is loaded.
 *
 * @see AbstractServiceDiscoveryFactory
 * @see EventPublishingServiceDiscovery
 * @since 2.7.5
 */
public class DefaultServiceDiscoveryFactory extends AbstractServiceDiscoveryFactory {

    /**
     * Create the {@link ServiceDiscovery} by {@link URL#getProtocol() the protocol} from {@link URL connection URL}
     *
     * @param registryURL
     * @return
     */
    @Override
    protected ServiceDiscovery createDiscovery(URL registryURL) {
        String protocol = registryURL.getProtocol();
        ExtensionLoader<ServiceDiscovery> loader = getExtensionLoader(ServiceDiscovery.class);
        return loader.getExtension(protocol);
    }
}
