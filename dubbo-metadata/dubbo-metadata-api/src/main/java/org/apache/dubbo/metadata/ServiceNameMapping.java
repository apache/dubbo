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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.extension.ExtensionScope.APPLICATION;
import static org.apache.dubbo.common.utils.StringUtils.SLASH;

/**
 * The interface for Dubbo service name Mapping
 *
 * @since 2.7.5
 */
@SPI(value = "metadata", scope = APPLICATION)
public interface ServiceNameMapping {

    String DEFAULT_MAPPING_GROUP = "mapping";

    /**
     * Map the specified Dubbo service interface, group, version and protocol to current Dubbo service name
     */
    boolean map(URL url);

    /**
     * Get the default extension of {@link ServiceNameMapping}
     *
     * @return non-null {@link ServiceNameMapping}
     */
    static ServiceNameMapping getDefaultExtension(ScopeModel scopeModel) {
        return ScopeModelUtil.getApplicationModel(scopeModel).getDefaultExtension(ServiceNameMapping.class);
    }

    static String buildMappingKey(URL url) {
        return buildGroup(url.getServiceInterface());
    }

    static String buildGroup(String serviceInterface) {
        //the issue : https://github.com/apache/dubbo/issues/4671
        return DEFAULT_MAPPING_GROUP + SLASH + serviceInterface;
    }

    static String toStringKeys(Set<String> serviceNames) {
        return serviceNames.toString();
    }

    static Set<String> getAppNames(String content) {
        if (StringUtils.isBlank(content)) {
            return emptySet();
        }
        return new HashSet<>(Arrays.asList(content.split(COMMA_SEPARATOR)));
    }

    /**
     * 1.developer explicitly specifies the application name this interface belongs to
     * 2.check Interface-App mapping
     */
    Set<String> getServices(URL subscribedURL);

    /**
     * 1.developer explicitly specifies the application name this interface belongs to
     * 2.check Interface-App mapping
     * 3.use the services specified in registry url.
     */
    Set<String> getAndListenServices(URL registryURL, URL subscribedURL, MappingListener listener);
}
