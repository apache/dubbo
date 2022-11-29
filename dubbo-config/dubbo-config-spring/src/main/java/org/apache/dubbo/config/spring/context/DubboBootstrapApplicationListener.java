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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.bootstrap.BootstrapTakeoverMode;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.context.event.DubboConfigInitEvent;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_DUBBO_BEAN_INITIALIZER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_DUBBO_BEAN_NOT_FOUND;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

/**
 * The {@link ApplicationListener} for {@link DubboBootstrap}'s lifecycle when the {@link ContextRefreshedEvent}
 * and {@link ContextClosedEvent} raised
 *
 * @since 2.7.5
 */
@Deprecated
public class DubboBootstrapApplicationListener implements ApplicationListener, ApplicationContextAware, Ordered {

    /**
     * The bean name of {@link DubboBootstrapApplicationListener}
     *
     * @since 2.7.6
     */
    public static final String BEAN_NAME = "dubboBootstrapApplicationListener";

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private ApplicationContext applicationContext;
    private DubboBootstrap bootstrap;
    private boolean shouldInitConfigBeans;
    private ModuleModel moduleModel;

    public DubboBootstrapApplicationListener() {
    }

    public DubboBootstrapApplicationListener(boolean shouldInitConfigBeans) {
        // maybe register DubboBootstrapApplicationListener manual during spring context starting
        this.shouldInitConfigBeans = shouldInitConfigBeans;
    }

    private void setBootstrap(DubboBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        if (bootstrap.getTakeoverMode() != BootstrapTakeoverMode.MANUAL) {
            bootstrap.setTakeoverMode(BootstrapTakeoverMode.SPRING);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (isOriginalEventSource(event)) {
            if (event instanceof DubboConfigInitEvent) {
                // This event will be notified at AbstractApplicationContext.registerListeners(),
                // init dubbo config beans before spring singleton beans
                initDubboConfigBeans();
            } else if (event instanceof ApplicationContextEvent) {
                this.onApplicationContextEvent((ApplicationContextEvent) event);
            }
        }
    }

    private void initDubboConfigBeans() {
        // load DubboConfigBeanInitializer to init config beans
        if (applicationContext.containsBean(DubboConfigBeanInitializer.BEAN_NAME)) {
            applicationContext.getBean(DubboConfigBeanInitializer.BEAN_NAME, DubboConfigBeanInitializer.class);
        } else {
            logger.warn(CONFIG_DUBBO_BEAN_NOT_FOUND, "", "", "Bean '" + DubboConfigBeanInitializer.BEAN_NAME + "' was not found");
        }

        // All infrastructure config beans are loaded, initialize dubbo here
        moduleModel.getDeployer().initialize();
    }

    private void onApplicationContextEvent(ApplicationContextEvent event) {
        if (DubboBootstrapStartStopListenerSpringAdapter.applicationContext == null) {
            DubboBootstrapStartStopListenerSpringAdapter.applicationContext = event.getApplicationContext();
        }

        if (event instanceof ContextRefreshedEvent) {
            onContextRefreshedEvent((ContextRefreshedEvent) event);
        } else if (event instanceof ContextClosedEvent) {
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        if (bootstrap.getTakeoverMode() == BootstrapTakeoverMode.SPRING) {
            moduleModel.getDeployer().start();
        }
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        if (bootstrap.getTakeoverMode() == BootstrapTakeoverMode.SPRING) {
            // will call dubboBootstrap.stop() through shutdown callback.
            //bootstrap.getApplicationModel().getBeanFactory().getBean(DubboShutdownHook.class).run();
            moduleModel.getDeployer().stop();
        }
    }

    /**
     * Is original {@link ApplicationContext} as the event source
     *
     * @param event {@link ApplicationEvent}
     * @return if original, return <code>true</code>, or <code>false</code>
     */
    private boolean isOriginalEventSource(ApplicationEvent event) {

        boolean originalEventSource = nullSafeEquals(getApplicationContext(), event.getSource());
        return originalEventSource;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
        this.setBootstrap(DubboBootstrap.getInstance(moduleModel.getApplicationModel()));
        if (shouldInitConfigBeans) {
            checkCallStackAndInit();
        }
    }

    private void checkCallStackAndInit() {
        // check call stack whether contains org.springframework.context.support.AbstractApplicationContext.registerListeners()
        Exception exception = new Exception();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        boolean found = false;
        for (StackTraceElement frame : stackTrace) {
            if (frame.getMethodName().equals("registerListeners") && frame.getClassName().endsWith("AbstractApplicationContext")) {
                found = true;
                break;
            }
        }
        if (found) {
            // init config beans here, compatible with spring 3.x/4.1.x
            initDubboConfigBeans();
        } else {
            logger.warn(CONFIG_DUBBO_BEAN_INITIALIZER, "", "", "DubboBootstrapApplicationListener initialization is unexpected, " +
                "it should be created in AbstractApplicationContext.registerListeners() method", exception);
        }
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
