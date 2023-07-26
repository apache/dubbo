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
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;

/**
 * Supporting interaction with Dubbo Spring Cloud at https://github.com/alibaba/spring-cloud-alibaba
 * Dubbo Spring Cloud is a Dubbo extension that favours a per instance registry model and exposes metadata service.
 *
 * @since 2.7.5
 */
public class SpringCloudMetadataServiceURLBuilder implements MetadataServiceURLBuilder {
    public static final String NAME = "spring-cloud";

    @Override
    public List<URL> build(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String dubboUrlsForJson = metadata.get(METADATA_SERVICE_URLS_PROPERTY_NAME);
        if (StringUtils.isBlank(dubboUrlsForJson)) {
            return Collections.emptyList();
        }
        List<String> urlStrings = JsonUtils.toJavaList(dubboUrlsForJson, String.class);
        return urlStrings.stream().map(URL::valueOf).collect(Collectors.toList());
    }
}
