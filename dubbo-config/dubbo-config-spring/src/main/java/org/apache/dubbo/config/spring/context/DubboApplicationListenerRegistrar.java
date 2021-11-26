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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import static org.springframework.util.TypeUtils.isAssignable;

/**
 * Dubbo {@link ApplicationListener ApplicationListeners} Registrar
 *
 * @since 2.7.9
 */
public class DubboApplicationListenerRegistrar implements ApplicationContextAware {

    /**
     * The bean name of {@link DubboApplicationListenerRegistrar}
     */
    public static final String BEAN_NAME = "dubboApplicationListenerRegister";

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (!isAssignable(ConfigurableApplicationContext.class, applicationContext.getClass())) {
            throw new IllegalArgumentException("The argument of ApplicationContext must be ConfigurableApplicationContext");
        }
        addApplicationListeners((ConfigurableApplicationContext) applicationContext);
    }

    private void addApplicationListeners(ConfigurableApplicationContext context) {
        context.addApplicationListener(createDubboBootstrapApplicationListener(context));
        context.addApplicationListener(createDubboLifecycleComponentApplicationListener(context));
    }

    private ApplicationListener<?> createDubboBootstrapApplicationListener(ConfigurableApplicationContext context) {
        return new DubboBootstrapApplicationListener(context);
    }

    private ApplicationListener<?> createDubboLifecycleComponentApplicationListener(ConfigurableApplicationContext context) {
        return new DubboLifecycleComponentApplicationListener(context);
    }
}
