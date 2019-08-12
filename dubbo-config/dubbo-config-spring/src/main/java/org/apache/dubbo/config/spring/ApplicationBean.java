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
package org.apache.dubbo.config.spring;


import org.apache.dubbo.bootstrap.DubboBootstrap;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spring.util.BeanFactoryUtils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;

public class ApplicationBean extends ApplicationConfig implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            List<RegistryConfig> registries = getBeans(RegistryConfig.class);
            List<ProtocolConfig> protocols = getBeans(ProtocolConfig.class);
            List<ConfigCenterConfig> configs = getBeans(ConfigCenterConfig.class);
            List<MetadataReportConfig> metadatas = getBeans(MetadataReportConfig.class);
            List<MonitorConfig> monitors = getBeans(MonitorConfig.class);
            List<ProviderConfig> providers = getBeans(ProviderConfig.class);
            List<ConsumerConfig> consumers = getBeans(ConsumerConfig.class);
            List<ModuleConfig> modules = getBeans(ModuleConfig.class);
            List<MetricsConfig> metrics = getBeans(MetricsConfig.class);
            List<ServiceConfig> services = getBeans(ServiceConfig.class);
            List<ReferenceConfig> references = getBeans(ReferenceConfig.class);

            DubboBootstrap bootstrap = new DubboBootstrap();
            bootstrap.application(this)
                    .monitor(CollectionUtils.isNotEmpty(monitors) ? monitors.get(0) : null)
                    .module(CollectionUtils.isNotEmpty(modules) ? modules.get(0) : null)
                    .metrics(CollectionUtils.isNotEmpty(metrics) ? metrics.get(0) : null)
                    .registries(registries)
                    .protocols(protocols)
                    .configCenters(configs)
                    .metadataReports(metadatas)
                    .providers(providers)
                    .consumers(consumers)
                    .services(services)
                    .references(references)
                    .start();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private <T> List<T> getBeans(Class<T> clazz) {
        return BeanFactoryUtils.getBeans(applicationContext, new String[]{""}, clazz);
    }
}
