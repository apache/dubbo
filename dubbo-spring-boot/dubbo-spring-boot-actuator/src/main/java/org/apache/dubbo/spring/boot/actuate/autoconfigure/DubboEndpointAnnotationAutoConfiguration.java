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
package org.apache.dubbo.spring.boot.actuate.autoconfigure;

import org.apache.dubbo.spring.boot.actuate.endpoint.DubboConfigsMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboPropertiesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboReferencesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboServicesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboShutdownEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.condition.CompatibleConditionalOnEnabledEndpoint;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Dubbo {@link Endpoint @Endpoint} Auto-{@link Configuration} for Spring Boot Actuator 2.0
 *
 * @see Endpoint
 * @see Configuration
 * @since 2.7.0
 */
@Configuration
@PropertySource(
        name = "Dubbo Endpoints Default Properties",
        value = "classpath:/META-INF/dubbo-endpoints-default.properties")
public class DubboEndpointAnnotationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @CompatibleConditionalOnEnabledEndpoint
    public DubboMetadataEndpoint dubboEndpoint() {
        return new DubboMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @CompatibleConditionalOnEnabledEndpoint
    public DubboConfigsMetadataEndpoint dubboConfigsMetadataEndpoint() {
        return new DubboConfigsMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @CompatibleConditionalOnEnabledEndpoint
    public DubboPropertiesMetadataEndpoint dubboPropertiesEndpoint() {
        return new DubboPropertiesMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @CompatibleConditionalOnEnabledEndpoint
    public DubboReferencesMetadataEndpoint dubboReferencesMetadataEndpoint() {
        return new DubboReferencesMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @CompatibleConditionalOnEnabledEndpoint
    public DubboServicesMetadataEndpoint dubboServicesMetadataEndpoint() {
        return new DubboServicesMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @CompatibleConditionalOnEnabledEndpoint
    public DubboShutdownEndpoint dubboShutdownEndpoint() {
        return new DubboShutdownEndpoint();
    }

}
