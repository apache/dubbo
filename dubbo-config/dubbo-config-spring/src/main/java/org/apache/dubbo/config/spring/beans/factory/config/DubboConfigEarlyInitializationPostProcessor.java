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
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;

import javax.annotation.PostConstruct;

/**
 * Generally, {@link AbstractConfig Dubbo Config} Bean will be added into {@link ConfigManager} on the bean initialization
 * life cycle through {@link CommonAnnotationBeanPostProcessor} executing the callback of
 * {@link PostConstruct @PostConstruct}. However, the instantiation and initialization of
 * {@link AbstractConfig Dubbo Config} Bean could be too early before {@link CommonAnnotationBeanPostProcessor}, e.g,
 * execution, thus it's required to register the current instance as a {@link BeanPostProcessor} into
 * {@link DefaultListableBeanFactory the BeanFatory} using {@link BeanDefinitionRegistryPostProcessor} as early as
 * possible.
 *
 * @see GenericBeanPostProcessorAdapter
 * @since 2.7.9
 */
public class DubboConfigEarlyInitializationPostProcessor extends GenericBeanPostProcessorAdapter<AbstractConfig> {

    private static final Log logger = LogFactory.getLog(DubboConfigEarlyInitializationPostProcessor.class.getName());

    private static DubboConfigEarlyInitializationPostProcessor SINGLETON;

    private DefaultListableBeanFactory beanFactory;

    private DubboConfigEarlyInitializationPostProcessor(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public static DubboConfigEarlyInitializationPostProcessor getSingleton(DefaultListableBeanFactory beanFactory) {
        if (SINGLETON == null && beanFactory != null) {
            synchronized (DubboConfigEarlyInitializationPostProcessor.class) {
                if (SINGLETON == null && beanFactory != null) {
                    SINGLETON = new DubboConfigEarlyInitializationPostProcessor(beanFactory);
                }
            }
        }
        return SINGLETON;
    }

    protected void processBeforeInitialization(AbstractConfig config, String beanName) throws BeansException {

        if (this.beanFactory == null) {
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

    private DefaultListableBeanFactory unwrap(Object registry) {
        if (registry instanceof DefaultListableBeanFactory) {
            return (DefaultListableBeanFactory) registry;
        }
        return null;
    }

    /**
     * {@link DefaultListableBeanFactory} has registered {@link CommonAnnotationBeanPostProcessor} or not?
     *
     * @return if registered, return <code>true</code>, or <code>false</code>
     */
    private boolean hasRegisteredCommonAnnotationBeanPostProcessor() {
        for (BeanPostProcessor beanPostProcessor : beanFactory.getBeanPostProcessors()) {
            if (CommonAnnotationBeanPostProcessor.class.equals(beanPostProcessor.getClass())) {
                return true;
            }
        }
        return false;
    }

}
