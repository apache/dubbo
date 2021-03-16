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
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * Post-processor Dubbo config initialization
 */
public class DubboConfigInitializationPostProcessor implements BeanFactoryPostProcessor {

    public static String BEAN_NAME = "dubboBeanFactoryPostProcessor";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        try {
            prepareDubboConfigBeans(beanFactory);
            prepareReferenceBeans(beanFactory);
        } catch (Throwable e) {
            throw new FatalBeanException("Initialization dubbo config beans failed", e);
        }

    }

    private void prepareReferenceBeans(ConfigurableListableBeanFactory beanFactory) throws Exception {
        ReferenceBeanManager referenceBeanManager = beanFactory.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
        referenceBeanManager.prepareReferenceBeans();
    }

    /**
     * Initializes there Dubbo's Config Beans before @Reference bean autowiring
     */
    private void prepareDubboConfigBeans(ConfigurableListableBeanFactory beanFactory) {
        //Make sure all these config beans are inited and registered to ConfigManager
        beansOfTypeIncludingAncestors(beanFactory, ApplicationConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, ModuleConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, RegistryConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, ProtocolConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, MonitorConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, ProviderConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, ConsumerConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, ConfigCenterBean.class);
        beansOfTypeIncludingAncestors(beanFactory, MetadataReportConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, MetricsConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, SslConfig.class);
        beansOfTypeIncludingAncestors(beanFactory, ServiceBean.class);
    }

}
