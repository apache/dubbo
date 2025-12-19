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

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.service.EchoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Spring MVC autoconfiguration for Dubbo Triple REST integration.
 * This configuration registers a custom RequestMappingHandlerMapping to prevent
 * Dubbo service beans and Reference proxies from being mistakenly scanned as
 * Spring MVC controllers.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({WebMvcRegistrations.class, RequestMappingHandlerMapping.class})
public class DubboTripleWebMvcConfiguration {

    @Bean
    public WebMvcRegistrations dubboMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new DubboExcludedRequestMappingHandlerMapping();
            }
        };
    }

    static class DubboExcludedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

        @Override
        protected boolean isHandler(@NonNull Class<?> beanType) {

            if (AnnotatedElementUtils.hasAnnotation(beanType, DubboService.class)) {
                return false;
            }

            if (EchoService.class.isAssignableFrom(beanType)) {
                return false;
            }

            return super.isHandler(beanType);
        }
    }
}
