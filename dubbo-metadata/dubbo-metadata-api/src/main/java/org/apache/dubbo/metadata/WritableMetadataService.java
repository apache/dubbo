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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.Map;
import java.util.Set;

/**
 * Local {@link MetadataService} that extends {@link MetadataService} and provides the modification, which is used for
 * Dubbo's consumers and providers.
 *
 * @since 2.7.5
 */
@SPI(value = "default", scope = ExtensionScope.APPLICATION)
public interface WritableMetadataService extends MetadataService {

    /**
     * Exports a {@link URL}
     *
     * @param url a {@link URL}
     * @return If success , return <code>true</code>
     */
    boolean exportURL(URL url);

    /**
     * Unexports a {@link URL}
     *
     * @param url a {@link URL}
     * @return If success , return <code>true</code>
     */
    boolean unexportURL(URL url);

    /**
     * Subscribes a {@link URL}
     *
     * @param url a {@link URL}
     * @return If success , return <code>true</code>
     */
    boolean subscribeURL(URL url);

    /**
     * Unsubscribes a {@link URL}
     *
     * @param url a {@link URL}
     * @return If success , return <code>true</code>
     */
    boolean unsubscribeURL(URL url);

    void publishServiceDefinition(URL url);

    default void setMetadataServiceURL(URL url) {

    }

    default URL getMetadataServiceURL() {
        return null;
    }

    void putCachedMapping(String serviceKey, Set<String> apps);

    Set<String> getCachedMapping(String mappingKey);

    Set<String> getCachedMapping(URL consumerURL);

    Set<String> removeCachedMapping(String serviceKey);

    Map<String, Set<String>> getCachedMapping();

    MetadataInfo getDefaultMetadataInfo();

    /**
     * Get {@link ExtensionLoader#getDefaultExtension() the defautl extension} of {@link WritableMetadataService}
     *
     * @return non-null
     */
    static WritableMetadataService getDefaultExtension(ScopeModel scopeModel) {
        return ScopeModelUtil.getExtensionLoader(WritableMetadataService.class, scopeModel).getDefaultExtension();
    }
}
