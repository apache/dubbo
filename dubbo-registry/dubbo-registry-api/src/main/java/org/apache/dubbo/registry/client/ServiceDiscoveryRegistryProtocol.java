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
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.rpc.Invoker;

import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;

/**
 * TODO, replace RegistryProtocol completely in the future.
 */
public class ServiceDiscoveryRegistryProtocol extends RegistryProtocol {

    @Override
    protected URL getRegistryUrl(Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (SERVICE_REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            return registryUrl;
        }
        return super.getRegistryUrl(originInvoker);
    }

    @Override
    protected URL getRegistryUrl(URL url) {
        if (SERVICE_REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return url;
        }
        return super.getRegistryUrl(url);
    }

}
