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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo spring initialization entry point
 */
public class DubboSpringInitializer {

    private static Map<BeanDefinitionRegistry, DubboSpringInitializationContext> contextMap = new ConcurrentHashMap<>();

    public synchronized static void initialize(BeanDefinitionRegistry registry) {

        if (contextMap.putIfAbsent(registry, new DubboSpringInitializationContext()) != null) {
            return;
        }

        // prepare context and do customize
        DubboSpringInitializationContext context = contextMap.get(registry);
        initContext(registry, context);
    }

    private static void initContext(BeanDefinitionRegistry registry, DubboSpringInitializationContext context) {
        context.setRegistry(registry);

        DubboBootstrap bootstrap;
        if (findContextForApplication(ApplicationModel.defaultModel()) == null) {
            // first spring context use default application instance
            bootstrap = DubboBootstrap.getInstance();
        } else {
            // create an new application instance for later spring context
            bootstrap = DubboBootstrap.newInstance();
        }
        ModuleModel defaultModule = bootstrap.getApplicationModel().getDefaultModule();
        context.setDubboBootstrap(bootstrap);
        context.setModuleModel(defaultModule);

        // customize context, you can change the bind module model via DubboSpringInitializationCustomizer SPI
        customize(context);

        // update module
        if (context.getModuleModel() != defaultModule) {

        }

        // bind dubbo initialization context to spring context
        bind(registry, context);

        // mark context as bound
        context.markAsBound();

        // register common beans
        DubboBeanUtils.registerCommonBeans(registry);
    }

    private static void bind(BeanDefinitionRegistry registry, DubboSpringInitializationContext context) {
        ConfigurableListableBeanFactory beanFactory = null;
        if (registry instanceof DefaultListableBeanFactory) {
            beanFactory = (DefaultListableBeanFactory) registry;
        } else if (registry instanceof GenericApplicationContext) {
            GenericApplicationContext genericApplicationContext = (GenericApplicationContext) registry;
            beanFactory = genericApplicationContext.getBeanFactory();
        } else {
            throw new IllegalStateException("Does not support binding to BeanDefinitionRegistry: " + registry.getClass().getName());
        }

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

    private DubboSpringInitializer() {
    }

    private static void customize(DubboSpringInitializationContext context) {

        // find initialization customizers
        Set<DubboSpringInitializationCustomizer> customizers = ExtensionLoader
            .getExtensionLoader(DubboSpringInitializationCustomizer.class)
            .getSupportedExtensionInstances();

        for (DubboSpringInitializationCustomizer customizer : customizers) {
            customizer.customize(context);
        }
    }
}
