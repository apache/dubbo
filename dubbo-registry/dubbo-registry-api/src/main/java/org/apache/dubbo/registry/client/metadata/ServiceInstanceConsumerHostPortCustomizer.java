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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_INIT_SERIALIZATION_OPTIMIZER;

public class ServiceInstanceConsumerHostPortCustomizer implements ServiceInstanceCustomizer, Prioritized {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(ServiceInstanceConsumerHostPortCustomizer.class);

    @Override
    public void customize(ServiceInstance serviceInstance, ApplicationModel applicationModel) {
        // need configure registryConsumer=true and metadata-service-port
        Boolean registerConsumer = applicationModel.getCurrentConfig().getRegisterConsumer();
        if (registerConsumer == null || !registerConsumer) {
            return;
        }
        // serviceInstance default host is null and port is 0
        if (serviceInstance.getHost() != null && serviceInstance.getPort() != 0) {
            return;
        }
        MetadataInfo metadataInfo = serviceInstance.getServiceMetadata();
        if (metadataInfo == null || CollectionUtils.isEmptyMap(metadataInfo.getSubscribedServiceURLs())) {
            return;
        }
        Set<URL> urls = metadataInfo.collectSubscribedURLSet();
        if (CollectionUtils.isEmpty(urls)) {
            return;
        }
        try {
            URL url = urls.iterator().next();
            String host = url.getHost();
            int port = url.getParameter(METADATA_SERVICE_PORT_KEY, 0);
            if (host == null || port == 0) {
                logger.warn(
                        PROTOCOL_FAILED_INIT_SERIALIZATION_OPTIMIZER,
                        "typo in preferred protocol",
                        "",
                        "Host or port is not a valid value"
                                + "Please try check the config of dubbo.application.metadata-service-port");
            }

            if (serviceInstance instanceof DefaultServiceInstance) {
                DefaultServiceInstance instance = (DefaultServiceInstance) serviceInstance;
                instance.setHost(host);
                instance.setPort(port);
            }
        } catch (Exception e) {
            logger.error(
                    PROTOCOL_FAILED_INIT_SERIALIZATION_OPTIMIZER,
                    "typo in preferred protocol",
                    "",
                    "Error to fill consumer host and port.",
                    e);
        }
    }

    @Override
    public int getPriority() {
        // after serviceInstanceHostPortCustomizer
        return Prioritized.MIN_PRIORITY;
    }
}
