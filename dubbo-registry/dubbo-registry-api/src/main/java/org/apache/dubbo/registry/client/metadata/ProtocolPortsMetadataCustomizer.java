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
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.Protocol;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.metadata.WritableMetadataService.getExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataStorageType;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setEndpoints;

/**
 * A Class to customize the ports of {@link Protocol protocols} into
 * {@link ServiceInstance#getMetadata() the metadata of service instance}
 *
 * @since 2.7.5
 */
public class ProtocolPortsMetadataCustomizer implements ServiceInstanceCustomizer {

    @Override
    public void customize(ServiceInstance serviceInstance) {

        String metadataStoredType = getMetadataStorageType(serviceInstance);

        WritableMetadataService writableMetadataService = getExtension(metadataStoredType);

        Map<String, Integer> protocols = new HashMap<>();
        writableMetadataService.getExportedURLs()
                .stream()
                .map(URL::valueOf)
                .filter(url -> !MetadataService.class.getName().equals(url.getServiceInterface()))
                .forEach(url -> {
                    // TODO, same protocol listen on different ports will override with each other.
                    protocols.put(url.getProtocol(), url.getPort());
                });

        setEndpoints(serviceInstance, protocols);
    }
}
