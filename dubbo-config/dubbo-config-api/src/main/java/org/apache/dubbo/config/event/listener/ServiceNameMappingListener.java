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
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.metadata.ServiceNameMapping;

import java.util.List;

import static org.apache.dubbo.metadata.ServiceNameMapping.getDefaultExtension;

/**
 * An {@link EventListener event listener} for mapping {@link ServiceConfig#getExportedUrls() the exported Dubbo
 * service inerface} to its service name
 *
 * @see ServiceNameMapping
 * @see ServiceConfig#getExportedUrls()
 * @since 2.7.5
 */
public class ServiceNameMappingListener implements EventListener<ServiceConfigExportedEvent> {

    private final ServiceNameMapping serviceNameMapping = getDefaultExtension();

    @Override
    public void onEvent(ServiceConfigExportedEvent event) {
        ServiceConfig serviceConfig = event.getServiceConfig();
        List<URL> exportedURLs = serviceConfig.getExportedUrls();
        exportedURLs.forEach(url -> {
            serviceNameMapping.map(url);
        });
    }
}
