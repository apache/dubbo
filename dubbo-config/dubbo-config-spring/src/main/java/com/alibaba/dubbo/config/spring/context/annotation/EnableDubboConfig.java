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
package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * As  a convenient and multiple {@link EnableDubboConfigBinding}
 * in default behavior , is equal to single bean bindings with below convention prefixes of properties:
 * <ul>
 * <li>{@link ApplicationConfig} binding to property : "dubbo.application"</li>
 * <li>{@link ModuleConfig} binding to property :  "dubbo.module"</li>
 * <li>{@link RegistryConfig} binding to property :  "dubbo.registry"</li>
 * <li>{@link ProtocolConfig} binding to property :  "dubbo.protocol"</li>
 * <li>{@link MonitorConfig} binding to property :  "dubbo.monitor"</li>
 * <li>{@link ProviderConfig} binding to property :  "dubbo.provider"</li>
 * <li>{@link ConsumerConfig} binding to property :  "dubbo.consumer"</li>
 * </ul>
 * <p>
 * In contrast, on multiple bean bindings that requires to set {@link #multiple()} to be <code>true</code> :
 * <ul>
 * <li>{@link ApplicationConfig} binding to property : "dubbo.applications"</li>
 * <li>{@link ModuleConfig} binding to property :  "dubbo.modules"</li>
 * <li>{@link RegistryConfig} binding to property :  "dubbo.registries"</li>
 * <li>{@link ProtocolConfig} binding to property :  "dubbo.protocols"</li>
 * <li>{@link MonitorConfig} binding to property :  "dubbo.monitors"</li>
 * <li>{@link ProviderConfig} binding to property :  "dubbo.providers"</li>
 * <li>{@link ConsumerConfig} binding to property :  "dubbo.consumers"</li>
 * </ul>
 *
 * @see EnableDubboConfigBinding
 * @see DubboConfigConfiguration
 * @see DubboConfigConfigurationSelector
 * @since 2.5.8
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(DubboConfigConfigurationSelector.class)
public @interface EnableDubboConfig {

    /**
     * It indicates whether binding to multiple Spring Beans.
     *
     * @return the default value is <code>false</code>
     * @revised 2.5.9
     */
    boolean multiple() default false;

}
