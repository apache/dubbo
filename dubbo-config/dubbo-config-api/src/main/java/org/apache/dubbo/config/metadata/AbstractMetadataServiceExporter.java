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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.MetadataServiceType;
import org.apache.dubbo.metadata.WritableMetadataService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.EnumSet.of;
import static org.apache.dubbo.metadata.MetadataServiceType.getOrDefault;

/**
 * The abstract implementation of {@link MetadataServiceExporter} to provider the commons features for sub-types
 *
 * @see MetadataServiceExporter
 * @see MetadataService
 * @since 2.7.8
 */
public abstract class AbstractMetadataServiceExporter implements MetadataServiceExporter {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final WritableMetadataService metadataService;

    private final int priority;

    private final Set<MetadataServiceType> supportedMetadataServiceTypes;

    private volatile boolean exported = false;

    public AbstractMetadataServiceExporter(String metadataType,
                                           int priority,
                                           MetadataServiceType supportMetadataServiceType,
                                           MetadataServiceType... otherSupportMetadataServiceTypes) {
        this(metadataType, priority, of(supportMetadataServiceType, otherSupportMetadataServiceTypes));
    }

    public AbstractMetadataServiceExporter(String metadataType,
                                           int priority,
                                           Set<MetadataServiceType> supportedMetadataServiceTypes) {
        this.metadataService = WritableMetadataService.getExtension(metadataType);
        this.priority = priority;
        this.supportedMetadataServiceTypes = supportedMetadataServiceTypes;
    }

    @Override
    public final MetadataServiceExporter export() {
        if (!isExported()) {
            try {
                doExport();
                exported = true;
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Exporting the MetadataService fails", e);
                }
                exported = false;
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("The MetadataService has been exported : " + getExportedURLs());
            }
        }
        return this;
    }

    @Override
    public final MetadataServiceExporter unexport() {
        if (isExported()) {
            try {
                doUnexport();
                exported = false;
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("UnExporting the MetadataService fails", e);
                }
            }
        }
        return this;
    }

    @Override
    public List<URL> getExportedURLs() {
        return metadataService
                .getExportedURLs()
                .stream()
                .map(URL::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isExported() {
        return exported;
    }

    @Override
    public final boolean supports(String metadataType) {
        MetadataServiceType metadataServiceType = getOrDefault(metadataType);
        return supportedMetadataServiceTypes.contains(metadataServiceType);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Exports the {@link MetadataService}
     *
     * @throws Exception If some exception occurs
     */
    protected abstract void doExport() throws Exception;

    /**
     * Unexports the {@link MetadataService}
     *
     * @throws Exception If some exception occurs
     */
    protected abstract void doUnexport() throws Exception;

    /**
     * Get the underlying of {@link MetadataService}
     *
     * @return non-null
     */
    public WritableMetadataService getMetadataService() {
        return metadataService;
    }
}
