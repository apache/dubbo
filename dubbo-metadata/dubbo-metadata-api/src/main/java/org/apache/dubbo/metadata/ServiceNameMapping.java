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
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.service.Destroyable;

import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.extension.ExtensionScope.APPLICATION;

/**
 * This will interact with remote metadata center to find the interface-app mapping and will cache the data locally.
 *
 * Call variants of getCachedMapping() methods whenever need to use the mapping data.
 */
@SPI(value = "metadata", scope = APPLICATION)
public interface ServiceNameMapping extends Destroyable {

    String DEFAULT_MAPPING_GROUP = "mapping";

    /**
     * Map the specified Dubbo service interface, group, version and protocol to current Dubbo service name
     */
    boolean map(URL url);

    boolean hasValidMetadataCenter();

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
        // the issue : https://github.com/apache/dubbo/issues/4671
        //        return DEFAULT_MAPPING_GROUP + SLASH + serviceInterface;
        return serviceInterface;
    }

    static String toStringKeys(Set<String> serviceNames) {
        if (CollectionUtils.isEmpty(serviceNames)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String n : serviceNames) {
            builder.append(n);
            builder.append(COMMA_SEPARATOR);
        }

        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    static Set<String> getAppNames(String content) {
        if (StringUtils.isBlank(content)) {
            return emptySet();
        }
        return new TreeSet<>(of(content.split(COMMA_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(toSet()));
    }

    static Set<String> getMappingByUrl(URL consumerURL) {
        String providedBy = consumerURL.getParameter(RegistryConstants.PROVIDED_BY);
        if (StringUtils.isBlank(providedBy)) {
            return null;
        }
        return AbstractServiceNameMapping.parseServices(providedBy);
    }

    /**
     * Get the latest mapping result from remote center and register listener at the same time to get notified once mapping changes.
     *
     * @param listener listener that will be notified on mapping change
     * @return the latest mapping result from remote center
     */
    Set<String> getAndListen(URL registryURL, URL subscribedURL, MappingListener listener);

    MappingListener stopListen(URL subscribeURL, MappingListener listener);

    void putCachedMapping(String serviceKey, Set<String> apps);

    Set<String> getMapping(URL consumerURL);

    Set<String> getRemoteMapping(URL consumerURL);

    Set<String> removeCachedMapping(String serviceKey);
}
