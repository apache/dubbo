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

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.extension.SpringExtensionInjector;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo spring initialization entry point
 */
public class DubboSpringInitializer {

    private static Map<BeanDefinitionRegistry, DubboSpringInitializationContext> contextMap = new ConcurrentHashMap<>();

    private DubboSpringInitializer() {
    }

    public static void initialize(BeanDefinitionRegistry registry) {

        if (contextMap.putIfAbsent(registry, new DubboSpringInitializationContext()) != null) {
            return;
        }

        // prepare context and do customize
        DubboSpringInitializationContext context = contextMap.get(registry);

        // find beanFactory and applicationContext
        ConfigurableListableBeanFactory beanFactory = findBeanFactory(registry);
        ApplicationContext applicationContext = findApplicationContext(registry, beanFactory);

        // init dubbo context
        initContext(context, registry, beanFactory, applicationContext);
    }


    private static void initContext(DubboSpringInitializationContext context, BeanDefinitionRegistry registry,
                                    ConfigurableListableBeanFactory beanFactory, ApplicationContext applicationContext) {
        context.setRegistry(registry);
        context.setApplicationContext(applicationContext);

        // customize context, you can change the bind module model via DubboSpringInitializationCustomizer SPI
        customize(context);

        // init ApplicationModel
        ApplicationModel applicationModel = context.getApplicationModel();
        if (applicationModel == null) {
            if (findContextForApplication(ApplicationModel.defaultModel()) == null) {
                // first spring context use default application instance
                applicationModel = ApplicationModel.defaultModel();
            } else {
                // create an new application instance for later spring context
                applicationModel = FrameworkModel.defaultModel().newApplication();
            }

            // init ModuleModel
            ModuleModel moduleModel = applicationModel.getDefaultModule();
            context.setModuleModel(moduleModel);
        }

        // Init SpringExtensionInjector
        // Maybe the applicationContext is null, that means the Spring ApplicationContext is not completely created, so can not retrieve bean from it.
        // We will reinitialize it again in DubboInfraBeanRegisterPostProcessor
        if (applicationContext != null) {
            SpringExtensionInjector.get(context.getApplicationModel()).init(applicationContext);
        }

        // create DubboBootstrap
        DubboBootstrap bootstrap = context.getDubboBootstrap();
        if (bootstrap == null) {
            if (applicationModel == ApplicationModel.defaultModel()) {
                bootstrap = DubboBootstrap.getInstance();
            } else {
                bootstrap = DubboBootstrap.newInstance(applicationModel);
            }
            context.setDubboBootstrap(bootstrap);
        }

        // bind dubbo initialization context to spring context
        registerContextBeans(beanFactory, context);

        // mark context as bound
        context.markAsBound();

        // register common beans
        DubboBeanUtils.registerCommonBeans(registry);
    }

    private static ConfigurableListableBeanFactory findBeanFactory(BeanDefinitionRegistry registry) {
        ConfigurableListableBeanFactory beanFactory = null;
        if (registry instanceof ConfigurableListableBeanFactory) {
            beanFactory = (ConfigurableListableBeanFactory) registry;
        } else if (registry instanceof GenericApplicationContext) {
            GenericApplicationContext genericApplicationContext = (GenericApplicationContext) registry;
            beanFactory = genericApplicationContext.getBeanFactory();
        } else {
            throw new IllegalStateException("Can not find Spring BeanFactory from registry: " + registry.getClass().getName());
        }
        return beanFactory;
    }

    private static ApplicationContext findApplicationContext(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
        // GenericApplicationContext
        if (registry instanceof ApplicationContext) {
            return (ApplicationContext) registry;
        }
        // find by ApplicationContextAware
        ApplicationContextAwareBean contextBean = new ApplicationContextAwareBean();
        beanFactory.initializeBean(contextBean, ApplicationContextAwareBean.class.getSimpleName());
        return contextBean.applicationContext;
    }

    private static void registerContextBeans(ConfigurableListableBeanFactory beanFactory, DubboSpringInitializationContext context) {
        // register singleton
        registerSingleton(beanFactory, context);
        registerSingleton(beanFactory, context.getApplicationModel());
        registerSingleton(beanFactory, context.getModuleModel());
        registerSingleton(beanFactory, context.getDubboBootstrap());
    }

    private static void registerSingleton(ConfigurableListableBeanFactory beanFactory, Object bean) {
        beanFactory.registerSingleton(bean.getClass().getName(), bean);
    }

    private static DubboSpringInitializationContext findContextForApplication(ApplicationModel applicationModel) {
        for (DubboSpringInitializationContext initializationContext : contextMap.values()) {
            if (initializationContext.getApplicationModel() == applicationModel) {
                return initializationContext;
            }
        }
        return null;
    }

    private static void customize(DubboSpringInitializationContext context) {

        // find initialization customizers
        Set<DubboSpringInitializationCustomizer> customizers = FrameworkModel.defaultModel()
            .getExtensionLoader(DubboSpringInitializationCustomizer.class)
            .getSupportedExtensionInstances();

        for (DubboSpringInitializationCustomizer customizer : customizers) {
            customizer.customize(context);
        }
    }

    static class ApplicationContextAwareBean implements ApplicationContextAware {

        private ApplicationContext applicationContext;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
        }
    }
}
