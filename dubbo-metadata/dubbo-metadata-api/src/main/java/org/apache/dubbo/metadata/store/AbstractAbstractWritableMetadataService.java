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
package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;

import com.google.gson.Gson;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.utils.ClassUtils.forName;
import static org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder.buildFullDefinition;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.support.ProtocolUtils.isGeneric;

/**
 * The abstract implementation of {@link WritableMetadataService}
 *
 * @see WritableMetadataService
 * @since 2.7.8
 */
public abstract class AbstractAbstractWritableMetadataService implements WritableMetadataService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void publishServiceDefinition(URL url) {
        if (SERVICE_INTERFACE_NAME.equals(url.getServiceInterface())) { // Ignore the interface "MetadataService"
            return;
        }

        // Remove the useless parameters
        url = url.removeParameters(PID_KEY, TIMESTAMP_KEY, BIND_IP_KEY, BIND_PORT_KEY, TIMESTAMP_KEY);

        String side = url.getParameter(SIDE_KEY);
        if (PROVIDER_SIDE.equalsIgnoreCase(side)) {
            publishProviderServiceDefinition(url);
        } else {
            publishConsumerParameters(url);
        }
    }

    protected void publishProviderServiceDefinition(URL url) {
        String serviceDefinition = getServiceDefinition(url);
        if (!StringUtils.isBlank(serviceDefinition)) {
            publishServiceDefinition(url.getServiceKey(), serviceDefinition);
        }
    }

    protected String getServiceDefinition(URL exportedURL) {
        String interfaceName = exportedURL.getParameter(INTERFACE_KEY);
        String json = null;
        try {
            if (StringUtils.isNotEmpty(interfaceName) && !isGeneric(exportedURL.getParameter(GENERIC_KEY))) {
                Class interfaceClass = forName(interfaceName);
                ServiceDefinition serviceDefinition = buildFullDefinition(interfaceClass, exportedURL.getParameters());
                Gson gson = new Gson();
                json = gson.toJson(serviceDefinition);
            }
        } catch (ClassNotFoundException e) {
            //ignore error
            if (logger.isErrorEnabled()) {
                logger.error("The interface class[name : " + interfaceName + "] can't be found , providerUrl: "
                        + exportedURL.toFullString());
            }
        }
        return json;
    }

    protected void publishConsumerParameters(URL url) {
    }

    protected void publishServiceDefinition(String key, String json) {
    }

}
