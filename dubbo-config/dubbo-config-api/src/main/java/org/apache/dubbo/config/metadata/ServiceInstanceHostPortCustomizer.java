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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Set;

/**
 * The {@link ServiceInstanceCustomizer} to customize the {@link ServiceInstance#getPort() port} of service instance.
 *
 * @since 2.7.5
 */
public class ServiceInstanceHostPortCustomizer implements ServiceInstanceCustomizer {
    private static final Logger logger = LoggerFactory.getLogger(ServiceInstanceHostPortCustomizer.class);
    

    @Override
    public void customize(ServiceInstance serviceInstance) {

        if (serviceInstance.getPort() > 0) {
            return;
        }

        WritableMetadataService writableMetadataService = WritableMetadataService.getDefaultExtension(serviceInstance.getApplicationModel());

        String host = null;
        int port = -1;
        Set<URL> urls = writableMetadataService.getExportedServiceURLs();
        if (CollectionUtils.isNotEmpty(urls)) {
            ApplicationModel applicationModel = serviceInstance.getApplicationModel();
            String preferredProtocol = applicationModel.getCurrentConfig().getProtocol();
            if (preferredProtocol != null) {
                for (URL exportedURL : urls) {
                    if (preferredProtocol.equals(exportedURL.getProtocol())) {
                        host = exportedURL.getHost();
                        port = exportedURL.getPort();
                        break;
                    }
                }
                
                if (host == null || port == -1) {
                    logger.warn("The default preferredProtocol \"" + preferredProtocol + "\" is not found, fall back to the strategy that pick the first found protocol. Please try to modify the config of dubbo.application.protocol");
                    URL url = urls.iterator().next();
                    host = url.getHost();
                    port = url.getPort();
                }
            } else {
                URL url = urls.iterator().next();
                host = url.getHost();
                port = url.getPort();
            }
            
            if (serviceInstance instanceof DefaultServiceInstance) {
                DefaultServiceInstance instance = (DefaultServiceInstance) serviceInstance;
                instance.setHost(host);
                instance.setPort(port);
            }
        }
    }
}
