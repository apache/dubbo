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
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.ServiceBean;
import org.springframework.util.StringUtils;

/**
 * Dubbo {@link Service @Service} Bean Builder
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Service
 * @see Reference
 * @see ServiceBean
 * @see ReferenceBean
 * @since 2.6.4
 */
class ServiceBeanNameBuilder {

    private static final String SEPARATOR = ":";

    private final Class<?> interfaceClass;

    private final String version;

    private final String group;

    private String application;

    private String module;

    private String[] registry;

    private ServiceBeanNameBuilder(Class<?> interfaceClass, String version, String group) {
        this.interfaceClass = interfaceClass;
        this.version = version;
        this.group = group;
    }

    private ServiceBeanNameBuilder(Service service, Class<?> interfaceClass) {
        this(service.interfaceClass() == null ? interfaceClass : service.interfaceClass(), service.version(), service.group());
        application(service.application());
        module(service.module());
        registry(service.registry());
    }

    private ServiceBeanNameBuilder(Reference reference, Class<?> interfaceClass) {
        this(reference.interfaceClass() == null ? interfaceClass : reference.interfaceClass(), reference.version(), reference.group());
        application(reference.application());
        module(reference.module());
        registry(reference.registry());
    }

    public static ServiceBeanNameBuilder create(Class<?> interfaceClass, String version, String group) {
        return new ServiceBeanNameBuilder(interfaceClass, version, group);
    }

    public static ServiceBeanNameBuilder create(Service service, Class<?> interfaceClass) {
        return new ServiceBeanNameBuilder(service, interfaceClass);
    }

    public static ServiceBeanNameBuilder create(Reference reference, Class<?> interfaceClass) {
        return new ServiceBeanNameBuilder(reference, interfaceClass);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(value).append(SEPARATOR);
        }
    }

    /**
     * Set {@link ApplicationConfig application} bean name
     *
     * @param application {@link ApplicationConfig application} bean name
     * @return {@link ServiceBeanNameBuilder}
     */
    public ServiceBeanNameBuilder application(String application) {
        this.application = application;
        return this;
    }

    /**
     * Set {@link ModuleConfig module} bean name
     *
     * @param module {@link ModuleConfig module} bean name
     * @return {@link ServiceBeanNameBuilder}
     */
    public ServiceBeanNameBuilder module(String module) {
        this.module = module;
        return this;
    }

    /**
     * Set {@link RegistryConfig monitor} bean names
     *
     * @param registry {@link RegistryConfig monitor} bean name
     * @return {@link ServiceBeanNameBuilder}
     */
    public ServiceBeanNameBuilder registry(String... registry) {
        this.registry = registry;
        return this;
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder();
        // Required
        append(beanNameBuilder, interfaceClass.getName());
        append(beanNameBuilder, version);
        append(beanNameBuilder, group);
        // Optional
        append(beanNameBuilder, application);
        append(beanNameBuilder, module);
        append(beanNameBuilder, StringUtils.arrayToCommaDelimitedString(registry));
        // Build and remove last ":"
        return beanNameBuilder.substring(0, beanNameBuilder.length() - 1);
    }
}
