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
package org.apache.dubbo.metadata.export;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.MetadataService;

import java.util.List;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The exporter of {@link MetadataService}
 *
 * @see MetadataService
 * @see #export()
 * @see #unexport()
 * @since 2.7.2
 */
@SPI("default")
public interface MetadataServiceExporter {

    /**
     * Exports the {@link MetadataService} as a Dubbo service
     *
     * @return the exported {@link URL URLs}
     */
    List<URL> export();

    /**
     * Unexports the {@link MetadataService}
     */
    void unexport();


    /**
     * Get {@link ExtensionLoader#getDefaultExtension() the defautl extension} of {@link MetadataServiceExporter}
     *
     * @return non-null
     * @see MetadataServiceExporter
     * @see ConfigurableMetadataServiceExporter
     * @see ExtensionLoader
     */
    static MetadataServiceExporter getDefaultExtension() {
        return getExtensionLoader(MetadataServiceExporter.class).getDefaultExtension();
    }
}
