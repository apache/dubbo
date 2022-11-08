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
package org.apache.dubbo.registry.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.client.AbstractServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_INITIALIZE_XDS;

public class XdsServiceDiscoveryFactory extends AbstractServiceDiscoveryFactory {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsServiceDiscoveryFactory.class);

    @Override
    protected ServiceDiscovery createDiscovery(URL registryURL) {
        XdsServiceDiscovery xdsServiceDiscovery = new XdsServiceDiscovery(ApplicationModel.defaultModel(), registryURL);
        try {
            xdsServiceDiscovery.doInitialize(registryURL);
        } catch (Exception e) {
            logger.error(REGISTRY_ERROR_INITIALIZE_XDS, "", "", "Error occurred when initialize xDS service discovery impl.", e);
        }
        return xdsServiceDiscovery;
    }
}
