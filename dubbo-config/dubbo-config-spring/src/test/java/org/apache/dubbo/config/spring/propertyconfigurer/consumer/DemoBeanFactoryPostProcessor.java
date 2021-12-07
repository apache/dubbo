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
package org.apache.dubbo.config.spring.propertyconfigurer.consumer;

import org.apache.dubbo.config.spring.api.HelloService;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.PriorityOrdered;

public class DemoBeanFactoryPostProcessor implements BeanFactoryPostProcessor, PriorityOrdered {

    private HelloService demoService;

    public DemoBeanFactoryPostProcessor(HelloService demoService) {
        this.demoService = demoService;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (demoService == null) {
            throw new IllegalStateException("demoService is not injected");
        }
        System.out.println("DemoBeanFactoryPostProcessor");
    }

    /**
     * call before PropertyPlaceholderConfigurer
     */
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
