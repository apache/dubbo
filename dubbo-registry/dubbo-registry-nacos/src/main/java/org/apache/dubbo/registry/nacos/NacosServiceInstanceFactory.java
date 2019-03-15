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

import com.alibaba.nacos.api.naming.pojo.Instance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.support.cloud.ServiceInstanceFactory;

import java.util.LinkedHashMap;
import java.util.Objects;

import static java.lang.System.getProperty;
import static org.apache.dubbo.common.Constants.CATEGORY_KEY;
import static org.apache.dubbo.common.Constants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.Constants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.Constants.GROUP_KEY;
import static org.apache.dubbo.common.Constants.INTERFACE_KEY;
import static org.apache.dubbo.common.Constants.PATH_KEY;
import static org.apache.dubbo.common.Constants.PROTOCOL_KEY;
import static org.apache.dubbo.common.Constants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.Constants.VERSION_KEY;

/**
 * Nacos {@link ServiceInstanceFactory}
 *
 * @since 2.7.1
 */
public class NacosServiceInstanceFactory implements ServiceInstanceFactory<NacosServiceInstance> {

    /**
     * The separator for service name
     */
    private static final String SERVICE_NAME_SEPARATOR = getProperty("dubbo.service.name.separator", ":");

    @Override
    public NacosServiceInstance create(URL url) {
        Instance instance = createInstance(url);
        return new NacosServiceInstance(instance);
    }

    private Instance createInstance(URL url) {
        // Append default category if absent
        String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
        URL newURL = url.addParameter(CATEGORY_KEY, category);
        newURL = newURL.addParameter(PROTOCOL_KEY, url.getProtocol());
        newURL = newURL.addParameter(PATH_KEY, url.getPath());
        String ip = url.getHost();
        int port = url.getPort();
        String serviceName = createServiceName(url);
        Instance instance = new Instance();
        instance.setServiceName(serviceName);
        instance.setIp(ip);
        instance.setPort(port);
        instance.setMetadata(new LinkedHashMap<>(newURL.getParameters()));
        return instance;
    }

    @Override
    public String createServiceName(URL url) {
        String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
        if (!Objects.equals(category, PROVIDERS_CATEGORY) && !Objects.equals(category, CONSUMERS_CATEGORY)) {
            category = PROVIDERS_CATEGORY;
        }
        return createServiceName(url, category);
    }

    private static String createServiceName(URL url, String category) {
        StringBuilder serviceNameBuilder = new StringBuilder(category);
        appendIfPresent(serviceNameBuilder, url, INTERFACE_KEY);
        appendIfPresent(serviceNameBuilder, url, VERSION_KEY);
        appendIfPresent(serviceNameBuilder, url, GROUP_KEY);
        return serviceNameBuilder.toString();
    }

    private static void appendIfPresent(StringBuilder target, URL url,
                                        String parameterName) {
        String parameterValue = url.getParameter(parameterName);
        appendIfPresent(target, parameterValue);
    }

    private static void appendIfPresent(StringBuilder target, String parameterValue) {
        if (StringUtils.isNotEmpty(parameterValue)) {
            target.append(SERVICE_NAME_SEPARATOR).append(parameterValue);
        }
    }
}
