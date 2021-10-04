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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils.createNamingService;

/**
 * Nacos {@link RegistryFactory}
 *
 * @since 2.6.5
 */
public class NacosRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected String createRegistryCacheKey(URL url) {
        String namespace = url.getParameter(CONFIG_NAMESPACE_KEY);
        url = URL.valueOf(url.toServiceStringWithoutResolving());
        if (StringUtils.isNotEmpty(namespace)) {
            url = url.addParameter(CONFIG_NAMESPACE_KEY, namespace);
        }

        return url.toFullString();
    }

    @Override
    protected Registry createRegistry(URL url) {
        return new NacosRegistry(url, createNamingService(url));
    }
}
