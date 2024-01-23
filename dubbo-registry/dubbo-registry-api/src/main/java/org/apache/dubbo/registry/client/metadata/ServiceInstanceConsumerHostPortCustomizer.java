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

import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_INIT_SERIALIZATION_OPTIMIZER;

public class ServiceInstanceConsumerHostPortCustomizer implements ServiceInstanceCustomizer, Prioritized {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(ServiceInstanceConsumerHostPortCustomizer.class);

    @Override
    public void customize(ServiceInstance serviceInstance, ApplicationModel applicationModel) {
        //serviceInstance default host is null and port is 0
        if (serviceInstance.getHost() != null && serviceInstance.getPort() != 0) {
            return;
        }
        String preferredProtocol = applicationModel.getCurrentConfig()
                .getProtocol();
        Protocol protocol = applicationModel.getFrameworkModel()
                .getExtensionLoader(Protocol.class)
                .getExtension(preferredProtocol, false);
        List<ProtocolServer> protocolServerList = protocol.getServers();
        if (CollectionUtils.isNotEmpty(protocolServerList)) {
            for (ProtocolServer protocolServer : protocolServerList) {
                if (protocolServer.getUrl() == null) {
                    continue;
                }
                String host = protocolServer.getUrl()
                        .getHost();
                int port = protocolServer.getUrl()
                        .getPort();
                //if not match continue
                if (host == null || port == 0) {
                    continue;
                }
                if (serviceInstance instanceof DefaultServiceInstance) {
                    DefaultServiceInstance instance = (DefaultServiceInstance) serviceInstance;
                    instance.setHost(host);
                    instance.setPort(port);
                    break;
                }
            }
        }

        if (serviceInstance.getHost() == null || serviceInstance.getPort() == 0) {
            logger.warn(PROTOCOL_FAILED_INIT_SERIALIZATION_OPTIMIZER, "typo in preferred protocol", "",
                    "Can't find an protocolServer using the default preferredProtocol \"" + preferredProtocol + "\", "
                            + "Failed to fill host and port to serviceInstance when only consumers are present.");
        }
    }

    @Override
    public int getPriority() {
        //after serviceInstanceHostPortCustomizer
        return Prioritized.MIN_PRIORITY;
    }
}
