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

import com.alibaba.spring.context.config.ConfigurationBeanBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_BEAN_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SCAN_PREFIX;
import static org.apache.dubbo.spring.boot.util.DubboUtils.RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Dubbo Relaxed Binding Auto-{@link Configuration} for Spring Boot 1.x
 */
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.boot.bind.RelaxedPropertyResolver")
@Configuration
public class DubboRelaxedBindingAutoConfiguration {

    public PropertyResolver dubboScanBasePackagesPropertyResolver(Environment environment) {
        return new RelaxedPropertyResolver(environment, DUBBO_SCAN_PREFIX);
    }

    /**
     * The bean is used to scan the packages of Dubbo Service classes
     *
     * @param environment {@link Environment} instance
     * @return non-null {@link Set}
     * @since 2.7.8
     */
    @ConditionalOnMissingBean(name = BASE_PACKAGES_BEAN_NAME)
    @Bean(name = BASE_PACKAGES_BEAN_NAME)
    public Set<String> dubboBasePackages(Environment environment) {
        PropertyResolver propertyResolver = dubboScanBasePackagesPropertyResolver(environment);
        return propertyResolver.getProperty(BASE_PACKAGES_PROPERTY_NAME, Set.class, emptySet());
    }

    @ConditionalOnMissingBean(name = RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME, value = ConfigurationBeanBinder.class)
    @Bean(RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME)
    @Scope(scopeName = SCOPE_PROTOTYPE)
    public ConfigurationBeanBinder relaxedDubboConfigBinder() {
        return new RelaxedDubboConfigBinder();
    }

}
