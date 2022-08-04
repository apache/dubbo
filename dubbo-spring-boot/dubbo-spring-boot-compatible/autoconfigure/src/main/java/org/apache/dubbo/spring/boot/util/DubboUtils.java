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
package org.apache.dubbo.spring.boot.util;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationPostProcessor;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.apache.dubbo.config.spring.context.properties.DubboConfigBinder;

import org.springframework.boot.context.ContextIdApplicationContextInitializer;
import org.springframework.core.env.PropertyResolver;

import java.util.Set;

/**
 * The utilities class for Dubbo
 *
 * @since 2.7.0
 */
public abstract class DubboUtils {

    /**
     * line separator
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");


    /**
     * The separator of property name
     */
    public static final String PROPERTY_NAME_SEPARATOR = ".";

    /**
     * The prefix of property name of Dubbo
     */
    public static final String DUBBO_PREFIX = "dubbo";

    /**
     * The prefix of property name for Dubbo scan
     */
    public static final String DUBBO_SCAN_PREFIX = DUBBO_PREFIX + PROPERTY_NAME_SEPARATOR + "scan" + PROPERTY_NAME_SEPARATOR;

    /**
     * The prefix of property name for Dubbo Config
     */
    public static final String DUBBO_CONFIG_PREFIX = DUBBO_PREFIX + PROPERTY_NAME_SEPARATOR + "config" + PROPERTY_NAME_SEPARATOR;

    /**
     * The property name of base packages to scan
     * <p>
     * The default value is empty set.
     */
    public static final String BASE_PACKAGES_PROPERTY_NAME = "base-packages";

    /**
     * The property name of multiple properties binding from externalized configuration
     * <p>
     * The default value is {@link #DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE}
     *
     * @deprecated 2.7.8 It will be remove in the future, {@link EnableDubboConfig} instead
     */
    @Deprecated
    public static final String MULTIPLE_CONFIG_PROPERTY_NAME = "multiple";

    /**
     * The default value of multiple properties binding from externalized configuration
     *
     * @deprecated 2.7.8 It will be remove in the future
     */
    @Deprecated
    public static final boolean DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE = true;

    /**
     * The property name of override Dubbo config
     * <p>
     * The default value is {@link #DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE}
     */
    public static final String OVERRIDE_CONFIG_FULL_PROPERTY_NAME = DUBBO_CONFIG_PREFIX + "override";

    /**
     * The default property value of  override Dubbo config
     */
    public static final boolean DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE = true;


    /**
     * The github URL of Dubbo Spring Boot
     */
    public static final String DUBBO_SPRING_BOOT_GITHUB_URL = "https://github.com/apache/dubbo/tree/3.0/dubbo-spring-boot";

    /**
     * The git URL of Dubbo Spring Boot
     */
    public static final String DUBBO_SPRING_BOOT_GIT_URL = "https://github.com/apache/dubbo.git";

    /**
     * The issues of Dubbo Spring Boot
     */
    public static final String DUBBO_SPRING_BOOT_ISSUES_URL = "https://github.com/apache/dubbo/issues";

    /**
     * The github URL of Dubbo
     */
    public static final String DUBBO_GITHUB_URL = "https://github.com/apache/dubbo";

    /**
     * The google group URL of Dubbo
     */
    public static final String DUBBO_MAILING_LIST = "dev@dubbo.apache.org";

    /**
     * The bean name of Relaxed-binding {@link DubboConfigBinder}
     */
    public static final String RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME = "relaxedDubboConfigBinder";

    /**
     * The bean name of {@link PropertyResolver} for {@link ServiceAnnotationPostProcessor}'s base-packages
     *
     * @deprecated 2.7.8 It will be remove in the future, please use {@link #BASE_PACKAGES_BEAN_NAME}
     */
    @Deprecated
    public static final String BASE_PACKAGES_PROPERTY_RESOLVER_BEAN_NAME = "dubboScanBasePackagesPropertyResolver";

    /**
     * The bean name of {@link Set} presenting {@link ServiceAnnotationPostProcessor}'s base-packages
     *
     * @since 2.7.8
     */
    public static final String BASE_PACKAGES_BEAN_NAME = "dubbo-service-class-base-packages";

    /**
     * The property name of Spring Application
     *
     * @see ContextIdApplicationContextInitializer
     * @since 2.7.1
     */
    public static final String SPRING_APPLICATION_NAME_PROPERTY = "spring.application.name";

    /**
     * The property id of {@link ApplicationConfig} Bean
     *
     * @see EnableDubboConfig
     * @since 2.7.1
     */
    public static final String DUBBO_APPLICATION_ID_PROPERTY = "dubbo.application.id";

    /**
     * The property name of {@link ApplicationConfig}
     *
     * @see EnableDubboConfig
     * @since 2.7.1
     */
    public static final String DUBBO_APPLICATION_NAME_PROPERTY = "dubbo.application.name";

    /**
     * The property name of {@link ApplicationConfig#getQosEnable() application's QOS enable}
     *
     * @since 2.7.1
     */
    public static final String DUBBO_APPLICATION_QOS_ENABLE_PROPERTY = "dubbo.application.qos-enable";

    /**
     * The property name of {@link EnableDubboConfig#multiple() @EnableDubboConfig.multiple()}
     *
     * @since 2.7.1
     */
    public static final String DUBBO_CONFIG_MULTIPLE_PROPERTY = "dubbo.config.multiple";


}
