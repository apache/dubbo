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
package org.apache.dubbo.config.spring.beans.factory.config;

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.context.ConfigManager;

import com.alibaba.spring.beans.factory.config.GenericBeanPostProcessorAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.PriorityOrdered;

import javax.annotation.PostConstruct;

/**
 * Generally, {@link AbstractConfig Dubbo Config} Bean will be added into {@link ConfigManager} on the bean initialization
 * life cycle through {@link CommonAnnotationBeanPostProcessor} executing the callback of
 * {@link PostConstruct @PostConstruct}. However, the instantiation and initialization of
 * {@link AbstractConfig Dubbo Config} Bean could be too early before {@link CommonAnnotationBeanPostProcessor}, e.g,
 * execution, thus it's required to register the {@link DubboConfigEarlyInitializationPostProcessor
 * DubboConfigEarlyInitializationPostProcessor} instance as a {@link BeanPostProcessor} into
 * {@link DefaultListableBeanFactory the BeanFatory} using {@link BeanDefinitionRegistryPostProcessor} as early as
 * possible.
 *
 * @author <a href="mailto:842761733@qq.com">chenjh</a>
 * @see DubboConfigEarlyInitializationPostProcessor
 * @since 2.7.15
 */
public class DubboConfigEarlyRegistrationPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    public static final String BEAN_NAME = "dubboConfigEarlyRegistrationPostProcessor";

    private static final Log logger = LogFactory.getLog(DubboConfigEarlyRegistrationPostProcessor.class.getName());

    private DefaultListableBeanFactory beanFactory;

    private DubboConfigEarlyInitializationPostProcessor configEarlyInitializationPostProcessor =
            new DubboConfigEarlyInitializationPostProcessor();

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.beanFactory = unwrap(registry);
        registryConfigEarlyInitializationPostProcessor(beanFactory);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (this.beanFactory == null) { // try again if postProcessBeanDefinitionRegistry method does not effect.
            this.beanFactory = unwrap(beanFactory);
            registryConfigEarlyInitializationPostProcessor(this.beanFactory);
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    /**
     * Register DubboConfigEarlyInitializationPostProcessor as BeanPostProcessor manually
     * before {@link AbstractApplicationContext#registerBeanPostProcessors(ConfigurableListableBeanFactory)
     * Spring Framework automatically register}
     */
    private void registryConfigEarlyInitializationPostProcessor(DefaultListableBeanFactory beanFactory) {
        if (beanFactory != null) {
            // Register DubboConfigEarlyInitializationPostProcessor
            beanFactory.addBeanPostProcessor(configEarlyInitializationPostProcessor);
            if (logger.isInfoEnabled()) {
                logger.info("DubboConfigEarlyInitializationPostProcessor has bean registered");
            }
        }
    }

    private DefaultListableBeanFactory unwrap(Object registry) {
        if (registry instanceof DefaultListableBeanFactory) {
            return (DefaultListableBeanFactory) registry;
        }
        return null;
    }

    class DubboConfigEarlyInitializationPostProcessor extends GenericBeanPostProcessorAdapter<AbstractConfig> {

        private boolean registeredCommonAnnotationBeanPostProcessor = false;

        protected void processBeforeInitialization(AbstractConfig config, String beanName) throws BeansException {
            if (beanFactory == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("Current Processor is not running in Spring container, next action will be skipped!");
                }
                return;
            }

            // If CommonAnnotationBeanPostProcessor is already registered,  the method addIntoConfigManager()
            // will be invoked in Bean life cycle.
            if (!hasRegisteredCommonAnnotationBeanPostProcessor()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("CommonAnnotationBeanPostProcessor is not registered yet, " +
                            "the method addIntoConfigManager() will be invoked directly");
                }
                config.addIntoConfigManager();
            }
        }

        /**
         * {@link DefaultListableBeanFactory} has registered {@link CommonAnnotationBeanPostProcessor} or not?
         *
         * @return if registered, return <code>true</code>, or <code>false</code>
         */
        private boolean hasRegisteredCommonAnnotationBeanPostProcessor() {
            if (registeredCommonAnnotationBeanPostProcessor) {
                return true;
            }
            for (BeanPostProcessor beanPostProcessor : beanFactory.getBeanPostProcessors()) {
                if (CommonAnnotationBeanPostProcessor.class.equals(beanPostProcessor.getClass())) {
                    this.registeredCommonAnnotationBeanPostProcessor = true;
                    return true;
                }
            }
            return false;
        }

    }
}
