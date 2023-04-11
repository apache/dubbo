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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_MISSING_METADATA_CONFIG_PORT;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_TIMEOUT_VALUE;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PROXY_TIMEOUT_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
import static org.apache.dubbo.remoting.Constants.CONNECTIONS_KEY;

/**
 * Standard Dubbo provider enabling introspection service discovery mode.
 *
 * @see MetadataService
 * @since 2.7.5
 */
public class StandardMetadataServiceURLBuilder implements MetadataServiceURLBuilder {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    public static final String NAME = "standard";

    private ApplicationModel applicationModel;
    private Integer metadataServicePort;

    public StandardMetadataServiceURLBuilder(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        metadataServicePort = applicationModel.getCurrentConfig().getMetadataServicePort();
    }

    /**
     * Build the {@link URL urls} from {@link ServiceInstance#getMetadata() the metadata} of {@link ServiceInstance}
     *
     * @param serviceInstance {@link ServiceInstance}
     * @return the not-null {@link List}
     */
    @Override
    public List<URL> build(ServiceInstance serviceInstance) {
        Map<String, String> paramsMap = getMetadataServiceURLsParams(serviceInstance);

        String serviceName = serviceInstance.getServiceName();

        String host = serviceInstance.getHost();

        URL url;
        if (paramsMap.isEmpty()) {
            // ServiceInstance Metadata is empty. Happened when registry not support metadata write.
            url = generateUrlWithoutMetadata(serviceName, host, serviceInstance.getPort());
        } else {
            url = generateWithMetadata(serviceName, host, paramsMap);
        }

        url = url.setScopeModel(serviceInstance.getApplicationModel().getInternalModule());

        return Collections.singletonList(url);
    }

    private URL generateWithMetadata(String serviceName, String host, Map<String, String> params) {
        String protocol = params.get(PROTOCOL_KEY);
        int port = Integer.parseInt(params.get(PORT_KEY));
        URLBuilder urlBuilder = new URLBuilder()
            .setHost(host)
            .setPort(port)
            .setProtocol(protocol)
            .setPath(MetadataService.class.getName())
            .addParameter(TIMEOUT_KEY, ConfigurationUtils.get(applicationModel, METADATA_PROXY_TIMEOUT_KEY, DEFAULT_METADATA_TIMEOUT_VALUE))
            .addParameter(SIDE_KEY, CONSUMER)
            .addParameter(CONNECTIONS_KEY, 1)
            .addParameter(THREADPOOL_KEY, "cached")
            .addParameter(THREADS_KEY, "100")
            .addParameter(CORE_THREADS_KEY, "2")
            .addParameter(RETRIES_KEY, 0);

        // add parameters
        params.forEach(urlBuilder::addParameter);

        // add the default parameters
        urlBuilder.addParameter(GROUP_KEY, serviceName);
        return urlBuilder.build();
    }

    private URL generateUrlWithoutMetadata(String serviceName, String host, Integer instancePort) {
        Integer port = metadataServicePort;
        if (port == null || port < 1) {

            // 1-18 - Metadata Service Port should be specified for consumer.

            logger.warn(REGISTRY_MISSING_METADATA_CONFIG_PORT, "missing configuration of metadata service port", "",
                "Metadata Service Port is not provided. Since DNS is not able to negotiate the metadata port " +
                    "between Provider and Consumer, Dubbo will try using instance port as the default metadata port.");

            port = instancePort;
        }

        if (port == null || port < 1) {

            // 1-18 - Metadata Service Port should be specified for consumer.

            String message = "Metadata Service Port should be specified for consumer. " +
                    "Please set dubbo.application.metadataServicePort and " +
                    "make sure it has been set on provider side. " +
                    "ServiceName: " + serviceName + " Host: " + host;

            IllegalStateException illegalStateException = new IllegalStateException(message);

            logger.error(REGISTRY_MISSING_METADATA_CONFIG_PORT, "missing configuration of metadata service port", "",
                message, illegalStateException);

            throw illegalStateException;
        }

        URLBuilder urlBuilder = new URLBuilder()
                .setHost(host)
                .setPort(port)
                .setProtocol(DUBBO_PROTOCOL)
                .setPath(MetadataService.class.getName())
                .addParameter(TIMEOUT_KEY, ConfigurationUtils.get(applicationModel, METADATA_PROXY_TIMEOUT_KEY, DEFAULT_METADATA_TIMEOUT_VALUE))
                .addParameter(Constants.RECONNECT_KEY, false)
                .addParameter(SIDE_KEY, CONSUMER)
                .addParameter(GROUP_KEY, serviceName)
                .addParameter(VERSION_KEY, MetadataService.VERSION)
                .addParameter(RETRIES_KEY, 0);

        // add ServiceInstance Metadata notify support
        urlBuilder.addParameter("getAndListenInstanceMetadata.1.callback", true);

        return urlBuilder.build();
    }

    /**
     * Get the multiple {@link URL urls'} parameters of {@link MetadataService MetadataService's} Metadata
     *
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return non-null {@link Map}, the key is {@link URL#getProtocol() the protocol of URL}, the value is
     */
    private Map<String, String> getMetadataServiceURLsParams(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String param = metadata.get(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME);
        return isBlank(param) ? emptyMap() : (Map) JsonUtils.toJavaObject(param, Map.class);
    }
}
