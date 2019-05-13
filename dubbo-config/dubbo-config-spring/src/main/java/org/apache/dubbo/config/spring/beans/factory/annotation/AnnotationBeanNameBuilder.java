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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.registry.Registry;

import org.springframework.core.env.Environment;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.config.spring.util.AnnotationUtils.resolveInterfaceName;
import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;
import static org.springframework.util.StringUtils.hasText;

/**
 * The Bean Name Builder for the annotations {@link Service} and {@link Reference}
 * <p>
 * The naming rule is consistent with the the implementation {@link Registry} that is based on the service-name aware
 * infrastructure, e.g Spring Cloud, Cloud Native and so on.
 * <p>
 * The pattern of bean name : ${category}:${protocol}:${serviceInterface}:${version}:${group}.
 * <p>
 * ${version} and ${group} are optional.
 *
 * @since 2.6.6
 */
class AnnotationBeanNameBuilder {

    private static final String SEPARATOR = ":";

    // Required properties

    private final String category;

    private final String protocol;

    private final String interfaceClassName;

    // Optional properties

    private String version;

    private String group;

    private Environment environment;

    private AnnotationBeanNameBuilder(String category, String protocol, String interfaceClassName) {
        this.category = category;
        this.protocol = protocol;
        this.interfaceClassName = interfaceClassName;
    }

    private AnnotationBeanNameBuilder(Service service, Class<?> interfaceClass) {
        this(PROVIDERS_CATEGORY, resolveProtocol(service.protocol()), resolveInterfaceName(service, interfaceClass));
        this.group(service.group());
        this.version(service.version());
    }

    private AnnotationBeanNameBuilder(Reference reference, Class<?> interfaceClass) {
        this(CONSUMERS_CATEGORY, resolveProtocol(reference.protocol()), resolveInterfaceName(reference, interfaceClass));
        this.group(reference.group());
        this.version(reference.version());
    }

    public static AnnotationBeanNameBuilder create(Service service, Class<?> interfaceClass) {
        return new AnnotationBeanNameBuilder(service, interfaceClass);
    }

    public static AnnotationBeanNameBuilder create(Reference reference, Class<?> interfaceClass) {
        return new AnnotationBeanNameBuilder(reference, interfaceClass);
    }

    private static void append(StringBuilder builder, String value) {
        if (hasText(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public AnnotationBeanNameBuilder group(String group) {
        this.group = group;
        return this;
    }

    public AnnotationBeanNameBuilder version(String version) {
        this.version = version;
        return this;
    }

    public AnnotationBeanNameBuilder environment(Environment environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Resolve the protocol
     *
     * @param protocols one or more protocols
     * @return if <code>protocols</code> == <code>null</code>, it will return
     * {@link CommonConstants#DEFAULT_PROTOCOL "dubbo"} as the default protocol
     * @see CommonConstants#DEFAULT_PROTOCOL
     */
    private static String resolveProtocol(String... protocols) {
        String protocol = arrayToCommaDelimitedString(protocols);
        return hasText(protocol) ? protocol : DEFAULT_PROTOCOL;
    }

    /**
     * Build bean name while resolve the placeholders if possible.
     *
     * @return pattern : ${category}:${protocol}:${serviceInterface}:${version}:${group}
     */
    public String build() {
        // Append the required properties
        StringBuilder beanNameBuilder = new StringBuilder(category);
        append(beanNameBuilder, protocol);
        append(beanNameBuilder, interfaceClassName);
        // Append the optional properties
        append(beanNameBuilder, version);
        append(beanNameBuilder, group);
        String beanName = beanNameBuilder.toString();
        // Resolve placeholders
        return environment != null ? environment.resolvePlaceholders(beanName) : beanName;
    }
}
