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
package org.apache.dubbo.config.event.listener;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.event.ReferenceConfigInitializedEvent;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.event.GenericEventListener;
import org.apache.dubbo.metadata.WritableMetadataService;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.metadata.WritableMetadataService.getExtension;

/**
 * An {@link EventListener} {@link WritableMetadataService#publishServiceDefinition(URL) publishs the service definition}
 * when {@link ServiceConfigExportedEvent the event of the exported Dubbo service} and
 * {@link ReferenceConfigInitializedEvent the event of the referenced Dubbo service} is raised.
 *
 * @see GenericEventListener
 * @see ServiceConfigExportedEvent
 * @see ReferenceConfigInitializedEvent
 * @since 2.7.8
 */
public class PublishingServiceDefinitionListener extends GenericEventListener {

    public void onEvent(ReferenceConfigInitializedEvent event) {
        handleEvent(event.getReferenceConfig());
    }

    public void onEvent(ServiceConfigExportedEvent event) {
        handleEvent(event.getServiceConfig());
    }

    private void handleEvent(AbstractInterfaceConfig config) {
        String metadataType = getMetadataType(config);
        for (URL exportedURL : config.getExportedUrls()) {
            WritableMetadataService metadataService = getExtension(metadataType);
            if (metadataService != null) {
                metadataService.publishServiceDefinition(exportedURL);
            }
        }
    }

    private String getMetadataType(AbstractInterfaceConfig config) {
        ApplicationConfig applicationConfig = config.getApplication();
        String metadataType = applicationConfig.getMetadataType();
        if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
            MetadataReportConfig metadataReportConfig = config.getMetadataReportConfig();
            if (metadataReportConfig == null || !metadataReportConfig.isValid()) {
                metadataType = DEFAULT_METADATA_STORAGE_TYPE;
            }
        }
        return metadataType;
    }
}
