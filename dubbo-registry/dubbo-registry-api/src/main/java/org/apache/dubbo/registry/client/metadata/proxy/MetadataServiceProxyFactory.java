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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * A factory to create a {@link MetadataService} proxy
 *
 * @see ServiceInstance
 * @see MetadataService
 * @since 2.7.5
 */
@SPI(DEFAULT_METADATA_STORAGE_TYPE)
public interface MetadataServiceProxyFactory {

    /**
     * Create a {@link MetadataService} proxy via the specified {@link ServiceInstance}
     *WritableMetadataService
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return non-null
     */
    MetadataService getProxy(ServiceInstance serviceInstance);

    /**
     * Get the default extension of {@link MetadataServiceProxyFactory}
     *
     * @return non-null
     */
    static MetadataServiceProxyFactory getDefaultExtension() {
        return getExtensionLoader(MetadataServiceProxyFactory.class).getDefaultExtension();
    }

    static MetadataServiceProxyFactory getExtension(String name) {
        return getExtensionLoader(MetadataServiceProxyFactory.class).getExtension(name);
    }
}
