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
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setEndpoints;

/**
 * A Class to customize the ports of {@link Protocol protocols} into
 * {@link ServiceInstance#getMetadata() the metadata of service instance}
 *
 * @since 2.7.5
 */
public class ProtocolPortsMetadataCustomizer implements ServiceInstanceCustomizer {
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(ProtocolPortsMetadataCustomizer.class);
    @Override
    public void customize(ServiceInstance serviceInstance, ApplicationModel applicationModel) {
        MetadataInfo metadataInfo = serviceInstance.getServiceMetadata();
        if (metadataInfo == null || CollectionUtils.isEmptyMap(metadataInfo.getExportedServiceURLs())) {
            return;
        }

        Map<String, Integer> protocols = new HashMap<>();
        Set<URL> urls = metadataInfo.collectExportedURLSet();
        urls.forEach(url -> {
            // TODO, same protocol listen on different ports will override with each other.
            String protocol = url.getProtocol();
            Integer oldPort = protocols.get(protocol);
            int newPort = url.getPort();
            if (oldPort != null && oldPort != newPort) {
                LOGGER.warn(LoggerCodeConstants.PROTOCOL_INCORRECT_PARAMETER_VALUES, "the protocol is listening multiple ports", "", "Same protocol " + "[" + protocol + "]" + " listens on different ports " + "[" + oldPort + "," + newPort + "]" + " will override with each other" +
                    ". The port [" + oldPort + "] is overridden with port [" + newPort + "].");
            }
            protocols.put(protocol, newPort);
        });

        if (protocols.size() > 0) {// set endpoints only for multi-protocol scenario
            setEndpoints(serviceInstance, protocols);
        }
    }
}
