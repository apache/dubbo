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

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.spring.ConfigCenterBean;

import com.alibaba.spring.beans.factory.annotation.EnableConfigurationBeanBinding;
import com.alibaba.spring.beans.factory.annotation.EnableConfigurationBeanBindings;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo {@link AbstractConfig Config} {@link Configuration}
 *
 * @revised 2.7.5
 * @see Configuration
 * @see EnableConfigurationBeanBindings
 * @see EnableConfigurationBeanBinding
 * @see ApplicationConfig
 * @see ModuleConfig
 * @see RegistryConfig
 * @see ProtocolConfig
 * @see MonitorConfig
 * @see ProviderConfig
 * @see ConsumerConfig
 * @see org.apache.dubbo.config.ConfigCenterConfig
 * @since 2.5.8
 */
public class DubboConfigConfiguration {

    /**
     * Single Dubbo {@link AbstractConfig Config} Bean Binding
     */
    @EnableConfigurationBeanBindings({
            @EnableConfigurationBeanBinding(prefix = "dubbo.application", type = ApplicationConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.module", type = ModuleConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.registry", type = RegistryConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.protocol", type = ProtocolConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.monitor", type = MonitorConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.provider", type = ProviderConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.consumer", type = ConsumerConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.config-center", type = ConfigCenterBean.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.metadata-report", type = MetadataReportConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.metrics", type = MetricsConfig.class),
            @EnableConfigurationBeanBinding(prefix = "dubbo.ssl", type = SslConfig.class)
    })
    public static class Single {

    }

    /**
     * Multiple Dubbo {@link AbstractConfig Config} Bean Binding
     */
    @EnableConfigurationBeanBindings({
            @EnableConfigurationBeanBinding(prefix = "dubbo.applications", type = ApplicationConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.modules", type = ModuleConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.registries", type = RegistryConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.protocols", type = ProtocolConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.monitors", type = MonitorConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.providers", type = ProviderConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.consumers", type = ConsumerConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.config-centers", type = ConfigCenterBean.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.metadata-reports", type = MetadataReportConfig.class, multiple = true),
            @EnableConfigurationBeanBinding(prefix = "dubbo.metricses", type = MetricsConfig.class, multiple = true)
    })
    public static class Multiple {

    }
}
