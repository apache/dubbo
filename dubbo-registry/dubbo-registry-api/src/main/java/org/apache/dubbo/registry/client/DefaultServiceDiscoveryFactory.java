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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import static org.apache.dubbo.common.utils.CollectionUtils.sort;

/**
 * The default implementation of {@link ServiceDiscoveryFactory} as the default extension of
 * {@link ServiceDiscoveryFactory}, default elements are loaded by @link ServiceLoader Java SPI} from in different
 * artifacts(jars) in the class path.
 *
 * @see ServiceDiscoveryFactory
 * @see ServiceLoader#load(Class)
 * @since 2.7.3
 */
public class DefaultServiceDiscoveryFactory implements ServiceDiscoveryFactory {

    private final List<ServiceDiscoveryFactory> serviceDiscoveryFactories = new LinkedList<>();

    public DefaultServiceDiscoveryFactory() {
        initServiceDiscoveryFactories();
    }

    private void initServiceDiscoveryFactories() {
        ServiceLoader<ServiceDiscoveryFactory> serviceLoader =
                ServiceLoader.load(ServiceDiscoveryFactory.class, getClass().getClassLoader());
        Iterator<ServiceDiscoveryFactory> iterator = serviceLoader.iterator();
        iterator.forEachRemaining(serviceDiscoveryFactories::add);
        sort(serviceDiscoveryFactories);
    }

    @Override
    public boolean supports(URL connectionURL) {
        return serviceDiscoveryFactories
                .stream()
                .filter(factory -> factory.supports(connectionURL))
                .count() > 0;
    }

    @Override
    public ServiceDiscovery create(URL connectionURL) {
        return serviceDiscoveryFactories
                .stream()
                .filter(factory -> factory.supports(connectionURL))
                .findFirst() // find the highest priority one
                .map(factory -> factory.create(connectionURL)) // create the original instance
                .map(serviceDiscovery -> new EventPublishingServiceDiscovery(serviceDiscovery)) // wrap the event-based
                .orElseThrow(() ->
                        new IllegalStateException("The ServiceDiscovery can't be created by the connection URL["
                                + connectionURL.toFullString() + "]") // If not found
                );
    }
}
