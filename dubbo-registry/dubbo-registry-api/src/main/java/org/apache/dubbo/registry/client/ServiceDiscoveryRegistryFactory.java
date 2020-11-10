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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.registry.Constants.DEFAULT_REGISTRY;

public class ServiceDiscoveryRegistryFactory extends AbstractRegistryFactory {

    /**
     * service-discovery-registry://113.96.131.199:8848/org.apache.dubbo.registry.RegistryService?application=dubbo-nacos-provider-demo&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&metadata-type=remote&password=nacos&pid=12916&registry=nacos&registry-type=service&registry.type=service&timestamp=1604897547641&username=nacos
     * @param url
     * @return
     */
    @Override
    protected Registry createRegistry(URL url) {
        /**
         * 服务自省
         */
        if (SERVICE_REGISTRY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            // 则获取真正的注册中心协议
            String protocol = url.getParameter(REGISTRY_KEY, DEFAULT_REGISTRY);
            // 修改协议并去除属性
            url = url.setProtocol(protocol).removeParameter(REGISTRY_KEY);
        }
        return new ServiceDiscoveryRegistry(url);
    }

}
