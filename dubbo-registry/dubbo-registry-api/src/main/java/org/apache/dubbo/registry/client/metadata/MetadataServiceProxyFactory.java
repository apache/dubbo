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

import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The factory of {@link MetadataService}'s {@link Proxy}
 *
 * @since 2.7.3
 */
public class MetadataServiceProxyFactory {

    private static final String DUBBO_METADATA_SERVICE_PARAMS_KEY = "dubbo.metadata-service.params";

    private final Protocol protocol;

    private final ProxyFactory proxyFactory;

    private Map<String, Map<String, MetadataService>> metadataServiceCache = new HashMap<>();

    public MetadataServiceProxyFactory() {
        this.protocol = getExtensionLoader(Protocol.class).getAdaptiveExtension();
        this.proxyFactory = getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    }

    public MetadataService createProxy(ServiceInstance serviceInstance) {

        URLBuilder urlBuilder = createURLBuilder(serviceInstance);

        Invoker<MetadataService> invoker = this.protocol.refer(MetadataService.class, urlBuilder.build());

        return proxyFactory.getProxy(invoker);
    }


    private URLBuilder createURLBuilder(ServiceInstance serviceInstance) {
        String serviceName = serviceInstance.getServiceName();
        Map<String, String> metadata = serviceInstance.getMetadata();
        String params = metadata.get(DUBBO_METADATA_SERVICE_PARAMS_KEY);
        Map<String, String> paramsMap = (Map) JSON.parse(params);
        URLBuilder urlBuilder = new URLBuilder()
                .setProtocol(paramsMap.get(PROTOCOL_KEY))
                .setHost(serviceInstance.getHost())
                .setPort(serviceInstance.getPort())
                .setPath(MetadataService.class.getName())
                .addParameter(GROUP_KEY, serviceName);

        // add parameters
        metadata.forEach((name, value) -> urlBuilder.addParameter(name, value));

        return urlBuilder;
    }

}
