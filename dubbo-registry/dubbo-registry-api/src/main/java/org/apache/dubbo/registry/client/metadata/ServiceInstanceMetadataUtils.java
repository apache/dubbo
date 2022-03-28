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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.DefaultServiceInstance.Endpoint;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.registry.integration.InterfaceCompatibleRegistryProtocol.DEFAULT_REGISTER_PROVIDER_KEYS;
import static org.apache.dubbo.rpc.Constants.DEPRECATED_KEY;

/**
 * The Utilities class for the {@link ServiceInstance#getMetadata() metadata of the service instance}
 *
 * @see StandardMetadataServiceURLBuilder
 * @see ServiceInstance#getMetadata()
 * @see MetadataService
 * @see URL
 * @since 2.7.5
 */
public class ServiceInstanceMetadataUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInstanceMetadataUtils.class);

    /**
     * The prefix of {@link MetadataService} : "dubbo.metadata-service."
     */
    public static final String METADATA_SERVICE_PREFIX = "dubbo.metadata-service.";

    public static final String ENDPOINTS = "dubbo.endpoints";

    /**
     * The property name of metadata JSON of {@link MetadataService}'s {@link URL}
     */
    public static final String METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME = METADATA_SERVICE_PREFIX + "url-params";

    /**
     * The {@link URL URLs} property name of {@link MetadataService} :
     * "dubbo.metadata-service.urls", which is used to be compatible with Dubbo Spring Cloud and
     * discovery the metadata of instance
     */
    public static final String METADATA_SERVICE_URLS_PROPERTY_NAME = METADATA_SERVICE_PREFIX + "urls";

    /**
     * The property name of The revision for all exported Dubbo services.
     */
    public static final String EXPORTED_SERVICES_REVISION_PROPERTY_NAME = "dubbo.metadata.revision";

    /**
     * The property name of metadata storage type.
     */
    public static final String METADATA_STORAGE_TYPE_PROPERTY_NAME = "dubbo.metadata.storage-type";

    public static final String METADATA_CLUSTER_PROPERTY_NAME = "dubbo.metadata.cluster";

    public static final String INSTANCE_REVISION_UPDATED_KEY = "dubbo.instance.revision.updated";

    public static final Gson gson = new Gson();

    public static String getMetadataServiceParameter(URL url) {
        if (url == null) {
            return "";
        }
        url = url.removeParameters(APPLICATION_KEY, GROUP_KEY, DEPRECATED_KEY, TIMESTAMP_KEY);
        Map<String, String> params = getParams(url);

        if (params.isEmpty()) {
            return null;
        }

        return gson.toJson(params);
    }

    private static Map<String, String> getParams(URL providerURL) {
        Map<String, String> params = new LinkedHashMap<>();
        setDefaultParams(params, providerURL);
        params.put(PORT_KEY, String.valueOf(providerURL.getPort()));
        params.put(PROTOCOL_KEY, providerURL.getProtocol());
        return params;
    }

    /**
     * The revision for all exported Dubbo services from the specified {@link ServiceInstance}.
     *
     * @param serviceInstance the specified {@link ServiceInstance}
     * @return <code>null</code> if not exits
     */
    public static String getExportedServicesRevision(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        return metadata.get(EXPORTED_SERVICES_REVISION_PROPERTY_NAME);
    }

    /**
     * Get metadata's storage type
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return if not found in {@link URL#getParameters() parameters} of {@link URL registry URL}, return
     */
    public static String getMetadataStorageType(URL registryURL) {
        return registryURL.getParameter(METADATA_STORAGE_TYPE_PROPERTY_NAME, DEFAULT_METADATA_STORAGE_TYPE);
    }

    /**
     * Get the metadata storage type specified by the peer instance.
     * @return storage type, remote or local
     */
    public static String getMetadataStorageType(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        return metadata.getOrDefault(METADATA_STORAGE_TYPE_PROPERTY_NAME, DEFAULT_METADATA_STORAGE_TYPE);
    }

    /**
     * Set the metadata storage type in specified {@link ServiceInstance service instance}
     *
     * @param serviceInstance {@link ServiceInstance service instance}
     * @param metadataType    remote or local
     */
    public static void setMetadataStorageType(ServiceInstance serviceInstance, String metadataType) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        metadata.put(METADATA_STORAGE_TYPE_PROPERTY_NAME, metadataType);
    }

    public static String getRemoteCluster(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        return metadata.get(METADATA_CLUSTER_PROPERTY_NAME);
    }

    public static boolean hasEndpoints(ServiceInstance serviceInstance) {
        return StringUtils.isNotEmpty(serviceInstance.getMetadata().get(ENDPOINTS));
    }

    public static void setEndpoints(ServiceInstance serviceInstance, Map<String, Integer> protocolPorts) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        List<Endpoint> endpoints = new ArrayList<>();
        protocolPorts.forEach((k, v) -> {
            Endpoint endpoint = new Endpoint(v, k);
            endpoints.add(endpoint);
        });

        metadata.put(ENDPOINTS, gson.toJson(endpoints));
    }

    /**
     * Get the property value of port by the specified {@link ServiceInstance#getMetadata() the metadata of
     * service instance} and protocol
     *
     * @param serviceInstance {@link ServiceInstance service instance}
     * @param protocol        the name of protocol, e.g, dubbo, rest, and so on
     * @return if not found, return <code>null</code>
     */
    public static Endpoint getEndpoint(ServiceInstance serviceInstance, String protocol) {
        List<Endpoint> endpoints = ((DefaultServiceInstance) serviceInstance).getEndpoints();
        if (endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                if (endpoint.getProtocol().equals(protocol)) {
                    return endpoint;
                }
            }
        }
        return null;
    }

    public static void registerMetadataAndInstance(ApplicationModel applicationModel) {
        LOGGER.info("Start registering instance address to registry.");
        RegistryManager registryManager = applicationModel.getBeanFactory().getBean(RegistryManager.class);
        // register service instance
        registryManager.getServiceDiscoveries().forEach(ServiceDiscovery::register);
    }

    public static void refreshMetadataAndInstance(ApplicationModel applicationModel) {
        RegistryManager registryManager = applicationModel.getBeanFactory().getBean(RegistryManager.class);
        // update service instance revision
        registryManager.getServiceDiscoveries().forEach(ServiceDiscovery::update);
    }

    public static void unregisterMetadataAndInstance(ApplicationModel applicationModel) {
        RegistryManager registryManager = applicationModel.getBeanFactory().getBean(RegistryManager.class);
        registryManager.getServiceDiscoveries().forEach(serviceDiscovery -> {
            try {
                serviceDiscovery.unregister();
            } catch (Exception ignored) {
                // ignored
            }
        });
    }

    public static void customizeInstance(ServiceInstance instance, ApplicationModel applicationModel) {
        ExtensionLoader<ServiceInstanceCustomizer> loader =
                instance.getOrDefaultApplicationModel().getExtensionLoader(ServiceInstanceCustomizer.class);
        // FIXME, sort customizer before apply
        loader.getSupportedExtensionInstances().forEach(customizer -> {
            // customize
            customizer.customize(instance, applicationModel);
        });
    }

    public static boolean isValidInstance(ServiceInstance instance) {
        return instance != null && instance.getHost() != null && instance.getPort() != 0;
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
