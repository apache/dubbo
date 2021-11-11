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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.config.spring.context.event.DubboConfigInitEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;

/**
 * Prior to loading of DubboConfigApplicationListener
 */
public class InitializeDubboConfigBeanInAdvanceApplicationListener implements ApplicationListener<DubboConfigInitEvent>,
    ApplicationContextAware, Ordered {

    private final static Log logger = LogFactory.getLog(InitializeDubboConfigBeanInAdvanceApplicationListener.class);

    private ApplicationContext applicationContext;

    private final AtomicBoolean initialized = new AtomicBoolean();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void onApplicationEvent(DubboConfigInitEvent event) {

        if (initialized.compareAndSet(false, true)) {
            initBeanForDubboConfigurationProperties();
        }
    }

    private void initBeanForDubboConfigurationProperties() {
        // load DubboConfigurationProperties to init config beans
        String beanName = DUBBO_PREFIX + "-" + DubboConfigurationProperties.class.getName();
        if (applicationContext.containsBean(beanName)) {
            applicationContext.getBean(beanName, DubboConfigurationProperties.class);
        } else {
            logger.warn("Bean '" + beanName + "' was not found");
        }

    }
}
