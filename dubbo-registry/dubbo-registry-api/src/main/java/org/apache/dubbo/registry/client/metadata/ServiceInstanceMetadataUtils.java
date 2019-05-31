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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.remoting.Constants.DUBBO_VERSION_KEY;

/**
 * The Utilities class for the {@link ServiceInstance#getMetadata() metadata of the service instance}
 *
 * @see ServiceInstance#getMetadata()
 * @see MetadataService
 * @see URL
 * @since 2.7.3
 */
public class ServiceInstanceMetadataUtils {

    /**
     * The prefix of protocols
     */
    public static final String PROTOCOL_PREFIX = "dubbo.protocols.";

    /**
     * The name {@link String#format(String, Object...) format pattern} for the protocol port
     */
    public static String PROTOCOL_PORT_METADATA_KEY_PATTERN = PROTOCOL_PREFIX + "%s.port";

    /**
     * The required {@link URL} parameter names of {@link MetadataService}
     */
    private static String[] METADATA_SERVICE_URL_PARAM_NAMES = {
            APPLICATION_KEY,
            BIND_IP_KEY,
            BIND_PORT_KEY,
            DUBBO_VERSION_KEY,
            RELEASE_KEY,
            SIDE_KEY,
            VERSION_KEY
    };

    /**
     * Build the metadata key of the protocol port
     *
     * @param protocol the name of protocol
     * @return non-null
     */
    public static String protocolPortMetadataKey(String protocol) {
        return format(PROTOCOL_PORT_METADATA_KEY_PATTERN, protocol);
    }

    /**
     * The protocol port from {@link ServiceInstance the specified service instance}
     *
     * @param serviceInstance {@link ServiceInstance the specified service instance}
     * @param protocol        the protocol name
     * @return The protocol port if found, or <code>null</code>
     */
    public static Integer getProtocolPort(ServiceInstance serviceInstance, String protocol) {
        Map<String, Integer> protocolPorts = getProtocolPorts(serviceInstance);
        return protocolPorts.get(protocol);
    }

    /**
     * Get a {@link Map} of the protocol ports from the metadata {@link Map}
     *
     * @param serviceInstance the {@link ServiceInstance service instance}
     * @return the key is the name of protocol, and the value is the port of protocol
     */
    public static Map<String, Integer> getProtocolPorts(ServiceInstance serviceInstance) {
        return getProtocolPorts(serviceInstance.getMetadata());
    }

    /**
     * Get a {@link Map} of the protocol ports from the metadata {@link Map}
     *
     * @param metadata the metadata {@link Map}
     * @return the key is the name of protocol, and the value is the port of protocol
     */
    public static Map<String, Integer> getProtocolPorts(Map<String, String> metadata) {
        Map<String, Integer> protocolPorts = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key.startsWith(PROTOCOL_PREFIX)) {
                String[] parts = StringUtils.split(key, '.');
                String protocol = parts[2];
                protocolPorts.put(protocol, Integer.valueOf(value));
            }
        });
        return protocolPorts;
    }

    public static String getMetadataServiceParameter(List<URL> urls) {
        Map<String, Map<String, String>> params = new HashMap<>();

        urls.forEach(url -> {
            String protocol = url.getProtocol();
            params.put(protocol, getParams(url, METADATA_SERVICE_URL_PARAM_NAMES));
        });

        if (params.isEmpty()) {
            return null;
        }

        return JSON.toJSONString(params);
    }

    private static Map<String, String> getParams(URL url, String... parameterNames) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String parameterName : parameterNames) {
            String parameterValue = url.getParameter(parameterName);
            if (!isBlank(parameterName)) {
                params.put(parameterName, parameterValue);
            }
        }
        return params;
    }
}
