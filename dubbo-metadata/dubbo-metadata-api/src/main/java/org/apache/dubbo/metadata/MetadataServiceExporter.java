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

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The exporter of {@link MetadataService}
 *
 * @see MetadataService
 * @see #export()
 * @see #unexport()
 * @since 2.7.5
 */
@SPI(DEFAULT_METADATA_STORAGE_TYPE)
public interface MetadataServiceExporter extends Prioritized {

    /**
     * Exports the {@link MetadataService} as a Dubbo service
     *
     * @return {@link MetadataServiceExporter itself}
     */
    MetadataServiceExporter export();

    /**
     * Unexports the {@link MetadataService}
     *
     * @return {@link MetadataServiceExporter itself}
     */
    MetadataServiceExporter unexport();

    /**
     * Get the {@link URL URLs} that were exported
     *
     * @return non-null
     */
    List<URL> getExportedURLs();

    /**
     * {@link MetadataService} is export or not
     *
     * @return if {@link #export()} was executed, return <code>true</code>, or <code>false</code>
     */
    boolean isExported();

    /**
     * Does current implementation support the specified metadata type?
     *
     * @param metadataType the specified metadata type
     * @return If supports, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    default boolean supports(String metadataType) {
        return true;
    }

    /**
     * Get the extension of {@link MetadataServiceExporter} by the type.
     * If not found, return the default implementation
     *
     * @param metadataType the metadata type
     * @return non-null
     * @since 2.7.8
     */
    static MetadataServiceExporter getExtension(String metadataType) {
        return getExtensionLoader(MetadataServiceExporter.class).getOrDefaultExtension(metadataType);
    }

    /**
     * Get the default extension of {@link MetadataServiceExporter}
     *
     * @return non-null
     * @since 2.7.8
     */
    static MetadataServiceExporter getDefaultExtension() {
        return getExtension(DEFAULT_METADATA_STORAGE_TYPE);
    }
}

