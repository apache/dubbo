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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.lang.Prioritized;

import java.util.Set;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The interface for Dubbo service name Mapping
 *
 * @since 2.7.5
 */
@SPI("default")
public interface ServiceNameMapping extends Prioritized {

    /**
     * Map the specified Dubbo service interface, group, version and protocol to current Dubbo service name
     *
     * @param serviceInterface the class name of Dubbo service interface
     * @param group            the group of Dubbo service interface (optional)
     * @param version          the version of Dubbo service interface version (optional)
     * @param protocol         the protocol of Dubbo service interface exported (optional)
     * @deprecated 2.7.8 This method will be removed since 3.0
     */
    @Deprecated
    default void map(String serviceInterface, String group, String version, String protocol) {
        throw new UnsupportedOperationException("This method has been deprecated and should not be invoked!");
    }

    /**
     * Map the specified Dubbo service {@link URL} to current Dubbo service name
     *
     * @param exportedURL the {@link URL} that the Dubbo Provider exported
     * @since 2.7.8
     */
    void map(URL exportedURL);

    /**
     * Get the service names from the specified Dubbo service interface, group, version and protocol
     *
     * @param serviceInterface the class name of Dubbo service interface
     * @param group            the group of Dubbo service interface (optional)
     * @param version          the version of Dubbo service interface version (optional)
     * @param protocol         the protocol of Dubbo service interface exported (optional)
     * @return non-null {@link Set}
     * @deprecated 2.7.8 This method will be removed since 3.0
     */
    @Deprecated
    default Set<String> get(String serviceInterface, String group, String version, String protocol) {
        throw new UnsupportedOperationException("This method has been deprecated and should not be invoked!");
    }

    /**
     * Get the service names from the subscribed Dubbo service {@link URL}
     *
     * @param subscribedURL the {@link URL} that the Dubbo consumer subscribed
     * @return non-null {@link Set}
     * @since 2.7.8
     */
    Set<String> get(URL subscribedURL);

    /**
     * Get the default extension of {@link ServiceNameMapping}
     *
     * @return non-null {@link ServiceNameMapping}
     * @see DynamicConfigurationServiceNameMapping
     */
    static ServiceNameMapping getDefaultExtension() {
        return getExtensionLoader(ServiceNameMapping.class).getDefaultExtension();
    }
}
