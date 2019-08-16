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

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.metadata.WritableMetadataService.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.registry.integration.RegistryProtocol.DEFAULT_REGISTER_PROVIDER_KEYS;

/**
 * The Utilities class for the {@link ServiceInstance#getMetadata() metadata of the service instance}
 *
 * @see ServiceInstance#getMetadata()
 * @see MetadataService
 * @see URL
 * @since 2.7.4
 */
public class ServiceInstanceMetadataUtils {

    /**
     * The prefix of {@link MetadataService} : "dubbo.metadata-service."
     */
    public static final String METADATA_SERVICE_PREFIX = "dubbo.metadata-service.";

    /**
     * The key of metadata JSON of {@link MetadataService}'s {@link URL}
     */
    public static String METADATA_SERVICE_URL_PARAMS_KEY = METADATA_SERVICE_PREFIX + "url-params";

    /**
     * The {@link URL URLs} property name of {@link MetadataService} :
     * "dubbo.metadata-service.urls", which is used to be compatible with Dubbo Spring Cloud and
     * discovery the metadata of instance
     */
    public static final String METADATA_SERVICE_URLS_PROPERTY_NAME = METADATA_SERVICE_PREFIX + "urls";

    /**
     * The key of The revision for all exported Dubbo services.
     */
    public static String EXPORTED_SERVICES_REVISION_KEY = "dubbo.exported-services.revision";

    /**
     * The key of The revision for all subscribed Dubbo services.
     */
    public static String SUBSCRIBER_SERVICES_REVISION_KEY = "dubbo.subscribed-services.revision";

    /**
     * The key of metadata storage type.
     */
    public static String METADATA_STORAGE_TYPE_KEY = "dubbo.metadata.storage-type";

    /**
     * The {@link URL url's} parameter name of Dubbo Provider host
     */
    public static final String HOST_PARAM_NAME = "provider.host";

    /**
     * The {@link URL url's} parameter name of Dubbo Provider port
     */
    public static final String PORT_PARAM_NAME = "provider.port";

    /**
     * Get the multiple {@link URL urls'} parameters of {@link MetadataService MetadataService's} Metadata
     *
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return non-null {@link Map}, the key is {@link URL#getProtocol() the protocol of URL}, the value is
     * {@link #getMetadataServiceURLParams(ServiceInstance, String)}
     */
    public static Map<String, Map<String, Object>> getMetadataServiceURLsParams(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String param = metadata.get(METADATA_SERVICE_URL_PARAMS_KEY);
        return isBlank(param) ? emptyMap() : (Map) JSON.parse(param);
    }

    /**
     * Get the {@link URL url's} parameters of {@link MetadataService MetadataService's} Metadata
     *
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return non-null {@link Map}
     */
    public static Map<String, Object> getMetadataServiceURLParams(ServiceInstance serviceInstance, String protocol) {
        Map<String, Map<String, Object>> params = getMetadataServiceURLsParams(serviceInstance);
        return params.getOrDefault(protocol, emptyMap());
    }

    /**
     * The provider port from {@link ServiceInstance the specified service instance}
     *
     * @param serviceInstance {@link ServiceInstance the specified service instance}
     * @param protocol        the protocol name
     * @return The protocol port if found, or <code>null</code>
     */
    public static Integer getProviderPort(ServiceInstance serviceInstance, String protocol) {
        Map<String, Object> params = getMetadataServiceURLParams(serviceInstance, protocol);
        return getProviderPort(params);
    }

    public static String getProviderHost(ServiceInstance serviceInstance, String protocol) {
        Map<String, Object> params = getMetadataServiceURLParams(serviceInstance, protocol);
        return getProviderHost(params);
    }

    public static String getMetadataServiceParameter(List<URL> urls) {
        Map<String, Map<String, String>> params = new HashMap<>();

        urls.forEach(url -> {
            String protocol = url.getProtocol();
            params.put(protocol, getParams(url));
        });

        if (params.isEmpty()) {
            return null;
        }

        return JSON.toJSONString(params);
    }

    private static Map<String, String> getParams(URL providerURL) {
        Map<String, String> params = new LinkedHashMap<>();
        setDefaultParams(params, providerURL);
        // set provider host
        setProviderHostParam(params, providerURL);
        // set provider port
        setProviderPortParam(params, providerURL);
        return params;
    }

    public static String getProviderHost(Map<String, Object> params) {
        return valueOf(params.get(HOST_PARAM_NAME));
    }

    public static Integer getProviderPort(Map<String, Object> params) {
        return Integer.valueOf(valueOf(params.get(PORT_PARAM_NAME)));
    }

    /**
     * The revision for all exported Dubbo services from the specified {@link ServiceInstance}.
     *
     * @param serviceInstance the specified {@link ServiceInstance}
     * @return <code>null</code> if not exits
     */
    public static String getExportedServicesRevision(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        return metadata.get(EXPORTED_SERVICES_REVISION_KEY);
    }

    /**
     * The revision for all subscribed Dubbo services from the specified {@link ServiceInstance}.
     *
     * @param serviceInstance the specified {@link ServiceInstance}
     * @return <code>null</code> if not exits
     */
    public static String getSubscribedServicesRevision(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        return metadata.get(SUBSCRIBER_SERVICES_REVISION_KEY);
    }

    /**
     * Get metadata's storage type
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return if not found in {@link URL#getParameters() parameters} of {@link URL registry URL}, return
     * {@link WritableMetadataService#DEFAULT_METADATA_STORAGE_TYPE "default"}
     */
    public static String getMetadataStorageType(URL registryURL) {
        return registryURL.getParameter(METADATA_STORAGE_TYPE_KEY, DEFAULT_METADATA_STORAGE_TYPE);
    }

    /**
     * Get the metadata's storage type is used to which {@link WritableMetadataService} instance.
     *
     * @param serviceInstance the specified {@link ServiceInstance}
     * @return if not found in {@link ServiceInstance#getMetadata() metadata} of {@link ServiceInstance}, return
     * {@link WritableMetadataService#DEFAULT_METADATA_STORAGE_TYPE "default"}
     */
    public static String getMetadataStorageType(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        return metadata.getOrDefault(METADATA_STORAGE_TYPE_KEY, DEFAULT_METADATA_STORAGE_TYPE);
    }

    /**
     * Set the metadata storage type in specified {@link ServiceInstance service instance}
     *
     * @param serviceInstance      {@link ServiceInstance service instance}
     * @param isDefaultStorageType is default storage type or not
     */
    public static void setMetadataStorageType(ServiceInstance serviceInstance, boolean isDefaultStorageType) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        metadata.put(METADATA_STORAGE_TYPE_KEY, WritableMetadataService.getMetadataStorageType(isDefaultStorageType));
    }

    private static void setProviderHostParam(Map<String, String> params, URL providerURL) {
        params.put(HOST_PARAM_NAME, providerURL.getHost());
    }

    private static void setProviderPortParam(Map<String, String> params, URL providerURL) {
        params.put(PORT_PARAM_NAME, valueOf(providerURL.getPort()));
    }

    /**
     * Set the default parameters via the specified {@link URL providerURL}
     *
     * @param params      the parameters
     * @param providerURL the provider's {@link URL}
     */
    private static void setDefaultParams(Map<String, String> params, URL providerURL) {
        for (String parameterName : DEFAULT_REGISTER_PROVIDER_KEYS) {
            String parameterValue = providerURL.getParameter(parameterName);
            if (!isBlank(parameterValue)) {
                params.put(parameterName, parameterValue);
            }
        }
    }
}
