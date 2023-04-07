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
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.context.AbstractConfigManager;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *
 * Dubbo config bean initializer.
 *
 * NOTE: Dubbo config beans MUST be initialized after registering all BeanPostProcessors,
 * that is after the AbstractApplicationContext#registerBeanPostProcessors() method.
 */
public class DubboConfigBeanInitializer implements BeanFactoryAware, InitializingBean {

    public static String BEAN_NAME = "dubboConfigBeanInitializer";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private ConfigurableListableBeanFactory beanFactory;
    private ReferenceBeanManager referenceBeanManager;

    @Autowired
    private ConfigManager configManager;

    @Autowired
    @Qualifier("org.apache.dubbo.rpc.model.ModuleModel")
    private ModuleModel moduleModel;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        if (initialized.compareAndSet(false, true)) {
            referenceBeanManager = beanFactory.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
            try {
                prepareDubboConfigBeans();
                referenceBeanManager.prepareReferenceBeans();
            } catch (Throwable e) {
                throw new FatalBeanException("Initialization dubbo config beans failed", e);
            }
        }
    }

    /**
     * Initializes there Dubbo's Config Beans before @Reference bean autowiring
     */
    private void prepareDubboConfigBeans() {
        logger.info("loading dubbo config beans ...");

        //Make sure all these config beans are initialed and registered to ConfigManager
        // load application config beans
        loadConfigBeansOfType(ApplicationConfig.class, configManager);
        loadConfigBeansOfType(RegistryConfig.class, configManager);
        loadConfigBeansOfType(ProtocolConfig.class, configManager);
        loadConfigBeansOfType(MonitorConfig.class, configManager);
        loadConfigBeansOfType(ConfigCenterBean.class, configManager);
        loadConfigBeansOfType(MetadataReportConfig.class, configManager);
        loadConfigBeansOfType(MetricsConfig.class, configManager);
        loadConfigBeansOfType(TracingConfig.class, configManager);
        loadConfigBeansOfType(SslConfig.class, configManager);

        // load module config beans
        loadConfigBeansOfType(ModuleConfig.class, moduleModel.getConfigManager());
        loadConfigBeansOfType(ProviderConfig.class, moduleModel.getConfigManager());
        loadConfigBeansOfType(ConsumerConfig.class, moduleModel.getConfigManager());

        // load ConfigCenterBean from properties, fix https://github.com/apache/dubbo/issues/9207
        List<ConfigCenterBean> configCenterBeans = configManager.loadConfigsOfTypeFromProps(ConfigCenterBean.class);
        for (ConfigCenterBean configCenterBean : configCenterBeans) {
            String beanName = configCenterBean.getId() != null ? configCenterBean.getId() : "configCenterBean";
            beanFactory.initializeBean(configCenterBean, beanName);
        }

        logger.info("dubbo config beans are loaded.");
    }

    private void loadConfigBeansOfType(Class<? extends AbstractConfig> configClass, AbstractConfigManager configManager) {
        String[] beanNames = beanFactory.getBeanNamesForType(configClass, true, false);
        for (String beanName : beanNames) {
            AbstractConfig configBean = beanFactory.getBean(beanName, configClass);
            // Register config bean here, avoid relying on unreliable @PostConstruct init method
            configManager.addConfig(configBean);
        }
    }

}
