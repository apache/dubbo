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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Conditional(SpringBoot3Condition.class)
public class DubboSpringBoot3DependencyCheckAutoConfiguration {

    public static final String SERVLET_PREFIX = "dubbo.protocol.triple.servlet";

    public static final String WEBSOCKET_PREFIX = "dubbo.protocol.triple.websocket";

    public static final String JAKARATA_SERVLET_FILTER = "jakarta.servlet.Filter";

    public static final String DUBBO_TRIPLE_3_AUTOCONFIGURATION =
            "org.apache.dubbo.spring.boot.autoconfigure.DubboTriple3AutoConfiguration";

    private static final String SPRING_BOOT_3_DEPENDENCY_CHECK_WARNING =
            "Couldn't enable servlet support for triple at SpringBoot3: Missing dubbo-spring-boot-3-autoconfigure";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = JAKARATA_SERVLET_FILTER)
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnProperty(prefix = SERVLET_PREFIX, name = "enabled", havingValue = "true")
    @ConditionalOnMissingClass(DUBBO_TRIPLE_3_AUTOCONFIGURATION)
    public static class tripleProtocolFilterDependencyCheckConfiguration {
        @Bean
        public Object tripleProtocolFilterDependencyCheck() {
            throw new IllegalStateException(SPRING_BOOT_3_DEPENDENCY_CHECK_WARNING);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = JAKARATA_SERVLET_FILTER)
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnProperty(prefix = WEBSOCKET_PREFIX, name = "enabled", havingValue = "true")
    @ConditionalOnMissingClass(DUBBO_TRIPLE_3_AUTOCONFIGURATION)
    public static class tripleWebSocketFilterDependencyCheckConfiguration {
        @Bean
        public Object tripleWebSocketFilterDependencyCheck() {
            throw new IllegalStateException(SPRING_BOOT_3_DEPENDENCY_CHECK_WARNING);
        }
    }
}
