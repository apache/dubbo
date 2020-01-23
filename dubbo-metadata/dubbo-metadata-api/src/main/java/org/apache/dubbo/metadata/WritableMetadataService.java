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
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_TYPE_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * Local {@link MetadataService} that extends {@link MetadataService} and provides the modification, which is used for
 * Dubbo's consumers and providers.
 *
 * @since 2.7.5
 */
@SPI(DEFAULT_METADATA_STORAGE_TYPE)
public interface WritableMetadataService extends MetadataService {
    /**
     * Gets the current Dubbo Service name
     *
     * @return non-null
     */
    @Override
    default String serviceName() {
        return ApplicationModel.getApplication();
    }

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
     * fresh Exports
     *
     * @return If success , return <code>true</code>
     */
    default boolean refreshMetadata(String exportedRevision, String subscribedRevision) {
        return true;
    }

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

    void publishServiceDefinition(URL providerUrl);

    /**
     * Get {@link ExtensionLoader#getDefaultExtension() the defautl extension} of {@link WritableMetadataService}
     *
     * @return non-null
     * @see InMemoryWritableMetadataService
     */
    static WritableMetadataService getDefaultExtension() {
        return getExtensionLoader(WritableMetadataService.class).getDefaultExtension();
    }

    /**
     * Get
     *
     * @param paramMap
     * @return
     */
    static WritableMetadataService getExtensionForCompatible(Map<String, String> paramMap){
        String metadataType = paramMap.get(METADATA_TYPE_KEY);
        if(StringUtils.isEmpty(metadataType)){
            /**
             * Since 2.7.5  in dubbo.xsd "metadata" has been modified to "metadata-type".
             * But a bug occurs which leads  "metadata-type" not work, some developers maybe
             * use self-defined such as flowing to make metadata reporter work:
             * <dubbo:parameter key="metadata" value="remote"/>
             * @see https://github.com/apache/dubbo/issues/5667
             *
             * The following code aims to be compatible with this case.
             */
            metadataType = paramMap.get(METADATA_KEY);
        }
        return getExtension(StringUtils.isEmpty(metadataType) ? DEFAULT_METADATA_STORAGE_TYPE : metadataType);
    }

    static WritableMetadataService getExtension(String name) {
        return getExtensionLoader(WritableMetadataService.class).getOrDefaultExtension(name);
    }
}