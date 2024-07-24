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

import org.apache.dubbo.rpc.protocol.tri.ServletExchanger;
import org.apache.dubbo.rpc.protocol.tri.servlet.TripleFilter;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Conditional(SpringBoot12Condition.class)
public class DubboTripleAutoConfiguration {

    public static final String PREFIX = "dubbo.protocol.triple.servlet";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Filter.class)
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnProperty(prefix = PREFIX, name = "enable")
    public static class TripleServletConfiguration {

        @Bean
        public FilterRegistrationBean<TripleFilter> tripleProtocolFilter(
                @Value("${" + PREFIX + ".filter-url-patterns:/*}") String[] urlPatterns,
                @Value("${" + PREFIX + ".filter-order:-1000000}") int order,
                @Value("${server.port:8080}") int serverPort) {
            ServletExchanger.bindServerPort(serverPort);
            FilterRegistrationBean<TripleFilter> registrationBean = new FilterRegistrationBean<>();
            registrationBean.setFilter(new TripleFilter());
            registrationBean.addUrlPatterns(urlPatterns);
            registrationBean.setOrder(order);
            return registrationBean;
        }
    }
}
