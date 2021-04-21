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
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 *
 * Post-processor Dubbo config bean initialization.
 *
 * NOTE: Dubbo config beans MUST be initialized after registering all BeanPostProcessors,
 * that is after the AbstractApplicationContext#registerBeanPostProcessors() method.
 */
public class DubboConfigInitializationPostProcessor implements BeanPostProcessor, BeanFactoryAware, Ordered {

    public static String BEAN_NAME = "dubboConfigInitializationPostProcessor";

    /**
     * This bean post processor should run before seata GlobalTransactionScanner(1024)
     */
    @Value("${dubbo.config-initialization-post-processor.order:1000}")
    private int order = 1000;

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private ConfigurableListableBeanFactory beanFactory;
    private ReferenceBeanManager referenceBeanManager;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (initialized.compareAndSet(false, true)) {
            try {
                prepareDubboConfigBeans(beanFactory);
                prepareReferenceBeans(beanFactory);
            } catch (Throwable e) {
                throw new FatalBeanException("Initialization dubbo config beans failed", e);
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
        referenceBeanManager = beanFactory.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
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

        //SHOULD NOT init service beans here, avoid conflicts with seata
        //beansOfTypeIncludingAncestors(beanFactory, ServiceBean.class);
    }

}
