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


import org.apache.dubbo.spring.boot.actuate.endpoint.DubboEndpoint;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration;
import org.apache.dubbo.spring.boot.autoconfigure.DubboRelaxedBindingAutoConfiguration;

import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo {@link Endpoint} Auto Configuration is compatible with Spring Boot Actuator 1.x
 *
 * @since 2.7.0
 */
@Configuration
@ConditionalOnClass(name = {
        "org.springframework.boot.actuate.endpoint.Endpoint" // Spring Boot 1.x
})
@AutoConfigureAfter(value = {
        DubboAutoConfiguration.class,
        DubboRelaxedBindingAutoConfiguration.class
})
@EnableConfigurationProperties(DubboEndpoint.class)
public class DubboEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledEndpoint(value = "dubbo")
    public DubboEndpoint dubboEndpoint() {
        return new DubboEndpoint();
    }
}
