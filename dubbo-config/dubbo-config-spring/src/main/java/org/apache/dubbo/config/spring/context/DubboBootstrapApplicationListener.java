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
package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
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
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.ReferenceBeanManager;
import org.apache.dubbo.config.spring.ServiceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * The {@link ApplicationListener} for {@link DubboBootstrap}'s lifecycle when the {@link ContextRefreshedEvent}
 * and {@link ContextClosedEvent} raised
 *
 * @since 2.7.5
 */
public class DubboBootstrapApplicationListener extends OneTimeExecutionApplicationContextEventListener
        implements Ordered {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The bean name of {@link DubboBootstrapApplicationListener}
     *
     * @since 2.7.6
     */
    public static final String BEAN_NAME = "dubboBootstrapApplicationListener";

    private final DubboBootstrap dubboBootstrap;

    public DubboBootstrapApplicationListener() {
        this.dubboBootstrap = DubboBootstrap.getInstance();
    }

    @Override
    public void onApplicationContextEvent(ApplicationContextEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            onContextRefreshedEvent((ContextRefreshedEvent) event);
        } else if (event instanceof ContextClosedEvent) {
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        try {
            prepareDubboConfigBeans(event.getApplicationContext());
            prepareReferenceBeans(event.getApplicationContext());
            dubboBootstrap.start();
        } catch (Exception e) {
            logger.error("Dubbo start failed", e);
        }
    }

    private void prepareReferenceBeans(ApplicationContext applicationContext) throws Exception {
        ReferenceBeanManager referenceBeanManager = applicationContext.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
        referenceBeanManager.prepareReferenceBeans();
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        dubboBootstrap.stop();
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    /**
     * Initializes there Dubbo's Config Beans before @Reference bean autowiring
     */
    private void prepareDubboConfigBeans(ApplicationContext applicationContext) {
        //Make sure all these config beans are inited and registered to ConfigManager
        beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ConfigCenterBean.class);
        beansOfTypeIncludingAncestors(applicationContext, MetadataReportConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, MetricsConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, SslConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ServiceBean.class);
    }

}
