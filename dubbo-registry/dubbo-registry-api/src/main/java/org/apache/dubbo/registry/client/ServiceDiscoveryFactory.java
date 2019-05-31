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

import static java.lang.Integer.compare;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The Factory interface to create an instance of {@link ServiceDiscovery}
 *
 * @see ServiceDiscovery
 * @since 2.7.3
 */
@SPI("default")
public interface ServiceDiscoveryFactory extends Comparable<ServiceDiscoveryFactory> {

    /**
     * It indicates the current implementation supports or not in the specified {@link URL connnection url}.
     *
     * @param connectionURL the  {@link URL connection url}
     * @return if supports, return <code>true</code>, or <code>false</code>
     */
    boolean supports(URL connectionURL);

    /**
     * Creates an instance of {@link ServiceDiscovery} if {@link #supports(URL)} returns <code>true</code>,
     *
     * @param connectionURL the  {@link URL connection url}
     * @return an instance of {@link ServiceDiscovery} if supported, or <code>null</code>
     */
    ServiceDiscovery create(URL connectionURL);

    /**
     * The priority of current {@link ServiceDiscoveryFactory}
     *
     * @return The {@link Integer#MIN_VALUE minimum integer} indicates the highest priority, in contrast，
     * the lowest priority is {@link Integer#MAX_VALUE the maximum integer}
     */
    default int getPriority() {
        return Integer.MAX_VALUE;
    }

    /**
     * Compares its priority
     *
     * @param that {@link ServiceDiscovery}
     * @return
     */
    @Override
    default int compareTo(ServiceDiscoveryFactory that) {
        return compare(this.getPriority(), that.getPriority());
    }

    /**
     * Get the default extension of {@link ServiceDiscoveryFactory}
     *
     * @return non-null
     */
    static ServiceDiscoveryFactory getDefaultExtension() {
        return getExtensionLoader(ServiceDiscoveryFactory.class).getDefaultExtension();
    }
}
