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
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.DefaultServiceInstance.Endpoint;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.registry.support.RegistryManager;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
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

    private static final Gson gson = new Gson();

    /**
     * Get the multiple {@link URL urls'} parameters of {@link MetadataService MetadataService's} Metadata
     *
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return non-null {@link Map}, the key is {@link URL#getProtocol() the protocol of URL}, the value is
     */
    public static Map<String, String> getMetadataServiceURLsParams(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String param = metadata.get(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME);
        return isBlank(param) ? emptyMap() : (Map) gson.fromJson(param,Map.class);
    }

    public static String getMetadataServiceParameter(URL url) {
        if (url == null) {
            return "";
        }
        url = url.removeParameter(APPLICATION_KEY);
        url = url.removeParameter(GROUP_KEY);
        url = url.removeParameter(DEPRECATED_KEY);
        url = url.removeParameter(TIMESTAMP_KEY);
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
     * Get the metadata's storage type is used to which {@link WritableMetadataService} instance.
     *
     * @param serviceInstance the specified {@link ServiceInstance}
     * @return if not found in {@link ServiceInstance#getMetadata() metadata} of {@link ServiceInstance}, return
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

    public static void calInstanceRevision(ServiceDiscovery serviceDiscovery, ServiceInstance instance) {
        String registryCluster = serviceDiscovery.getUrl() == null ? DEFAULT_KEY : serviceDiscovery.getUrl().getParameter(REGISTRY_CLUSTER_KEY);
        if (registryCluster == null) {
            registryCluster = DEFAULT_KEY;
        }
        WritableMetadataService writableMetadataService = WritableMetadataService.getDefaultExtension(instance.getApplicationModel());
        MetadataInfo metadataInfo = writableMetadataService.getMetadataInfos().get(registryCluster);
        if (metadataInfo == null) {
            metadataInfo = writableMetadataService.getDefaultMetadataInfo();
        }
        if (metadataInfo != null) {
            String existingInstanceRevision = instance.getMetadata().get(EXPORTED_SERVICES_REVISION_PROPERTY_NAME);
            if (!metadataInfo.calAndGetRevision().equals(existingInstanceRevision)) {
                instance.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, metadataInfo.calAndGetRevision());
                if (existingInstanceRevision != null) {// skip the first registration.
                    instance.putExtendParam(INSTANCE_REVISION_UPDATED_KEY, "true");
                }
            }
        }
    }

    public static boolean isInstanceUpdated(ServiceInstance instance) {
        return "true".equals(instance.getExtendParam(INSTANCE_REVISION_UPDATED_KEY));
    }

    public static void resetInstanceUpdateKey(ServiceInstance instance) {
        instance.removeExtendParam(INSTANCE_REVISION_UPDATED_KEY);
    }

    public static void registerMetadataAndInstance(ServiceInstance serviceInstance) {
        // register instance only when at least one service is exported.
        if (serviceInstance.getPort() > 0) {
            reportMetadataToRemote(serviceInstance);
            LOGGER.info("Start registering instance address to registry.");
            RegistryManager registryManager = serviceInstance.getOrDefaultApplicationModel().getBeanFactory().getBean(RegistryManager.class);
            registryManager.getServiceDiscoveries().forEach(serviceDiscovery ->
            {
                // copy instance for each registry to make sure instance in each registry can evolve independently
                ServiceInstance serviceInstanceForRegistry = new DefaultServiceInstance((DefaultServiceInstance) serviceInstance);
                calInstanceRevision(serviceDiscovery, serviceInstanceForRegistry);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Start registering instance address to registry" + serviceDiscovery.getUrl() + ", instance " + serviceInstanceForRegistry);
                }
                // register service instance
                serviceDiscovery.register(serviceInstanceForRegistry);
            });
        }
    }

    public static void refreshMetadataAndInstance(ServiceInstance serviceInstance) {
        reportMetadataToRemote(serviceInstance);
        RegistryManager registryManager = serviceInstance.getOrDefaultApplicationModel().getBeanFactory().getBean(RegistryManager.class);
        registryManager.getServiceDiscoveries().forEach(serviceDiscovery -> {
            ServiceInstance instance = serviceDiscovery.getLocalInstance();
            if (instance == null) {
                LOGGER.warn("Refreshing of service instance started, but instance hasn't been registered yet.");
                instance = serviceInstance;
            }
            // copy instance again, in case the same instance accidently shared among registries
            instance = new DefaultServiceInstance((DefaultServiceInstance) instance);
            calInstanceRevision(serviceDiscovery, instance);
            customizeInstance(instance);
            if (instance.getPort() > 0) {
                // update service instance revision
                serviceDiscovery.update(instance);
            }
        });
    }

    public static void customizeInstance(ServiceInstance instance) {
        ExtensionLoader<ServiceInstanceCustomizer> loader =
                instance.getOrDefaultApplicationModel().getExtensionLoader(ServiceInstanceCustomizer.class);
        // FIXME, sort customizer before apply
        loader.getSupportedExtensionInstances().forEach(customizer -> {
            // customize
            customizer.customize(instance);
        });
    }

    private static void reportMetadataToRemote(ServiceInstance serviceInstance) {
        if (REMOTE_METADATA_STORAGE_TYPE.equalsIgnoreCase(getMetadataStorageType(serviceInstance))) {
            RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService(serviceInstance.getApplicationModel());
            remoteMetadataService.publishMetadata(serviceInstance.getApplicationModel().getApplicationName());
        }
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
