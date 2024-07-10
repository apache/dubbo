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
package org.apache.dubbo.config.spring.context.annotation;

import org.apache.dubbo.config.spring.context.config.ConfigurationBeanCustomizer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertySources;

/**
 * Enables Spring's annotation-driven configuration bean from {@link PropertySources properties}.
 *
 * @see ConfigurationBeanBindingRegistrar
 * @see ConfigurationBeanBindingPostProcessor
 * @see ConfigurationBeanCustomizer
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ConfigurationBeanBindingRegistrar.class)
public @interface EnableConfigurationBeanBinding {

    /**
     * The default value for {@link #multiple()}
     *
     * @since 1.0.6
     */
    boolean DEFAULT_MULTIPLE = false;

    /**
     * The default value for {@link #ignoreUnknownFields()}
     *
     * @since 1.0.6
     */
    boolean DEFAULT_IGNORE_UNKNOWN_FIELDS = true;

    /**
     * The default value for {@link #ignoreInvalidFields()}
     *
     * @since 1.0.6
     */
    boolean DEFAULT_IGNORE_INVALID_FIELDS = true;

    /**
     * The name prefix of the properties that are valid to bind to the type of configuration.
     *
     * @return the name prefix of the properties to bind
     */
    String prefix();

    /**
     * @return The binding type of configuration.
     */
    Class<?> type();

    /**
     * It indicates whether {@link #prefix()} binding to multiple Spring Beans.
     *
     * @return the default value is <code>false</code>
     * @see #DEFAULT_MULTIPLE
     */
    boolean multiple() default DEFAULT_MULTIPLE;

    /**
     * Set whether to ignore unknown fields, that is, whether to ignore bind
     * parameters that do not have corresponding fields in the target object.
     * <p>Default is "true". Turn this off to enforce that all bind parameters
     * must have a matching field in the target object.
     *
     * @return the default value is <code>true</code>
     * @see #DEFAULT_IGNORE_UNKNOWN_FIELDS
     */
    boolean ignoreUnknownFields() default DEFAULT_IGNORE_UNKNOWN_FIELDS;

    /**
     * Set whether to ignore invalid fields, that is, whether to ignore bind
     * parameters that have corresponding fields in the target object which are
     * not accessible (for example because of null values in the nested path).
     * <p>Default is "true".
     *
     * @return the default value is <code>true</code>
     * @see #DEFAULT_IGNORE_INVALID_FIELDS
     */
    boolean ignoreInvalidFields() default DEFAULT_IGNORE_INVALID_FIELDS;
}
