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
package org.apache.dubbo.config.spring.context.config;

import org.apache.dubbo.config.spring.context.annotation.EnableConfigurationBeanBinding;

import java.util.Map;

import org.springframework.core.env.Environment;

/**
 * The binder for the configuration bean
 *
 */
public interface ConfigurationBeanBinder {

    /**
     * Bind the properties in the {@link Environment} to Configuration bean under specified prefix.
     *
     * @param configurationProperties The configuration properties
     * @param ignoreUnknownFields     whether to ignore unknown fields, the value is come
     *                                from the attribute of {@link EnableConfigurationBeanBinding#ignoreUnknownFields()}
     * @param ignoreInvalidFields     whether to ignore invalid fields, the value is come
     *                                from the attribute of {@link EnableConfigurationBeanBinding#ignoreInvalidFields()}
     * @param configurationBean       the bean of configuration
     */
    void bind(
            Map<String, Object> configurationProperties,
            boolean ignoreUnknownFields,
            boolean ignoreInvalidFields,
            Object configurationBean);
}
