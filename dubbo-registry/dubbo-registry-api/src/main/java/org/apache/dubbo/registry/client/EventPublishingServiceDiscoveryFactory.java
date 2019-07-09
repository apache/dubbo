package org.apache.dubbo.registry.client;/*
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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The factory class to create an instance of {@link ServiceDiscoveryFactory} based on Event-Publishing as the default
 * {@link SPI} implementation
 *
 * @see ServiceDiscoveryFactory
 * @see EventPublishingServiceDiscovery
 * @see ServiceDiscovery
 * @since 2.7.4
 */
public class EventPublishingServiceDiscoveryFactory implements ServiceDiscoveryFactory {

    private static final Class<ServiceDiscoveryFactory> FACTORY_CLASS = ServiceDiscoveryFactory.class;

    @Override
    public ServiceDiscovery create(URL connectionURL) {
        String protocol = connectionURL.getProtocol();
        ServiceDiscoveryFactory serviceDiscoveryFactory = loadFactoryByProtocol(protocol);
        ServiceDiscovery originalServiceDiscovery = serviceDiscoveryFactory.create(connectionURL);
        return new EventPublishingServiceDiscovery(originalServiceDiscovery);
    }

    protected ServiceDiscoveryFactory loadFactoryByProtocol(String protocol) {
        return getExtensionLoader(FACTORY_CLASS).getExtension(protocol);
    }
}
