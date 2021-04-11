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
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.Protocol;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.registry.Constants.REGISTER_KEY;

/**
 * {@link SubscribedURLsSynthesizer} implementation for REST {@link Protocol protocol}
 *
 * @since 2.7.5
 */
public class RestProtocolSubscribedURLsSynthesizer implements SubscribedURLsSynthesizer {

    @Override
    public boolean supports(URL subscribedURL) {
        return "rest".equals(subscribedURL.getProtocol()) ||
                "rest".equals(subscribedURL.getParameter(PROTOCOL_KEY));
    }

    @Override
    public List<URL> synthesize(URL subscribedURL, Collection<ServiceInstance> serviceInstances) {

        String protocol = subscribedURL.getParameter(PROTOCOL_KEY);

        return serviceInstances.stream().map(serviceInstance -> {
            URLBuilder urlBuilder = new URLBuilder()
                    .setProtocol(protocol)
                    .setHost(serviceInstance.getHost())
                    .setPort(serviceInstance.getPort())
                    .setPath(subscribedURL.getServiceInterface())
                    .addParameter(SIDE_KEY, PROVIDER)
                    .addParameter(APPLICATION_KEY, serviceInstance.getServiceName())
                    .addParameter(REGISTER_KEY, TRUE.toString());

            return urlBuilder.build();
        }).collect(Collectors.toList());
    }

}
