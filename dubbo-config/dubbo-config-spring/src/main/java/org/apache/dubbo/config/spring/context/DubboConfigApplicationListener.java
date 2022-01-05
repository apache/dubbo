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

import org.apache.dubbo.config.spring.context.event.DubboConfigInitEvent;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An ApplicationListener to load config beans
 */
public class DubboConfigApplicationListener implements ApplicationListener<DubboConfigInitEvent>, ApplicationContextAware {

    private final static Log logger = LogFactory.getLog(DubboConfigApplicationListener.class);

    private ApplicationContext applicationContext;

    private ModuleModel moduleModel;

    private AtomicBoolean initialized = new AtomicBoolean();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
    }

    @Override
    public void onApplicationEvent(DubboConfigInitEvent event) {
        // It's expected to be notify at org.springframework.context.support.AbstractApplicationContext.registerListeners(),
        // before loading non-lazy singleton beans. At this moment, all BeanFactoryPostProcessor have been processed,
        if (initialized.compareAndSet(false, true)) {
            initDubboConfigBeans();
        }
    }

    private void initDubboConfigBeans() {
        // load DubboConfigBeanInitializer to init config beans
        if (applicationContext.containsBean(DubboConfigBeanInitializer.BEAN_NAME)) {
            applicationContext.getBean(DubboConfigBeanInitializer.BEAN_NAME, DubboConfigBeanInitializer.class);
        } else {
            logger.warn("Bean '" + DubboConfigBeanInitializer.BEAN_NAME + "' was not found");
        }

        // All infrastructure config beans are loaded, initialize dubbo here
        moduleModel.getDeployer().prepare();
    }


}
