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
import org.apache.dubbo.common.extension.SPI;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The Factory interface to create an instance of {@link ServiceDiscovery}
 *
 * @see ServiceDiscovery
 * @since 2.7.3
 */
@SPI("event-publishing")
public interface ServiceDiscoveryFactory {

    /**
     * Creates an instance of {@link ServiceDiscovery}.
     *
     * @param connectionURL the  {@link URL connection url}
     * @return an instance of {@link ServiceDiscovery}
     */
    ServiceDiscovery create(URL connectionURL);

    /**
     * Get the default extension of {@link ServiceDiscoveryFactory}
     *
     * @return non-null
     */
    static ServiceDiscoveryFactory getDefaultExtension() {
        return getExtensionLoader(ServiceDiscoveryFactory.class).getDefaultExtension();
    }
}
