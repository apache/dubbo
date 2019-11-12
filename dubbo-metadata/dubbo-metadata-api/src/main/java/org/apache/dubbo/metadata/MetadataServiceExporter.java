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

import java.util.List;

/**
 * The exporter of {@link MetadataService}
 *
 * @see MetadataService
 * @see #export()
 * @see #unexport()
 * @since 2.7.5
 */
public interface MetadataServiceExporter {

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
}

