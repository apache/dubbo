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
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo spring initialization entry point
 */
public class DubboSpringInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DubboSpringInitializer.class);

    private static final Map<BeanDefinitionRegistry, DubboSpringInitContext> contextMap = new ConcurrentHashMap<>();

    private DubboSpringInitializer() {
    }

    public static void initialize(BeanDefinitionRegistry registry) {

        // Spring ApplicationContext may not ready at this moment (e.g. load from xml), so use registry as key
        if (contextMap.putIfAbsent(registry, new DubboSpringInitContext()) != null) {
            return;
        }

        // prepare context and do customize
        DubboSpringInitContext context = contextMap.get(registry);

        // find beanFactory
        ConfigurableListableBeanFactory beanFactory = findBeanFactory(registry);

        // init dubbo context
        initContext(context, registry, beanFactory);
    }

    public static boolean remove(BeanDefinitionRegistry registry) {
        return contextMap.remove(registry) != null;
    }

    public static boolean remove(ApplicationContext springContext) {
        for (Map.Entry<BeanDefinitionRegistry, DubboSpringInitContext> entry : contextMap.entrySet()) {
            DubboSpringInitContext initContext = entry.getValue();
            if (initContext.getApplicationContext() == springContext ||
                initContext.getBeanFactory() == springContext.getAutowireCapableBeanFactory() ||
                initContext.getRegistry() == springContext.getAutowireCapableBeanFactory()
            ) {
                DubboSpringInitContext context = contextMap.remove(entry.getKey());
                logger.info("Unbind " + safeGetModelDesc(context.getModuleModel()) + " from spring container: " +
                    ObjectUtils.identityToString(entry.getKey()));
                return true;
            }
        }
        return false;
    }

    static Map<BeanDefinitionRegistry, DubboSpringInitContext> getContextMap() {
        return contextMap;
    }

    static DubboSpringInitContext findBySpringContext(ApplicationContext applicationContext) {
        for (Map.Entry<BeanDefinitionRegistry, DubboSpringInitContext> entry : contextMap.entrySet()) {
            DubboSpringInitContext initContext = entry.getValue();
            if (initContext.getApplicationContext() == applicationContext) {
                return initContext;
            }
        }
        return null;
    }

    private static void initContext(DubboSpringInitContext context, BeanDefinitionRegistry registry,
                                    ConfigurableListableBeanFactory beanFactory) {
        context.setRegistry(registry);
        context.setBeanFactory(beanFactory);

        // customize context, you can change the bind module model via DubboSpringInitCustomizer SPI
        customize(context);

        // init ModuleModel
        ModuleModel moduleModel = context.getModuleModel();
        if (moduleModel == null) {
            ApplicationModel applicationModel;
            if (findContextForApplication(ApplicationModel.defaultModel()) == null) {
                // first spring context use default application instance
                applicationModel = ApplicationModel.defaultModel();
                logger.info("Use default application: " + safeGetModelDesc(applicationModel));
            } else {
                // create a new application instance for later spring context
                applicationModel = FrameworkModel.defaultModel().newApplication();
                logger.info("Create new application: " + safeGetModelDesc(applicationModel));
            }

            // init ModuleModel
            moduleModel = applicationModel.getDefaultModule();
            context.setModuleModel(moduleModel);
            logger.info("Use default module model of target application: " + safeGetModelDesc(moduleModel));
        } else {
            logger.info("Use module model from customizer: " + safeGetModelDesc(moduleModel));
        }
        logger.info("Bind " + safeGetModelDesc(moduleModel) + " to spring container: " + ObjectUtils.identityToString(registry));

        // set module attributes
        if (context.getModuleAttributes().size() > 0) {
            context.getModuleModel().getAttributes().putAll(context.getModuleAttributes());
        }

        // bind dubbo initialization context to spring context
        registerContextBeans(beanFactory, context);

        // mark context as bound
        context.markAsBound();
        moduleModel.setLifeCycleManagedExternally(true);

        // register common beans
        DubboBeanUtils.registerCommonBeans(registry);
    }

    private static String safeGetModelDesc(ScopeModel scopeModel) {
        return scopeModel != null ? scopeModel.getDesc() : null;
    }

    private static ConfigurableListableBeanFactory findBeanFactory(BeanDefinitionRegistry registry) {
        ConfigurableListableBeanFactory beanFactory;
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

    private static void registerContextBeans(ConfigurableListableBeanFactory beanFactory, DubboSpringInitContext context) {
        // register singleton
        registerSingleton(beanFactory, context);
        registerSingleton(beanFactory, context.getApplicationModel());
        registerSingleton(beanFactory, context.getModuleModel());
    }

    private static void registerSingleton(ConfigurableListableBeanFactory beanFactory, Object bean) {
        beanFactory.registerSingleton(bean.getClass().getName(), bean);
    }

    private static DubboSpringInitContext findContextForApplication(ApplicationModel applicationModel) {
        for (DubboSpringInitContext initializationContext : contextMap.values()) {
            if (initializationContext.getApplicationModel() == applicationModel) {
                return initializationContext;
            }
        }
        return null;
    }

    private static void customize(DubboSpringInitContext context) {

        // find initialization customizers
        Set<DubboSpringInitCustomizer> customizers = FrameworkModel.defaultModel()
            .getExtensionLoader(DubboSpringInitCustomizer.class)
            .getSupportedExtensionInstances();
        for (DubboSpringInitCustomizer customizer : customizers) {
            customizer.customize(context);
        }

        // load customizers in thread local holder
        DubboSpringInitCustomizerHolder customizerHolder = DubboSpringInitCustomizerHolder.get();
        customizers = customizerHolder.getCustomizers();
        for (DubboSpringInitCustomizer customizer : customizers) {
            customizer.customize(context);
        }
        customizerHolder.clearCustomizers();

    }

}
