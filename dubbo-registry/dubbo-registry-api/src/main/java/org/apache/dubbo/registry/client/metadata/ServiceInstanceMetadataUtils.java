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
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.registry.integration.InterfaceCompatibleRegistryProtocol.DEFAULT_REGISTER_PROVIDER_KEYS;
import static org.apache.dubbo.rpc.Constants.DEPRECATED_KEY;
import static org.apache.dubbo.rpc.Constants.ID_KEY;

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

    /**
     * The prefix of {@link MetadataService} : "dubbo.metadata-service."
     */
    public static final String METADATA_SERVICE_PREFIX = "dubbo.metadata-service.";

    public static final String ENDPOINTS = "dubbo.endpoints";

    /**
     * The property name of metadata JSON of {@link MetadataService}'s {@link URL}
     */
    public static String METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME = METADATA_SERVICE_PREFIX + "url-params";

    /**
     * The {@link URL URLs} property name of {@link MetadataService} :
     * "dubbo.metadata-service.urls", which is used to be compatible with Dubbo Spring Cloud and
     * discovery the metadata of instance
     */
    public static final String METADATA_SERVICE_URLS_PROPERTY_NAME = METADATA_SERVICE_PREFIX + "urls";

    /**
     * The property name of The revision for all exported Dubbo services.
     */
    public static String EXPORTED_SERVICES_REVISION_PROPERTY_NAME = "dubbo.metadata.revision";

    /**
     * The property name of metadata storage type.
     */
    public static String METADATA_STORAGE_TYPE_PROPERTY_NAME = "dubbo.metadata.storage-type";

    public static String METADATA_CLUSTER_PROPERTY_NAME = "dubbo.metadata.cluster";

    public static String INSTANCE_REVISION_UPDATED_KEY = "dubbo.instance.revision.updated";

    /**
     * Get the multiple {@link URL urls'} parameters of {@link MetadataService MetadataService's} Metadata
     *
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return non-null {@link Map}, the key is {@link URL#getProtocol() the protocol of URL}, the value is
     * {@link #getMetadataServiceURLParams(ServiceInstance, String)}
     */
    public static Map<String, Map<String, String>> getMetadataServiceURLsParams(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String param = metadata.get(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME);
        return isBlank(param) ? emptyMap() : (Map) JSON.parse(param);
    }

    /**
     * Get the {@link URL url's} parameters of {@link MetadataService MetadataService's} Metadata
     *
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return non-null {@link Map}
     */
    public static Map<String, String> getMetadataServiceURLParams(ServiceInstance serviceInstance, String protocol) {
        Map<String, Map<String, String>> params = getMetadataServiceURLsParams(serviceInstance);
        return params.getOrDefault(protocol, emptyMap());
    }

    /**
     * 获取url对应的元数据服务部分参数
     * @param urls
     * @return
     */
    public static String getMetadataServiceParameter(List<URL> urls) {

        Map<String, Map<String, String>> params = new HashMap<>();

        urls.stream()
                // remove APPLICATION_KEY because service name must be present
                .map(url -> url.removeParameter(APPLICATION_KEY))
                // remove GROUP_KEY, always uses application name.
                .map(url -> url.removeParameter(GROUP_KEY))
                // remove DEPRECATED_KEY because it's always false
                .map(url -> url.removeParameter(DEPRECATED_KEY))
                // remove TIMESTAMP_KEY because it's nonsense
                .map(url -> url.removeParameter(TIMESTAMP_KEY))
                .forEach(url -> {
                    String protocol = url.getProtocol();
                    //缓存协议 以及对应的参数
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
        // 缓存port
        params.put(PORT_KEY, String.valueOf(providerURL.getPort()));
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
        // serviceInstance中的dubbo.exported-services.revision
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

    /**
     * Is Dubbo Service instance or not
     *
     * @param serviceInstance {@link ServiceInstance service instance}
     * @return if Dubbo Service instance, return <code>true</code>, or <code>false</code>
     */
    public static boolean isDubboServiceInstance(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        // 是否是元数据参数
        return metadata.containsKey(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME)
                || metadata.containsKey(METADATA_SERVICE_URLS_PROPERTY_NAME);
    }

    /**
     * 缓存协议以及端口号
     * @param serviceInstance
     * @param protocolPorts
     */
    public static void setEndpoints(ServiceInstance serviceInstance, Map<String, Integer> protocolPorts) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        List<Endpoint> endpoints = new ArrayList<>();
        protocolPorts.forEach((k, v) -> {
            Endpoint endpoint = new Endpoint(v, k);
            endpoints.add(endpoint);
        });
        // dubbo.endpoints
        metadata.put(ENDPOINTS, JSON.toJSONString(endpoints));
    }

    /**
     * Get the property value of port by the specified {@link ServiceInstance#getMetadata() the metadata of
     * service instance} and protocol
     *
     * @param serviceInstance {@link ServiceInstance service instance}
     * @param protocol        the name of protocol, e.g, dubbo, rest, and so on
     * @return if not found, return <code>null</code>
     */
    public static Integer getProtocolPort(ServiceInstance serviceInstance, String protocol) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String rawEndpoints = metadata.get(ENDPOINTS);
        if (StringUtils.isNotEmpty(rawEndpoints)) {
            List<Endpoint> endpoints = JSON.parseArray(rawEndpoints, Endpoint.class);
            for (Endpoint endpoint : endpoints) {
                if (endpoint.getProtocol().equals(protocol)) {
                    return endpoint.getPort();
                }
            }
        }
        return null;
    }

    /**
     * 为元数据添加dubbo.metadata.revision -> 6E05C73112CB34FBD5FFAC83B2299BDE
     * @param serviceDiscovery
     * @param instance
     */
    public static void calInstanceRevision(ServiceDiscovery serviceDiscovery, ServiceInstance instance) {
        /**
         * 获取注册中心url中parameters对应的id
         */
        String registryCluster = serviceDiscovery.getUrl().getParameter(ID_KEY);
        if (registryCluster == null) {
            return;
        }
        /**
         * 本地缓存中  {registryCluster}对应的元数据信息（包括待导出服务）
         */
        MetadataInfo metadataInfo = WritableMetadataService.getDefaultExtension().getMetadataInfos().get(registryCluster);
        if (metadataInfo != null) {
            /**
             * 元数据中的dubbo.metadata.revision
             */
            String existingInstanceRevision = instance.getMetadata().get(EXPORTED_SERVICES_REVISION_PROPERTY_NAME);
            /**
             * revision不相同
             */
            if (!metadataInfo.calAndGetRevision().equals(existingInstanceRevision)) {
                /**
                 * 将revision注入到元数据
                 * 将revision注入到元数据
                 * 将revision注入到元数据
                 */
                instance.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, metadataInfo.calAndGetRevision());
                /**
                 * 更新元数据中的extendParams  提示revision有变化  应该通知注册中心
                 * 更新元数据中的extendParams  提示revision有变化  应该通知注册中心
                 * 更新元数据中的extendParams  提示revision有变化  应该通知注册中心
                 */
                if (existingInstanceRevision != null) {// skip the first registration.
                    instance.getExtendParams().put(INSTANCE_REVISION_UPDATED_KEY, "true");
                }
            }
        }
    }

    /**
     * 比对是否需要更新   即revision是否有变化
     * @param instance
     * @return
     */
    public static boolean isInstanceUpdated(ServiceInstance instance) {
        /**
         * revision有变化
         */
        return "true".equals(instance.getExtendParams().get(INSTANCE_REVISION_UPDATED_KEY));
    }

    public static void resetInstanceUpdateKey(ServiceInstance instance) {
        instance.getExtendParams().remove(INSTANCE_REVISION_UPDATED_KEY);
    }

    /**
     * 检查导出服务是否有变化  有则刷新
     */
    public static void refreshMetadataAndInstance() {
        /**
         * 初始化RemoteMetadataServiceImpl    内部为InMemoryWritableMetadataService
         */
        RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService();
        /**
         * 遍历本地链接的注册中心  发布相应的服务配置
         */
        remoteMetadataService.publishMetadata(ApplicationModel.getName());

        /**
         * 遍历服务自省策略的注册中心
         */
        AbstractRegistryFactory.getServiceDiscoveries().forEach(serviceDiscovery -> {
            /**
             * 计算元数据的revision   dubbo.metadata.revision -> 6E05C73112CB34FBD5FFAC83B2299BDE
             * 并比较revision是否有变化
             */
            calInstanceRevision(serviceDiscovery, serviceDiscovery.getLocalInstance());
            // update service instance revision
            /**
             * revision有变化向注册中心更新服务列表
             */
            serviceDiscovery.update(serviceDiscovery.getLocalInstance());
        });
    }

    /**
     * Set the default parameters via the specified {@link URL providerURL}
     *
     * @param params      the parameters
     * @param providerURL the provider's {@link URL}
     */
    private static void setDefaultParams(Map<String, String> params, URL providerURL) {
        // 遍历DEFAULT_REGISTER_PROVIDER_KEYS  获取providerURL中的参数
        for (String parameterName : DEFAULT_REGISTER_PROVIDER_KEYS) {
            String parameterValue = providerURL.getParameter(parameterName);
            // 参数值不为空则缓存
            if (!isBlank(parameterValue)) {
                params.put(parameterName, parameterValue);
            }
        }
    }

    public static class Endpoint {
        Integer port;
        String protocol;

        public Endpoint(Integer port, String protocol) {
            this.port = port;
            this.protocol = protocol;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }
}
