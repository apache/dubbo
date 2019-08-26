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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataServiceURLsParams;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getProtocolPort;

/**
 * The {@link MetadataServiceURLBuilder} implementation for The standard Dubbo scenario
 *
 * @see MetadataService
 * @since 2.7.4
 */
public class StandardMetadataServiceURLBuilder implements MetadataServiceURLBuilder {

    /**
     * Build the {@link URL urls} from {@link ServiceInstance#getMetadata() the metadata} of {@link ServiceInstance}
     *
     * @param serviceInstance {@link ServiceInstance}
     * @return the not-null {@link List}
     */
    @Override
    public List<URL> build(ServiceInstance serviceInstance) {

        Map<String, Map<String, Object>> paramsMap = getMetadataServiceURLsParams(serviceInstance);

        List<URL> urls = new ArrayList<>(paramsMap.size());

        String host = serviceInstance.getHost();

        for (Map.Entry<String, Map<String, Object>> entry : paramsMap.entrySet()) {
            String protocol = entry.getKey();
            Integer port = getProtocolPort(serviceInstance, protocol);
            URLBuilder urlBuilder = new URLBuilder()
                    .setHost(host)
                    .setPort(port)
                    .setProtocol(protocol)
                    .setPath(MetadataService.class.getName());

            // add parameters
            entry.getValue().forEach((name, value) -> urlBuilder.addParameter(name, valueOf(value)));

            urls.add(urlBuilder.build());
        }

        return urls;
    }
}
